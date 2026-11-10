import { check, near, throws } from "../common.ts";

abstract class Account {
  protected balance: number;
  constructor(opening: number) { this.balance = opening; }
  getBalance(): number { return this.balance; }
  deposit(amount: number): void { this.balance += amount; }
}

/** Only accounts that CAN withdraw promise it. */
interface Withdrawable { withdraw(amount: number): void }

class SavingsAccount extends Account implements Withdrawable {
  withdraw(amount: number): void {
    if (amount > this.balance) throw new Error("insufficient funds");
    this.balance -= amount;
  }
}

/** Honest hierarchy: an Account, but it does NOT offer withdraw(). */
class FixedDepositAccount extends Account {}

function withdrawFromAll(accounts: Withdrawable[], amount: number): void {
  for (const a of accounts) a.withdraw(amount);
}

export function demo(): void {
  console.log("LSP: subtypes must keep the parent's promises");
  const a = new SavingsAccount(500), b = new SavingsAccount(300);
  withdrawFromAll([a, b], 100);
  near(a.getBalance(), 400, "first savings account debited");
  near(b.getBalance(), 200, "second savings account debited");

  const fd = new FixedDepositAccount(10_000);
  // withdrawFromAll([fd], 1) would not even COMPILE: the type system enforces the promise.
  check(!("withdraw" in fd), "fixed deposit never promised withdraw()");
  throws(() => a.withdraw(1_000_000), "overdraw is rejected consistently");
}
