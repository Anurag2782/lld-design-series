"""SRP: one class, one reason to change."""
from dataclasses import dataclass
from typing import Optional, Protocol

from lld.common import check, raises


@dataclass(frozen=True)
class Appointment:
    patient: str
    slot: Optional[str]


class AppointmentRepository(Protocol):
    def save(self, a: Appointment) -> None: ...


class NotificationService(Protocol):
    def confirm(self, a: Appointment) -> None: ...


class InvoiceGenerator(Protocol):
    def generate(self, a: Appointment) -> None: ...


class GodAppointmentService:
    """BEFORE: four reasons to change in one class (validation, DB, email, PDF)."""

    def book(self, a: Appointment) -> None:
        if a.slot is None:
            raise ValueError("slot required")
        print("INSERT INTO appointments ...")
        print("smtp.send(...)")
        print("pdf.generate(...)")


class AppointmentService:
    """AFTER: only coordinates. Each collaborator has exactly one reason to change."""

    def __init__(self, repo: AppointmentRepository, notifier: NotificationService, invoices: InvoiceGenerator):
        self._repo, self._notifier, self._invoices = repo, notifier, invoices

    def book(self, a: Appointment) -> None:
        self._validate(a)
        self._repo.save(a)
        self._notifier.confirm(a)
        self._invoices.generate(a)

    @staticmethod
    def _validate(a: Appointment) -> None:
        if a.slot is None:
            raise ValueError("slot required")


class _Recorder:
    def __init__(self, log, name):
        self._log, self._name = log, name

    def save(self, a): self._log.append(self._name)
    def confirm(self, a): self._log.append(self._name)
    def generate(self, a): self._log.append(self._name)


def demo() -> None:
    print("SRP: AppointmentService only coordinates")
    log: list = []
    service = AppointmentService(_Recorder(log, "saved"), _Recorder(log, "notified"), _Recorder(log, "invoiced"))
    service.book(Appointment("P1", "10:00"))
    check(log == ["saved", "notified", "invoiced"], "collaborators run in order")

    log.clear()
    raises(lambda: service.book(Appointment("P2", None)), ValueError, "invalid booking is rejected")
    check(log == [], "nothing saved/sent when validation fails")


if __name__ == "__main__":
    demo()
