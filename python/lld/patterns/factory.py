"""Factory: simple factory, factory method, abstract factory."""
from abc import ABC, abstractmethod

from lld.common import check, raises


# ---- Simple Factory ----
class Exporter(ABC):
    @abstractmethod
    def export(self, report: str) -> str: ...


class PdfExporter(Exporter):
    def export(self, report): return "PDF:" + report


class CsvExporter(Exporter):
    def export(self, report): return "CSV:" + report


class ExcelExporter(Exporter):
    def export(self, report): return "XLSX:" + report


class ExporterFactory:
    _registry = {"PDF": PdfExporter, "CSV": CsvExporter, "EXCEL": ExcelExporter}

    @classmethod
    def create(cls, kind: str) -> Exporter:
        try:
            return cls._registry[kind.upper()]()
        except KeyError:
            raise ValueError("Unknown: " + kind) from None


# ---- Factory Method: a SUBCLASS decides which object is created ----
class ReportJob(ABC):
    @abstractmethod
    def create_exporter(self) -> Exporter: ...

    def run(self, report: str) -> str:
        return self.create_exporter().export(report)


class MonthlyJob(ReportJob):
    def create_exporter(self): return PdfExporter()


class DailyJob(ReportJob):
    def create_exporter(self): return CsvExporter()


# ---- Abstract Factory: a FAMILY of related objects that must stay compatible ----
class InfraFactory(ABC):
    @abstractmethod
    def put(self, key: str) -> str: ...
    @abstractmethod
    def publish(self, msg: str) -> str: ...


class LocalFactory(InfraFactory):
    def put(self, key): return "disk:" + key
    def publish(self, msg): return "in-memory:" + msg


class CloudFactory(InfraFactory):
    def put(self, key): return "bucket:" + key
    def publish(self, msg): return "cloud-queue:" + msg


def demo() -> None:
    print("Factory: simple, method, abstract")
    check(ExporterFactory.create("pdf").export("r") == "PDF:r", "simple factory builds a PDF exporter")
    check(ExporterFactory.create("CSV").export("r") == "CSV:r", "simple factory builds a CSV exporter")
    raises(lambda: ExporterFactory.create("doc"), ValueError, "unknown type is rejected")
    check(MonthlyJob().run("sales") == "PDF:sales", "factory method: monthly job exports PDF")
    check(DailyJob().run("sales") == "CSV:sales", "factory method: daily job exports CSV")
    local, cloud = LocalFactory(), CloudFactory()
    check(local.put("a") == "disk:a" and local.publish("m") == "in-memory:m", "abstract factory: local family is consistent")
    check(cloud.put("a") == "bucket:a" and cloud.publish("m") == "cloud-queue:m", "abstract factory: cloud family is consistent")


if __name__ == "__main__":
    demo()
