using System.Collections.Concurrent;
using Lld.Common;

namespace Lld.Problems.RateLimiter;

interface IRateLimiter { bool Allow(string key); }

/// <summary>Token bucket with LAZY refill: no background thread, tokens are computed on each request.
/// (In ASP.NET Core 7+, the built-in System.Threading.RateLimiting package offers production versions.)</summary>
class TokenBucketLimiter(long capacity, double refillPerSec, Func<long> nanoClock) : IRateLimiter
{
    private readonly ConcurrentDictionary<string, Bucket> _buckets = new();

    public bool Allow(string key) =>
        _buckets.GetOrAdd(key, _ => new Bucket(capacity, nanoClock())).TryConsume(capacity, refillPerSec, nanoClock());

    private sealed class Bucket(long capacity, long now)
    {
        private double _tokens = capacity;
        private long _last = now;

        public bool TryConsume(long cap, double rate, long nowNanos)
        {
            lock (this)
            {
                _tokens = Math.Min(cap, _tokens + (nowNanos - _last) / 1e9 * rate);   // lazy refill
                _last = nowNanos;
                if (_tokens < 1) return false;
                _tokens -= 1;
                return true;
            }
        }
    }
}

public static class Demo
{
    public static void Run()
    {
        Console.WriteLine("Rate limiter: token bucket (capacity 10, refill 5/sec)");
        long nanos = 0;
        IRateLimiter limiter = new TokenBucketLimiter(10, 5, () => nanos);

        var allowed = 0;
        for (var i = 0; i < 10; i++) if (limiter.Allow("u1")) allowed++;
        Check.That(allowed == 10, "a burst of 10 is allowed (full bucket)");
        Check.That(!limiter.Allow("u1"), "the 11th request is rejected (bucket empty)");
        Check.That(limiter.Allow("u2"), "another user has an independent bucket");

        nanos += 1_000_000_000L;      // 1 second later: 5 tokens refilled
        allowed = 0;
        for (var i = 0; i < 10; i++) if (limiter.Allow("u1")) allowed++;
        Check.That(allowed == 5, "after 1s, exactly 5 more requests are allowed");

        nanos += 2_000_000_000L;      // 2 seconds: 10 tokens would be added, capped at capacity 10
        allowed = 0;
        for (var i = 0; i < 20; i++) if (limiter.Allow("u1")) allowed++;
        Check.That(allowed == 10, "after 2s the bucket is full again, capped at capacity (10)");
    }
}
