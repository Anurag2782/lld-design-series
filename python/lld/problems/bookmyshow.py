"""BookMyShow: timed seat locks, all-or-nothing, no double booking."""
import threading
import uuid
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from enum import Enum
from typing import Callable, Dict, List, Optional

from lld.common import check, raises


class SeatStatus(Enum):
    AVAILABLE = "AVAILABLE"
    LOCKED = "LOCKED"
    BOOKED = "BOOKED"


class SeatUnavailable(Exception):
    pass


class ShowSeat:
    """A seat FOR ONE SHOW. (The physical Seat has no status; availability belongs to the show.)"""

    def __init__(self, seat_id: str):
        self.seat_id = seat_id
        self._status = SeatStatus.AVAILABLE
        self._locked_by: Optional[str] = None
        self._expiry: Optional[datetime] = None
        self._lock = threading.Lock()

    def try_lock(self, user_id: str, now: datetime, hold: timedelta) -> bool:
        with self._lock:
            if self._status == SeatStatus.LOCKED and now > self._expiry:
                self._status = SeatStatus.AVAILABLE            # expired lock is free
            if self._status != SeatStatus.AVAILABLE:
                return False
            self._status, self._locked_by, self._expiry = SeatStatus.LOCKED, user_id, now + hold
            return True

    def confirm(self, user_id: str, now: datetime) -> bool:
        with self._lock:
            if self._status != SeatStatus.LOCKED or user_id != self._locked_by or now > self._expiry:
                return False
            self._status = SeatStatus.BOOKED
            return True

    def release(self, user_id: str) -> None:
        with self._lock:
            if self._status == SeatStatus.LOCKED and user_id == self._locked_by:
                self._status = SeatStatus.AVAILABLE

    @property
    def status(self) -> SeatStatus:
        with self._lock:
            return self._status


class Show:
    def __init__(self, seat_ids: List[str]):
        self._seats: Dict[str, ShowSeat] = {i: ShowSeat(i) for i in seat_ids}

    def seat(self, seat_id: str) -> ShowSeat:
        if seat_id not in self._seats:
            raise ValueError("No such seat: " + seat_id)
        return self._seats[seat_id]


@dataclass(frozen=True)
class Booking:
    id: str
    user_id: str
    seat_ids: List[str]


HOLD = timedelta(minutes=5)


class BookingService:
    def __init__(self, clock: Callable[[], datetime]): self._clock = clock

    def lock_seats(self, show: Show, seat_ids: List[str], user_id: str) -> List[ShowSeat]:
        """All-or-nothing. Each try_lock holds ONE seat's lock briefly and never waits, so no deadlock."""
        locked: List[ShowSeat] = []
        for sid in seat_ids:
            s = show.seat(sid)
            if s.try_lock(user_id, self._clock(), HOLD):
                locked.append(s)
            else:
                for l in locked:
                    l.release(user_id)                          # roll back what we already took
                raise SeatUnavailable("Seat unavailable: " + sid)
        return locked

    def confirm(self, show: Show, seat_ids: List[str], user_id: str) -> Booking:
        for sid in seat_ids:
            if not show.seat(sid).confirm(user_id, self._clock()):
                raise SeatUnavailable("Seat unavailable: " + sid)
        return Booking(str(uuid.uuid4()), user_id, seat_ids)


def demo() -> None:
    print("BookMyShow: timed seat locks, all-or-nothing, no double booking")
    now = {"t": datetime(2026, 11, 20, 18, 0, tzinfo=timezone.utc)}
    service = BookingService(lambda: now["t"])
    show = Show(["C7", "C8", "C9"])

    service.lock_seats(show, ["C7", "C8"], "alice")
    check(show.seat("C7").status == SeatStatus.LOCKED, "alice holds C7 and C8")
    raises(lambda: service.lock_seats(show, ["C7"], "bob"), SeatUnavailable, "bob cannot take a locked seat")

    raises(lambda: service.lock_seats(show, ["C9", "C8"], "bob"), SeatUnavailable, "bob wants C9 + C8: fails on C8")
    check(show.seat("C9").status == SeatStatus.AVAILABLE, "all-or-nothing: C9 was released, not left half-held")

    b = service.confirm(show, ["C7", "C8"], "alice")
    check(show.seat("C7").status == SeatStatus.BOOKED and len(b.seat_ids) == 2, "alice confirms within the hold time")
    raises(lambda: service.confirm(show, ["C9"], "bob"), SeatUnavailable, "cannot confirm seats you never locked")

    service.lock_seats(show, ["C9"], "bob")
    now["t"] += timedelta(minutes=6)
    raises(lambda: service.confirm(show, ["C9"], "bob"), SeatUnavailable, "an expired lock cannot be confirmed")
    service.lock_seats(show, ["C9"], "carol")
    check(show.seat("C9").status == SeatStatus.LOCKED, "after expiry the seat is free again for carol")

    # Concurrency: 100 users click the same seat at once. Exactly one wins.
    hot = Show(["A1"])
    live = BookingService(lambda: datetime.now(timezone.utc))

    def click(i: int) -> bool:
        try:
            live.lock_seats(hot, ["A1"], f"user{i}")
            return True
        except SeatUnavailable:
            return False

    with ThreadPoolExecutor(max_workers=16) as pool:
        winners = sum(pool.map(click, range(100)))
    check(winners == 1, "100 simultaneous clicks, exactly one lock winner")


if __name__ == "__main__":
    demo()
