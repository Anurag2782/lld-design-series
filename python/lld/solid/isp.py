"""ISP: small, role-based interfaces."""
from typing import Protocol, runtime_checkable

from lld.common import check


@runtime_checkable
class Treats(Protocol):
    def treat_patient(self) -> str: ...


@runtime_checkable
class Prescribes(Protocol):
    def prescribe(self, drug: str) -> str: ...


@runtime_checkable
class HandlesBilling(Protocol):
    def handle_billing(self) -> str: ...


class Doctor:
    def treat_patient(self): return "examined"
    def prescribe(self, drug): return f"prescribed {drug}"


class Receptionist:
    """Implements ONLY what she does. No forced empty methods."""
    def handle_billing(self): return "invoice issued"


def demo() -> None:
    print("ISP: small role-based interfaces")
    d, r = Doctor(), Receptionist()
    check(d.treat_patient() == "examined", "doctor treats")
    check(d.prescribe("paracetamol") == "prescribed paracetamol", "doctor prescribes")
    check(r.handle_billing() == "invoice issued", "receptionist bills")
    check(not isinstance(r, Prescribes), "receptionist is not forced to prescribe")


if __name__ == "__main__":
    demo()
