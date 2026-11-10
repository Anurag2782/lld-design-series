package lld.problems.ratelimiter;

import lld.common.Check;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

interface RateLimiter { boolean allow(String key); }

/** Token bucket with LAZY refill: no background thread, tokens are computed on each request. */
class TokenBucketLimiter implements RateLimiter {
    private final long capacity;
    private final double refillPerSec;
    private final LongSupplier nanoClock;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    TokenBucketLimiter(long capacity, double refillPerSec, LongSupplier nanoClock) {
        this.capacity = capacity; this.refillPerSec = refillPerSec; this.nanoClock = nanoClock;
    }

    public boolean allow(String key) {
        return buckets.computeIfAbsent(key, k -> new Bucket(capacity, nanoClock.getAsLong()))
                      .tryConsume(capacity, refillPerSec, nanoClock.getAsLong());
    }

    private static class Bucket {
        private double tokens;
        private long last;
        Bucket(long capacity, long now) { tokens = capacity; last = now; }

        synchronized boolean tryConsume(long cap, double rate, long now) {
            tokens = Math.min(cap, tokens + (now - last) / 1e9 * rate);   // lazy refill
            last = now;
            if (tokens < 1) return false;
            tokens -= 1;
            return true;
        }
    }
}

public class Demo {
    public static void main(String[] args) {
        System.out.println("Rate limiter: token bucket (capacity 10, refill 5/sec)");
        long[] nanos = { 0 };
        RateLimiter limiter = new TokenBucketLimiter(10, 5, () -> nanos[0]);

        int allowed = 0;
        for (int i = 0; i < 10; i++) if (limiter.allow("u1")) allowed++;
        Check.that(allowed == 10, "a burst of 10 is allowed (full bucket)");
        Check.that(!limiter.allow("u1"), "the 11th request is rejected (bucket empty)");
        Check.that(limiter.allow("u2"), "another user has an independent bucket");

        nanos[0] += 1_000_000_000L;      // 1 second later: 5 tokens refilled
        allowed = 0;
        for (int i = 0; i < 10; i++) if (limiter.allow("u1")) allowed++;
        Check.that(allowed == 5, "after 1s, exactly 5 more requests are allowed");

        nanos[0] += 2_000_000_000L;      // 2 seconds: 10 tokens would be added, capped at capacity 10
        allowed = 0;
        for (int i = 0; i < 20; i++) if (limiter.allow("u1")) allowed++;
        Check.that(allowed == 10, "after 2s the bucket is full again, capped at capacity (10)");
    }
}
