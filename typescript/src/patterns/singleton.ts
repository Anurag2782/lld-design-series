import { check } from "../common.ts";

/** Classic singleton. JavaScript is single-threaded, so no lock is needed for this check. */
class Config {
  private static instance: Config | undefined;
  private constructor() {}
  static getInstance(): Config {
    if (!Config.instance) Config.instance = new Config();
    return Config.instance;
  }
}

/** The most idiomatic singleton in JS/TS: a module-level object. Modules are evaluated once and cached. */
const registry = new Map<string, string>();

export function demo(): void {
  console.log("Singleton: one shared instance");
  const instances = Array.from({ length: 50 }, () => Config.getInstance());
  check(instances.every((i) => i === instances[0]), "50 callers all receive the same Config instance");
  registry.set("env", "prod");
  check(registry.get("env") === "prod", "module-level singleton holds shared state");
  check(Config.getInstance() === Config.getInstance(), "always the same Config");
  // Note: Node is single-threaded per event loop, but worker_threads each get their OWN copy of module state.
}
