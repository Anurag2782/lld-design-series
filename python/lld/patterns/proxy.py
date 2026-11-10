"""Proxy controls access. Decorator adds behavior."""
from abc import ABC, abstractmethod
from typing import Callable, Optional, Set

from lld.common import check, raises


class ReportService(ABC):
    @abstractmethod
    def generate(self, id: int) -> str: ...


class RealReportService(ReportService):
    def generate(self, id): return f"report-{id}"


class AccessDenied(Exception):
    pass


class SecureReportProxy(ReportService):
    """Protection proxy: decides WHETHER the real object is called."""
    def __init__(self, real: ReportService, roles: Set[str]): self._real, self._roles = real, roles
    def generate(self, id):
        if "ADMIN" not in self._roles:
            raise AccessDenied("admin role required")
        return self._real.generate(id)


class LazyReportProxy(ReportService):
    """Virtual proxy: decides WHEN the real object is created."""
    def __init__(self, factory: Callable[[], ReportService]):
        self._factory, self._real = factory, None
    def generate(self, id):
        if self._real is None:
            self._real = self._factory()
        return self._real.generate(id)


class UppercaseDecorator(ReportService):
    """Decorator for contrast: ALWAYS calls the real object and adds something."""
    def __init__(self, inner: ReportService): self._inner = inner
    def generate(self, id): return self._inner.generate(id).upper()


def demo() -> None:
    print("Proxy: control access. Decorator: add behavior")
    real = RealReportService()
    raises(lambda: SecureReportProxy(real, {"USER"}).generate(1), AccessDenied, "protection proxy blocks a non-admin; real service never called")
    check(SecureReportProxy(real, {"ADMIN"}).generate(1) == "report-1", "admin is allowed through")

    created = {"n": 0}
    def factory():
        created["n"] += 1
        return real
    lazy = LazyReportProxy(factory)
    check(created["n"] == 0, "virtual proxy: nothing created yet")
    lazy.generate(1); lazy.generate(2)
    check(created["n"] == 1, "virtual proxy: created once, on first use")
    check(UppercaseDecorator(real).generate(3) == "REPORT-3", "decorator always calls through and adds behavior")


if __name__ == "__main__":
    demo()
