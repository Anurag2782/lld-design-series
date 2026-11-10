/** Tiny assertion helpers so every demo fails loudly if behavior is wrong. */
export function check(condition: boolean, message: string): void {
  if (!condition) throw new Error("FAILED: " + message);
  console.log("  ok: " + message);
}

export function near(actual: number, expected: number, message: string): void {
  check(Math.abs(actual - expected) < 1e-9, `${message} (${actual})`);
}

export function throws(action: () => unknown, message: string, type: new (...args: any[]) => Error = Error): void {
  try {
    action();
  } catch (e) {
    if (e instanceof type) {
      console.log("  ok: " + message);
      return;
    }
    throw new Error(`FAILED: ${message} threw ${String(e)}`);
  }
  throw new Error(`FAILED: ${message} (nothing thrown)`);
}
