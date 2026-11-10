package lld.patterns.observer;

import lld.common.Check;
import java.util.ArrayList;
import java.util.List;

record Appointment(long id, String patient) {}

interface AppointmentObserver { void onBooked(Appointment a); }

/** Subject: announces "something happened"; never knows who is listening. */
class AppointmentService {
    private final List<AppointmentObserver> observers = new ArrayList<>();
    final List<String> observerErrors = new ArrayList<>();

    void register(AppointmentObserver o) { observers.add(o); }

    void book(Appointment a) {
        // ... save to the database here ...
        for (AppointmentObserver o : observers) {
            try {
                o.onBooked(a);
            } catch (RuntimeException e) {          // one bad observer must not block the rest
                observerErrors.add(e.getMessage());
            }
        }
    }
}

public class Demo {
    public static void main(String[] args) {
        System.out.println("Observer: one event, many independent reactions");
        List<String> log = new ArrayList<>();
        AppointmentService service = new AppointmentService();
        service.register(a -> log.add("sms:" + a.patient()));
        service.register(a -> { throw new IllegalStateException("calendar down"); });
        service.register(a -> log.add("analytics:" + a.id()));

        service.book(new Appointment(7, "Asha"));
        Check.that(log.equals(List.of("sms:Asha", "analytics:7")), "healthy observers still run");
        Check.that(service.observerErrors.equals(List.of("calendar down")), "failure was captured, not propagated");

        // Adding a new reaction = register one more observer. book() is untouched.
        service.register(a -> log.add("doctor:" + a.patient()));
        service.book(new Appointment(8, "Ravi"));
        Check.that(log.contains("doctor:Ravi"), "new observer added without editing book()");
    }
}
