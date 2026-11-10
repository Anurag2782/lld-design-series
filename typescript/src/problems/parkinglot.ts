import { check, throws } from "../common.ts";

type VehicleType = "BIKE" | "CAR" | "TRUCK";
interface Vehicle { plate: string; type: VehicleType; electric?: boolean }

class NoSlotAvailableError extends Error {
  constructor() { super("No slot available"); }
}

/** The slot owns its own "does this vehicle fit?" rule. */
abstract class ParkingSlot {
  readonly id: string;
  private occupied = false;
  constructor(id: string) { this.id = id; }
  protected abstract accepts(v: Vehicle): boolean;

  /** Check AND set in one synchronous step. (In Java/C# this needs a lock; in single-threaded JS it is atomic
   *  as long as there is no `await` between the check and the set.) */
  tryOccupy(v: Vehicle): boolean {
    if (this.occupied || !this.accepts(v)) return false;
    this.occupied = true;
    return true;
  }
  free(): void { this.occupied = false; }
}

class StandardSlot extends ParkingSlot {
  private type: VehicleType;
  constructor(id: string, type: VehicleType) { super(id); this.type = type; }
  protected accepts(v: Vehicle) { return v.type === this.type; }
}

/** The extension: only electric cars fit. No other existing class changes. */
class EvChargingSlot extends ParkingSlot {
  protected accepts(v: Vehicle) { return v.electric === true && v.type === "CAR"; }
}

interface Ticket { id: string; vehicle: Vehicle; slot: ParkingSlot; entry: Date }

class ParkingFloor {
  private slots: ParkingSlot[];
  constructor(slots: ParkingSlot[]) { this.slots = slots; }
  occupySlotFor(v: Vehicle): ParkingSlot | undefined { return this.slots.find((s) => s.tryOccupy(v)); }
}

function createFloor(floorNo: number, bikes: number, cars: number, trucks: number, evSlots: number): ParkingFloor {
  const slots: ParkingSlot[] = [];
  for (let i = 0; i < evSlots; i++) slots.push(new EvChargingSlot(`F${floorNo}-EV-${i}`));
  const add = (type: VehicleType, n: number) => { for (let i = 0; i < n; i++) slots.push(new StandardSlot(`F${floorNo}-${type}-${i}`, type)); };
  add("BIKE", bikes); add("CAR", cars); add("TRUCK", trucks);
  return new ParkingFloor(slots);
}

/** Strategy: money is in paise (integers), never fractional floats. */
interface PricingStrategy { calculate(t: Ticket, exit: Date): number }

class HourlyPricing implements PricingStrategy {
  private rates: Record<VehicleType, number>;
  constructor(rates: Record<VehicleType, number>) { this.rates = rates; }
  calculate(t: Ticket, exit: Date) {
    const minutes = Math.floor((exit.getTime() - t.entry.getTime()) / 60_000);
    const hours = Math.max(1, Math.ceil(minutes / 60));      // round UP, minimum 1 hour
    return hours * this.rates[t.vehicle.type];
  }
}

/** Decorator over any PricingStrategy. */
class EvSurcharge implements PricingStrategy {
  private base: PricingStrategy;
  private fee: number;
  constructor(base: PricingStrategy, fee: number) { this.base = base; this.fee = fee; }
  calculate(t: Ticket, exit: Date) {
    const fee = this.base.calculate(t, exit);
    return t.vehicle.electric ? fee + this.fee : fee;
  }
}

let ticketCounter = 0;
class ParkingLot {
  private floors: ParkingFloor[];
  private pricing: PricingStrategy;
  constructor(floors: ParkingFloor[], pricing: PricingStrategy) { this.floors = floors; this.pricing = pricing; }

  park(v: Vehicle, now: Date): Ticket {
    for (const f of this.floors) {
      const slot = f.occupySlotFor(v);
      if (slot) return { id: `T${++ticketCounter}`, vehicle: v, slot, entry: now };
    }
    throw new NoSlotAvailableError();
  }
  unpark(t: Ticket, exit: Date): number {
    const fee = this.pricing.calculate(t, exit);
    t.slot.free();
    return fee;
  }
}

export function demo(): void {
  console.log("Parking Lot: slots decide fit, strategy prices, decorator adds EV fee");
  const pricing = new EvSurcharge(new HourlyPricing({ BIKE: 1000, CAR: 3000, TRUCK: 6000 }), 5000);
  const lot = new ParkingLot([createFloor(1, 2, 3, 1, 1)], pricing);
  const t0 = new Date("2026-11-20T10:00:00Z");
  const plus = (mins: number) => new Date(t0.getTime() + mins * 60_000);

  const car = lot.park({ plate: "UK08-1", type: "CAR" }, t0);
  check(lot.unpark(car, plus(61)) === 6000, "61 minutes rounds up to 2 hours x 3000 paise = 6000");

  const ev = lot.park({ plate: "UK08-EV", type: "CAR", electric: true }, t0);
  check(ev.slot instanceof EvChargingSlot, "electric car is given the EV slot first");
  check(lot.unpark(ev, plus(30)) === 3000 + 5000, "1 hour minimum + EV surcharge");

  const truck = lot.park({ plate: "UK08-T", type: "TRUCK" }, t0);
  throws(() => lot.park({ plate: "UK08-T2", type: "TRUCK" }, t0), "second truck: only one truck slot exists", NoSlotAvailableError);
  lot.unpark(truck, plus(60));
  lot.park({ plate: "UK08-T3", type: "TRUCK" }, t0);
  check(true, "freed slot can be reused");

  // 50 cars arrive for 10 slots. Exactly 10 may succeed, and no slot is used twice.
  const busy = new ParkingLot([createFloor(1, 0, 10, 0, 0)], pricing);
  const tickets: Ticket[] = [];
  for (let i = 0; i < 50; i++) {
    try { tickets.push(busy.park({ plate: `CAR-${i}`, type: "CAR" }, t0)); } catch (e) { if (!(e instanceof NoSlotAvailableError)) throw e; }
  }
  check(tickets.length === 10, "50 arriving cars, exactly 10 tickets issued");
  check(new Set(tickets.map((t) => t.slot.id)).size === 10, "no slot was handed to two cars");
}
