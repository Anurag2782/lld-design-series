import { check, throws } from "../common.ts";

interface Appointment {
  readonly patientId: string;
  readonly doctorId: string;
  readonly time: Date;
  readonly notes?: string;
  readonly reminder: boolean;
}

class AppointmentBuilder {
  private _patientId?: string;
  private _doctorId?: string;
  private _time?: Date;
  private _notes?: string;
  private _reminder = true;

  patientId(v: string) { this._patientId = v; return this; }
  doctorId(v: string) { this._doctorId = v; return this; }
  time(v: Date) { this._time = v; return this; }
  notes(v: string | undefined) { this._notes = v; return this; }
  reminder(v: boolean) { this._reminder = v; return this; }

  /** The gatekeeper: an invalid Appointment can never exist. */
  build(): Appointment {
    if (this._patientId === undefined || this._doctorId === undefined || this._time === undefined)
      throw new Error("patientId, doctorId and time are required");
    return Object.freeze({
      patientId: this._patientId, doctorId: this._doctorId, time: this._time,
      notes: this._notes, reminder: this._reminder,
    });
  }

  /** Copy with one change: the original stays immutable. */
  static from(a: Appointment): AppointmentBuilder {
    return new AppointmentBuilder().patientId(a.patientId).doctorId(a.doctorId).time(a.time).notes(a.notes).reminder(a.reminder);
  }
}

export function demo(): void {
  console.log("Builder: readable construction, validation in build()");
  const t = new Date("2026-11-20T10:00:00Z");
  const a = new AppointmentBuilder().patientId("P1").doctorId("D7").time(t).reminder(false).build();
  check(a.patientId === "P1" && !a.reminder, "fields set by name, not position");

  const later = new Date(t.getTime() + 2 * 3600_000);
  const moved = AppointmentBuilder.from(a).time(later).build();
  check(moved.time.getTime() === later.getTime() && a.time.getTime() === t.getTime(), "from() copies; original unchanged");
  throws(() => new AppointmentBuilder().patientId("P1").build(), "missing required fields are rejected in build()");
  // In TypeScript, an options object + spread often replaces a builder: { ...a, notes: "x" }
  check({ ...a, notes: "bring reports" }.notes === "bring reports", "idiomatic TS alternative: object spread");
}
