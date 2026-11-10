using Lld.Common;

namespace Lld.Patterns.Factory;

// ---- Simple Factory ----
interface IExporter { string Export(string report); }
class PdfExporter : IExporter { public string Export(string r) => "PDF:" + r; }
class CsvExporter : IExporter { public string Export(string r) => "CSV:" + r; }
class ExcelExporter : IExporter { public string Export(string r) => "XLSX:" + r; }

static class ExporterFactory
{
    public static IExporter Create(string type) => type.ToUpperInvariant() switch
    {
        "PDF" => new PdfExporter(),
        "CSV" => new CsvExporter(),
        "EXCEL" => new ExcelExporter(),
        _ => throw new ArgumentException("Unknown: " + type)
    };
}

// ---- Factory Method: a SUBCLASS decides which object is created ----
abstract class ReportJob
{
    protected abstract IExporter CreateExporter();
    public string Run(string report) => CreateExporter().Export(report);
}
class MonthlyJob : ReportJob { protected override IExporter CreateExporter() => new PdfExporter(); }
class DailyJob : ReportJob { protected override IExporter CreateExporter() => new CsvExporter(); }

// ---- Abstract Factory: a FAMILY of related objects that must stay compatible ----
interface IStorage { string Put(string key); }
interface IMessageQueue { string Publish(string msg); }
interface IInfraFactory { IStorage Storage(); IMessageQueue Queue(); }

class DiskStorage : IStorage { public string Put(string k) => "disk:" + k; }
class InMemoryQueue : IMessageQueue { public string Publish(string m) => "in-memory:" + m; }
class BucketStorage : IStorage { public string Put(string k) => "bucket:" + k; }
class CloudQueue : IMessageQueue { public string Publish(string m) => "cloud-queue:" + m; }

class LocalFactory : IInfraFactory
{
    public IStorage Storage() => new DiskStorage();
    public IMessageQueue Queue() => new InMemoryQueue();
}
class CloudFactory : IInfraFactory
{
    public IStorage Storage() => new BucketStorage();
    public IMessageQueue Queue() => new CloudQueue();
}

public static class Demo
{
    public static void Run()
    {
        Console.WriteLine("Factory: simple, method, abstract");
        Check.That(ExporterFactory.Create("pdf").Export("r") == "PDF:r", "simple factory builds a PDF exporter");
        Check.That(ExporterFactory.Create("CSV").Export("r") == "CSV:r", "simple factory builds a CSV exporter");
        Check.Throws<ArgumentException>(() => ExporterFactory.Create("doc"), "unknown type is rejected");

        Check.That(new MonthlyJob().Run("sales") == "PDF:sales", "factory method: monthly job exports PDF");
        Check.That(new DailyJob().Run("sales") == "CSV:sales", "factory method: daily job exports CSV");

        IInfraFactory local = new LocalFactory(), cloud = new CloudFactory();
        Check.That(local.Storage().Put("a") == "disk:a" && local.Queue().Publish("m") == "in-memory:m", "abstract factory: local family is consistent");
        Check.That(cloud.Storage().Put("a") == "bucket:a" && cloud.Queue().Publish("m") == "cloud-queue:m", "abstract factory: cloud family is consistent");
    }
}
