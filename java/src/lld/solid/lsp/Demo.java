package lld.solid.lsp;

import lld.common.Check;
import java.util.List;

abstract class Account {
    protected double balance;
    Account(double opening) { this.balance = opening; }
    double balance() { return balance; }
    void deposit(double amount) { balance += amount; }
}

/** Only accounts that CAN withdraw promise it. */
interface Withdrawable { void withdraw(double amount); }

class SavingsAccount extends Account implements Withdrawable {
    SavingsAccount(double opening) { super(opening); }
    public void withdraw(double amount) {
        if (amount > balance) throw new IllegalStateException("insufficient funds");
        balance -= amount;
    }
}

/** Honest hierarchy: a fixed deposit is an Account but is NOT Withdrawable. */
class FixedDepositAccount extends Account {
    FixedDepositAccount(double opening) { super(opening); }
}

class Bank {
    /** Works for every Withdrawable: no instanceof checks, no surprises. */
    static void withdrawFromAll(List<? extends Withdrawable> accounts, double amount) {
        for (Withdrawable w : accounts) w.withdraw(amount);
    }
}

public class Demo {
    public static void main(String[] args) {
        System.out.println("LSP: subtypes must keep the parent's promises");
        SavingsAccount a = new SavingsAccount(500), b = new SavingsAccount(300);
        Bank.withdrawFromAll(List.of(a, b), 100);
        Check.near(a.balance(), 400, "first savings account debited");
        Check.near(b.balance(), 200, "second savings account debited");

        FixedDepositAccount fd = new FixedDepositAccount(10_000);
        Object asObject = fd;
        Check.that(!(asObject instanceof Withdrawable), "fixed deposit never promised withdraw()");
        Check.throwsError(() -> a.withdraw(1_000_000), IllegalStateException.class,
            "overdraw is rejected consistently");
    }
}
