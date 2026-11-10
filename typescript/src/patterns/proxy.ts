import { check, throws } from "../common.ts";

interface ReportService { generate(id: number): string }

class RealReportService implements ReportService {
  generate(id: number) { return `report-${id}`; }
}

class AccessDeniedError extends Error {}

/** Protection proxy: decides WHETHER the real object is called. */
class SecureReportProxy implements ReportService {
  private real: ReportService;
  private roles: Set<string>;
  constructor(real: ReportService, roles: Set<string>) { this.real = real; this.roles = roles; }
  generate(id: number) {
    if (!this.roles.has("ADMIN")) throw new AccessDeniedError("admin role required");
    return this.real.generate(id);
  }
}

/** Virtual proxy: decides WHEN the real object is created. */
class LazyReportProxy implements ReportService {
  private factory: () => ReportService;
  private real?: ReportService;
  constructor(factory: () => ReportService) { this.factory = factory; }
  generate(id: number) {
    this.real ??= this.factory();
    return this.real.generate(id);
  }
}

/** Decorator for contrast: ALWAYS calls the real object and adds something. */
class UppercaseDecorator implements ReportService {
  private inner: ReportService;
  constructor(inner: ReportService) { this.inner = inner; }
  generate(id: number) { return this.inner.generate(id).toUpperCase(); }
}

export function demo(): void {
  console.log("Proxy: control access. Decorator: add behavior");
  const real = new RealReportService();
  throws(() => new SecureReportProxy(real, new Set(["USER"])).generate(1), "protection proxy blocks a non-admin; real service never called", AccessDeniedError);
  check(new SecureReportProxy(real, new Set(["ADMIN"])).generate(1) === "report-1", "admin is allowed through");

  let created = 0;
  const lazy = new LazyReportProxy(() => { created++; return real; });
  check(created === 0, "virtual proxy: nothing created yet");
  lazy.generate(1); lazy.generate(2);
  check(created === 1, "virtual proxy: created once, on first use");
  check(new UppercaseDecorator(real).generate(3) === "REPORT-3", "decorator always calls through and adds behavior");
  // JavaScript also has a built-in Proxy object for building virtual/protection proxies at runtime.
}
