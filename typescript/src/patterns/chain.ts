import { check } from "../common.ts";

interface BookingRequest { doctor: string; hour: number; patient: string }
class ValidationError extends Error {}

/** Each handler does ONE check, then passes the request on. (Express/Koa middleware is this same pattern.) */
abstract class Validator {
  private next?: Validator;
  then(next: Validator): Validator { this.next = next; return next; }   // returns the new tail for chaining
  validate(r: BookingRequest): void {
    this.check(r);
    this.next?.validate(r);
  }
  protected abstract check(r: BookingRequest): void;
}

class WorkingHoursCheck extends Validator {
  protected check(r: BookingRequest) {
    if (r.hour < 9 || r.hour >= 17) throw new ValidationError("Doctor not available at this time");
  }
}
class SlotFreeCheck extends Validator {
  private taken: Set<string>;
  constructor(taken: Set<string>) { super(); this.taken = taken; }
  protected check(r: BookingRequest) {
    if (this.taken.has(`${r.doctor}@${r.hour}`)) throw new ValidationError("Slot already booked");
  }
}
class DuplicateBookingCheck extends Validator {
  private patientHours: Set<string>;
  constructor(patientHours: Set<string>) { super(); this.patientHours = patientHours; }
  protected check(r: BookingRequest) {
    if (this.patientHours.has(`${r.patient}@${r.hour}`)) throw new ValidationError("Patient already has a booking then");
  }
}

function messageOf(action: () => void): string {
  try { action(); } catch (e) { if (e instanceof ValidationError) return e.message; throw e; }
  return "none";
}

export function demo(): void {
  console.log("Chain of Responsibility: small independent checks in a pipeline");
  const chain = new WorkingHoursCheck();
  chain.then(new SlotFreeCheck(new Set(["drA@10"]))).then(new DuplicateBookingCheck(new Set(["asha@11"])));

  chain.validate({ doctor: "drA", hour: 12, patient: "ravi" });
  check(true, "a valid request passes every handler");
  check(messageOf(() => chain.validate({ doctor: "drA", hour: 20, patient: "ravi" })) === "Doctor not available at this time", "rejected by WorkingHoursCheck");
  check(messageOf(() => chain.validate({ doctor: "drA", hour: 10, patient: "ravi" })) === "Slot already booked", "rejected by SlotFreeCheck");
  check(messageOf(() => chain.validate({ doctor: "drB", hour: 11, patient: "asha" })) === "Patient already has a booking then", "rejected by DuplicateBookingCheck");
}
