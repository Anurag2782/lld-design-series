using Lld.Common;

namespace Lld.Patterns.Builder;

sealed class Appointment
{
    public string PatientId { get; }
    public string DoctorId { get; }
    public string? Notes { get; }
    public DateTime Time { get; }
    public bool Reminder { get; }

    private Appointment(Builder b)
    {
        PatientId = b.PatientIdValue!; DoctorId = b.DoctorIdValue!; Time = b.TimeValue!.Value;
        Notes = b.NotesValue; Reminder = b.ReminderValue;
    }

    public static Builder NewBuilder() => new();

    /// <summary>Copy with one change: the original stays immutable.</summary>
    public Builder ToBuilder() => new Builder().PatientId(PatientId).DoctorId(DoctorId).Time(Time).Notes(Notes).Reminder(Reminder);

    public sealed class Builder
    {
        internal string? PatientIdValue, DoctorIdValue, NotesValue;
        internal DateTime? TimeValue;
        internal bool ReminderValue = true;

        public Builder PatientId(string v) { PatientIdValue = v; return this; }
        public Builder DoctorId(string v) { DoctorIdValue = v; return this; }
        public Builder Time(DateTime v) { TimeValue = v; return this; }
        public Builder Notes(string? v) { NotesValue = v; return this; }
        public Builder Reminder(bool v) { ReminderValue = v; return this; }

        /// <summary>The gatekeeper: an invalid Appointment can never exist.</summary>
        public Appointment Build()
        {
            if (PatientIdValue is null || DoctorIdValue is null || TimeValue is null)
                throw new InvalidOperationException("PatientId, DoctorId and Time are required");
            return new Appointment(this);
        }
    }
}

public static class Demo
{
    public static void Run()
    {
        Console.WriteLine("Builder: readable construction, validation in Build()");
        var t = new DateTime(2026, 11, 20, 10, 0, 0);
        var a = Appointment.NewBuilder().PatientId("P1").DoctorId("D7").Time(t).Reminder(false).Build();
        Check.That(a.PatientId == "P1" && !a.Reminder, "fields set by name, not position");

        var moved = a.ToBuilder().Time(t.AddHours(2)).Build();
        Check.That(moved.Time == t.AddHours(2) && a.Time == t, "ToBuilder copies; original unchanged");

        Check.Throws<InvalidOperationException>(() => Appointment.NewBuilder().PatientId("P1").Build(), "missing required fields are rejected in Build()");
        // For simple immutable data, C# records + `with` expressions often replace a hand-written builder:
        //   var moved = record with { Time = t.AddHours(2) };
    }
}
