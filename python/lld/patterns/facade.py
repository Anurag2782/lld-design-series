"""Facade: one simple entry point over a complex subsystem."""
from dataclasses import dataclass
from typing import List

from lld.common import check, raises


class InventoryService:
    def __init__(self, log): self.log = log
    def reserve(self, item): self.log.append("reserve:" + item)
    def release(self, item): self.log.append("release:" + item)


class PaymentService:
    def __init__(self, log): self.log, self.fail_next = log, False
    def charge(self, paise):
        if self.fail_next:
            raise RuntimeError("card declined")
        self.log.append(f"charge:{paise}")
        return "rcpt-1"


class ShippingService:
    def __init__(self, log): self.log = log
    def schedule(self, address):
        self.log.append("ship:" + address)
        return "track-9"


class Notifier:
    def __init__(self, log): self.log = log
    def order_placed(self, email): self.log.append("notify:" + email)


@dataclass(frozen=True)
class OrderConfirmation:
    receipt_id: str
    tracking_id: str


class CheckoutFacade:
    """ONE simple method hides four subsystems and the order they must be called in."""

    def __init__(self, inventory, payment, shipping, notifier):
        self._inventory, self._payment, self._shipping, self._notifier = inventory, payment, shipping, notifier

    def place_order(self, item: str, paise: int, address: str, email: str) -> OrderConfirmation:
        self._inventory.reserve(item)
        try:
            receipt = self._payment.charge(paise)
            tracking = self._shipping.schedule(address)
            self._notifier.order_placed(email)
            return OrderConfirmation(receipt, tracking)
        except Exception:
            self._inventory.release(item)        # compensating action: undo what we already did
            raise


def demo() -> None:
    print("Facade: one simple entry point over a complex subsystem")
    log: List[str] = []
    payment = PaymentService(log)
    checkout = CheckoutFacade(InventoryService(log), payment, ShippingService(log), Notifier(log))

    c = checkout.place_order("book", 49900, "Haridwar", "a@x.com")
    check(c.receipt_id == "rcpt-1" and c.tracking_id == "track-9", "caller makes ONE call and gets one confirmation")
    check(log == ["reserve:book", "charge:49900", "ship:Haridwar", "notify:a@x.com"], "facade calls the subsystems in the right order")

    log.clear()
    payment.fail_next = True
    raises(lambda: checkout.place_order("book", 49900, "Haridwar", "a@x.com"), RuntimeError, "payment failure propagates")
    check(log == ["reserve:book", "release:book"], "inventory reservation was released (compensation), nothing shipped")


if __name__ == "__main__":
    demo()
