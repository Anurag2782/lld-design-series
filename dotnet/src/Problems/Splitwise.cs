using Lld.Common;

namespace Lld.Problems.Splitwise;

record User(string Id, string Name);
record Split(User User, long Paise);
record Expense(string Id, User PaidBy, long TotalPaise, List<Split> Splits);
record Payment(string From, string To, long Paise);
enum SplitType { Equal, Exact, Percent }

/// <summary>Strategy: how an expense is divided. Shares ALWAYS add up to the total.</summary>
interface ISplitStrategy
{
    List<Split> Split(long totalPaise, List<User> users, List<long>? values);
}

class EqualSplit : ISplitStrategy
{
    public List<Split> Split(long total, List<User> users, List<long>? ignored)
    {
        var baseShare = total / users.Count;
        var extra = total % users.Count;                 // leftover paise go to the first few people
        return users.Select((u, i) => new Split(u, baseShare + (i < extra ? 1 : 0))).ToList();
    }
}

class ExactSplit : ISplitStrategy
{
    public List<Split> Split(long total, List<User> users, List<long>? amounts)
    {
        if (amounts is null || amounts.Count != users.Count) throw new ArgumentException("one amount per user");
        if (amounts.Sum() != total) throw new ArgumentException("amounts must add up to the total");
        return users.Select((u, i) => new Split(u, amounts[i])).ToList();
    }
}

class PercentSplit : ISplitStrategy
{
    public List<Split> Split(long total, List<User> users, List<long>? percents)
    {
        if (percents is null || percents.Count != users.Count) throw new ArgumentException("one percent per user");
        if (percents.Sum() != 100) throw new ArgumentException("percentages must add up to 100");
        var shares = percents.Select(p => total * p / 100).ToArray();
        var leftover = total - shares.Sum();             // rounding remainder
        for (var i = 0; leftover > 0; i = (i + 1) % shares.Length, leftover--) shares[i]++;
        return users.Select((u, i) => new Split(u, shares[i])).ToList();
    }
}

class BalanceSheet
{
    private readonly object _lock = new();
    // debtor id -> (creditor id -> paise owed)
    private readonly Dictionary<string, Dictionary<string, long>> _owes = new();

    public void Apply(Expense e)
    {
        lock (_lock)
        {
            foreach (var s in e.Splits)
            {
                if (s.User == e.PaidBy) continue;        // you don't owe yourself
                if (!_owes.TryGetValue(s.User.Id, out var m)) _owes[s.User.Id] = m = new();
                m[e.PaidBy.Id] = m.GetValueOrDefault(e.PaidBy.Id) + s.Paise;
            }
        }
    }

    public long Owed(string debtor, string creditor)
    {
        lock (_lock) return _owes.TryGetValue(debtor, out var m) ? m.GetValueOrDefault(creditor) : 0;
    }

    /// <summary>Net position per person: positive = is owed money, negative = owes money.</summary>
    public SortedDictionary<string, long> Net()
    {
        lock (_lock)
        {
            var net = new SortedDictionary<string, long>();
            foreach (var (debtor, m) in _owes)
                foreach (var (creditor, amt) in m)
                {
                    net[debtor] = net.GetValueOrDefault(debtor) - amt;
                    net[creditor] = net.GetValueOrDefault(creditor) + amt;
                }
            return net;
        }
    }
}

static class DebtSimplifier
{
    /// <summary>Greedy: biggest creditor is paid by biggest debtor. At most (n - 1) payments.</summary>
    public static List<Payment> Simplify(IDictionary<string, long> net)
    {
        var creditors = new PriorityQueue<(string Id, long Paise), (long, string)>();
        var debtors = new PriorityQueue<(string Id, long Paise), (long, string)>();
        foreach (var (id, amt) in net)
        {
            if (amt > 0) creditors.Enqueue((id, amt), (-amt, id));        // biggest credit first
            else if (amt < 0) debtors.Enqueue((id, amt), (amt, id));      // most negative first
        }
        var result = new List<Payment>();
        while (creditors.Count > 0 && debtors.Count > 0)
        {
            var c = creditors.Dequeue();
            var d = debtors.Dequeue();
            var pay = Math.Min(c.Paise, -d.Paise);
            result.Add(new Payment(d.Id, c.Id, pay));
            if (c.Paise > pay) creditors.Enqueue((c.Id, c.Paise - pay), (-(c.Paise - pay), c.Id));
            if (-d.Paise > pay) debtors.Enqueue((d.Id, d.Paise + pay), (d.Paise + pay, d.Id));
        }
        return result;
    }
}

class ExpenseService(IReadOnlyDictionary<SplitType, ISplitStrategy> strategies, BalanceSheet sheet)
{
    public Expense AddExpense(User paidBy, long total, List<User> users, SplitType type, List<long>? values)
    {
        var splits = strategies[type].Split(total, users, values);
        var e = new Expense(Guid.NewGuid().ToString(), paidBy, total, splits);
        sheet.Apply(e);
        return e;
    }
}

public static class Demo
{
    public static void Run()
    {
        Console.WriteLine("Splitwise: strategy per split type, paise not doubles, simplify debts");
        User a = new("a", "Amit"), p = new("p", "Priya"), r = new("r", "Ravi");

        var equal = new EqualSplit().Split(10000, new() { a, p, r }, null);
        Check.That(equal.Select(s => s.Paise).SequenceEqual(new long[] { 3334, 3333, 3333 }), "Rs100 / 3 = 3334 + 3333 + 3333 paise");
        Check.That(equal.Sum(s => s.Paise) == 10000, "shares add up to the total exactly (no lost paisa)");

        var pct = new PercentSplit().Split(10001, new() { a, p, r }, new() { 50, 30, 20 });
        Check.That(pct.Sum(s => s.Paise) == 10001 && pct[0].Paise == 5001, "percent split keeps the rounding remainder");
        Check.Throws<ArgumentException>(() => new ExactSplit().Split(100, new() { a, p }, new() { 60, 30 }), "exact amounts must add up to the total");
        Check.Throws<ArgumentException>(() => new PercentSplit().Split(100, new() { a, p }, new() { 60, 30 }), "percentages must add up to 100");

        var sheet = new BalanceSheet();
        var service = new ExpenseService(new Dictionary<SplitType, ISplitStrategy>
        {
            [SplitType.Equal] = new EqualSplit(), [SplitType.Exact] = new ExactSplit(), [SplitType.Percent] = new PercentSplit()
        }, sheet);
        service.AddExpense(p, 90000, new() { p, a, r }, SplitType.Equal, null);   // Priya pays Rs900 dinner
        Check.That(sheet.Owed("a", "p") == 30000 && sheet.Owed("r", "p") == 30000, "Amit and Ravi each owe Priya Rs300");
        Check.That(sheet.Owed("p", "p") == 0, "nobody owes themselves");

        // The worked example from the post: A owes B 500, D owes B 200, B owes C 700
        var net = new SortedDictionary<string, long> { ["A"] = -500, ["B"] = 0, ["C"] = 700, ["D"] = -200 };
        var plan = DebtSimplifier.Simplify(net);
        Check.That(plan.Count == 2, "three debts collapse into two payments");
        Check.That(plan.Contains(new Payment("A", "C", 500)) && plan.Contains(new Payment("D", "C", 200)), "A pays C 500, D pays C 200");
    }
}
