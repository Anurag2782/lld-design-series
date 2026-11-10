import { check, throws } from "../common.ts";

type SeatStatus = "AVAILABLE" | "LOCKED" | "BOOKED";

class SeatUnavailableError extends Error {
  constructor(seatId: string) { super("Seat unavailable: " + seatId); }
}

/** A seat FOR ONE SHOW. (The physical Seat has no status; availability belongs to the show.)
 *  Each method below is synchronous, so check-and-set is atomic in single-threaded JS.
 *  If you add an `await` between a check and its set, you MUST add a real lock (or a DB/Redis lock). */
class ShowSeat {
  readonly seatId: string;
  private _status: SeatStatus = "AVAILABLE";
  private lockedBy?: string;
  private expiry = 0;
  constructor(seatId: string) { this.seatId = seatId; }

  tryLock(userId: string, now: number, holdMs: number): boolean {
    if (this._status === "LOCKED" && now > this.expiry) this._status = "AVAILABLE";   // expired lock is free
    if (this._status !== "AVAILABLE") return false;
    this._status = "LOCKED"; this.lockedBy = userId; this.expiry = now + holdMs;
    return true;
  }
  confirm(userId: string, now: number): boolean {
    if (this._status !== "LOCKED" || userId !== this.lockedBy || now > this.expiry) return false;
    this._status = "BOOKED";
    return true;
  }
  release(userId: string): void {
    if (this._status === "LOCKED" && userId === this.lockedBy) this._status = "AVAILABLE";
  }
  get status(): SeatStatus { return this._status; }
}

class Show {
  private seats = new Map<string, ShowSeat>();
  constructor(seatIds: string[]) { for (const id of seatIds) this.seats.set(id, new ShowSeat(id)); }
  seat(id: string): ShowSeat {
    const s = this.seats.get(id);
    if (!s) throw new Error("No such seat: " + id);
    return s;
  }
}

const HOLD_MS = 5 * 60_000;
let bookingCounter = 0;

class BookingService {
  private clock: () => number;
  constructor(clock: () => number) { this.clock = clock; }

  /** All-or-nothing: if any seat fails, release the ones already taken. */
  lockSeats(show: Show, seatIds: string[], userId: string): ShowSeat[] {
    const locked: ShowSeat[] = [];
    for (const id of seatIds) {
      const s = show.seat(id);
      if (s.tryLock(userId, this.clock(), HOLD_MS)) locked.push(s);
      else {
        locked.forEach((l) => l.release(userId));        // roll back what we already took
        throw new SeatUnavailableError(id);
      }
    }
    return locked;
  }

  confirm(show: Show, seatIds: string[], userId: string): { id: string; userId: string; seatIds: string[] } {
    for (const id of seatIds) if (!show.seat(id).confirm(userId, this.clock())) throw new SeatUnavailableError(id);
    return { id: `B${++bookingCounter}`, userId, seatIds };
  }
}

export function demo(): void {
  console.log("BookMyShow: timed seat locks, all-or-nothing, no double booking");
  let now = Date.parse("2026-11-20T18:00:00Z");
  const service = new BookingService(() => now);
  const show = new Show(["C7", "C8", "C9"]);

  service.lockSeats(show, ["C7", "C8"], "alice");
  check(show.seat("C7").status === "LOCKED", "alice holds C7 and C8");
  throws(() => service.lockSeats(show, ["C7"], "bob"), "bob cannot take a locked seat", SeatUnavailableError);

  throws(() => service.lockSeats(show, ["C9", "C8"], "bob"), "bob wants C9 + C8: fails on C8", SeatUnavailableError);
  check(show.seat("C9").status === "AVAILABLE", "all-or-nothing: C9 was released, not left half-held");

  const b = service.confirm(show, ["C7", "C8"], "alice");
  check(show.seat("C7").status === "BOOKED" && b.seatIds.length === 2, "alice confirms within the hold time");
  throws(() => service.confirm(show, ["C9"], "bob"), "cannot confirm seats you never locked", SeatUnavailableError);

  service.lockSeats(show, ["C9"], "bob");
  now += 6 * 60_000;
  throws(() => service.confirm(show, ["C9"], "bob"), "an expired lock cannot be confirmed", SeatUnavailableError);
  service.lockSeats(show, ["C9"], "carol");
  check(show.seat("C9").status === "LOCKED", "after expiry the seat is free again for carol");

  // 100 users click the same seat. Exactly one wins.
  const hot = new Show(["A1"]);
  const live = new BookingService(() => Date.now());
  let winners = 0;
  for (let i = 0; i < 100; i++) {
    try { live.lockSeats(hot, ["A1"], `user${i}`); winners++; } catch (e) { if (!(e instanceof SeatUnavailableError)) throw e; }
  }
  check(winners === 1, "100 simultaneous clicks, exactly one lock winner");
}
