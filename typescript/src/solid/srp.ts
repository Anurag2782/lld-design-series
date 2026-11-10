import { check, throws } from "../common.ts";

interface Appointment { patient: string; slot: string | null }
interface AppointmentRepository { save(a: Appointment): void }
interface NotificationService { confirm(a: Appointment): void }
interface InvoiceGenerator { generate(a: Appointment): void }

/** BEFORE: four reasons to change in one class (validation, DB, email, PDF). */
export class GodAppointmentService {
  book(a: Appointment): void {
    if (a.slot === null) throw new Error("slot required");
    console.log("INSERT INTO appointments ...");
    console.log("smtp.send(...)");
    console.log("pdf.generate(...)");
  }
}

/** AFTER: only coordinates. Each collaborator has exactly one reason to change. */
class AppointmentService {
  private repo: AppointmentRepository;
  private notifier: NotificationService;
  private invoices: InvoiceGenerator;
  constructor(repo: AppointmentRepository, notifier: NotificationService, invoices: InvoiceGenerator) {
    this.repo = repo; this.notifier = notifier; this.invoices = invoices;
  }
  book(a: Appointment): void {
    this.validate(a);
    this.repo.save(a);
    this.notifier.confirm(a);
    this.invoices.generate(a);
  }
  private validate(a: Appointment): void {
    if (a.slot === null) throw new Error("slot required");
  }
}

export function demo(): void {
  console.log("SRP: AppointmentService only coordinates");
  const log: string[] = [];
  const service = new AppointmentService(
    { save: () => log.push("saved") },
    { confirm: () => log.push("notified") },
    { generate: () => log.push("invoiced") },
  );
  service.book({ patient: "P1", slot: "10:00" });
  check(log.join(",") === "saved,notified,invoiced", "collaborators run in order");
  log.length = 0;
  throws(() => service.book({ patient: "P2", slot: null }), "invalid booking is rejected");
  check(log.length === 0, "nothing saved/sent when validation fails");
}
