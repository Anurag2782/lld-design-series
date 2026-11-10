using Lld.Common;

namespace Lld.Patterns.Strategy;

// ---- Part 1: pick an algorithm behind an interface (retry policy) ----
interface IRetryPolicy
{
    bool ShouldRetry(int attempt);
    long DelayMs(int attempt);
}

class FixedDelayRetry(int maxRetries, long delay) : IRetryPolicy
{
    public bool ShouldRetry(int attempt) => attempt < maxRetries;
    public long DelayMs(int attempt) => delay;
}

class ExponentialBackoffRetry : IRetryPolicy
{
    public bool ShouldRetry(int attempt) => attempt < 5;
    public long DelayMs(int attempt) => 100L << attempt;
}

class NoRetry : IRetryPolicy
{
    public bool ShouldRetry(int attempt) => false;
    public long DelayMs(int attempt) => 0;
}

/// <summary>Context: uses a policy, doesn't know which one.</summary>
class RetryingClient(IRetryPolicy policy, Action<long> sleeper)
{
    public T Call<T>(Func<T> action)
    {
        for (var attempt = 0; ; attempt++)
        {
            try { return action(); }
            catch (Exception) when (policy.ShouldRetry(attempt)) { sleeper(policy.DelayMs(attempt)); }
        }
    }
}

// ---- Part 2: choose the strategy by key.
// In ASP.NET Core you would register keyed services (.NET 8) or inject IEnumerable<IPaymentStrategy>. ----
interface IPaymentStrategy { string Pay(long paise); }
class UpiPayment : IPaymentStrategy { public string Pay(long p) => $"UPI:{p}"; }
class CardPayment : IPaymentStrategy { public string Pay(long p) => $"CARD:{p}"; }

class PaymentService(IReadOnlyDictionary<string, IPaymentStrategy> strategies)
{
    public string Pay(string type, long paise) =>
        strategies.TryGetValue(type, out var s) ? s.Pay(paise) : throw new ArgumentException($"Unknown payment type: {type}");
}

public static class Demo
{
    public static void Run()
    {
        Console.WriteLine("Strategy: swap the algorithm, keep the caller");
        var delays = new List<long>();
        var calls = 0;
        Func<string> flaky = () => { if (++calls < 3) throw new Exception("boom"); return "ok"; };

        var result = new RetryingClient(new ExponentialBackoffRetry(), delays.Add).Call(flaky);
        Check.That(result == "ok" && calls == 3, "succeeds on the third attempt");
        Check.That(delays.SequenceEqual(new long[] { 100, 200 }), "exponential backoff delays: 100ms, 200ms");

        calls = 0;
        Check.Throws<Exception>(() => new RetryingClient(new NoRetry(), _ => { }).Call(flaky), "NoRetry fails fast");
        Check.That(calls == 1, "NoRetry made exactly one call");

        var service = new PaymentService(new Dictionary<string, IPaymentStrategy> { ["upi"] = new UpiPayment(), ["card"] = new CardPayment() });
        Check.That(service.Pay("upi", 500) == "UPI:500", "key 'upi' selects UpiPayment");
        Check.That(service.Pay("card", 700) == "CARD:700", "key 'card' selects CardPayment");
        Check.Throws<ArgumentException>(() => service.Pay("crypto", 1), "unknown key fails clearly (no NullReferenceException)");
    }
}
