import { check, throws } from "../common.ts";

// ---------- Order lifecycle: each state knows what is allowed FROM it ----------
class OrderState {
  pay(): OrderState { throw this.bad("pay"); }
  ship(): OrderState { throw this.bad("ship"); }
  cancel(): OrderState { throw this.bad("cancel"); }
  name(): string { return this.constructor.name; }
  private bad(action: string) { return new Error(`Cannot ${action} when ${this.name()}`); }
}
class Created extends OrderState {
  override pay() { return new Paid(); }
  override cancel() { return new Cancelled(); }
}
class Paid extends OrderState {
  override ship() { return new Shipped(); }
  override cancel() { return new Cancelled(); }   // refund would happen here
}
class Shipped extends OrderState {}
class Cancelled extends OrderState {}

class Order {
  private state: OrderState = new Created();
  pay() { this.state = this.state.pay(); }
  ship() { this.state = this.state.ship(); }
  cancel() { this.state = this.state.cancel(); }
  status() { return this.state.name(); }
}

// ---------- Vending machine ----------
abstract class VendingState {
  abstract insert(m: VendingMachine, coin: number): void;
  abstract select(m: VendingMachine, item: string): void;
  name(): string { return this.constructor.name; }
}

class VendingMachine {
  state: VendingState = new Idle();
  balance = 0;
  stock = new Map<string, number>();
  prices = new Map<string, number>();
  dispensed: string[] = [];
  returned = 0;

  insert(coin: number) { this.state.insert(this, coin); }
  select(item: string) { this.state.select(this, item); }
  dispense(item: string) {
    this.stock.set(item, (this.stock.get(item) ?? 0) - 1);
    this.dispensed.push(item);
    this.returned += this.balance - this.prices.get(item)!;   // change
    this.balance = 0;
  }
  refundAll() { this.returned += this.balance; this.balance = 0; }
}

class Idle extends VendingState {
  insert(m: VendingMachine, coin: number) { m.balance += coin; m.state = new HasMoney(); }
  select(_m: VendingMachine, _item: string) { /* insert money first: ignored */ }
}
class HasMoney extends VendingState {
  insert(m: VendingMachine, coin: number) { m.balance += coin; }
  select(m: VendingMachine, item: string) {
    if ((m.stock.get(item) ?? 0) === 0) { m.refundAll(); m.state = new Idle(); return; }
    if (m.balance < m.prices.get(item)!) return;      // need more money, stay in HasMoney
    m.dispense(item);
    m.state = new Idle();
  }
}

export function demo(): void {
  console.log("State: behavior changes with lifecycle, no if (status === ...) chains");
  const o = new Order();
  check(o.status() === "Created", "starts Created");
  o.pay(); o.ship();
  check(o.status() === "Shipped", "Created -> Paid -> Shipped");
  throws(() => o.cancel(), "a shipped order cannot be cancelled");
  throws(() => new Order().ship(), "cannot ship before paying");

  const m = new VendingMachine();
  m.stock.set("chips", 1); m.prices.set("chips", 30);
  m.select("chips");
  check(m.state.name() === "Idle" && m.dispensed.length === 0, "select with no money does nothing");
  m.insert(20); m.select("chips");
  check(m.state.name() === "HasMoney" && m.dispensed.length === 0, "not enough money: stays in HasMoney");
  m.insert(30); m.select("chips");
  check(m.dispensed.join() === "chips" && m.returned === 20, "dispensed and returned 20 change");
  check(m.state.name() === "Idle", "back to Idle after dispensing");
  m.insert(50); m.select("chips");   // out of stock now
  check(m.returned === 70 && m.state.name() === "Idle", "out of stock: full refund, back to Idle");
}
