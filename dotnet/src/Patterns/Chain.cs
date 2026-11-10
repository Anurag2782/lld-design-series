using Lld.Common;

namespace Lld.Patterns.Chain;

record BookingRequest(string Doctor, int Hour, string Patient);

class ValidationException(string message) : Exception(message);

/// <summary>Each handler does ONE check, then passes the request on.
/// (ASP.NET Core middleware and MVC filters are this same pattern.)</summary>
abstract class Validator
{
    private Validator? _next;
    public Validator Then(Validator next) { _next = next; return next; }   // returns the new tail for chaining

    public void Validate(BookingRequest r)
    {
        Check(r);
        _next?.Validate(r);
    }
    protected abstract void Check(BookingRequest r);
}

class WorkingHoursCheck : Validator
{
    protected override void Check(BookingRequest r)
    {
        if (r.Hour < 9 || r.Hour >= 17) throw new ValidationException("Doctor not available at this time");
    }
}
class SlotFreeCheck(ISet<string> taken) : Validator
{
    protected override void Check(BookingRequest r)
    {
        if (taken.Contains($"{r.Doctor}@{r.Hour}")) throw new ValidationException("Slot already booked");
    }
}
class DuplicateBookingCheck(ISet<string> patientHours) : Validator
{
    protected override void Check(BookingRequest r)
    {
        if (patientHours.Contains($"{r.Patient}@{r.Hour}")) throw new ValidationException("Patient already has a booking then");
    }
}

public static class Demo
{
    public static void Run()
    {
        Console.WriteLine("Chain of Responsibility: small independent checks in a pipeline");
        var taken = new HashSet<string> { "drA@10" };
        var patientHours = new HashSet<string> { "asha@11" };

        Validator chain = new WorkingHoursCheck();
        chain.Then(new SlotFreeCheck(taken)).Then(new DuplicateBookingCheck(patientHours));

        chain.Validate(new BookingRequest("drA", 12, "ravi"));
        Lld.Common.Check.That(true, "a valid request passes every handler");

        Lld.Common.Check.That(MessageOf(() => chain.Validate(new BookingRequest("drA", 20, "ravi"))) == "Doctor not available at this time", "rejected by WorkingHoursCheck");
        Lld.Common.Check.That(MessageOf(() => chain.Validate(new BookingRequest("drA", 10, "ravi"))) == "Slot already booked", "rejected by SlotFreeCheck");
        Lld.Common.Check.That(MessageOf(() => chain.Validate(new BookingRequest("drB", 11, "asha"))) == "Patient already has a booking then", "rejected by DuplicateBookingCheck");
    }

    private static string MessageOf(Action a)
    {
        try { a(); } catch (ValidationException e) { return e.Message; }
        return "none";
    }
}
