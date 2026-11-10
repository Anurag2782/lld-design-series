using System.Collections.Concurrent;
using Lld.Common;

namespace Lld.Patterns.Decorator;

interface IAppointmentRepository { string FindById(long id); }

class DbRepository : IAppointmentRepository
{
    public int Hits;
    public string FindById(long id) { Hits++; return $"appointment-{id}"; }
}

/// <summary>Decorator: SAME interface, adds caching around any repository.</summary>
class CachingRepository(IAppointmentRepository inner) : IAppointmentRepository
{
    private readonly ConcurrentDictionary<long, string> _cache = new();
    public string FindById(long id) => _cache.GetOrAdd(id, inner.FindById);
}

/// <summary>Decorator: SAME interface, adds logging around any repository.</summary>
class LoggingRepository(IAppointmentRepository inner) : IAppointmentRepository
{
    public List<string> Log { get; } = new();
    public string FindById(long id) { Log.Add($"FindById({id})"); return inner.FindById(id); }
}

public static class Demo
{
    public static void Run()
    {
        Console.WriteLine("Decorator: add behavior by wrapping, same interface");
        var db = new DbRepository();
        var repo = new LoggingRepository(new CachingRepository(db));
        repo.FindById(1);
        repo.FindById(1);
        Check.That(db.Hits == 1, "caching decorator: second call never reached the database");
        Check.That(repo.Log.Count == 2, "logging is outermost: it saw both calls (including the cache hit)");

        var db2 = new DbRepository();
        var inner = new LoggingRepository(db2);
        IAppointmentRepository cachedOutside = new CachingRepository(inner);
        cachedOutside.FindById(1);
        cachedOutside.FindById(1);
        Check.That(inner.Log.Count == 1, "swap the wrapper order and logging only sees cache misses");
    }
}
