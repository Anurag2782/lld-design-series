import { check, throws } from "../common.ts";

interface User { id: string; name: string }
interface Split { user: User; paise: number }
interface Expense { id: string; paidBy: User; totalPaise: number; splits: Split[] }
interface Payment { from: string; to: string; paise: number }
type SplitType = "EQUAL" | "EXACT" | "PERCENT";

/** Strategy: how an expense is divided. Shares ALWAYS add up to the total. */
interface SplitStrategy { split(total: number, users: User[], values?: number[]): Split[] }

const sum = (xs: number[]) => xs.reduce((a, b) => a + b, 0);

class EqualSplit implements SplitStrategy {
  split(total: number, users: User[]): Split[] {
    const base = Math.floor(total / users.length);
    const extra = total % users.length;                // leftover paise go to the first few people
    return users.map((user, i) => ({ user, paise: base + (i < extra ? 1 : 0) }));
  }
}

class ExactSplit implements SplitStrategy {
  split(total: number, users: User[], amounts?: number[]): Split[] {
    if (!amounts || amounts.length !== users.length) throw new Error("one amount per user");
    if (sum(amounts) !== total) throw new Error("amounts must add up to the total");
    return users.map((user, i) => ({ user, paise: amounts[i]! }));
  }
}

class PercentSplit implements SplitStrategy {
  split(total: number, users: User[], percents?: number[]): Split[] {
    if (!percents || percents.length !== users.length) throw new Error("one percent per user");
    if (sum(percents) !== 100) throw new Error("percentages must add up to 100");
    const shares = percents.map((p) => Math.floor((total * p) / 100));
    let leftover = total - sum(shares);                // rounding remainder
    for (let i = 0; leftover > 0; i = (i + 1) % shares.length, leftover--) shares[i]!++;
    return users.map((user, i) => ({ user, paise: shares[i]! }));
  }
}

class BalanceSheet {
  // debtor id -> (creditor id -> paise owed)
  private owes = new Map<string, Map<string, number>>();

  apply(e: Expense): void {
    for (const s of e.splits) {
      if (s.user.id === e.paidBy.id) continue;          // you don't owe yourself
      const m = this.owes.get(s.user.id) ?? new Map<string, number>();
      m.set(e.paidBy.id, (m.get(e.paidBy.id) ?? 0) + s.paise);
      this.owes.set(s.user.id, m);
    }
  }
  owed(debtor: string, creditor: string): number { return this.owes.get(debtor)?.get(creditor) ?? 0; }

  /** Positive = is owed money, negative = owes money. */
  net(): Record<string, number> {
    const net: Record<string, number> = {};
    for (const [debtor, m] of this.owes)
      for (const [creditor, amt] of m) {
        net[debtor] = (net[debtor] ?? 0) - amt;
        net[creditor] = (net[creditor] ?? 0) + amt;
      }
    return net;
  }
}

/** Greedy: biggest creditor is paid by biggest debtor. At most (n - 1) payments. */
function simplify(net: Record<string, number>): Payment[] {
  const byAmountThenId = (a: [string, number], b: [string, number]) => b[1] - a[1] || a[0].localeCompare(b[0]);
  const creditors = Object.entries(net).filter(([, v]) => v > 0).sort(byAmountThenId);
  const debtors = Object.entries(net).filter(([, v]) => v < 0).map(([k, v]): [string, number] => [k, -v]).sort(byAmountThenId);
  const out: Payment[] = [];
  while (creditors.length && debtors.length) {
    const c = creditors.shift()!, d = debtors.shift()!;
    const pay = Math.min(c[1], d[1]);
    out.push({ from: d[0], to: c[0], paise: pay });
    if (c[1] > pay) { creditors.push([c[0], c[1] - pay]); creditors.sort(byAmountThenId); }
    if (d[1] > pay) { debtors.push([d[0], d[1] - pay]); debtors.sort(byAmountThenId); }
  }
  return out;
}

let expenseCounter = 0;
class ExpenseService {
  private strategies: Record<SplitType, SplitStrategy>;
  private sheet: BalanceSheet;
  constructor(strategies: Record<SplitType, SplitStrategy>, sheet: BalanceSheet) { this.strategies = strategies; this.sheet = sheet; }
  addExpense(paidBy: User, total: number, users: User[], type: SplitType, values?: number[]): Expense {
    const e: Expense = { id: `E${++expenseCounter}`, paidBy, totalPaise: total, splits: this.strategies[type].split(total, users, values) };
    this.sheet.apply(e);
    return e;
  }
}

export function demo(): void {
  console.log("Splitwise: strategy per split type, paise not floats, simplify debts");
  const a: User = { id: "a", name: "Amit" }, p: User = { id: "p", name: "Priya" }, r: User = { id: "r", name: "Ravi" };

  const equal = new EqualSplit().split(10000, [a, p, r]);
  check(equal.map((s) => s.paise).join() === "3334,3333,3333", "Rs100 / 3 = 3334 + 3333 + 3333 paise");
  check(sum(equal.map((s) => s.paise)) === 10000, "shares add up to the total exactly (no lost paisa)");

  const pct = new PercentSplit().split(10001, [a, p, r], [50, 30, 20]);
  check(sum(pct.map((s) => s.paise)) === 10001 && pct[0]!.paise === 5001, "percent split keeps the rounding remainder");
  throws(() => new ExactSplit().split(100, [a, p], [60, 30]), "exact amounts must add up to the total");
  throws(() => new PercentSplit().split(100, [a, p], [60, 30]), "percentages must add up to 100");

  const sheet = new BalanceSheet();
  const service = new ExpenseService({ EQUAL: new EqualSplit(), EXACT: new ExactSplit(), PERCENT: new PercentSplit() }, sheet);
  service.addExpense(p, 90000, [p, a, r], "EQUAL");   // Priya pays Rs900 dinner
  check(sheet.owed("a", "p") === 30000 && sheet.owed("r", "p") === 30000, "Amit and Ravi each owe Priya Rs300");
  check(sheet.owed("p", "p") === 0, "nobody owes themselves");

  const plan = simplify({ A: -500, B: 0, C: 700, D: -200 });   // A owes B 500, D owes B 200, B owes C 700
  check(plan.length === 2, "three debts collapse into two payments");
  check(plan.some((x) => x.from === "A" && x.to === "C" && x.paise === 500) && plan.some((x) => x.from === "D" && x.to === "C" && x.paise === 200), "A pays C 500, D pays C 200");
}
