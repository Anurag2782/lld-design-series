using Lld.Common;

namespace Lld.Patterns.State;

// ---------- Order lifecycle: each state knows what is allowed FROM it ----------
interface IOrderState
{
    IOrderState Pay() => throw Bad("pay");
    IOrderState Ship() => throw Bad("ship");
    IOrderState Cancel() => throw Bad("cancel");
    private InvalidOperationException Bad(string action) => new($"Cannot {action} when {GetType().Name}");
}
class Created : IOrderState
{
    public IOrderState Pay() => new Paid();
    public IOrderState Cancel() => new Cancelled();
}
class Paid : IOrderState
{
    public IOrderState Ship() => new Shipped();
    public IOrderState Cancel() => new Cancelled();   // refund would happen here
}
class Shipped : IOrderState { }
class Cancelled : IOrderState { }

class Order
{
    private IOrderState _state = new Created();
    public void Pay() => _state = _state.Pay();
    public void Ship() => _state = _state.Ship();
    public void Cancel() => _state = _state.Cancel();
    public string Status => _state.GetType().Name;
}

// ---------- Vending machine ----------
interface IVendingState
{
    void Insert(VendingMachine m, int coin);
    void Select(VendingMachine m, string item);
}

class VendingMachine
{
    private IVendingState _state = new Idle();
    public int Balance { get; private set; }
    public Dictionary<string, int> Stock { get; } = new();
    public Dictionary<string, int> Prices { get; } = new();
    public List<string> Dispensed { get; } = new();
    public int Returned { get; private set; }

    public void Insert(int coin) => _state.Insert(this, coin);
    public void Select(string item) => _state.Select(this, item);

    public void SetState(IVendingState s) => _state = s;
    public string StateName => _state.GetType().Name;
    public void AddBalance(int coin) => Balance += coin;
    public void Dispense(string item)
    {
        Stock[item]--;
        Dispensed.Add(item);
        Returned += Balance - Prices[item];   // change
        Balance = 0;
    }
    public void RefundAll() { Returned += Balance; Balance = 0; }
}

class Idle : IVendingState
{
    public void Insert(VendingMachine m, int coin) { m.AddBalance(coin); m.SetState(new HasMoney()); }
    public void Select(VendingMachine m, string item) { /* insert money first: ignored */ }
}

class HasMoney : IVendingState
{
    public void Insert(VendingMachine m, int coin) => m.AddBalance(coin);
    public void Select(VendingMachine m, string item)
    {
        if (m.Stock.GetValueOrDefault(item) == 0) { m.RefundAll(); m.SetState(new Idle()); return; }
        if (m.Balance < m.Prices[item]) return;        // need more money, stay in HasMoney
        m.Dispense(item);
        m.SetState(new Idle());
    }
}

public static class Demo
{
    public static void Run()
    {
        Console.WriteLine("State: behavior changes with lifecycle, no if (status == ...) chains");
        var o = new Order();
        Check.That(o.Status == "Created", "starts Created");
        o.Pay(); o.Ship();
        Check.That(o.Status == "Shipped", "Created -> Paid -> Shipped");
        Check.Throws<InvalidOperationException>(o.Cancel, "a shipped order cannot be cancelled");
        var fresh = new Order();
        Check.Throws<InvalidOperationException>(fresh.Ship, "cannot ship before paying");

        var m = new VendingMachine();
        m.Stock["chips"] = 1; m.Prices["chips"] = 30;
        m.Select("chips");
        Check.That(m.StateName == "Idle" && m.Dispensed.Count == 0, "select with no money does nothing");
        m.Insert(20);
        m.Select("chips");
        Check.That(m.StateName == "HasMoney" && m.Dispensed.Count == 0, "not enough money: stays in HasMoney");
        m.Insert(30);
        m.Select("chips");
        Check.That(m.Dispensed.SequenceEqual(new[] { "chips" }) && m.Returned == 20, "dispensed and returned 20 change");
        Check.That(m.StateName == "Idle", "back to Idle after dispensing");
        m.Insert(50);
        m.Select("chips");   // out of stock now
        Check.That(m.Returned == 70 && m.StateName == "Idle", "out of stock: full refund, back to Idle");
    }
}
