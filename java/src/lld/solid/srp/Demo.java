package lld.solid.srp;

import lld.common.Check;
import java.util.ArrayList;
import java.util.List;

record Appointment(String patient, String slot) {}

interface AppointmentRepository { void save(Appointment a); }
interface NotificationService { void confirm(Appointment a); }
interface InvoiceGenerator { void generate(Appointment a); }

/** BEFORE: four reasons to change in one class (validation, DB, email, PDF). */
class GodAppointmentService {
    void book(Appointment a) {
        if (a.slot() == null) throw new IllegalArgumentException("slot required");
        System.out.println("INSERT INTO appointments ...");   // persistence
        System.out.println("smtp.send(...)");                  // notification
        System.out.println("pdf.generate(...)");               // invoice
    }
}

/** AFTER: only coordinates. Each collaborator has exactly one reason to change. */
class AppointmentService {
    private final AppointmentRepository repo;
    private final NotificationService notifier;
    private final InvoiceGenerator invoices;

    AppointmentService(AppointmentRepository repo, NotificationService notifier, InvoiceGenerator invoices) {
        this.repo = repo;
        this.notifier = notifier;
        this.invoices = invoices;
    }

    void book(Appointment a) {
        validate(a);
        repo.save(a);
        notifier.confirm(a);
        invoices.generate(a);
    }

    private void validate(Appointment a) {
        if (a.slot() == null) throw new IllegalArgumentException("slot required");
    }
}

public class Demo {
    public static void main(String[] args) {
        System.out.println("SRP: AppointmentService only coordinates");
        List<String> log = new ArrayList<>();
        var service = new AppointmentService(
            a -> log.add("saved"), a -> log.add("notified"), a -> log.add("invoiced"));

        service.book(new Appointment("P1", "10:00"));
        Check.that(log.equals(List.of("saved", "notified", "invoiced")), "collaborators run in order");

        log.clear();
        Check.throwsError(() -> service.book(new Appointment("P2", null)),
            IllegalArgumentException.class, "invalid booking is rejected");
        Check.that(log.isEmpty(), "nothing saved/sent when validation fails");
    }
}
