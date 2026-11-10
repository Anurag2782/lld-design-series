using Lld.Common;

namespace Lld.Patterns.TemplateMethod;

record Row(string Name, int Qty)
{
    public bool IsValid => !string.IsNullOrWhiteSpace(Name) && Qty >= 0;
}
record ImportResult(int Imported, int Rejected);

/// <summary>Template Method: the base class owns the ALGORITHM; subclasses fill in the steps.</summary>
abstract class DataImporter
{
    /// <summary>The template. Not virtual: subclasses cannot change the order of steps.</summary>
    public ImportResult Run(List<string> lines)
    {
        var parsed = Parse(lines);
        var valid = Validate(parsed);
        Save(valid);
        AfterSave(valid);
        return new ImportResult(valid.Count, parsed.Count - valid.Count);
    }
    protected abstract List<Row> Parse(List<string> lines);                 // required step
    protected abstract void Save(List<Row> rows);                           // required step
    protected virtual List<Row> Validate(List<Row> rows) => rows.Where(r => r.IsValid).ToList();   // default step
    protected virtual void AfterSave(List<Row> rows) { }                    // hook: does nothing by default
}

class CsvImporter : DataImporter
{
    public List<Row> Saved { get; } = new();
    protected override List<Row> Parse(List<string> lines) =>
        lines.Select(l => l.Split(',')).Select(p => new Row(p[0].Trim(), int.Parse(p[1].Trim()))).ToList();
    protected override void Save(List<Row> rows) => Saved.AddRange(rows);
}

class PipeImporter : DataImporter
{
    public List<Row> Saved { get; } = new();
    public List<string> Audit { get; } = new();
    protected override List<Row> Parse(List<string> lines) =>
        lines.Select(l => l.Split('|')).Select(p => new Row(p[0].Trim(), int.Parse(p[1].Trim()))).ToList();
    protected override List<Row> Validate(List<Row> rows) => base.Validate(rows).Where(r => r.Qty <= 1000).ToList();   // extra rule
    protected override void Save(List<Row> rows) => Saved.AddRange(rows);
    protected override void AfterSave(List<Row> rows) => Audit.Add($"saved {rows.Count}");
}

/// <summary>Callback flavour (what Spring's JdbcTemplate does; in .NET think TransactionScope / using blocks):
/// the template owns open/close, YOU pass only the part that varies.</summary>
class ConnectionTemplate
{
    public List<string> Events { get; } = new();
    public T Execute<T>(Func<T> callback)
    {
        Events.Add("open");
        try { return callback(); }
        finally { Events.Add("close"); }                                    // always runs, even on exception
    }
}

public static class Demo
{
    public static void Run()
    {
        Console.WriteLine("Template Method: fixed skeleton, variable steps");
        var csv = new CsvImporter();
        Check.That(csv.Run(new() { "pen, 10", "book, 3", "bad, -1" }) == new ImportResult(2, 1), "CSV: 2 imported, 1 rejected by the default Validate()");

        var pipe = new PipeImporter();
        Check.That(pipe.Run(new() { "pen | 10", "bulk | 5000" }) == new ImportResult(1, 1), "pipe importer overrides Validate() with an extra rule");
        Check.That(pipe.Audit.SequenceEqual(new[] { "saved 1" }), "pipe importer uses the AfterSave() hook");

        var template = new ConnectionTemplate();
        var answer = template.Execute(() => 42);
        Check.That(answer == 42 && template.Events.SequenceEqual(new[] { "open", "close" }), "callback template opens and closes around your code");
        Check.Throws<InvalidOperationException>(() => template.Execute<int>(() => throw new InvalidOperationException("boom")), "exceptions still propagate");
        Check.That(template.Events.SequenceEqual(new[] { "open", "close", "open", "close" }), "...and the resource is still closed after a failure");
    }
}
