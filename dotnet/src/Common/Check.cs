namespace Lld.Common;

/// <summary>Tiny assertion helper so every demo fails loudly if behavior is wrong.</summary>
public static class Check
{
    public static void That(bool condition, string message)
    {
        if (!condition) throw new InvalidOperationException("FAILED: " + message);
        Console.WriteLine("  ok: " + message);
    }

    public static void Near(double actual, double expected, string message) =>
        That(Math.Abs(actual - expected) < 1e-9, $"{message} ({actual})");

    public static void Throws<T>(Action action, string message) where T : Exception
    {
        try { action(); }
        catch (T) { Console.WriteLine("  ok: " + message); return; }
        catch (Exception e) { throw new InvalidOperationException($"FAILED: {message} threw {e.GetType().Name}"); }
        throw new InvalidOperationException($"FAILED: {message} (nothing thrown)");
    }
}
