package lld.problems.splitwise;

import lld.common.Check;
import java.util.*;

record User(String id, String name) {}
record Split(User user, long paise) {}
record Expense(String id, User paidBy, long totalPaise, List<Split> splits) {}
record Payment(String from, String to, long paise) {}
enum SplitType { EQUAL, EXACT, PERCENT }

/** Strategy: how an expense is divided. Shares ALWAYS add up to the total. */
interface SplitStrategy {
    List<Split> split(long totalPaise, List<User> users, List<Long> values);
}

class EqualSplit implements SplitStrategy {
    public List<Split> split(long total, List<User> users, List<Long> ignored) {
        long base = total / users.size();
        long extra = total % users.size();               // leftover paise go to the first few people
        List<Split> out = new ArrayList<>();
        for (int i = 0; i < users.size(); i++) out.add(new Split(users.get(i), base + (i < extra ? 1 : 0)));
        return out;
    }
}

class ExactSplit implements SplitStrategy {
    public List<Split> split(long total, List<User> users, List<Long> amounts) {
        if (amounts == null || amounts.size() != users.size()) throw new IllegalArgumentException("one amount per user");
        if (amounts.stream().mapToLong(Long::longValue).sum() != total) throw new IllegalArgumentException("amounts must add up to the total");
        List<Split> out = new ArrayList<>();
        for (int i = 0; i < users.size(); i++) out.add(new Split(users.get(i), amounts.get(i)));
        return out;
    }
}

class PercentSplit implements SplitStrategy {
    public List<Split> split(long total, List<User> users, List<Long> percents) {
        if (percents == null || percents.size() != users.size()) throw new IllegalArgumentException("one percent per user");
        if (percents.stream().mapToLong(Long::longValue).sum() != 100) throw new IllegalArgumentException("percentages must add up to 100");
        long[] shares = new long[users.size()];
        long assigned = 0;
        for (int i = 0; i < shares.length; i++) { shares[i] = total * percents.get(i) / 100; assigned += shares[i]; }
        long leftover = total - assigned;                 // rounding remainder
        for (int i = 0; leftover > 0; i = (i + 1) % shares.length, leftover--) shares[i]++;
        List<Split> out = new ArrayList<>();
        for (int i = 0; i < shares.length; i++) out.add(new Split(users.get(i), shares[i]));
        return out;
    }
}

class BalanceSheet {
    // debtor id -> (creditor id -> paise owed)
    private final Map<String, Map<String, Long>> owes = new HashMap<>();

    synchronized void apply(Expense e) {
        for (Split s : e.splits()) {
            if (s.user().equals(e.paidBy())) continue;       // you don't owe yourself
            owes.computeIfAbsent(s.user().id(), k -> new HashMap<>()).merge(e.paidBy().id(), s.paise(), Long::sum);
        }
    }

    synchronized long owed(String debtor, String creditor) {
        return owes.getOrDefault(debtor, Map.of()).getOrDefault(creditor, 0L);
    }

    /** Net position per person: positive = is owed money, negative = owes money. */
    synchronized Map<String, Long> net() {
        Map<String, Long> net = new TreeMap<>();
        owes.forEach((debtor, m) -> m.forEach((creditor, amt) -> {
            net.merge(debtor, -amt, Long::sum);
            net.merge(creditor, amt, Long::sum);
        }));
        return net;
    }
}

class DebtSimplifier {
    private record Bal(String id, long paise) {}

    /** Greedy: biggest creditor is paid by biggest debtor. At most (n - 1) payments. */
    static List<Payment> simplify(Map<String, Long> net) {
        PriorityQueue<Bal> creditors = new PriorityQueue<>(Comparator.comparingLong(Bal::paise).reversed().thenComparing(Bal::id));
        PriorityQueue<Bal> debtors = new PriorityQueue<>(Comparator.comparingLong(Bal::paise).thenComparing(Bal::id)); // most negative first
        net.forEach((id, amt) -> {
            if (amt > 0) creditors.add(new Bal(id, amt));
            else if (amt < 0) debtors.add(new Bal(id, amt));
        });
        List<Payment> out = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Bal c = creditors.poll(), d = debtors.poll();
            long pay = Math.min(c.paise(), -d.paise());
            out.add(new Payment(d.id(), c.id(), pay));
            if (c.paise() > pay) creditors.add(new Bal(c.id(), c.paise() - pay));
            if (-d.paise() > pay) debtors.add(new Bal(d.id(), d.paise() + pay));
        }
        return out;
    }
}

class ExpenseService {
    private final Map<SplitType, SplitStrategy> strategies;
    private final BalanceSheet sheet;
    ExpenseService(Map<SplitType, SplitStrategy> strategies, BalanceSheet sheet) { this.strategies = strategies; this.sheet = sheet; }

    Expense addExpense(User paidBy, long total, List<User> users, SplitType type, List<Long> values) {
        List<Split> splits = strategies.get(type).split(total, users, values);
        Expense e = new Expense(UUID.randomUUID().toString(), paidBy, total, splits);
        sheet.apply(e);
        return e;
    }
}

public class Demo {
    static long sum(List<Split> s) { return s.stream().mapToLong(Split::paise).sum(); }

    public static void main(String[] args) {
        System.out.println("Splitwise: strategy per split type, paise not doubles, simplify debts");
        User a = new User("a", "Amit"), p = new User("p", "Priya"), r = new User("r", "Ravi");

        List<Split> equal = new EqualSplit().split(10000, List.of(a, p, r), null);
        Check.that(equal.stream().map(Split::paise).toList().equals(List.of(3334L, 3333L, 3333L)), "Rs100 / 3 = 3334 + 3333 + 3333 paise");
        Check.that(sum(equal) == 10000, "shares add up to the total exactly (no lost paisa)");

        List<Split> pct = new PercentSplit().split(10001, List.of(a, p, r), List.of(50L, 30L, 20L));
        Check.that(sum(pct) == 10001 && pct.get(0).paise() == 5001, "percent split keeps the rounding remainder");
        Check.throwsError(() -> new ExactSplit().split(100, List.of(a, p), List.of(60L, 30L)),
            IllegalArgumentException.class, "exact amounts must add up to the total");
        Check.throwsError(() -> new PercentSplit().split(100, List.of(a, p), List.of(60L, 30L)),
            IllegalArgumentException.class, "percentages must add up to 100");

        BalanceSheet sheet = new BalanceSheet();
        ExpenseService service = new ExpenseService(
            Map.of(SplitType.EQUAL, new EqualSplit(), SplitType.EXACT, new ExactSplit(), SplitType.PERCENT, new PercentSplit()), sheet);
        service.addExpense(p, 90000, List.of(p, a, r), SplitType.EQUAL, null);   // Priya pays Rs900 dinner
        Check.that(sheet.owed("a", "p") == 30000 && sheet.owed("r", "p") == 30000, "Amit and Ravi each owe Priya Rs300");
        Check.that(sheet.owed("p", "p") == 0, "nobody owes themselves");

        // The worked example from the post: A owes B 500, D owes B 200, B owes C 700
        Map<String, Long> net = new TreeMap<>(Map.of("A", -500L, "B", 0L, "C", 700L, "D", -200L));
        List<Payment> plan = DebtSimplifier.simplify(net);
        Check.that(plan.size() == 2, "three debts collapse into two payments");
        Check.that(plan.contains(new Payment("A", "C", 500)) && plan.contains(new Payment("D", "C", 200)), "A pays C 500, D pays C 200");
    }
}
