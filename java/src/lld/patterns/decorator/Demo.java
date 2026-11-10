package lld.patterns.decorator;

import lld.common.Check;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

interface AppointmentRepository { String findById(long id); }

class DbRepository implements AppointmentRepository {
    int hits = 0;
    public String findById(long id) { hits++; return "appointment-" + id; }
}

/** Decorator: SAME interface, adds caching around any repository. */
class CachingRepository implements AppointmentRepository {
    private final AppointmentRepository inner;
    private final Map<Long, String> cache = new ConcurrentHashMap<>();
    CachingRepository(AppointmentRepository inner) { this.inner = inner; }
    public String findById(long id) { return cache.computeIfAbsent(id, inner::findById); }
}

/** Decorator: SAME interface, adds logging around any repository. */
class LoggingRepository implements AppointmentRepository {
    private final AppointmentRepository inner;
    final List<String> log = new ArrayList<>();
    LoggingRepository(AppointmentRepository inner) { this.inner = inner; }
    public String findById(long id) {
        log.add("findById(" + id + ")");
        return inner.findById(id);
    }
}

public class Demo {
    public static void main(String[] args) {
        System.out.println("Decorator: add behavior by wrapping, same interface");
        DbRepository db = new DbRepository();
        LoggingRepository repo = new LoggingRepository(new CachingRepository(db));

        repo.findById(1);
        repo.findById(1);
        Check.that(db.hits == 1, "caching decorator: second call never reached the database");
        Check.that(repo.log.size() == 2, "logging is outermost: it saw both calls (including the cache hit)");

        // Order matters: logging INSIDE caching only sees real database hits.
        DbRepository db2 = new DbRepository();
        LoggingRepository inner = new LoggingRepository(db2);
        AppointmentRepository cachedOutside = new CachingRepository(inner);
        cachedOutside.findById(1);
        cachedOutside.findById(1);
        Check.that(inner.log.size() == 1, "swap the wrapper order and logging only sees cache misses");
    }
}
