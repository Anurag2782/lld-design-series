"""Observer: one event, many independent reactions."""
from dataclasses import dataclass
from typing import Callable, List

from lld.common import check


@dataclass(frozen=True)
class Appointment:
    id: int
    patient: str


Observer = Callable[[Appointment], None]


class AppointmentService:
    """Subject: announces "something happened"; never knows who is listening."""

    def __init__(self):
        self._observers: List[Observer] = []
        self.observer_errors: List[str] = []

    def register(self, o: Observer) -> None:
        self._observers.append(o)

    def book(self, a: Appointment) -> None:
        # ... save to the database here ...
        for o in self._observers:
            try:
                o(a)
            except Exception as e:          # one bad observer must not block the rest
                self.observer_errors.append(str(e))


def _boom(a: Appointment) -> None:
    raise RuntimeError("calendar down")


def demo() -> None:
    print("Observer: one event, many independent reactions")
    log: List[str] = []
    service = AppointmentService()
    service.register(lambda a: log.append("sms:" + a.patient))
    service.register(_boom)
    service.register(lambda a: log.append(f"analytics:{a.id}"))

    service.book(Appointment(7, "Asha"))
    check(log == ["sms:Asha", "analytics:7"], "healthy observers still run")
    check(service.observer_errors == ["calendar down"], "failure was captured, not propagated")

    service.register(lambda a: log.append("doctor:" + a.patient))
    service.book(Appointment(8, "Ravi"))
    check("doctor:Ravi" in log, "new observer added without editing book()")


if __name__ == "__main__":
    demo()
