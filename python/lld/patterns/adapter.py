"""Adapter: make an incompatible API fit your interface."""
from abc import ABC, abstractmethod
from dataclasses import dataclass
from decimal import Decimal
from typing import Optional

from lld.common import check


# ---- OUR interface: what our code wants ----
@dataclass(frozen=True)
class PaymentResult:
    success: bool
    reference: str


class PaymentGateway(ABC):
    @abstractmethod
    def charge(self, user_id: str, paise: int) -> PaymentResult: ...


# ---- THEIR SDK: we cannot change it (made-up vendor for the example) ----
@dataclass(frozen=True)
class AcmeResponse:
    ok: bool
    txn_id: Optional[str]
    error_code: Optional[str]


class AcmePayClient:
    def make_payment(self, customer_ref: str, rupees: Decimal, currency: str) -> AcmeResponse:
        if rupees > Decimal("10000"):
            return AcmeResponse(False, None, "LIMIT_EXCEEDED")
        return AcmeResponse(True, f"txn-{customer_ref}-{rupees:.2f}", None)


class AcmePayAdapter(PaymentGateway):
    """The adapter translates paise<->rupees and their response<->ours."""
    def __init__(self, client: AcmePayClient): self._client = client

    def charge(self, user_id, paise):
        rupees = Decimal(paise) / Decimal(100)          # 1234 paise -> 12.34 exactly
        r = self._client.make_payment(user_id, rupees, "INR")
        return PaymentResult(True, r.txn_id) if r.ok else PaymentResult(False, r.error_code)


class FakeGateway(PaymentGateway):
    def charge(self, user_id, paise): return PaymentResult(True, "fake")


def demo() -> None:
    print("Adapter: make an incompatible API fit your interface")
    gateway: PaymentGateway = AcmePayAdapter(AcmePayClient())
    ok = gateway.charge("u1", 1234)
    check(ok.success and ok.reference == "txn-u1-12.34", "paise converted to rupees exactly: 1234 -> 12.34")
    big = gateway.charge("u1", 2_000_000)
    check(not big.success and big.reference == "LIMIT_EXCEEDED", "vendor error mapped into our PaymentResult")
    check(FakeGateway().charge("x", 1).reference == "fake", "callers depend on the interface, so tests use a fake")


if __name__ == "__main__":
    demo()
