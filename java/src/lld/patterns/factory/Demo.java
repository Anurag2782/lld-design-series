package lld.patterns.factory;

import lld.common.Check;

// ---- Simple Factory: one place decides which concrete class to create ----
interface Exporter { String export(String report); }
class PdfExporter implements Exporter { public String export(String r) { return "PDF:" + r; } }
class CsvExporter implements Exporter { public String export(String r) { return "CSV:" + r; } }
class ExcelExporter implements Exporter { public String export(String r) { return "XLSX:" + r; } }

class ExporterFactory {
    static Exporter create(String type) {
        return switch (type.toUpperCase()) {
            case "PDF" -> new PdfExporter();
            case "CSV" -> new CsvExporter();
            case "EXCEL" -> new ExcelExporter();
            default -> throw new IllegalArgumentException("Unknown: " + type);
        };
    }
}

// ---- Factory Method: a SUBCLASS decides which object is created ----
abstract class ReportJob {
    abstract Exporter createExporter();
    String run(String report) { return createExporter().export(report); }
}
class MonthlyJob extends ReportJob { Exporter createExporter() { return new PdfExporter(); } }
class DailyJob extends ReportJob { Exporter createExporter() { return new CsvExporter(); } }

// ---- Abstract Factory: a FAMILY of related objects that must stay compatible ----
interface Storage { String put(String key); }
interface MessageQueue { String publish(String msg); }
interface InfraFactory { Storage storage(); MessageQueue queue(); }

class LocalFactory implements InfraFactory {
    public Storage storage() { return k -> "disk:" + k; }
    public MessageQueue queue() { return m -> "in-memory:" + m; }
}
class CloudFactory implements InfraFactory {
    public Storage storage() { return k -> "bucket:" + k; }
    public MessageQueue queue() { return m -> "cloud-queue:" + m; }
}

public class Demo {
    public static void main(String[] args) {
        System.out.println("Factory: simple, method, abstract");
        Check.that(ExporterFactory.create("pdf").export("r").equals("PDF:r"), "simple factory builds a PDF exporter");
        Check.that(ExporterFactory.create("CSV").export("r").equals("CSV:r"), "simple factory builds a CSV exporter");
        Check.throwsError(() -> ExporterFactory.create("doc"), IllegalArgumentException.class, "unknown type is rejected");

        Check.that(new MonthlyJob().run("sales").equals("PDF:sales"), "factory method: monthly job exports PDF");
        Check.that(new DailyJob().run("sales").equals("CSV:sales"), "factory method: daily job exports CSV");

        InfraFactory local = new LocalFactory(), cloud = new CloudFactory();
        Check.that(local.storage().put("a").equals("disk:a") && local.queue().publish("m").equals("in-memory:m"),
            "abstract factory: local family is consistent");
        Check.that(cloud.storage().put("a").equals("bucket:a") && cloud.queue().publish("m").equals("cloud-queue:m"),
            "abstract factory: cloud family is consistent");
    }
}
