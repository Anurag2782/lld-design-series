using Lld.Common;

namespace Lld.Patterns.Proxy;

interface IReportService { string Generate(long id); }

class RealReportService : IReportService
{
    public string Generate(long id) => $"report-{id}";
}

class AccessDeniedException() : Exception("admin role required");

/// <summary>Protection proxy: decides WHETHER the real object is called.</summary>
class SecureReportProxy(IReportService real, ISet<string> roles) : IReportService
{
    public string Generate(long id)
    {
        if (!roles.Contains("ADMIN")) throw new AccessDeniedException();
        return real.Generate(id);
    }
}

/// <summary>Virtual proxy: decides WHEN the real object is created.</summary>
class LazyReportProxy(Func<IReportService> factory) : IReportService
{
    private readonly Lazy<IReportService> _real = new(factory);
    public string Generate(long id) => _real.Value.Generate(id);
}

/// <summary>Decorator for contrast: ALWAYS calls the real object and adds something.</summary>
class UppercaseDecorator(IReportService inner) : IReportService
{
    public string Generate(long id) => inner.Generate(id).ToUpperInvariant();
}

public static class Demo
{
    public static void Run()
    {
        Console.WriteLine("Proxy: control access. Decorator: add behavior");
        IReportService real = new RealReportService();
        Check.Throws<AccessDeniedException>(() => new SecureReportProxy(real, new HashSet<string> { "USER" }).Generate(1), "protection proxy blocks a non-admin; real service never called");
        Check.That(new SecureReportProxy(real, new HashSet<string> { "ADMIN" }).Generate(1) == "report-1", "admin is allowed through");

        var created = 0;
        IReportService lazy = new LazyReportProxy(() => { created++; return real; });
        Check.That(created == 0, "virtual proxy: nothing created yet");
        lazy.Generate(1); lazy.Generate(2);
        Check.That(created == 1, "virtual proxy: created once, on first use");

        Check.That(new UppercaseDecorator(real).Generate(3) == "REPORT-3", "decorator always calls through and adds behavior");
    }
}
