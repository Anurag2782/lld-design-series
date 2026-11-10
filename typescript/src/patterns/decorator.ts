import { check } from "../common.ts";

interface AppointmentRepository { findById(id: number): string }

class DbRepository implements AppointmentRepository {
  hits = 0;
  findById(id: number) { this.hits++; return `appointment-${id}`; }
}

/** Decorator: SAME interface, adds caching around any repository. */
class CachingRepository implements AppointmentRepository {
  private inner: AppointmentRepository;
  private cache = new Map<number, string>();
  constructor(inner: AppointmentRepository) { this.inner = inner; }
  findById(id: number) {
    if (!this.cache.has(id)) this.cache.set(id, this.inner.findById(id));
    return this.cache.get(id)!;
  }
}

/** Decorator: SAME interface, adds logging around any repository. */
class LoggingRepository implements AppointmentRepository {
  private inner: AppointmentRepository;
  log: string[] = [];
  constructor(inner: AppointmentRepository) { this.inner = inner; }
  findById(id: number) { this.log.push(`findById(${id})`); return this.inner.findById(id); }
}

export function demo(): void {
  console.log("Decorator: add behavior by wrapping, same interface");
  const db = new DbRepository();
  const repo = new LoggingRepository(new CachingRepository(db));
  repo.findById(1);
  repo.findById(1);
  check(db.hits === 1, "caching decorator: second call never reached the database");
  check(repo.log.length === 2, "logging is outermost: it saw both calls (including the cache hit)");

  const db2 = new DbRepository();
  const inner = new LoggingRepository(db2);
  const cachedOutside = new CachingRepository(inner);
  cachedOutside.findById(1);
  cachedOutside.findById(1);
  check(inner.log.length === 1, "swap the wrapper order and logging only sees cache misses");
}
