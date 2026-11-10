"""Tiny assertion helpers so every demo fails loudly if behavior is wrong."""
import math


def check(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError("FAILED: " + message)
    print("  ok: " + message)


def near(actual: float, expected: float, message: str) -> None:
    check(math.isclose(actual, expected, abs_tol=1e-9), f"{message} ({actual})")


def raises(action, exc_type, message: str) -> None:
    try:
        action()
    except exc_type:
        print("  ok: " + message)
        return
    except Exception as e:  # wrong exception type
        raise AssertionError(f"FAILED: {message} threw {type(e).__name__}") from e
    raise AssertionError(f"FAILED: {message} (nothing thrown)")
