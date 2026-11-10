using Lld.Common;

namespace Lld.Problems.Atm;

/// <summary>The bank is an abstraction the ATM depends on (DIP). Real code would call a core-banking API.</summary>
interface IBankService
{
    bool VerifyPin(string card, string pin);
    long Balance(string card);
    bool Debit(string card, long amount);
}

class CannotDispenseException(string message) : Exception(message);

/// <summary>Chain of Responsibility: one handler per note. Plan() does NOT change counts; Commit() does.</summary>
class NoteHandler(int note, int available, NoteHandler? next)
{
    public int Available { get; private set; } = available;

    public SortedDictionary<int, int> Plan(long amount, SortedDictionary<int, int> output)
    {
        var use = (int)Math.Min(Available, amount / note);
        if (use > 0) output[note] = use;
        var rest = amount - (long)use * note;
        if (rest == 0) return output;
        if (next is null) throw new CannotDispenseException($"cannot make {amount} with the notes available");
        return next.Plan(rest, output);
    }

    public void Commit(IReadOnlyDictionary<int, int> plan)
    {
        Available -= plan.GetValueOrDefault(note);
        next?.Commit(plan);
    }
}

class CashDispenser(int n500, int n200, int n100)
{
    private readonly NoteHandler _head = new(500, n500, new NoteHandler(200, n200, new NoteHandler(100, n100, null)));
    public SortedDictionary<int, int> Plan(long amount) => _head.Plan(amount, new SortedDictionary<int, int>());
    public void Commit(IReadOnlyDictionary<int, int> plan) => _head.Commit(plan);
    public int Count500 => _head.Available;
}

// ---------- ATM states: what each action means depends on where we are ----------
interface IAtmState
{
    void InsertCard(Atm a, string card) => throw new InvalidOperationException("Cannot insert card now");
    void EnterPin(Atm a, string pin) => throw new InvalidOperationException("Insert a card first");
    SortedDictionary<int, int> Withdraw(Atm a, long amount) => throw new InvalidOperationException("Enter your PIN first");
    void Eject(Atm a) { }
}

class IdleState : IAtmState
{
    public void InsertCard(Atm a, string card) { a.Card = card; a.PinAttempts = 0; a.State = new HasCardState(); }
}

class HasCardState : IAtmState
{
    public void EnterPin(Atm a, string pin)
    {
        if (a.Bank.VerifyPin(a.Card!, pin)) { a.State = new AuthenticatedState(); return; }
        if (++a.PinAttempts >= 3) { a.RetainedCards.Add(a.Card!); a.Reset(); throw new InvalidOperationException("Card retained after 3 wrong PINs"); }
        throw new ArgumentException("Wrong PIN");
    }
    public void Eject(Atm a) => a.Reset();
}

class AuthenticatedState : IAtmState
{
    public SortedDictionary<int, int> Withdraw(Atm a, long amount)
    {
        if (amount <= 0 || amount % 100 != 0) throw new ArgumentException("Amount must be a multiple of 100");
        if (a.Bank.Balance(a.Card!) < amount) throw new InvalidOperationException("Insufficient balance");
        var plan = a.Dispenser.Plan(amount);                                 // 1. can we physically pay it?
        if (!a.Bank.Debit(a.Card!, amount)) throw new InvalidOperationException("Bank declined");   // 2. take the money
        a.Dispenser.Commit(plan);                                            // 3. hand out the notes
        return plan;
    }
    public void Eject(Atm a) => a.Reset();
}

class Atm(IBankService bank, CashDispenser dispenser)
{
    public IAtmState State { get; set; } = new IdleState();
    public string? Card { get; set; }
    public int PinAttempts { get; set; }
    public IBankService Bank { get; } = bank;
    public CashDispenser Dispenser { get; } = dispenser;
    public List<string> RetainedCards { get; } = new();

    public void InsertCard(string card) => State.InsertCard(this, card);
    public void EnterPin(string pin) => State.EnterPin(this, pin);
    public SortedDictionary<int, int> Withdraw(long amount) => State.Withdraw(this, amount);
    public void Eject() => State.Eject(this);
    public void Reset() { Card = null; PinAttempts = 0; State = new IdleState(); }
    public string StateName => State.GetType().Name;
}

class FakeBank : IBankService
{
    public Dictionary<string, long> Balances { get; } = new() { ["card-1"] = 5000 };
    public bool VerifyPin(string card, string pin) => pin == "1234";
    public long Balance(string card) => Balances[card];
    public bool Debit(string card, long amount)
    {
        if (Balances[card] < amount) return false;
        Balances[card] -= amount;
        return true;
    }
}

public static class Demo
{
    public static void Run()
    {
        Console.WriteLine("ATM: State for the session, Chain of Responsibility for the notes");
        var bank = new FakeBank();
        var atm = new Atm(bank, new CashDispenser(10, 10, 10));

        Check.Throws<InvalidOperationException>(() => atm.Withdraw(500), "cannot withdraw before inserting a card");
        atm.InsertCard("card-1");
        Check.Throws<InvalidOperationException>(() => atm.Withdraw(500), "cannot withdraw before entering the PIN");
        Check.Throws<ArgumentException>(() => atm.EnterPin("0000"), "wrong PIN is rejected");
        atm.EnterPin("1234");
        Check.That(atm.StateName == "AuthenticatedState", "correct PIN moves to Authenticated");

        var notes = atm.Withdraw(1300);
        Check.That(notes.Count == 3 && notes[500] == 2 && notes[200] == 1 && notes[100] == 1, "1300 = 2 x 500 + 1 x 200 + 1 x 100");
        Check.That(bank.Balance("card-1") == 3700, "account debited by 1300");
        Check.Throws<ArgumentException>(() => atm.Withdraw(150), "amount must be a multiple of 100");
        Check.Throws<InvalidOperationException>(() => atm.Withdraw(9000), "cannot withdraw more than the balance");
        atm.Eject();
        Check.That(atm.StateName == "IdleState", "eject returns to Idle");

        atm.InsertCard("card-1");
        for (var i = 0; i < 2; i++) { try { atm.EnterPin("9999"); } catch (ArgumentException) { } }
        Check.Throws<InvalidOperationException>(() => atm.EnterPin("9999"), "third wrong PIN retains the card");
        Check.That(atm.RetainedCards.SequenceEqual(new[] { "card-1" }) && atm.StateName == "IdleState", "card kept, machine back to Idle");

        var tricky = new CashDispenser(1, 3, 0);
        Check.Throws<CannotDispenseException>(() => tricky.Plan(600), "greedy chain fails 600 from {500x1, 200x3} even though 3 x 200 works (a DP/backtracking search would fix it)");

        var lowCash = new Atm(new FakeBank(), new CashDispenser(1, 0, 0));
        lowCash.InsertCard("card-1"); lowCash.EnterPin("1234");
        Check.Throws<CannotDispenseException>(() => lowCash.Withdraw(1000), "machine cannot pay 1000 with one 500 note");
        Check.That(lowCash.Dispenser.Count500 == 1 && lowCash.Bank.Balance("card-1") == 5000, "failed withdrawal changed neither the notes nor the balance");
    }
}
