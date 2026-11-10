package lld.patterns.proxy;

import lld.common.Check;
import java.util.Set;
import java.util.function.Supplier;

interface ReportService { String generate(long id); }

class RealReportService implements ReportService {
    public String generate(long id) { return "report-" + id; }
}

class AccessDeniedException extends RuntimeException {
    AccessDeniedException() { super("admin role required"); }
}

/** Protection proxy: decides WHETHER the real object is called. */
class SecureReportProxy implements ReportService {
    private final ReportService real; private final Set<String> roles;
    SecureReportProxy(ReportService real, Set<String> roles) { this.real = real; this.roles = roles; }
    public String generate(long id) {
        if (!roles.contains("ADMIN")) throw new AccessDeniedException();
        return real.generate(id);
    }
}

/** Virtual proxy: decides WHEN the real object is created. */
class LazyReportProxy implements ReportService {
    private final Supplier<ReportService> factory;
    private ReportService real;
    LazyReportProxy(Supplier<ReportService> factory) { this.factory = factory; }
    public synchronized String generate(long id) {
        if (real == null) real = factory.get();
        return real.generate(id);
    }
}

/** Decorator for contrast: ALWAYS calls the real object and adds something. */
class UppercaseDecorator implements ReportService {
    private final ReportService inner;
    UppercaseDecorator(ReportService inner) { this.inner = inner; }
    public String generate(long id) { return inner.generate(id).toUpperCase(); }
}

public class Demo {
    public static void main(String[] args) {
        System.out.println("Proxy: control access. Decorator: add behavior");
        ReportService real = new RealReportService();
        Check.throwsError(() -> new SecureReportProxy(real, Set.of("USER")).generate(1),
            AccessDeniedException.class, "protection proxy blocks a non-admin; real service never called");
        Check.that(new SecureReportProxy(real, Set.of("ADMIN")).generate(1).equals("report-1"), "admin is allowed through");

        int[] created = {0};
        ReportService lazy = new LazyReportProxy(() -> { created[0]++; return real; });
        Check.that(created[0] == 0, "virtual proxy: nothing created yet");
        lazy.generate(1); lazy.generate(2);
        Check.that(created[0] == 1, "virtual proxy: created once, on first use");

        Check.that(new UppercaseDecorator(real).generate(3).equals("REPORT-3"), "decorator always calls through and adds behavior");
    }
}
