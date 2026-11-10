package lld.common;

/** Tiny assertion helper so every demo fails loudly if behavior is wrong. */
public final class Check {
    private Check() {}

    public static void that(boolean condition, String message) {
        if (!condition) throw new AssertionError("FAILED: " + message);
        System.out.println("  ok: " + message);
    }

    public static void near(double actual, double expected, String message) {
        that(Math.abs(actual - expected) < 1e-9, message + " (" + actual + ")");
    }

    public static void throwsError(Runnable action, Class<? extends Throwable> type, String message) {
        try {
            action.run();
        } catch (Throwable t) {
            if (type.isInstance(t)) { System.out.println("  ok: " + message); return; }
            throw new AssertionError("FAILED: " + message + " threw " + t, t);
        }
        throw new AssertionError("FAILED: " + message + " (nothing thrown)");
    }
}
