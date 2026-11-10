"""Builder: readable construction, validation in build()."""
from dataclasses import dataclass, replace
from datetime import datetime, timedelta
from typing import Optional

from lld.common import check, raises


@dataclass(frozen=True)
class Appointment:
    patient_id: str
    doctor_id: str
    time: datetime
    notes: Optional[str] = None
    reminder: bool = True

    @staticmethod
    def builder() -> "AppointmentBuilder":
        return AppointmentBuilder()

    def to_builder(self) -> "AppointmentBuilder":
        """Copy with one change: the original stays immutable."""
        return AppointmentBuilder().patient_id(self.patient_id).doctor_id(self.doctor_id) \
            .time(self.time).notes(self.notes).reminder(self.reminder)


class AppointmentBuilder:
    def __init__(self):
        self._patient_id = self._doctor_id = self._time = self._notes = None
        self._reminder = True

    def patient_id(self, v): self._patient_id = v; return self
    def doctor_id(self, v): self._doctor_id = v; return self
    def time(self, v): self._time = v; return self
    def notes(self, v): self._notes = v; return self
    def reminder(self, v): self._reminder = v; return self

    def build(self) -> Appointment:
        """The gatekeeper: an invalid Appointment can never exist."""
        if self._patient_id is None or self._doctor_id is None or self._time is None:
            raise ValueError("patient_id, doctor_id and time are required")
        return Appointment(self._patient_id, self._doctor_id, self._time, self._notes, self._reminder)


def demo() -> None:
    print("Builder: readable construction, validation in build()")
    t = datetime(2026, 11, 20, 10, 0)
    a = Appointment.builder().patient_id("P1").doctor_id("D7").time(t).reminder(False).build()
    check(a.patient_id == "P1" and not a.reminder, "fields set by name, not position")

    moved = a.to_builder().time(t + timedelta(hours=2)).build()
    check(moved.time == t + timedelta(hours=2) and a.time == t, "to_builder copies; original unchanged")
    raises(lambda: Appointment.builder().patient_id("P1").build(), ValueError, "missing required fields are rejected in build()")
    # In Python, keyword arguments + frozen dataclasses + dataclasses.replace() often replace a builder:
    check(replace(a, notes="bring reports").notes == "bring reports", "idiomatic Python alternative: dataclasses.replace")


if __name__ == "__main__":
    demo()
