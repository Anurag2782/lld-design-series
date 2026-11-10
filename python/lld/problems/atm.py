"""ATM: State for the session, Chain of Responsibility for the notes."""
from typing import Dict, List, Optional, Protocol

from lld.common import check, raises


class BankService(Protocol):
    """The bank is an abstraction the ATM depends on (DIP). Real code would call a core-banking API."""
    def verify_pin(self, card: str, pin: str) -> bool: ...
    def balance(self, card: str) -> int: ...
    def debit(self, card: str, amount: int) -> bool: ...


class CannotDispense(Exception):
    pass


class NoteHandler:
    """Chain of Responsibility: one handler per note. plan() does NOT change counts; commit() does."""

    def __init__(self, note: int, available: int, nxt: Optional["NoteHandler"]):
        self.note, self.available, self._next = note, available, nxt

    def plan(self, amount: int, out: Dict[int, int]) -> Dict[int, int]:
        use = min(self.available, amount // self.note)
        if use > 0:
            out[self.note] = use
        rest = amount - use * self.note
        if rest == 0:
            return out
        if self._next is None:
            raise CannotDispense(f"cannot make {amount} with the notes available")
        return self._next.plan(rest, out)

    def commit(self, plan: Dict[int, int]) -> None:
        self.available -= plan.get(self.note, 0)
        if self._next:
            self._next.commit(plan)


class CashDispenser:
    def __init__(self, n500: int, n200: int, n100: int):
        self._head = NoteHandler(500, n500, NoteHandler(200, n200, NoteHandler(100, n100, None)))

    def plan(self, amount: int) -> Dict[int, int]: return self._head.plan(amount, {})
    def commit(self, plan: Dict[int, int]) -> None: self._head.commit(plan)
    @property
    def count500(self) -> int: return self._head.available


# ---------- ATM states: what each action means depends on where we are ----------
class AtmState:
    def insert_card(self, a: "Atm", card: str): raise RuntimeError("Cannot insert card now")
    def enter_pin(self, a: "Atm", pin: str): raise RuntimeError("Insert a card first")
    def withdraw(self, a: "Atm", amount: int): raise RuntimeError("Enter your PIN first")
    def eject(self, a: "Atm"): pass


class IdleState(AtmState):
    def insert_card(self, a, card):
        a.card, a.pin_attempts, a.state = card, 0, HasCardState()


class HasCardState(AtmState):
    def enter_pin(self, a, pin):
        if a.bank.verify_pin(a.card, pin):
            a.state = AuthenticatedState()
            return
        a.pin_attempts += 1
        if a.pin_attempts >= 3:
            a.retained_cards.append(a.card)
            a.reset()
            raise RuntimeError("Card retained after 3 wrong PINs")
        raise ValueError("Wrong PIN")

    def eject(self, a): a.reset()


class AuthenticatedState(AtmState):
    def withdraw(self, a, amount):
        if amount <= 0 or amount % 100 != 0:
            raise ValueError("Amount must be a multiple of 100")
        if a.bank.balance(a.card) < amount:
            raise RuntimeError("Insufficient balance")
        plan = a.dispenser.plan(amount)                       # 1. can we physically pay it?
        if not a.bank.debit(a.card, amount):                  # 2. take the money
            raise RuntimeError("Bank declined")
        a.dispenser.commit(plan)                              # 3. hand out the notes
        return plan

    def eject(self, a): a.reset()


class Atm:
    def __init__(self, bank: BankService, dispenser: CashDispenser):
        self.bank, self.dispenser = bank, dispenser
        self.state: AtmState = IdleState()
        self.card: Optional[str] = None
        self.pin_attempts = 0
        self.retained_cards: List[str] = []

    def insert_card(self, card): self.state.insert_card(self, card)
    def enter_pin(self, pin): self.state.enter_pin(self, pin)
    def withdraw(self, amount): return self.state.withdraw(self, amount)
    def eject(self): self.state.eject(self)
    def reset(self): self.card, self.pin_attempts, self.state = None, 0, IdleState()
    @property
    def state_name(self): return type(self.state).__name__


class FakeBank:
    def __init__(self): self.balances = {"card-1": 5000}
    def verify_pin(self, card, pin): return pin == "1234"
    def balance(self, card): return self.balances[card]
    def debit(self, card, amount):
        if self.balances[card] < amount:
            return False
        self.balances[card] -= amount
        return True


def demo() -> None:
    print("ATM: State for the session, Chain of Responsibility for the notes")
    bank = FakeBank()
    atm = Atm(bank, CashDispenser(10, 10, 10))

    raises(lambda: atm.withdraw(500), RuntimeError, "cannot withdraw before inserting a card")
    atm.insert_card("card-1")
    raises(lambda: atm.withdraw(500), RuntimeError, "cannot withdraw before entering the PIN")
    raises(lambda: atm.enter_pin("0000"), ValueError, "wrong PIN is rejected")
    atm.enter_pin("1234")
    check(atm.state_name == "AuthenticatedState", "correct PIN moves to Authenticated")

    notes = atm.withdraw(1300)
    check(notes == {500: 2, 200: 1, 100: 1}, "1300 = 2 x 500 + 1 x 200 + 1 x 100")
    check(bank.balance("card-1") == 3700, "account debited by 1300")
    raises(lambda: atm.withdraw(150), ValueError, "amount must be a multiple of 100")
    raises(lambda: atm.withdraw(9000), RuntimeError, "cannot withdraw more than the balance")
    atm.eject()
    check(atm.state_name == "IdleState", "eject returns to Idle")

    atm.insert_card("card-1")
    for _ in range(2):
        try:
            atm.enter_pin("9999")
        except ValueError:
            pass
    raises(lambda: atm.enter_pin("9999"), RuntimeError, "third wrong PIN retains the card")
    check(atm.retained_cards == ["card-1"] and atm.state_name == "IdleState", "card kept, machine back to Idle")

    tricky = CashDispenser(1, 3, 0)
    raises(lambda: tricky.plan(600), CannotDispense, "greedy chain fails 600 from {500x1, 200x3} even though 3 x 200 works (a DP/backtracking search would fix it)")

    low = Atm(FakeBank(), CashDispenser(1, 0, 0))
    low.insert_card("card-1"); low.enter_pin("1234")
    raises(lambda: low.withdraw(1000), CannotDispense, "machine cannot pay 1000 with one 500 note")
    check(low.dispenser.count500 == 1 and low.bank.balance("card-1") == 5000, "failed withdrawal changed neither the notes nor the balance")


if __name__ == "__main__":
    demo()
