using System.Collections.Concurrent;
using Lld.Common;

namespace Lld.Patterns.Singleton;

/// <summary>Lazy&lt;T&gt;: lazy and thread-safe by default. (In ASP.NET Core prefer services.AddSingleton&lt;T&gt;().)</summary>
sealed class Config
{
    private static readonly Lazy<Config> _instance = new(() => new Config());
    private Config() { }
    public static Config Instance => _instance.Value;
}

sealed class Registry
{
    private static readonly Lazy<Registry> _instance = new(() => new Registry());
    private readonly ConcurrentDictionary<string, string> _data = new();
    private Registry() { }
    public static Registry Instance => _instance.Value;
    public void Put(string k, string v) => _data[k] = v;
    public string? Get(string k) => _data.TryGetValue(k, out var v) ? v : null;
}

public static class Demo
{
    public static void Run()
    {
        Console.WriteLine("Singleton: one shared instance, thread-safe");
        var seen = new ConcurrentBag<Config>();
        Parallel.For(0, 50, new ParallelOptions { MaxDegreeOfParallelism = 16 }, _ => seen.Add(Config.Instance));
        var first = Config.Instance;
        Check.That(seen.Count == 50 && seen.All(c => ReferenceEquals(c, first)), "50 concurrent callers all receive the same Config instance");

        Registry.Instance.Put("env", "prod");
        Check.That(Registry.Instance.Get("env") == "prod", "singleton holds shared state");
        Check.That(ReferenceEquals(Registry.Instance, Registry.Instance), "always the same Registry");
    }
}
