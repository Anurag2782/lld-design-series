package lld.patterns.strategy;

import lld.common.Check;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

// ---- Part 1: pick an algorithm behind an interface (retry policy) ----
interface RetryPolicy {
    boolean shouldRetry(int attempt);
    long delayMs(int attempt);
}

class FixedDelayRetry implements RetryPolicy {
    private final int maxRetries; private final long delay;
    FixedDelayRetry(int maxRetries, long delay) { this.maxRetries = maxRetries; this.delay = delay; }
    public boolean shouldRetry(int attempt) { return attempt < maxRetries; }
    public long delayMs(int attempt) { return delay; }
}

class ExponentialBackoffRetry implements RetryPolicy {
    public boolean shouldRetry(int attempt) { return attempt < 5; }
    public long delayMs(int attempt) { return 100L * (1L << attempt); }
}

class NoRetry implements RetryPolicy {
    public boolean shouldRetry(int attempt) { return false; }
    public long delayMs(int attempt) { return 0; }
}

interface Sleeper { void sleep(long ms); }

/** Context: uses a policy, doesn't know which one. */
class RetryingClient {
    private final RetryPolicy policy;
    private final Sleeper sleeper;
    RetryingClient(RetryPolicy policy, Sleeper sleeper) { this.policy = policy; this.sleeper = sleeper; }

    <T> T call(Callable<T> action) throws Exception {
        for (int attempt = 0; ; attempt++) {
            try {
                return action.call();
            } catch (Exception e) {
                if (!policy.shouldRetry(attempt)) throw e;
                sleeper.sleep(policy.delayMs(attempt));
            }
        }
    }
}

// ---- Part 2: choose the strategy by key (what Spring's Map<String, Bean> injection does) ----
interface PaymentStrategy { String pay(long paise); }
class UpiPayment implements PaymentStrategy { public String pay(long p) { return "UPI:" + p; } }
class CardPayment implements PaymentStrategy { public String pay(long p) { return "CARD:" + p; } }

class PaymentService {
    private final Map<String, PaymentStrategy> strategies;
    PaymentService(Map<String, PaymentStrategy> strategies) { this.strategies = strategies; }

    String pay(String type, long paise) {
        PaymentStrategy s = strategies.get(type);
        if (s == null) throw new IllegalArgumentException("Unknown payment type: " + type);
        return s.pay(paise);
    }
}

public class Demo {
    public static void main(String[] args) throws Exception {
        System.out.println("Strategy: swap the algorithm, keep the caller");
        List<Long> delays = new ArrayList<>();
        AtomicInteger calls = new AtomicInteger();
        Callable<String> flaky = () -> {
            if (calls.incrementAndGet() < 3) throw new RuntimeException("boom");
            return "ok";
        };

        String result = new RetryingClient(new ExponentialBackoffRetry(), delays::add).call(flaky);
        Check.that(result.equals("ok") && calls.get() == 3, "succeeds on the third attempt");
        Check.that(delays.equals(List.of(100L, 200L)), "exponential backoff delays: 100ms, 200ms");

        calls.set(0);
        Check.throwsError(() -> {
            try { new RetryingClient(new NoRetry(), ms -> {}).call(flaky); }
            catch (Exception e) { throw new RuntimeException(e); }
        }, RuntimeException.class, "NoRetry fails fast");
        Check.that(calls.get() == 1, "NoRetry made exactly one call");

        // Part 2
        PaymentService service = new PaymentService(Map.of("upi", new UpiPayment(), "card", new CardPayment()));
        Check.that(service.pay("upi", 500).equals("UPI:500"), "key 'upi' selects UpiPayment");
        Check.that(service.pay("card", 700).equals("CARD:700"), "key 'card' selects CardPayment");
        Check.throwsError(() -> service.pay("crypto", 1), IllegalArgumentException.class,
            "unknown key fails clearly (no NPE)");
    }
}
