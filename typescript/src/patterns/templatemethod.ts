import { check, throws } from "../common.ts";

interface Row { name: string; qty: number }
interface ImportResult { imported: number; rejected: number }
const isValid = (r: Row) => r.name.trim() !== "" && r.qty >= 0;

/** The base class owns the ALGORITHM; subclasses fill in the steps. */
abstract class DataImporter {
  /** The template: subclasses should not override this. */
  run(lines: string[]): ImportResult {
    const parsed = this.parse(lines);
    const valid = this.validate(parsed);
    this.save(valid);
    this.afterSave(valid);
    return { imported: valid.length, rejected: parsed.length - valid.length };
  }
  protected abstract parse(lines: string[]): Row[];          // required step
  protected abstract save(rows: Row[]): void;                // required step
  protected validate(rows: Row[]): Row[] { return rows.filter(isValid); }   // default step (overridable)
  protected afterSave(_rows: Row[]): void {}                 // hook: does nothing by default
}

const splitRows = (lines: string[], sep: string): Row[] =>
  lines.map((l) => l.split(sep)).map(([a, b]) => ({ name: a!.trim(), qty: Number(b!.trim()) }));

class CsvImporter extends DataImporter {
  saved: Row[] = [];
  protected parse(lines: string[]) { return splitRows(lines, ","); }
  protected save(rows: Row[]) { this.saved.push(...rows); }
}

class PipeImporter extends DataImporter {
  saved: Row[] = [];
  audit: string[] = [];
  protected parse(lines: string[]) { return splitRows(lines, "|"); }
  protected override validate(rows: Row[]) { return super.validate(rows).filter((r) => r.qty <= 1000); }   // extra rule
  protected save(rows: Row[]) { this.saved.push(...rows); }
  protected override afterSave(rows: Row[]) { this.audit.push(`saved ${rows.length}`); }
}

/** Callback flavour (what Spring's JdbcTemplate does): the template owns open/close, YOU pass the varying part. */
class ConnectionTemplate {
  events: string[] = [];
  execute<T>(callback: () => T): T {
    this.events.push("open");
    try {
      return callback();
    } finally {
      this.events.push("close");                              // always runs, even on exception
    }
  }
}

export function demo(): void {
  console.log("Template Method: fixed skeleton, variable steps");
  const csv = new CsvImporter();
  const r1 = csv.run(["pen, 10", "book, 3", "bad, -1"]);
  check(r1.imported === 2 && r1.rejected === 1, "CSV: 2 imported, 1 rejected by the default validate()");
  const pipe = new PipeImporter();
  const r2 = pipe.run(["pen | 10", "bulk | 5000"]);
  check(r2.imported === 1 && r2.rejected === 1, "pipe importer overrides validate() with an extra rule");
  check(pipe.audit.join() === "saved 1", "pipe importer uses the afterSave() hook");

  const template = new ConnectionTemplate();
  check(template.execute(() => 42) === 42 && template.events.join() === "open,close", "callback template opens and closes around your code");
  throws(() => template.execute(() => { throw new Error("boom"); }), "exceptions still propagate");
  check(template.events.join() === "open,close,open,close", "...and the resource is still closed after a failure");
}
