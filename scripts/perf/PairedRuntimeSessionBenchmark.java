import java.io.StringReader;
import java.io.StringWriter;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/** Alternating A/A or A/B diagnostic; no engine class is linked into this harness's class loader. */
public class PairedRuntimeSessionBenchmark {
    private static final int WARMUPS = 128;
    private static final int SAMPLES = 101;
    private static final int BATCH = 8;
    private static final int TICKS = 35;
    private static final double BUDGET_PERCENT = 5.0;
    private static final ThreadMXBean THREADS = ManagementFactory.getThreadMXBean();
    private static final com.sun.management.ThreadMXBean ALLOCATIONS =
            THREADS instanceof com.sun.management.ThreadMXBean bean ? bean : null;
    private static final com.sun.management.OperatingSystemMXBean PROCESS =
            ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean bean ? bean : null;
    private static final List<GarbageCollectorMXBean> COLLECTORS = ManagementFactory.getGarbageCollectorMXBeans();
    private static volatile String consumedHash;

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--self-test")) {
            selfTest();
            System.out.println("{\"status\":\"pass\",\"self_test\":true}");
            return;
        }
        if (args.length != 3 || !(args[0].equals("calibration") || args[0].equals("comparison"))) {
            throw new IllegalArgumentException("Expected calibration|comparison baseline-root other-root.");
        }
        if (THREADS.isCurrentThreadCpuTimeSupported() && !THREADS.isThreadCpuTimeEnabled()) THREADS.setThreadCpuTimeEnabled(true);
        if (ALLOCATIONS != null && ALLOCATIONS.isThreadAllocatedMemorySupported() && !ALLOCATIONS.isThreadAllocatedMemoryEnabled()) {
            ALLOCATIONS.setThreadAllocatedMemoryEnabled(true);
        }
        Path leftRoot = Path.of(args[1]).toRealPath();
        Path rightRoot = Path.of(args[2]).toRealPath();
        if (args[0].equals("calibration") != leftRoot.equals(rightRoot)) {
            throw new IllegalArgumentException("Calibration requires identical roots; comparison requires distinct roots.");
        }
        try (GameFacade left = new GameFacade(leftRoot); GameFacade right = new GameFacade(rightRoot)) {
            if (left.sessionType == right.sessionType || left.registry.getClass() == right.registry.getClass()) {
                throw new IllegalStateException("Engine classloader isolation failed.");
            }
            ScenarioReport canonical = measure(left, right, "canonical", 30, 32, "12a65fd2b87593cf");
            ScenarioReport kill = measure(left, right, "kill", 2, 2, "bb37eefc1903cc77");
            boolean calibration = args[0].equals("calibration");
            // Calibration must be symmetric: a >5% advantage is noise just as a >5% regression is.
            boolean withinBudget = canonical.withinBudget(calibration) && kill.withinBudget(calibration);
            System.out.println("{\"mode\":" + quote(args[0])
                    + ",\"status\":" + quote(withinBudget ? "pass" : calibration ? "inconclusive" : "fail")
                    + ",\"method\":\"paired-isolated-classloaders\",\"warmups\":" + WARMUPS
                    + ",\"samples\":" + SAMPLES + ",\"sessions_per_sample\":" + BATCH
                    + ",\"ticks_per_session\":" + TICKS + ",\"maximum_regression_percent\":" + BUDGET_PERCENT
                    + ",\"classloader_isolation_verified\":true,\"every_hash_verified\":true"
                    + ",\"java_runtime_version\":" + quote(System.getProperty("java.runtime.version"))
                    + ",\"os_name\":" + quote(System.getProperty("os.name"))
                    + ",\"available_processors\":" + Runtime.getRuntime().availableProcessors()
                    + ",\"left_root\":" + quote(leftRoot.toString()) + ",\"right_root\":" + quote(rightRoot.toString())
                    + ",\"scenarios\":[" + canonical.json() + "," + kill.json() + "]}");
        }
    }

    private static ScenarioReport measure(GameFacade left, GameFacade right, String name, int x, int y,
                                          String expectedHash) throws Exception {
        String leftSave = fixture(left.emptySave, x, y);
        String rightSave = fixture(right.emptySave, x, y);
        Samples leftSamples = new Samples();
        Samples rightSamples = new Samples();
        for (int iteration = -WARMUPS; iteration < SAMPLES; iteration++) {
            Sample leftSample;
            Sample rightSample;
            // Alternate within each adjacent pair, not only between long independent JVM runs.
            if ((iteration & 1) == 0) {
                leftSample = left.runBatch(leftSave, expectedHash);
                rightSample = right.runBatch(rightSave, expectedHash);
            } else {
                rightSample = right.runBatch(rightSave, expectedHash);
                leftSample = left.runBatch(leftSave, expectedHash);
            }
            if (iteration >= 0) {
                leftSamples.add(iteration, leftSample);
                rightSamples.add(iteration, rightSample);
            }
        }
        return new ScenarioReport(name, expectedHash, leftSamples, rightSamples);
    }

    private static final class GameFacade implements AutoCloseable {
        final URLClassLoader loader;
        final Class<?> sessionType;
        final Object companion;
        final Object registry;
        final Method restore;
        final Method step;
        final Method hash;
        final String emptySave;

        GameFacade(Path root) throws Exception {
            Path libraries = root.resolve("engine-devtools/build/install/engine-devtools/lib");
            URL[] urls;
            try (var paths = Files.list(libraries)) {
                urls = paths.filter(path -> path.toString().endsWith(".jar")).sorted()
                        .map(path -> { try { return path.toUri().toURL(); } catch (Exception failure) { throw new IllegalArgumentException(failure); } })
                        .toArray(URL[]::new);
            }
            if (urls.length == 0) throw new IllegalArgumentException("No distribution jars: " + libraries);
            // Platform-only parent shares JDK classes, never engine/Kotlin classes between adapters.
            loader = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader());
            Class<?> registryType = loader.loadClass("dev.myengine.content.ContentRegistry");
            Class<?> gameType = loader.loadClass("dev.myengine.games.sandbox.SandboxGame");
            Object game = gameType.getField("INSTANCE").get(null);
            registry = gameType.getMethod("loadRegistry", Path.class, String.class)
                    .invoke(game, root.resolve("games/sandbox/content/sandbox"), null);
            sessionType = loader.loadClass("dev.myengine.games.sandbox.SandboxSession");
            companion = sessionType.getField("Companion").get(null);
            Object emptySession = companion.getClass().getMethod("start", registryType, long.class, String.class, String.class)
                    .invoke(companion, registry, 7L, null, null);
            emptySave = (String) sessionType.getMethod("save").invoke(emptySession);
            restore = companion.getClass().getMethod("restore", String.class, registryType);
            step = sessionType.getMethod("step", int.class);
            hash = sessionType.getMethod("stableHash");
        }

        Sample runBatch(String save, String expectedHash) throws Exception {
            Object[] sessions = new Object[BATCH];
            for (int i = 0; i < BATCH; i++) sessions[i] = restore.invoke(companion, save, registry);
            long gcBefore = gcCount();
            long gcMillisBefore = gcMillis();
            long processBefore = processCpu();
            long cpuBefore = threadCpu();
            long allocationBefore = allocated();
            long started = System.nanoTime();
            // Exactly one reflective call per session on BOTH sides; setup/hash/counters excluded.
            for (Object session : sessions) step.invoke(session, TICKS);
            long elapsed = System.nanoTime() - started;
            long bytes = delta(allocationBefore, allocated());
            long cpu = delta(cpuBefore, threadCpu());
            long process = delta(processBefore, processCpu());
            long gcs = delta(gcBefore, gcCount());
            long gcTime = delta(gcMillisBefore, gcMillis());
            for (Object session : sessions) {
                consumedHash = (String) hash.invoke(session);
                if (!expectedHash.equals(consumedHash)) throw new IllegalStateException("Scenario contamination/golden mismatch: " + consumedHash);
            }
            return new Sample(elapsed, cpu, process, bytes, gcs, gcTime);
        }

        @Override public void close() throws Exception { loader.close(); }
    }

    private record Sample(long wall, long cpu, long process, long bytes, long gcs, long gcMillis) {}

    private static final class Samples {
        final long[] wall = new long[SAMPLES];
        final long[] cpu = new long[SAMPLES];
        final long[] process = new long[SAMPLES];
        final long[] bytes = new long[SAMPLES];
        final long[] gcs = new long[SAMPLES];
        final long[] gcMillis = new long[SAMPLES];
        void add(int index, Sample sample) {
            wall[index] = sample.wall; cpu[index] = sample.cpu; process[index] = sample.process;
            bytes[index] = sample.bytes; gcs[index] = sample.gcs; gcMillis[index] = sample.gcMillis;
        }
        String json() {
            return "{\"median_batch_ns\":" + median(wall) + ",\"median_session_ns\":" + median(wall) / BATCH
                    + ",\"minimum_batch_ns\":" + Arrays.stream(wall).min().orElseThrow()
                    + ",\"p90_batch_ns\":" + p90(wall) + ",\"maximum_batch_ns\":" + Arrays.stream(wall).max().orElseThrow()
                    + ",\"median_batch_thread_cpu_ns\":" + median(cpu)
                    + ",\"median_batch_allocated_bytes\":" + median(bytes)
                    + ",\"batch_samples_ns\":" + Arrays.toString(wall)
                    + ",\"batch_thread_cpu_ns\":" + Arrays.toString(cpu)
                    + ",\"batch_process_cpu_ns\":" + Arrays.toString(process)
                    + ",\"batch_allocated_bytes\":" + Arrays.toString(bytes)
                    + ",\"batch_gc_collections\":" + Arrays.toString(gcs)
                    + ",\"batch_gc_time_ms\":" + Arrays.toString(gcMillis) + "}";
        }
    }

    private record ScenarioReport(String name, String hash, Samples left, Samples right) {
        double regression() { return ((double) median(right.wall) / median(left.wall) - 1.0) * 100.0; }
        boolean withinBudget(boolean calibration) { return calibration ? Math.abs(regression()) <= BUDGET_PERCENT : regression() <= BUDGET_PERCENT; }
        String json() {
            double[] pairedRatios = new double[SAMPLES];
            for (int i = 0; i < SAMPLES; i++) pairedRatios[i] = (double) right.wall[i] / left.wall[i];
            double[] orderedRatios = pairedRatios.clone();
            Arrays.sort(orderedRatios);
            return "{\"scenario\":" + quote(name) + ",\"final_hash\":" + quote(hash)
                    + ",\"gate_aggregation\":\"ratio-of-wall-medians\",\"regression_percent\":" + regression()
                    + ",\"median_paired_ratio\":" + orderedRatios[SAMPLES / 2]
                    + ",\"paired_ratios\":" + Arrays.toString(pairedRatios)
                    + ",\"left\":" + left.json() + ",\"right\":" + right.json() + "}";
        }
    }

    private static String fixture(String emptySave, int x, int y) throws Exception {
        Properties properties = new Properties();
        properties.load(new StringReader(emptySave));
        properties.setProperty("pendingCommands", "build_tower|1|1||pulse:" + x + ":" + y);
        StringWriter writer = new StringWriter();
        properties.store(writer, "ENG-036 paired benchmark fixture");
        return writer.toString();
    }
    private static long threadCpu() { return THREADS.isCurrentThreadCpuTimeSupported() ? THREADS.getCurrentThreadCpuTime() : -1; }
    private static long processCpu() { return PROCESS == null ? -1 : PROCESS.getProcessCpuTime(); }
    @SuppressWarnings("deprecation")
    private static long allocated() { return ALLOCATIONS != null && ALLOCATIONS.isThreadAllocatedMemorySupported() ? ALLOCATIONS.getThreadAllocatedBytes(Thread.currentThread().getId()) : -1; }
    private static long gcCount() { long total = 0; for (var collector : COLLECTORS) { long value = collector.getCollectionCount(); if (value < 0) return -1; total += value; } return total; }
    private static long gcMillis() { long total = 0; for (var collector : COLLECTORS) { long value = collector.getCollectionTime(); if (value < 0) return -1; total += value; } return total; }
    private static long delta(long before, long after) { return before < 0 || after < before ? -1 : after - before; }
    private static long median(long[] values) { long[] sorted = values.clone(); Arrays.sort(sorted); int middle = sorted.length / 2; return sorted.length % 2 == 1 ? sorted[middle] : sorted[middle - 1] + (sorted[middle] - sorted[middle - 1]) / 2; }
    private static long p90(long[] values) { long[] sorted = values.clone(); Arrays.sort(sorted); return sorted[(int) Math.ceil(sorted.length * 0.9) - 1]; }
    private static String quote(String value) { StringBuilder result = new StringBuilder("\""); for (char c : value.toCharArray()) { if (c == '"' || c == '\\') result.append('\\').append(c); else if (c < 0x20) result.append(String.format("\\u%04x", (int) c)); else result.append(c); } return result.append('"').toString(); }
    private static void selfTest() {
        long[] original = {9, 1, 5};
        if (median(original) != 5 || !Arrays.equals(original, new long[]{9, 1, 5})) throw new AssertionError("Median");
        if (median(new long[]{Long.MAX_VALUE - 2, Long.MAX_VALUE}) != Long.MAX_VALUE - 1) throw new AssertionError("Median overflow");
        if (p90(new long[]{10, 1, 8, 4, 2, 9, 5, 6, 3, 7}) != 9) throw new AssertionError("P90");
        if (delta(-1, 5) != -1 || delta(100, 150) != 50) throw new AssertionError("Diagnostic delta");
        if (!quote("a\"b\\c\n").equals("\"a\\\"b\\\\c\\u000a\"")) throw new AssertionError("JSON escaping");
        Samples left = new Samples(); Samples right = new Samples();
        Arrays.fill(left.wall, 1000); Arrays.fill(right.wall, 800);
        ScenarioReport faster = new ScenarioReport("self-test", "none", left, right);
        if (faster.withinBudget(true) || !faster.withinBudget(false)) throw new AssertionError("Symmetric A/A budget");
    }
}
