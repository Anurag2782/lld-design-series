"""Strategy: swap the algorithm, keep the caller."""
from abc import ABC, abstractmethod
from typing import Callable, Dict, List, TypeVar

from lld.common import check, raises

T = TypeVar("T")


# ---- Part 1: a retry policy behind an interface ----
class RetryPolicy(ABC):
    @abstractmethod
    def should_retry(self, attempt: int) -> bool: ...
    @abstractmethod
    def delay_ms(self, attempt: int) -> int: ...


class FixedDelayRetry(RetryPolicy):
    def __init__(self, max_retries: int, delay: int): self._max, self._delay = max_retries, delay
    def should_retry(self, attempt): return attempt < self._max
    def delay_ms(self, attempt): return self._delay


class ExponentialBackoffRetry(RetryPolicy):
    def should_retry(self, attempt): return attempt < 5
    def delay_ms(self, attempt): return 100 * (1 << attempt)


class NoRetry(RetryPolicy):
    def should_retry(self, attempt): return False
    def delay_ms(self, attempt): return 0


class RetryingClient:
    """Context: uses a policy, doesn't know which one."""
    def __init__(self, policy: RetryPolicy, sleeper: Callable[[int], None]):
        self._policy, self._sleeper = policy, sleeper

    def call(self, action: Callable[[], T]) -> T:
        attempt = 0
        while True:
            try:
                return action()
            except Exception:
                if not self._policy.should_retry(attempt):
                    raise
                self._sleeper(self._policy.delay_ms(attempt))
                attempt += 1


# ---- Part 2: pick the strategy by key (the dict plays the role of Spring's Map<String, Bean>) ----
class PaymentStrategy(ABC):
    @abstractmethod
    def pay(self, paise: int) -> str: ...


class UpiPayment(PaymentStrategy):
    def pay(self, paise): return f"UPI:{paise}"


class CardPayment(PaymentStrategy):
    def pay(self, paise): return f"CARD:{paise}"


class PaymentService:
    def __init__(self, strategies: Dict[str, PaymentStrategy]): self._strategies = strategies

    def pay(self, kind: str, paise: int) -> str:
        strategy = self._strategies.get(kind)
        if strategy is None:
            raise ValueError(f"Unknown payment type: {kind}")
        return strategy.pay(paise)


def demo() -> None:
    print("Strategy: swap the algorithm, keep the caller")
    delays: List[int] = []
    calls = {"n": 0}

    def flaky() -> str:
        calls["n"] += 1
        if calls["n"] < 3:
            raise RuntimeError("boom")
        return "ok"

    result = RetryingClient(ExponentialBackoffRetry(), delays.append).call(flaky)
    check(result == "ok" and calls["n"] == 3, "succeeds on the third attempt")
    check(delays == [100, 200], "exponential backoff delays: 100ms, 200ms")

    calls["n"] = 0
    raises(lambda: RetryingClient(NoRetry(), lambda ms: None).call(flaky), RuntimeError, "NoRetry fails fast")
    check(calls["n"] == 1, "NoRetry made exactly one call")

    service = PaymentService({"upi": UpiPayment(), "card": CardPayment()})
    check(service.pay("upi", 500) == "UPI:500", "key 'upi' selects UpiPayment")
    check(service.pay("card", 700) == "CARD:700", "key 'card' selects CardPayment")
    raises(lambda: service.pay("crypto", 1), ValueError, "unknown key fails clearly (no KeyError/None call)")


if __name__ == "__main__":
    demo()
