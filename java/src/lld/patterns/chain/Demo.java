package lld.patterns.chain;

import lld.common.Check;
import java.util.HashSet;
import java.util.Set;

record BookingRequest(String doctor, int hour, String patient) {}

class ValidationException extends RuntimeException {
    ValidationException(String m) { super(m); }
}

/** Each handler does ONE check, then passes the request on. */
abstract class Validator {
    private Validator next;
    Validator then(Validator next) { this.next = next; return next; }   // returns the new tail for chaining

    final void validate(BookingRequest r) {
        check(r);
        if (next != null) next.validate(r);
    }
    protected abstract void check(BookingRequest r);
}

class WorkingHoursCheck extends Validator {
    protected void check(BookingRequest r) {
        if (r.hour() < 9 || r.hour() >= 17) throw new ValidationException("Doctor not available at this time");
    }
}
class SlotFreeCheck extends Validator {
    private final Set<String> taken;
    SlotFreeCheck(Set<String> taken) { this.taken = taken; }
    protected void check(BookingRequest r) {
        if (taken.contains(r.doctor() + "@" + r.hour())) throw new ValidationException("Slot already booked");
    }
}
class DuplicateBookingCheck extends Validator {
    private final Set<String> patientHours;
    DuplicateBookingCheck(Set<String> patientHours) { this.patientHours = patientHours; }
    protected void check(BookingRequest r) {
        if (patientHours.contains(r.patient() + "@" + r.hour())) throw new ValidationException("Patient already has a booking then");
    }
}

public class Demo {
    public static void main(String[] args) {
        System.out.println("Chain of Responsibility: small independent checks in a pipeline");
        Set<String> taken = new HashSet<>(Set.of("drA@10"));
        Set<String> patientHours = new HashSet<>(Set.of("asha@11"));

        Validator chain = new WorkingHoursCheck();
        chain.then(new SlotFreeCheck(taken)).then(new DuplicateBookingCheck(patientHours));

        chain.validate(new BookingRequest("drA", 12, "ravi"));
        Check.that(true, "a valid request passes every handler");

        String m1 = messageOf(() -> chain.validate(new BookingRequest("drA", 20, "ravi")));
        Check.that(m1.equals("Doctor not available at this time"), "rejected by WorkingHoursCheck");
        String m2 = messageOf(() -> chain.validate(new BookingRequest("drA", 10, "ravi")));
        Check.that(m2.equals("Slot already booked"), "rejected by SlotFreeCheck");
        String m3 = messageOf(() -> chain.validate(new BookingRequest("drB", 11, "asha")));
        Check.that(m3.equals("Patient already has a booking then"), "rejected by DuplicateBookingCheck");
    }

    private static String messageOf(Runnable r) {
        try { r.run(); } catch (ValidationException e) { return e.getMessage(); }
        return "none";
    }
}
