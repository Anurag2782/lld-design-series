import { check } from "../common.ts";

interface Appointment { id: number; patient: string }
type Observer = (a: Appointment) => void;

/** Subject: announces "something happened"; never knows who is listening. (Node also has EventEmitter.) */
class AppointmentService {
  private observers: Observer[] = [];
  observerErrors: string[] = [];

  register(o: Observer): void { this.observers.push(o); }

  book(a: Appointment): void {
    // ... save to the database here ...
    for (const o of this.observers) {
      try {
        o(a);
      } catch (e) {                       // one bad observer must not block the rest
        this.observerErrors.push((e as Error).message);
      }
    }
  }
}

export function demo(): void {
  console.log("Observer: one event, many independent reactions");
  const log: string[] = [];
  const service = new AppointmentService();
  service.register((a) => log.push("sms:" + a.patient));
  service.register(() => { throw new Error("calendar down"); });
  service.register((a) => log.push("analytics:" + a.id));

  service.book({ id: 7, patient: "Asha" });
  check(log.join() === "sms:Asha,analytics:7", "healthy observers still run");
  check(service.observerErrors.join() === "calendar down", "failure was captured, not propagated");

  service.register((a) => log.push("doctor:" + a.patient));
  service.book({ id: 8, patient: "Ravi" });
  check(log.includes("doctor:Ravi"), "new observer added without editing book()");
}
