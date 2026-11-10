package lld.patterns.templatemethod;

import lld.common.Check;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

record Row(String name, int qty) {
    boolean isValid() { return !name.isBlank() && qty >= 0; }
}
record ImportResult(int imported, int rejected) {}

/** Template Method: the base class owns the ALGORITHM; subclasses fill in the steps. */
abstract class DataImporter {
    /** The template. final: subclasses cannot change the order of steps. */
    public final ImportResult run(List<String> lines) {
        List<Row> parsed = parse(lines);
        List<Row> valid = validate(parsed);
        save(valid);
        afterSave(valid);
        return new ImportResult(valid.size(), parsed.size() - valid.size());
    }
    protected abstract List<Row> parse(List<String> lines);                 // required step
    protected abstract void save(List<Row> rows);                           // required step
    protected List<Row> validate(List<Row> rows) {                          // default step (overridable)
        return rows.stream().filter(Row::isValid).toList();
    }
    protected void afterSave(List<Row> rows) { }                            // hook: does nothing by default
}

class CsvImporter extends DataImporter {
    final List<Row> saved = new ArrayList<>();
    protected List<Row> parse(List<String> lines) {
        return lines.stream().map(l -> l.split(",")).map(p -> new Row(p[0].trim(), Integer.parseInt(p[1].trim()))).toList();
    }
    protected void save(List<Row> rows) { saved.addAll(rows); }
}

class PipeImporter extends DataImporter {
    final List<Row> saved = new ArrayList<>();
    final List<String> audit = new ArrayList<>();
    protected List<Row> parse(List<String> lines) {
        return lines.stream().map(l -> l.split("\\|")).map(p -> new Row(p[0].trim(), Integer.parseInt(p[1].trim()))).toList();
    }
    @Override protected List<Row> validate(List<Row> rows) {                // extra business rule
        return super.validate(rows).stream().filter(r -> r.qty() <= 1000).toList();
    }
    protected void save(List<Row> rows) { saved.addAll(rows); }
    @Override protected void afterSave(List<Row> rows) { audit.add("saved " + rows.size()); }
}

/** Callback flavour (what Spring's JdbcTemplate / TransactionTemplate do): the template owns open/close,
 *  YOU pass only the part that varies. */
class ConnectionTemplate {
    final List<String> events = new ArrayList<>();
    <T> T execute(Function<List<String>, T> callback) {
        events.add("open");
        try {
            return callback.apply(events);
        } finally {
            events.add("close");                                            // always runs, even on exception
        }
    }
}

public class Demo {
    public static void main(String[] args) {
        System.out.println("Template Method: fixed skeleton, variable steps");
        CsvImporter csv = new CsvImporter();
        ImportResult r1 = csv.run(List.of("pen, 10", "book, 3", "bad, -1"));
        Check.that(r1.equals(new ImportResult(2, 1)), "CSV: 2 imported, 1 rejected by the default validate()");

        PipeImporter pipe = new PipeImporter();
        ImportResult r2 = pipe.run(List.of("pen | 10", "bulk | 5000"));
        Check.that(r2.equals(new ImportResult(1, 1)), "pipe importer overrides validate() with an extra rule");
        Check.that(pipe.audit.equals(List.of("saved 1")), "pipe importer uses the afterSave() hook");

        ConnectionTemplate template = new ConnectionTemplate();
        int answer = template.execute(ev -> 42);
        Check.that(answer == 42 && template.events.equals(List.of("open", "close")), "callback template opens and closes around your code");
        Check.throwsError(() -> template.execute(ev -> { throw new IllegalStateException("boom"); }),
            IllegalStateException.class, "exceptions still propagate");
        Check.that(template.events.equals(List.of("open", "close", "open", "close")), "...and the resource is still closed after a failure");
    }
}
