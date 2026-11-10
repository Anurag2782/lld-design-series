"""Chain of Responsibility: small independent checks in a pipeline."""
from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Optional, Set

from lld.common import check


@dataclass(frozen=True)
class BookingRequest:
    doctor: str
    hour: int
    patient: str


class ValidationError(Exception):
    pass


class Validator(ABC):
    """Each handler does ONE check, then passes the request on."""

    def __init__(self): self._next: Optional["Validator"] = None

    def then(self, nxt: "Validator") -> "Validator":
        self._next = nxt
        return nxt                      # returns the new tail so calls can be chained

    def validate(self, r: BookingRequest) -> None:
        self._check(r)
        if self._next:
            self._next.validate(r)

    @abstractmethod
    def _check(self, r: BookingRequest) -> None: ...


class WorkingHoursCheck(Validator):
    def _check(self, r):
        if r.hour < 9 or r.hour >= 17:
            raise ValidationError("Doctor not available at this time")


class SlotFreeCheck(Validator):
    def __init__(self, taken: Set[str]): super().__init__(); self._taken = taken
    def _check(self, r):
        if f"{r.doctor}@{r.hour}" in self._taken:
            raise ValidationError("Slot already booked")


class DuplicateBookingCheck(Validator):
    def __init__(self, patient_hours: Set[str]): super().__init__(); self._ph = patient_hours
    def _check(self, r):
        if f"{r.patient}@{r.hour}" in self._ph:
            raise ValidationError("Patient already has a booking then")


def _message_of(action) -> str:
    try:
        action()
    except ValidationError as e:
        return str(e)
    return "none"


def demo() -> None:
    print("Chain of Responsibility: small independent checks in a pipeline")
    chain = WorkingHoursCheck()
    chain.then(SlotFreeCheck({"drA@10"})).then(DuplicateBookingCheck({"asha@11"}))

    chain.validate(BookingRequest("drA", 12, "ravi"))
    check(True, "a valid request passes every handler")
    check(_message_of(lambda: chain.validate(BookingRequest("drA", 20, "ravi"))) == "Doctor not available at this time", "rejected by WorkingHoursCheck")
    check(_message_of(lambda: chain.validate(BookingRequest("drA", 10, "ravi"))) == "Slot already booked", "rejected by SlotFreeCheck")
    check(_message_of(lambda: chain.validate(BookingRequest("drB", 11, "asha"))) == "Patient already has a booking then", "rejected by DuplicateBookingCheck")


if __name__ == "__main__":
    demo()
