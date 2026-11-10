using Lld.Common;

namespace Lld.Patterns.Observer;

record Appointment(long Id, string Patient);

interface IAppointmentObserver { void OnBooked(Appointment a); }

class ActionObserver(Action<Appointment> handler) : IAppointmentObserver
{
    public void OnBooked(Appointment a) => handler(a);
}

/// <summary>Subject: announces "something happened"; never knows who is listening.
/// (C# also has built-in `event` + delegates for the same idea.)</summary>
class AppointmentService
{
    private readonly List<IAppointmentObserver> _observers = new();
    public List<string> ObserverErrors { get; } = new();

    public void Register(IAppointmentObserver o) => _observers.Add(o);

    public void Book(Appointment a)
    {
        // ... save to the database here ...
        foreach (var o in _observers)
        {
            try { o.OnBooked(a); }
            catch (Exception e) { ObserverErrors.Add(e.Message); }   // one bad observer must not block the rest
        }
    }
}

public static class Demo
{
    public static void Run()
    {
        Console.WriteLine("Observer: one event, many independent reactions");
        var log = new List<string>();
        var service = new AppointmentService();
        service.Register(new ActionObserver(a => log.Add("sms:" + a.Patient)));
        service.Register(new ActionObserver(_ => throw new InvalidOperationException("calendar down")));
        service.Register(new ActionObserver(a => log.Add("analytics:" + a.Id)));

        service.Book(new Appointment(7, "Asha"));
        Check.That(log.SequenceEqual(new[] { "sms:Asha", "analytics:7" }), "healthy observers still run");
        Check.That(service.ObserverErrors.SequenceEqual(new[] { "calendar down" }), "failure was captured, not propagated");

        service.Register(new ActionObserver(a => log.Add("doctor:" + a.Patient)));
        service.Book(new Appointment(8, "Ravi"));
        Check.That(log.Contains("doctor:Ravi"), "new observer added without editing Book()");
    }
}
