using Lld.Common;

namespace Lld.Solid.Lsp;

abstract class Account(double opening)
{
    protected double balance = opening;
    public double Balance => balance;
    public void Deposit(double amount) => balance += amount;
}

/// <summary>Only accounts that CAN withdraw promise it.</summary>
interface IWithdrawable { void Withdraw(double amount); }

class SavingsAccount(double opening) : Account(opening), IWithdrawable
{
    public void Withdraw(double amount)
    {
        if (amount > balance) throw new InvalidOperationException("insufficient funds");
        balance -= amount;
    }
}

/// <summary>Honest hierarchy: a fixed deposit is an Account but is NOT withdrawable.</summary>
class FixedDepositAccount(double opening) : Account(opening);

static class Bank
{
    public static void WithdrawFromAll(IEnumerable<IWithdrawable> accounts, double amount)
    {
        foreach (var w in accounts) w.Withdraw(amount);
    }
}

public static class Demo
{
    public static void Run()
    {
        Console.WriteLine("LSP: subtypes must keep the parent's promises");
        var a = new SavingsAccount(500);
        var b = new SavingsAccount(300);
        Bank.WithdrawFromAll(new[] { a, b }, 100);
        Check.Near(a.Balance, 400, "first savings account debited");
        Check.Near(b.Balance, 200, "second savings account debited");

        object fd = new FixedDepositAccount(10_000);
        Check.That(fd is not IWithdrawable, "fixed deposit never promised Withdraw()");
        Check.Throws<InvalidOperationException>(() => a.Withdraw(1_000_000), "overdraw is rejected consistently");
    }
}
