"""State: behavior changes with lifecycle, no if (status == ...) chains."""
from typing import Dict, List

from lld.common import check, raises


# ---------- Order lifecycle: each state knows what is allowed FROM it ----------
class OrderState:
    def pay(self) -> "OrderState": raise self._bad("pay")
    def ship(self) -> "OrderState": raise self._bad("ship")
    def cancel(self) -> "OrderState": raise self._bad("cancel")
    def _bad(self, action): return RuntimeError(f"Cannot {action} when {type(self).__name__}")


class Created(OrderState):
    def pay(self): return Paid()
    def cancel(self): return Cancelled()


class Paid(OrderState):
    def ship(self): return Shipped()
    def cancel(self): return Cancelled()   # refund would happen here


class Shipped(OrderState): pass
class Cancelled(OrderState): pass


class Order:
    def __init__(self): self._state: OrderState = Created()
    def pay(self): self._state = self._state.pay()
    def ship(self): self._state = self._state.ship()
    def cancel(self): self._state = self._state.cancel()
    @property
    def status(self): return type(self._state).__name__


# ---------- Vending machine ----------
class VendingMachine:
    def __init__(self):
        self._state: "VendingState" = Idle()
        self.balance = 0
        self.stock: Dict[str, int] = {}
        self.prices: Dict[str, int] = {}
        self.dispensed: List[str] = []
        self.returned = 0

    def insert(self, coin): self._state.insert(self, coin)
    def select(self, item): self._state.select(self, item)
    def set_state(self, s): self._state = s
    @property
    def state_name(self): return type(self._state).__name__

    def dispense(self, item):
        self.stock[item] -= 1
        self.dispensed.append(item)
        self.returned += self.balance - self.prices[item]   # change
        self.balance = 0

    def refund_all(self):
        self.returned += self.balance
        self.balance = 0


class VendingState:
    def insert(self, m: VendingMachine, coin: int): raise NotImplementedError
    def select(self, m: VendingMachine, item: str): raise NotImplementedError


class Idle(VendingState):
    def insert(self, m, coin): m.balance += coin; m.set_state(HasMoney())
    def select(self, m, item): pass   # insert money first: ignored


class HasMoney(VendingState):
    def insert(self, m, coin): m.balance += coin
    def select(self, m, item):
        if m.stock.get(item, 0) == 0:
            m.refund_all(); m.set_state(Idle()); return
        if m.balance < m.prices[item]:
            return                      # need more money, stay in HasMoney
        m.dispense(item)
        m.set_state(Idle())


def demo() -> None:
    print("State: behavior changes with lifecycle, no if (status == ...) chains")
    o = Order()
    check(o.status == "Created", "starts Created")
    o.pay(); o.ship()
    check(o.status == "Shipped", "Created -> Paid -> Shipped")
    raises(o.cancel, RuntimeError, "a shipped order cannot be cancelled")
    raises(Order().ship, RuntimeError, "cannot ship before paying")

    m = VendingMachine()
    m.stock["chips"], m.prices["chips"] = 1, 30
    m.select("chips")
    check(m.state_name == "Idle" and not m.dispensed, "select with no money does nothing")
    m.insert(20); m.select("chips")
    check(m.state_name == "HasMoney" and not m.dispensed, "not enough money: stays in HasMoney")
    m.insert(30); m.select("chips")
    check(m.dispensed == ["chips"] and m.returned == 20, "dispensed and returned 20 change")
    check(m.state_name == "Idle", "back to Idle after dispensing")
    m.insert(50); m.select("chips")   # out of stock now
    check(m.returned == 70 and m.state_name == "Idle", "out of stock: full refund, back to Idle")


if __name__ == "__main__":
    demo()
