package lld.problems.atm;

import lld.common.Check;
import java.util.*;

/** The bank is an abstraction the ATM depends on (DIP). Real code would call a core-banking API. */
interface BankService {
    boolean verifyPin(String card, String pin);
    long balance(String card);
    boolean debit(String card, long amount);
}

class CannotDispenseException extends RuntimeException {
    CannotDispenseException(String m) { super(m); }
}

/** Chain of Responsibility: one handler per note. plan() does NOT change counts; commit() does. */
class NoteHandler {
    private final int note;
    private int available;
    private final NoteHandler next;
    NoteHandler(int note, int available, NoteHandler next) { this.note = note; this.available = available; this.next = next; }

    Map<Integer, Integer> plan(long amount, Map<Integer, Integer> out) {
        int use = (int) Math.min(available, amount / note);
        if (use > 0) out.put(note, use);
        long rest = amount - (long) use * note;
        if (rest == 0) return out;
        if (next == null) throw new CannotDispenseException("cannot make " + amount + " with the notes available");
        return next.plan(rest, out);
    }
    void commit(Map<Integer, Integer> plan) {
        available -= plan.getOrDefault(note, 0);
        if (next != null) next.commit(plan);
    }
    int available() { return available; }
}

class CashDispenser {
    private final NoteHandler head;
    CashDispenser(int n500, int n200, int n100) {
        head = new NoteHandler(500, n500, new NoteHandler(200, n200, new NoteHandler(100, n100, null)));
    }
    Map<Integer, Integer> plan(long amount) { return head.plan(amount, new TreeMap<>(Comparator.reverseOrder())); }
    void commit(Map<Integer, Integer> plan) { head.commit(plan); }
    int count500() { return head.available(); }
}

// ---------- ATM states: what each action means depends on where we are ----------
interface AtmState {
    default void insertCard(Atm a, String card) { throw new IllegalStateException("Cannot insert card now"); }
    default void enterPin(Atm a, String pin)    { throw new IllegalStateException("Insert a card first"); }
    default Map<Integer, Integer> withdraw(Atm a, long amount) { throw new IllegalStateException("Enter your PIN first"); }
    default void eject(Atm a) { }
}

class IdleState implements AtmState {
    public void insertCard(Atm a, String card) { a.card = card; a.pinAttempts = 0; a.state = new HasCardState(); }
}

class HasCardState implements AtmState {
    public void enterPin(Atm a, String pin) {
        if (a.bank.verifyPin(a.card, pin)) { a.state = new AuthenticatedState(); return; }
        if (++a.pinAttempts >= 3) { a.retainedCards.add(a.card); a.reset(); throw new IllegalStateException("Card retained after 3 wrong PINs"); }
        throw new IllegalArgumentException("Wrong PIN");
    }
    public void eject(Atm a) { a.reset(); }
}

class AuthenticatedState implements AtmState {
    public Map<Integer, Integer> withdraw(Atm a, long amount) {
        if (amount <= 0 || amount % 100 != 0) throw new IllegalArgumentException("Amount must be a multiple of 100");
        if (a.bank.balance(a.card) < amount) throw new IllegalStateException("Insufficient balance");
        Map<Integer, Integer> plan = a.dispenser.plan(amount);          // 1. can we physically pay it?
        if (!a.bank.debit(a.card, amount)) throw new IllegalStateException("Bank declined");   // 2. take the money
        a.dispenser.commit(plan);                                       // 3. hand out the notes
        return plan;
    }
    public void eject(Atm a) { a.reset(); }
}

class Atm {
    AtmState state = new IdleState();
    String card;
    int pinAttempts;
    final BankService bank;
    final CashDispenser dispenser;
    final List<String> retainedCards = new ArrayList<>();

    Atm(BankService bank, CashDispenser dispenser) { this.bank = bank; this.dispenser = dispenser; }
    void insertCard(String card) { state.insertCard(this, card); }
    void enterPin(String pin) { state.enterPin(this, pin); }
    Map<Integer, Integer> withdraw(long amount) { return state.withdraw(this, amount); }
    void eject() { state.eject(this); }
    void reset() { card = null; pinAttempts = 0; state = new IdleState(); }
    String stateName() { return state.getClass().getSimpleName(); }
}

class FakeBank implements BankService {
    final Map<String, Long> balances = new HashMap<>(Map.of("card-1", 5000L));
    public boolean verifyPin(String card, String pin) { return pin.equals("1234"); }
    public long balance(String card) { return balances.get(card); }
    public boolean debit(String card, long amount) {
        if (balances.get(card) < amount) return false;
        balances.merge(card, -amount, Long::sum);
        return true;
    }
}

public class Demo {
    public static void main(String[] args) {
        System.out.println("ATM: State for the session, Chain of Responsibility for the notes");
        FakeBank bank = new FakeBank();
        Atm atm = new Atm(bank, new CashDispenser(10, 10, 10));

        Check.throwsError(() -> atm.withdraw(500), IllegalStateException.class, "cannot withdraw before inserting a card");
        atm.insertCard("card-1");
        Check.throwsError(() -> atm.withdraw(500), IllegalStateException.class, "cannot withdraw before entering the PIN");
        Check.throwsError(() -> atm.enterPin("0000"), IllegalArgumentException.class, "wrong PIN is rejected");
        atm.enterPin("1234");
        Check.that(atm.stateName().equals("AuthenticatedState"), "correct PIN moves to Authenticated");

        Map<Integer, Integer> notes = atm.withdraw(1300);
        Check.that(notes.equals(Map.of(500, 2, 200, 1, 100, 1)), "1300 = 2 x 500 + 1 x 200 + 1 x 100");
        Check.that(bank.balance("card-1") == 3700, "account debited by 1300");
        Check.throwsError(() -> atm.withdraw(150), IllegalArgumentException.class, "amount must be a multiple of 100");
        Check.throwsError(() -> atm.withdraw(9000), IllegalStateException.class, "cannot withdraw more than the balance");
        atm.eject();
        Check.that(atm.stateName().equals("IdleState"), "eject returns to Idle");

        // Three wrong PINs: the card is retained.
        atm.insertCard("card-1");
        for (int i = 0; i < 2; i++) { try { atm.enterPin("9999"); } catch (IllegalArgumentException ignored) { } }
        Check.throwsError(() -> atm.enterPin("9999"), IllegalStateException.class, "third wrong PIN retains the card");
        Check.that(atm.retainedCards.equals(List.of("card-1")) && atm.stateName().equals("IdleState"), "card kept, machine back to Idle");

        // Known limitation of the greedy chain: it never backtracks.
        CashDispenser tricky = new CashDispenser(1, 3, 0);
        Check.throwsError(() -> tricky.plan(600), CannotDispenseException.class,
            "greedy chain fails 600 from {500x1, 200x3} even though 3 x 200 works (a DP/backtracking search would fix it)");

        // A failed plan must not consume notes.
        Atm lowCash = new Atm(new FakeBank(), new CashDispenser(1, 0, 0));
        lowCash.insertCard("card-1"); lowCash.enterPin("1234");
        Check.throwsError(() -> lowCash.withdraw(1000), CannotDispenseException.class, "machine cannot pay 1000 with one 500 note");
        Check.that(lowCash.dispenser.count500() == 1 && lowCash.bank.balance("card-1") == 5000, "failed withdrawal changed neither the notes nor the balance");
    }
}
