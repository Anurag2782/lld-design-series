import { check, throws } from "../common.ts";

// ---- Part 1: a retry policy behind an interface ----
interface RetryPolicy {
  shouldRetry(attempt: number): boolean;
  delayMs(attempt: number): number;
}

class FixedDelayRetry implements RetryPolicy {
  private maxRetries: number;
  private delay: number;
  constructor(maxRetries: number, delay: number) { this.maxRetries = maxRetries; this.delay = delay; }
  shouldRetry(attempt: number) { return attempt < this.maxRetries; }
  delayMs(_attempt: number) { return this.delay; }
}
class ExponentialBackoffRetry implements RetryPolicy {
  shouldRetry(attempt: number) { return attempt < 5; }
  delayMs(attempt: number) { return 100 * 2 ** attempt; }
}
class NoRetry implements RetryPolicy {
  shouldRetry() { return false; }
  delayMs() { return 0; }
}

/** Context: uses a policy, doesn't know which one. */
class RetryingClient {
  private policy: RetryPolicy;
  private sleeper: (ms: number) => void;
  constructor(policy: RetryPolicy, sleeper: (ms: number) => void) { this.policy = policy; this.sleeper = sleeper; }
  call<T>(action: () => T): T {
    for (let attempt = 0; ; attempt++) {
      try {
        return action();
      } catch (e) {
        if (!this.policy.shouldRetry(attempt)) throw e;
        this.sleeper(this.policy.delayMs(attempt));
      }
    }
  }
}

// ---- Part 2: pick the strategy by key (a Map plays the role of Spring's Map<String, Bean>) ----
interface PaymentStrategy { pay(paise: number): string }
class UpiPayment implements PaymentStrategy { pay(p: number) { return `UPI:${p}`; } }
class CardPayment implements PaymentStrategy { pay(p: number) { return `CARD:${p}`; } }

class PaymentService {
  private strategies: Map<string, PaymentStrategy>;
  constructor(strategies: Map<string, PaymentStrategy>) { this.strategies = strategies; }
  pay(type: string, paise: number): string {
    const s = this.strategies.get(type);
    if (!s) throw new Error(`Unknown payment type: ${type}`);
    return s.pay(paise);
  }
}

export function demo(): void {
  console.log("Strategy: swap the algorithm, keep the caller");
  const delays: number[] = [];
  let calls = 0;
  const flaky = () => { if (++calls < 3) throw new Error("boom"); return "ok"; };

  const result = new RetryingClient(new ExponentialBackoffRetry(), (ms) => delays.push(ms)).call(flaky);
  check(result === "ok" && calls === 3, "succeeds on the third attempt");
  check(delays.join() === "100,200", "exponential backoff delays: 100ms, 200ms");

  calls = 0;
  throws(() => new RetryingClient(new NoRetry(), () => {}).call(flaky), "NoRetry fails fast");
  check(calls === 1, "NoRetry made exactly one call");
  check(new FixedDelayRetry(2, 50).shouldRetry(1) && !new FixedDelayRetry(2, 50).shouldRetry(2), "fixed policy allows exactly 2 retries");

  const service = new PaymentService(new Map([["upi", new UpiPayment()], ["card", new CardPayment()]]));
  check(service.pay("upi", 500) === "UPI:500", "key 'upi' selects UpiPayment");
  check(service.pay("card", 700) === "CARD:700", "key 'card' selects CardPayment");
  throws(() => service.pay("crypto", 1), "unknown key fails clearly (no undefined call)");
}
