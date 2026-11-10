"""Rate limiter: token bucket with lazy refill (capacity 10, refill 5/sec)."""
import threading
from abc import ABC, abstractmethod
from typing import Callable, Dict

from lld.common import check


class RateLimiter(ABC):
    @abstractmethod
    def allow(self, key: str) -> bool: ...


class _Bucket:
    def __init__(self, capacity: float, now: float):
        self.tokens, self.last, self.lock = capacity, now, threading.Lock()

    def try_consume(self, cap: float, rate: float, now: float) -> bool:
        with self.lock:
            self.tokens = min(cap, self.tokens + (now - self.last) * rate)   # lazy refill, no background thread
            self.last = now
            if self.tokens < 1:
                return False
            self.tokens -= 1
            return True


class TokenBucketLimiter(RateLimiter):
    def __init__(self, capacity: int, refill_per_sec: float, clock: Callable[[], float]):
        self._cap, self._rate, self._clock = capacity, refill_per_sec, clock
        self._buckets: Dict[str, _Bucket] = {}
        self._guard = threading.Lock()

    def allow(self, key: str) -> bool:
        with self._guard:
            bucket = self._buckets.get(key)
            if bucket is None:
                bucket = self._buckets[key] = _Bucket(self._cap, self._clock())
        return bucket.try_consume(self._cap, self._rate, self._clock())


def demo() -> None:
    print("Rate limiter: token bucket (capacity 10, refill 5/sec)")
    now = {"t": 0.0}
    limiter = TokenBucketLimiter(10, 5, lambda: now["t"])

    allowed = sum(limiter.allow("u1") for _ in range(10))
    check(allowed == 10, "a burst of 10 is allowed (full bucket)")
    check(not limiter.allow("u1"), "the 11th request is rejected (bucket empty)")
    check(limiter.allow("u2"), "another user has an independent bucket")

    now["t"] += 1.0                      # 1 second later: 5 tokens refilled
    allowed = sum(limiter.allow("u1") for _ in range(10))
    check(allowed == 5, "after 1s, exactly 5 more requests are allowed")

    now["t"] += 2.0                      # 2 seconds: 10 tokens would be added, capped at capacity 10
    allowed = sum(limiter.allow("u1") for _ in range(20))
    check(allowed == 10, "after 2s the bucket is full again, capped at capacity (10)")


if __name__ == "__main__":
    demo()
