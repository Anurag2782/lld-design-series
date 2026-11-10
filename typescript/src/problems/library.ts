import { check, throws } from "../common.ts";

interface Book { isbn: string; title: string; author: string }

/** A physical copy. (Book = the title; BookCopy = the thing you borrow. Status belongs to the COPY.) */
class BookCopy {
  readonly barcode: string;
  readonly book: Book;
  private available = true;
  constructor(barcode: string, book: Book) { this.barcode = barcode; this.book = book; }
  tryCheckout(): boolean { if (!this.available) return false; this.available = false; return true; }
  checkin(): void { this.available = true; }
  isAvailable(): boolean { return this.available; }
}

class Member {
  readonly id: string;
  activeLoans: Loan[] = [];
  constructor(id: string) { this.id = id; }
}

interface Loan { member: Member; copy: BookCopy; issued: Date; due: Date }

const DAY = 86_400_000;
const addDays = (d: Date, n: number) => new Date(d.getTime() + n * DAY);
const daysBetween = (a: Date, b: Date) => Math.round((b.getTime() - a.getTime()) / DAY);

/** Strategy: fines are money in paise, per day late, after a grace period. */
interface FinePolicy { fine(loan: Loan, returned: Date): number }

class PerDayFine implements FinePolicy {
  private ratePerDay: number;
  private graceDays: number;
  constructor(ratePerDay: number, graceDays: number) { this.ratePerDay = ratePerDay; this.graceDays = graceDays; }
  fine(loan: Loan, returned: Date) { return Math.max(0, daysBetween(loan.due, returned) - this.graceDays) * this.ratePerDay; }
}

class LoanService {
  static readonly MAX_LOANS = 3;
  static readonly LOAN_DAYS = 14;
  private finePolicy: FinePolicy;
  private listeners: Array<(b: Book) => void> = [];      // Observer: told when a copy comes back
  constructor(finePolicy: FinePolicy) { this.finePolicy = finePolicy; }
  addListener(l: (b: Book) => void) { this.listeners.push(l); }

  issue(m: Member, copy: BookCopy, today: Date): Loan {
    if (m.activeLoans.length >= LoanService.MAX_LOANS) throw new Error("Loan limit reached");
    if (!copy.tryCheckout()) throw new Error("Copy already issued");   // check-and-set in one synchronous step
    const loan: Loan = { member: m, copy, issued: today, due: addDays(today, LoanService.LOAN_DAYS) };
    m.activeLoans.push(loan);
    return loan;
  }

  returnCopy(loan: Loan, today: Date): number {
    const fine = this.finePolicy.fine(loan, today);
    loan.member.activeLoans = loan.member.activeLoans.filter((l) => l !== loan);
    loan.copy.checkin();
    this.listeners.forEach((l) => l(loan.copy.book));
    return fine;
  }
}

class Catalog {
  private copies: BookCopy[] = [];
  add(c: BookCopy) { this.copies.push(c); }
  search(text: string): Book[] {
    const t = text.toLowerCase();
    const found: Book[] = [];
    for (const c of this.copies)
      if ((c.book.title.toLowerCase().includes(t) || c.book.author.toLowerCase().includes(t)) && !found.includes(c.book)) found.push(c.book);
    return found;
  }
  availableCopyOf(b: Book): BookCopy | undefined { return this.copies.find((c) => c.book === b && c.isAvailable()); }
}

export function demo(): void {
  console.log("Library: Book vs BookCopy, fine strategy, return listener");
  const dune: Book = { isbn: "111", title: "Dune", author: "Herbert" }, emma: Book = { isbn: "222", title: "Emma", author: "Austen" };
  const d1 = new BookCopy("D1", dune), d2 = new BookCopy("D2", dune);
  const catalog = new Catalog();
  [d1, d2, new BookCopy("E1", emma)].forEach((c) => catalog.add(c));

  check(catalog.search("herb").length === 1 && catalog.search("herb")[0] === dune, "search by author substring finds the title once");
  const service = new LoanService(new PerDayFine(200, 2));
  const notified: string[] = [];
  service.addListener((b) => notified.push(b.title));

  const asha = new Member("asha");
  const day0 = new Date("2026-11-01T00:00:00Z");
  const l1 = service.issue(asha, d1, day0);
  check(l1.due.getTime() === addDays(day0, 14).getTime(), "due date is 14 days after issue");
  throws(() => service.issue(new Member("ravi"), d1, day0), "the same physical copy cannot be issued twice");
  check(catalog.availableCopyOf(dune) === d2, "the second copy of the title is still available");

  service.issue(asha, d2, day0);
  service.issue(asha, new BookCopy("E1", emma), day0);
  throws(() => service.issue(asha, new BookCopy("X", emma), day0), "limit of 3 active loans enforced");

  check(service.returnCopy(l1, addDays(day0, 14)) === 0, "returned on the due date: no fine");
  check(notified.join() === "Dune", "listener was told a copy of Dune came back");
  const l2 = asha.activeLoans[0]!;
  check(service.returnCopy(l2, addDays(l2.due, 5)) === 600, "5 days late, 2 days grace: 3 billable days x 200 paise = 600");
}
