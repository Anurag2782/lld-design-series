import { check } from "../common.ts";

interface RateLimiter { allow(key: string): boolean }

/** Token bucket with LAZY refill: no timer, tokens are computed on each request. */
class TokenBucketLimiter implements RateLimiter {
  private capacity: number;
  private refillPerSec: number;
  private clock: () => number;       // seconds
  private buckets = new Map<string, { tokens: number; last: number }>();

  constructor(capacity: number, refillPerSec: number, clock: () => number) {
    this.capacity = capacity; this.refillPerSec = refillPerSec; this.clock = clock;
  }

  allow(key: string): boolean {
    const now = this.clock();
    const b = this.buckets.get(key) ?? { tokens: this.capacity, last: now };
    this.buckets.set(key, b);
    b.tokens = Math.min(this.capacity, b.tokens + (now - b.last) * this.refillPerSec);   // lazy refill
    b.last = now;
    if (b.tokens < 1) return false;
    b.tokens -= 1;
    return true;
  }
}

export function demo(): void {
  console.log("Rate limiter: token bucket (capacity 10, refill 5/sec)");
  let now = 0;
  const limiter = new TokenBucketLimiter(10, 5, () => now);
  const burst = (n: number) => Array.from({ length: n }, () => limiter.allow("u1")).filter(Boolean).length;

  check(burst(10) === 10, "a burst of 10 is allowed (full bucket)");
  check(!limiter.allow("u1"), "the 11th request is rejected (bucket empty)");
  check(limiter.allow("u2"), "another user has an independent bucket");

  now += 1;                    // 1 second later: 5 tokens refilled
  check(burst(10) === 5, "after 1s, exactly 5 more requests are allowed");

  now += 2;                    // 2 seconds: 10 tokens would be added, capped at capacity 10
  check(burst(20) === 10, "after 2s the bucket is full again, capped at capacity (10)");
}
