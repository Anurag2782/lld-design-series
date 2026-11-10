"""Decorator: add behavior by wrapping, same interface."""
from abc import ABC, abstractmethod
from typing import Dict, List

from lld.common import check


class AppointmentRepository(ABC):
    @abstractmethod
    def find_by_id(self, id: int) -> str: ...


class DbRepository(AppointmentRepository):
    def __init__(self): self.hits = 0
    def find_by_id(self, id):
        self.hits += 1
        return f"appointment-{id}"


class CachingRepository(AppointmentRepository):
    def __init__(self, inner: AppointmentRepository):
        self._inner, self._cache = inner, {}
    def find_by_id(self, id):
        if id not in self._cache:
            self._cache[id] = self._inner.find_by_id(id)
        return self._cache[id]


class LoggingRepository(AppointmentRepository):
    def __init__(self, inner: AppointmentRepository):
        self._inner, self.log = inner, []
    def find_by_id(self, id):
        self.log.append(f"find_by_id({id})")
        return self._inner.find_by_id(id)


def demo() -> None:
    print("Decorator: add behavior by wrapping, same interface")
    db = DbRepository()
    repo = LoggingRepository(CachingRepository(db))
    repo.find_by_id(1)
    repo.find_by_id(1)
    check(db.hits == 1, "caching decorator: second call never reached the database")
    check(len(repo.log) == 2, "logging is outermost: it saw both calls (including the cache hit)")

    db2 = DbRepository()
    inner = LoggingRepository(db2)
    cached_outside = CachingRepository(inner)
    cached_outside.find_by_id(1)
    cached_outside.find_by_id(1)
    check(len(inner.log) == 1, "swap the wrapper order and logging only sees cache misses")
    # Python's own function decorators (@functools.lru_cache, @wraps) are the same idea applied to functions.


if __name__ == "__main__":
    demo()
