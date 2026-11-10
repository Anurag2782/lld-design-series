"""Template Method: fixed skeleton, variable steps."""
from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Callable, List, TypeVar

from lld.common import check, raises

T = TypeVar("T")


@dataclass(frozen=True)
class Row:
    name: str
    qty: int

    @property
    def is_valid(self) -> bool:
        return bool(self.name.strip()) and self.qty >= 0


@dataclass(frozen=True)
class ImportResult:
    imported: int
    rejected: int


class DataImporter(ABC):
    """The base class owns the ALGORITHM; subclasses fill in the steps."""

    def run(self, lines: List[str]) -> ImportResult:          # the template: don't override this
        parsed = self.parse(lines)
        valid = self.validate(parsed)
        self.save(valid)
        self.after_save(valid)
        return ImportResult(len(valid), len(parsed) - len(valid))

    @abstractmethod
    def parse(self, lines: List[str]) -> List[Row]: ...           # required step

    @abstractmethod
    def save(self, rows: List[Row]) -> None: ...                  # required step

    def validate(self, rows: List[Row]) -> List[Row]:             # default step (overridable)
        return [r for r in rows if r.is_valid]

    def after_save(self, rows: List[Row]) -> None:                # hook: does nothing by default
        pass


class CsvImporter(DataImporter):
    def __init__(self): self.saved: List[Row] = []
    def parse(self, lines): return [Row(a.strip(), int(b)) for a, b in (l.split(",") for l in lines)]
    def save(self, rows): self.saved.extend(rows)


class PipeImporter(DataImporter):
    def __init__(self): self.saved: List[Row] = []; self.audit: List[str] = []
    def parse(self, lines): return [Row(a.strip(), int(b)) for a, b in (l.split("|") for l in lines)]
    def validate(self, rows): return [r for r in super().validate(rows) if r.qty <= 1000]   # extra rule
    def save(self, rows): self.saved.extend(rows)
    def after_save(self, rows): self.audit.append(f"saved {len(rows)}")


class ConnectionTemplate:
    """Callback flavour (what Spring's JdbcTemplate does; Python's `with` blocks are the same idea):
    the template owns open/close, YOU pass only the part that varies."""

    def __init__(self): self.events: List[str] = []

    def execute(self, callback: Callable[[], T]) -> T:
        self.events.append("open")
        try:
            return callback()
        finally:
            self.events.append("close")                          # always runs, even on exception


def demo() -> None:
    print("Template Method: fixed skeleton, variable steps")
    csv = CsvImporter()
    check(csv.run(["pen, 10", "book, 3", "bad, -1"]) == ImportResult(2, 1), "CSV: 2 imported, 1 rejected by the default validate()")
    pipe = PipeImporter()
    check(pipe.run(["pen | 10", "bulk | 5000"]) == ImportResult(1, 1), "pipe importer overrides validate() with an extra rule")
    check(pipe.audit == ["saved 1"], "pipe importer uses the after_save() hook")

    template = ConnectionTemplate()
    check(template.execute(lambda: 42) == 42 and template.events == ["open", "close"], "callback template opens and closes around your code")

    def boom():
        raise RuntimeError("boom")
    raises(lambda: template.execute(boom), RuntimeError, "exceptions still propagate")
    check(template.events == ["open", "close", "open", "close"], "...and the resource is still closed after a failure")


if __name__ == "__main__":
    demo()
