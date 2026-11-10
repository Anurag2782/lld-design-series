"""OCP: open for extension, closed for modification."""
from abc import ABC, abstractmethod

from lld.common import near


class DiscountStrategy(ABC):
    @abstractmethod
    def apply(self, amount: float) -> float: ...


class RegularDiscount(DiscountStrategy):
    def apply(self, amount): return amount * 0.05


class PremiumDiscount(DiscountStrategy):
    def apply(self, amount): return amount * 0.15


class FestivalDiscount(DiscountStrategy):
    """Added later: a NEW class, zero edits to Checkout or the existing strategies."""
    def apply(self, amount): return amount * 0.20


class Checkout:
    def total(self, amount: float, discount: DiscountStrategy) -> float:
        return amount - discount.apply(amount)


def demo() -> None:
    print("OCP: extend by adding classes, not editing methods")
    checkout = Checkout()
    near(checkout.total(1000, RegularDiscount()), 950.0, "regular gets 5% off")
    near(checkout.total(1000, PremiumDiscount()), 850.0, "premium gets 15% off")
    near(checkout.total(1000, FestivalDiscount()), 800.0, "festival (new class) gets 20% off")


if __name__ == "__main__":
    demo()
