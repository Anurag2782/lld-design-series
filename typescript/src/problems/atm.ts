import { check, throws } from "../common.ts";

/** The bank is an abstraction the ATM depends on (DIP). Real code would call a core-banking API. */
interface BankService {
  verifyPin(card: string, pin: string): boolean;
  balance(card: string): number;
  debit(card: string, amount: number): boolean;
}

class CannotDispenseError extends Error {}

/** Chain of Responsibility: one handler per note. plan() does NOT change counts; commit() does. */
class NoteHandler {
  private note: number;
  available: number;
  private next: NoteHandler | null;
  constructor(note: number, available: number, next: NoteHandler | null) { this.note = note; this.available = available; this.next = next; }

  plan(amount: number, out: Record<number, number>): Record<number, number> {
    const use = Math.min(this.available, Math.floor(amount / this.note));
    if (use > 0) out[this.note] = use;
    const rest = amount - use * this.note;
    if (rest === 0) return out;
    if (!this.next) throw new CannotDispenseError(`cannot make ${amount} with the notes available`);
    return this.next.plan(rest, out);
  }
  commit(plan: Record<number, number>): void {
    this.available -= plan[this.note] ?? 0;
    this.next?.commit(plan);
  }
}

class CashDispenser {
  private head: NoteHandler;
  constructor(n500: number, n200: number, n100: number) {
    this.head = new NoteHandler(500, n500, new NoteHandler(200, n200, new NoteHandler(100, n100, null)));
  }
  plan(amount: number) { return this.head.plan(amount, {}); }
  commit(plan: Record<number, number>) { this.head.commit(plan); }
  get count500() { return this.head.available; }
}

// ---------- ATM states: what each action means depends on where we are ----------
class AtmState {
  insertCard(_a: Atm, _card: string): void { throw new Error("Cannot insert card now"); }
  enterPin(_a: Atm, _pin: string): void { throw new Error("Insert a card first"); }
  withdraw(_a: Atm, _amount: number): Record<number, number> { throw new Error("Enter your PIN first"); }
  eject(_a: Atm): void {}
  name(): string { return this.constructor.name; }
}

class IdleState extends AtmState {
  override insertCard(a: Atm, card: string) { a.card = card; a.pinAttempts = 0; a.state = new HasCardState(); }
}

class WrongPinError extends Error {}

class HasCardState extends AtmState {
  override enterPin(a: Atm, pin: string) {
    if (a.bank.verifyPin(a.card!, pin)) { a.state = new AuthenticatedState(); return; }
    if (++a.pinAttempts >= 3) { a.retainedCards.push(a.card!); a.reset(); throw new Error("Card retained after 3 wrong PINs"); }
    throw new WrongPinError("Wrong PIN");
  }
  override eject(a: Atm) { a.reset(); }
}

class AuthenticatedState extends AtmState {
  override withdraw(a: Atm, amount: number) {
    if (amount <= 0 || amount % 100 !== 0) throw new RangeError("Amount must be a multiple of 100");
    if (a.bank.balance(a.card!) < amount) throw new Error("Insufficient balance");
    const plan = a.dispenser.plan(amount);                          // 1. can we physically pay it?
    if (!a.bank.debit(a.card!, amount)) throw new Error("Bank declined");   // 2. take the money
    a.dispenser.commit(plan);                                       // 3. hand out the notes
    return plan;
  }
  override eject(a: Atm) { a.reset(); }
}

class Atm {
  state: AtmState = new IdleState();
  card: string | null = null;
  pinAttempts = 0;
  bank: BankService;
  dispenser: CashDispenser;
  retainedCards: string[] = [];
  constructor(bank: BankService, dispenser: CashDispenser) { this.bank = bank; this.dispenser = dispenser; }

  insertCard(card: string) { this.state.insertCard(this, card); }
  enterPin(pin: string) { this.state.enterPin(this, pin); }
  withdraw(amount: number) { return this.state.withdraw(this, amount); }
  eject() { this.state.eject(this); }
  reset() { this.card = null; this.pinAttempts = 0; this.state = new IdleState(); }
  stateName() { return this.state.name(); }
}

class FakeBank implements BankService {
  balances: Record<string, number> = { "card-1": 5000 };
  verifyPin(_card: string, pin: string) { return pin === "1234"; }
  balance(card: string) { return this.balances[card]!; }
  debit(card: string, amount: number) {
    if (this.balances[card]! < amount) return false;
    this.balances[card]! -= amount;
    return true;
  }
}

export function demo(): void {
  console.log("ATM: State for the session, Chain of Responsibility for the notes");
  const bank = new FakeBank();
  const atm = new Atm(bank, new CashDispenser(10, 10, 10));

  throws(() => atm.withdraw(500), "cannot withdraw before inserting a card");
  atm.insertCard("card-1");
  throws(() => atm.withdraw(500), "cannot withdraw before entering the PIN");
  throws(() => atm.enterPin("0000"), "wrong PIN is rejected", WrongPinError);
  atm.enterPin("1234");
  check(atm.stateName() === "AuthenticatedState", "correct PIN moves to Authenticated");

  const notes = atm.withdraw(1300);
  check(JSON.stringify(notes) === JSON.stringify({ 100: 1, 200: 1, 500: 2 }), "1300 = 2 x 500 + 1 x 200 + 1 x 100");
  check(bank.balance("card-1") === 3700, "account debited by 1300");
  throws(() => atm.withdraw(150), "amount must be a multiple of 100", RangeError);
  throws(() => atm.withdraw(9000), "cannot withdraw more than the balance");
  atm.eject();
  check(atm.stateName() === "IdleState", "eject returns to Idle");

  atm.insertCard("card-1");
  for (let i = 0; i < 2; i++) { try { atm.enterPin("9999"); } catch (e) { if (!(e instanceof WrongPinError)) throw e; } }
  throws(() => atm.enterPin("9999"), "third wrong PIN retains the card");
  check(atm.retainedCards.join() === "card-1" && atm.stateName() === "IdleState", "card kept, machine back to Idle");

  const tricky = new CashDispenser(1, 3, 0);
  throws(() => tricky.plan(600), "greedy chain fails 600 from {500x1, 200x3} even though 3 x 200 works (a DP/backtracking search would fix it)", CannotDispenseError);

  const low = new Atm(new FakeBank(), new CashDispenser(1, 0, 0));
  low.insertCard("card-1"); low.enterPin("1234");
  throws(() => low.withdraw(1000), "machine cannot pay 1000 with one 500 note", CannotDispenseError);
  check(low.dispenser.count500 === 1 && low.bank.balance("card-1") === 5000, "failed withdrawal changed neither the notes nor the balance");
}
