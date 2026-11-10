import { check, throws } from "../common.ts";

// ---- Simple Factory ----
interface Exporter { export(report: string): string }
class PdfExporter implements Exporter { export(r: string) { return "PDF:" + r; } }
class CsvExporter implements Exporter { export(r: string) { return "CSV:" + r; } }
class ExcelExporter implements Exporter { export(r: string) { return "XLSX:" + r; } }

function createExporter(type: string): Exporter {
  switch (type.toUpperCase()) {
    case "PDF": return new PdfExporter();
    case "CSV": return new CsvExporter();
    case "EXCEL": return new ExcelExporter();
    default: throw new Error("Unknown: " + type);
  }
}

// ---- Factory Method: a SUBCLASS decides which object is created ----
abstract class ReportJob {
  protected abstract createExporter(): Exporter;
  run(report: string): string { return this.createExporter().export(report); }
}
class MonthlyJob extends ReportJob { protected createExporter(): Exporter { return new PdfExporter(); } }
class DailyJob extends ReportJob { protected createExporter(): Exporter { return new CsvExporter(); } }

// ---- Abstract Factory: a FAMILY of related objects that must stay compatible ----
interface Storage { put(key: string): string }
interface MessageQueue { publish(msg: string): string }
interface InfraFactory { storage(): Storage; queue(): MessageQueue }

const localFactory: InfraFactory = {
  storage: () => ({ put: (k) => "disk:" + k }),
  queue: () => ({ publish: (m) => "in-memory:" + m }),
};
const cloudFactory: InfraFactory = {
  storage: () => ({ put: (k) => "bucket:" + k }),
  queue: () => ({ publish: (m) => "cloud-queue:" + m }),
};

export function demo(): void {
  console.log("Factory: simple, method, abstract");
  check(createExporter("pdf").export("r") === "PDF:r", "simple factory builds a PDF exporter");
  check(createExporter("CSV").export("r") === "CSV:r", "simple factory builds a CSV exporter");
  throws(() => createExporter("doc"), "unknown type is rejected");
  check(new MonthlyJob().run("sales") === "PDF:sales", "factory method: monthly job exports PDF");
  check(new DailyJob().run("sales") === "CSV:sales", "factory method: daily job exports CSV");
  check(localFactory.storage().put("a") === "disk:a" && localFactory.queue().publish("m") === "in-memory:m", "abstract factory: local family is consistent");
  check(cloudFactory.storage().put("a") === "bucket:a" && cloudFactory.queue().publish("m") === "cloud-queue:m", "abstract factory: cloud family is consistent");
}
