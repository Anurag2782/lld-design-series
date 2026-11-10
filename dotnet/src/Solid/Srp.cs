using Lld.Common;

namespace Lld.Solid.Srp;

record Appointment(string Patient, string? Slot);

interface IAppointmentRepository { void Save(Appointment a); }
interface INotificationService { void Confirm(Appointment a); }
interface IInvoiceGenerator { void Generate(Appointment a); }

/// <summary>BEFORE: four reasons to change in one class (validation, DB, email, PDF).</summary>
class GodAppointmentService
{
    public void Book(Appointment a)
    {
        if (a.Slot is null) throw new ArgumentException("slot required");
        Console.WriteLine("INSERT INTO appointments ...");
        Console.WriteLine("smtp.Send(...)");
        Console.WriteLine("pdf.Generate(...)");
    }
}

class FakeRepo(List<string> log) : IAppointmentRepository { public void Save(Appointment a) => log.Add("saved"); }
class FakeNotifier(List<string> log) : INotificationService { public void Confirm(Appointment a) => log.Add("notified"); }
class FakeInvoices(List<string> log) : IInvoiceGenerator { public void Generate(Appointment a) => log.Add("invoiced"); }

/// <summary>AFTER: only coordinates. Each collaborator has one reason to change.</summary>
class AppointmentService(IAppointmentRepository repo, INotificationService notifier, IInvoiceGenerator invoices)
{
    public void Book(Appointment a)
    {
        Validate(a);
        repo.Save(a);
        notifier.Confirm(a);
        invoices.Generate(a);
    }

    private static void Validate(Appointment a)
    {
        if (a.Slot is null) throw new ArgumentException("slot required");
    }
}

public static class Demo
{
    public static void Run()
    {
        Console.WriteLine("SRP: AppointmentService only coordinates");
        var log = new List<string>();
        var service = new AppointmentService(new FakeRepo(log), new FakeNotifier(log), new FakeInvoices(log));

        service.Book(new Appointment("P1", "10:00"));
        Check.That(log.SequenceEqual(new[] { "saved", "notified", "invoiced" }), "collaborators run in order");

        log.Clear();
        Check.Throws<ArgumentException>(() => service.Book(new Appointment("P2", null)), "invalid booking is rejected");
        Check.That(log.Count == 0, "nothing saved/sent when validation fails");
    }
}
