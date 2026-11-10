"""Library: Book vs BookCopy, fine strategy, return listener."""
import threading
from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from datetime import date, timedelta
from typing import Callable, List, Optional

from lld.common import check, raises


@dataclass(frozen=True)
class Book:
    isbn: str
    title: str
    author: str


class BookCopy:
    """A physical copy. (Book = the title; BookCopy = the thing you borrow. Status belongs to the COPY.)"""

    def __init__(self, barcode: str, book: Book):
        self.barcode, self.book = barcode, book
        self._available = True
        self._lock = threading.Lock()

    def try_checkout(self) -> bool:
        with self._lock:
            if not self._available:
                return False
            self._available = False
            return True

    def checkin(self) -> None:
        with self._lock:
            self._available = True

    @property
    def is_available(self) -> bool:
        with self._lock:
            return self._available


class Member:
    def __init__(self, member_id: str):
        self.id = member_id
        self.active_loans: List["Loan"] = []


@dataclass(frozen=True, eq=False)
class Loan:
    member: Member
    copy: BookCopy
    issued: date
    due: date


class FinePolicy(ABC):
    """Strategy: fines are money in paise."""
    @abstractmethod
    def fine(self, loan: Loan, returned: date) -> int: ...


class PerDayFine(FinePolicy):
    def __init__(self, rate_per_day: int, grace_days: int): self._rate, self._grace = rate_per_day, grace_days

    def fine(self, loan, returned):
        late = (returned - loan.due).days
        return max(0, late - self._grace) * self._rate


class LoanService:
    MAX_LOANS = 3
    LOAN_DAYS = 14

    def __init__(self, fine_policy: FinePolicy):
        self._fine_policy = fine_policy
        self._listeners: List[Callable[[Book], None]] = []     # Observer: told when a copy comes back

    def add_listener(self, listener: Callable[[Book], None]) -> None:
        self._listeners.append(listener)

    def issue(self, m: Member, copy: BookCopy, today: date) -> Loan:
        if len(m.active_loans) >= self.MAX_LOANS:
            raise RuntimeError("Loan limit reached")
        if not copy.try_checkout():                              # atomic: no double issue
            raise RuntimeError("Copy already issued")
        loan = Loan(m, copy, today, today + timedelta(days=self.LOAN_DAYS))
        m.active_loans.append(loan)
        return loan

    def return_copy(self, loan: Loan, today: date) -> int:
        fine = self._fine_policy.fine(loan, today)
        loan.member.active_loans.remove(loan)
        loan.copy.checkin()
        for l in self._listeners:
            l(loan.copy.book)
        return fine


class Catalog:
    def __init__(self): self._copies: List[BookCopy] = []
    def add(self, c: BookCopy): self._copies.append(c)

    def search(self, text: str) -> List[Book]:
        t = text.lower()
        seen: List[Book] = []
        for c in self._copies:
            b = c.book
            if (t in b.title.lower() or t in b.author.lower()) and b not in seen:
                seen.append(b)
        return seen

    def available_copy_of(self, book: Book) -> Optional[BookCopy]:
        return next((c for c in self._copies if c.book == book and c.is_available), None)


def demo() -> None:
    print("Library: Book vs BookCopy, fine strategy, return listener")
    dune, emma = Book("111", "Dune", "Herbert"), Book("222", "Emma", "Austen")
    d1, d2 = BookCopy("D1", dune), BookCopy("D2", dune)
    catalog = Catalog()
    for c in (d1, d2, BookCopy("E1", emma)):
        catalog.add(c)

    check(catalog.search("herb") == [dune], "search by author substring finds the title once")
    service = LoanService(PerDayFine(200, 2))
    notified: List[str] = []
    service.add_listener(lambda b: notified.append(b.title))

    asha = Member("asha")
    day0 = date(2026, 11, 1)
    l1 = service.issue(asha, d1, day0)
    check(l1.due == day0 + timedelta(days=14), "due date is 14 days after issue")
    raises(lambda: service.issue(Member("ravi"), d1, day0), RuntimeError, "the same physical copy cannot be issued twice")
    check(catalog.available_copy_of(dune) is d2, "the second copy of the title is still available")

    service.issue(asha, d2, day0)
    service.issue(asha, BookCopy("E1", emma), day0)
    raises(lambda: service.issue(asha, BookCopy("X", emma), day0), RuntimeError, "limit of 3 active loans enforced")

    check(service.return_copy(l1, day0 + timedelta(days=14)) == 0, "returned on the due date: no fine")
    check(notified == ["Dune"], "listener was told a copy of Dune came back")
    l2 = asha.active_loans[0]
    check(service.return_copy(l2, l2.due + timedelta(days=5)) == 600, "5 days late, 2 days grace: 3 billable days x 200 paise = 600")


if __name__ == "__main__":
    demo()
