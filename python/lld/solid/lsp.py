"""LSP: a subtype must keep every promise of its parent."""
from typing import Iterable, Protocol, runtime_checkable

from lld.common import check, near, raises


class Account:
    def __init__(self, opening: float):
        self.balance = opening

    def deposit(self, amount: float) -> None:
        self.balance += amount


@runtime_checkable
class Withdrawable(Protocol):
    """Only accounts that CAN withdraw promise it."""
    def withdraw(self, amount: float) -> None: ...


class SavingsAccount(Account):
    def withdraw(self, amount: float) -> None:
        if amount > self.balance:
            raise ValueError("insufficient funds")
        self.balance -= amount


class FixedDepositAccount(Account):
    """Honest hierarchy: an Account, but it does NOT offer withdraw()."""


def withdraw_from_all(accounts: Iterable[Withdrawable], amount: float) -> None:
    for a in accounts:
        a.withdraw(amount)


def demo() -> None:
    print("LSP: subtypes must keep the parent's promises")
    a, b = SavingsAccount(500), SavingsAccount(300)
    withdraw_from_all([a, b], 100)
    near(a.balance, 400, "first savings account debited")
    near(b.balance, 200, "second savings account debited")

    fd = FixedDepositAccount(10_000)
    check(not isinstance(fd, Withdrawable), "fixed deposit never promised withdraw()")
    raises(lambda: a.withdraw(1_000_000), ValueError, "overdraw is rejected consistently")


if __name__ == "__main__":
    demo()
