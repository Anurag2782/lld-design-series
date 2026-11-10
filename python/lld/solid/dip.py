"""DIP: depend on abstractions; inject dependencies."""
from typing import Protocol

from lld.common import check


class MessageSender(Protocol):
    """The abstraction the HIGH-LEVEL code owns."""
    def send(self, to: str, message: str) -> None: ...


class EmailSender:
    def send(self, to, message): print(f"  EMAIL to {to}: {message}")


class SmsSender:
    def send(self, to, message): print(f"  SMS to {to}: {message}")


class FakeSender:
    """Test double: records calls instead of touching real infrastructure."""
    def __init__(self): self.sent = []
    def send(self, to, message): self.sent.append(f"{to}|{message}")


class NotificationService:
    """High-level policy: knows WHAT it needs, not WHO provides it."""
    def __init__(self, sender: MessageSender): self._sender = sender
    def order_placed(self, to: str, order_id: str) -> None:
        self._sender.send(to, f"Order {order_id} placed")


def demo() -> None:
    print("DIP: depend on abstractions, inject dependencies")
    fake = FakeSender()
    NotificationService(fake).order_placed("a@x.com", "42")
    check(fake.sent == ["a@x.com|Order 42 placed"], "unit test with zero real infrastructure")
    NotificationService(SmsSender()).order_placed("9999", "43")
    NotificationService(EmailSender()).order_placed("b@x.com", "44")
    check(True, "same service works with SMS or email")


if __name__ == "__main__":
    demo()
