"""Splitwise: strategy per split type, paise not floats, simplify debts."""
import heapq
import threading
import uuid
from abc import ABC, abstractmethod
from collections import defaultdict
from dataclasses import dataclass
from enum import Enum
from typing import Dict, List, Optional

from lld.common import check, raises


@dataclass(frozen=True)
class User:
    id: str
    name: str


@dataclass(frozen=True)
class Split:
    user: User
    paise: int


@dataclass(frozen=True)
class Expense:
    id: str
    paid_by: User
    total_paise: int
    splits: List[Split]


@dataclass(frozen=True)
class Payment:
    from_id: str
    to_id: str
    paise: int


class SplitType(Enum):
    EQUAL = "EQUAL"
    EXACT = "EXACT"
    PERCENT = "PERCENT"


class SplitStrategy(ABC):
    """How an expense is divided. Shares ALWAYS add up to the total."""
    @abstractmethod
    def split(self, total: int, users: List[User], values: Optional[List[int]]) -> List[Split]: ...


class EqualSplit(SplitStrategy):
    def split(self, total, users, values=None):
        base, extra = divmod(total, len(users))         # leftover paise go to the first few people
        return [Split(u, base + (1 if i < extra else 0)) for i, u in enumerate(users)]


class ExactSplit(SplitStrategy):
    def split(self, total, users, values):
        if values is None or len(values) != len(users):
            raise ValueError("one amount per user")
        if sum(values) != total:
            raise ValueError("amounts must add up to the total")
        return [Split(u, v) for u, v in zip(users, values)]


class PercentSplit(SplitStrategy):
    def split(self, total, users, values):
        if values is None or len(values) != len(users):
            raise ValueError("one percent per user")
        if sum(values) != 100:
            raise ValueError("percentages must add up to 100")
        shares = [total * p // 100 for p in values]
        leftover = total - sum(shares)                  # rounding remainder
        i = 0
        while leftover > 0:
            shares[i % len(shares)] += 1
            leftover -= 1
            i += 1
        return [Split(u, s) for u, s in zip(users, shares)]


class BalanceSheet:
    def __init__(self):
        self._lock = threading.Lock()
        self._owes: Dict[str, Dict[str, int]] = defaultdict(lambda: defaultdict(int))   # debtor -> creditor -> paise

    def apply(self, e: Expense) -> None:
        with self._lock:
            for s in e.splits:
                if s.user == e.paid_by:
                    continue                            # you don't owe yourself
                self._owes[s.user.id][e.paid_by.id] += s.paise

    def owed(self, debtor: str, creditor: str) -> int:
        with self._lock:
            return self._owes.get(debtor, {}).get(creditor, 0)

    def net(self) -> Dict[str, int]:
        """Positive = is owed money, negative = owes money."""
        with self._lock:
            net: Dict[str, int] = defaultdict(int)
            for debtor, m in self._owes.items():
                for creditor, amt in m.items():
                    net[debtor] -= amt
                    net[creditor] += amt
            return dict(sorted(net.items()))


def simplify(net: Dict[str, int]) -> List[Payment]:
    """Greedy: biggest creditor is paid by biggest debtor. At most (n - 1) payments."""
    creditors = [(-amt, uid) for uid, amt in net.items() if amt > 0]    # max-heap via negation
    debtors = [(amt, uid) for uid, amt in net.items() if amt < 0]       # most negative first
    heapq.heapify(creditors)
    heapq.heapify(debtors)
    out: List[Payment] = []
    while creditors and debtors:
        c_neg, c_id = heapq.heappop(creditors)
        d_amt, d_id = heapq.heappop(debtors)
        credit, debt = -c_neg, -d_amt
        pay = min(credit, debt)
        out.append(Payment(d_id, c_id, pay))
        if credit > pay:
            heapq.heappush(creditors, (-(credit - pay), c_id))
        if debt > pay:
            heapq.heappush(debtors, (-(debt - pay), d_id))
    return out


class ExpenseService:
    def __init__(self, strategies: Dict[SplitType, SplitStrategy], sheet: BalanceSheet):
        self._strategies, self._sheet = strategies, sheet

    def add_expense(self, paid_by: User, total: int, users: List[User], kind: SplitType, values=None) -> Expense:
        splits = self._strategies[kind].split(total, users, values)
        e = Expense(str(uuid.uuid4()), paid_by, total, splits)
        self._sheet.apply(e)
        return e


def demo() -> None:
    print("Splitwise: strategy per split type, paise not floats, simplify debts")
    a, p, r = User("a", "Amit"), User("p", "Priya"), User("r", "Ravi")

    equal = EqualSplit().split(10000, [a, p, r])
    check([s.paise for s in equal] == [3334, 3333, 3333], "Rs100 / 3 = 3334 + 3333 + 3333 paise")
    check(sum(s.paise for s in equal) == 10000, "shares add up to the total exactly (no lost paisa)")

    pct = PercentSplit().split(10001, [a, p, r], [50, 30, 20])
    check(sum(s.paise for s in pct) == 10001 and pct[0].paise == 5001, "percent split keeps the rounding remainder")
    raises(lambda: ExactSplit().split(100, [a, p], [60, 30]), ValueError, "exact amounts must add up to the total")
    raises(lambda: PercentSplit().split(100, [a, p], [60, 30]), ValueError, "percentages must add up to 100")

    sheet = BalanceSheet()
    service = ExpenseService({SplitType.EQUAL: EqualSplit(), SplitType.EXACT: ExactSplit(), SplitType.PERCENT: PercentSplit()}, sheet)
    service.add_expense(p, 90000, [p, a, r], SplitType.EQUAL)   # Priya pays Rs900 dinner
    check(sheet.owed("a", "p") == 30000 and sheet.owed("r", "p") == 30000, "Amit and Ravi each owe Priya Rs300")
    check(sheet.owed("p", "p") == 0, "nobody owes themselves")

    plan = simplify({"A": -500, "B": 0, "C": 700, "D": -200})   # A owes B 500, D owes B 200, B owes C 700
    check(len(plan) == 2, "three debts collapse into two payments")
    check(Payment("A", "C", 500) in plan and Payment("D", "C", 200) in plan, "A pays C 500, D pays C 200")


if __name__ == "__main__":
    demo()
