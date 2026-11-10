package lld.patterns.singleton;

import lld.common.Check;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/** Holder idiom: lazy, thread-safe, no locks. */
final class Config {
    private Config() {}
    private static class Holder { static final Config INSTANCE = new Config(); }
    static Config getInstance() { return Holder.INSTANCE; }
}

/** Enum singleton: simplest, safe against reflection and serialization tricks. */
enum Registry {
    INSTANCE;
    private final Map<String, String> data = new ConcurrentHashMap<>();
    void put(String k, String v) { data.put(k, v); }
    String get(String k) { return data.get(k); }
}

public class Demo {
    public static void main(String[] args) throws Exception {
        System.out.println("Singleton: one shared instance, thread-safe");
        ExecutorService pool = Executors.newFixedThreadPool(16);
        List<Future<Config>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < 50; i++) futures.add(pool.submit(Config::getInstance));
        Config first = futures.get(0).get();
        boolean allSame = true;
        for (Future<Config> f : futures) allSame &= (f.get() == first);
        pool.shutdown();
        Check.that(allSame, "50 concurrent callers all receive the same Config instance");

        Registry.INSTANCE.put("env", "prod");
        Check.that(Registry.INSTANCE.get("env").equals("prod"), "enum singleton holds shared state");
        Check.that(Registry.valueOf("INSTANCE") == Registry.INSTANCE, "enum guarantees a single instance");
    }
}
