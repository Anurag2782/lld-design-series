package lld.patterns.builder;

import lld.common.Check;
import java.time.LocalDateTime;

final class Appointment {
    private final String patientId, doctorId, notes;
    private final LocalDateTime time;
    private final boolean reminder;

    private Appointment(Builder b) {
        patientId = b.patientId; doctorId = b.doctorId; time = b.time;
        notes = b.notes; reminder = b.reminder;
    }

    static Builder builder() { return new Builder(); }
    /** Copy with one change: the original stays immutable. */
    Builder toBuilder() {
        return new Builder().patientId(patientId).doctorId(doctorId).time(time).notes(notes).reminder(reminder);
    }

    String patientId() { return patientId; }
    LocalDateTime time() { return time; }
    boolean reminder() { return reminder; }

    static class Builder {
        private String patientId, doctorId, notes;
        private LocalDateTime time;
        private boolean reminder = true;

        Builder patientId(String v) { patientId = v; return this; }
        Builder doctorId(String v) { doctorId = v; return this; }
        Builder time(LocalDateTime v) { time = v; return this; }
        Builder notes(String v) { notes = v; return this; }
        Builder reminder(boolean v) { reminder = v; return this; }

        /** The gatekeeper: an invalid Appointment can never exist. */
        Appointment build() {
            if (patientId == null || doctorId == null || time == null)
                throw new IllegalStateException("patientId, doctorId and time are required");
            return new Appointment(this);
        }
    }
}

public class Demo {
    public static void main(String[] args) {
        System.out.println("Builder: readable construction, validation in build()");
        LocalDateTime t = LocalDateTime.of(2026, 11, 20, 10, 0);
        Appointment a = Appointment.builder().patientId("P1").doctorId("D7").time(t).reminder(false).build();
        Check.that(a.patientId().equals("P1") && !a.reminder(), "fields set by name, not position");

        Appointment moved = a.toBuilder().time(t.plusHours(2)).build();
        Check.that(moved.time().equals(t.plusHours(2)) && a.time().equals(t), "toBuilder copies; original unchanged");

        Check.throwsError(() -> Appointment.builder().patientId("P1").build(), IllegalStateException.class,
            "missing required fields are rejected in build()");
    }
}
