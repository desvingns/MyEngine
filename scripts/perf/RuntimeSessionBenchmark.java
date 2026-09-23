import dev.myengine.content.ContentRegistry;
import dev.myengine.games.sandbox.SandboxGame;
import dev.myengine.games.sandbox.SandboxSession;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Same-source baseline/candidate harness for ENG-036. Uses only the pre-extraction session API.
 * Run with Java 17+ source-file launch and engine-devtools installDist/lib/* on the classpath.
 * Runtime construction, save parsing, snapshot projection and hash verification are NOT timed.
 */
class RuntimeSessionBenchmark {
    private static final int TICKS = 35;
    private static volatile String consumedHash;
    private static final ThreadMXBean THREADS = ManagementFactory.getThreadMXBean();
    private static final com.sun.management.ThreadMXBean ALLOCATIONS =
            THREADS instanceof com.sun.management.ThreadMXBean bean ? bean : null;
    private static final com.sun.management.OperatingSystemMXBean PROCESS =
            ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean bean ? bean : null;
    private static final List<GarbageCollectorMXBean> COLLECTORS = ManagementFactory.getGarbageCollectorMXBeans();

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--self-test")) {
            selfTest();
            System.out.println("{\"status\":\"pass\",\"self_test\":true}");
            return;
        }
        if (args.length != 3) throw new IllegalArgumentException("Expected warmups samples sessions-per-sample.");
        int warmups = bounded(args[0], 1, 1000);
        int samples = bounded(args[1], 3, 1001);
        int batch = bounded(args[2], 1, 100);
        if (THREADS.isCurrentThreadCpuTimeSupported() && !THREADS.isThreadCpuTimeEnabled()) THREADS.setThreadCpuTimeEnabled(true);
        if (ALLOCATIONS != null && ALLOCATIONS.isThreadAllocatedMemorySupported() && !ALLOCATIONS.isThreadAllocatedMemoryEnabled()) {
            ALLOCATIONS.setThreadAllocatedMemoryEnabled(true);
        }
        ContentRegistry registry = SandboxGame.INSTANCE.loadRegistry(SandboxGame.INSTANCE.contentRoot(), null);
        String emptySave = SandboxSession.Companion.start(registry, 7L, null, null).save();
        String canonical = measure("canonical", fixture(emptySave, 30, 32), "12a65fd2b87593cf", registry, warmups, samples, batch);
        String kill = measure("kill", fixture(emptySave, 2, 2), "bb37eefc1903cc77", registry, warmups, samples, batch);
        System.out.println("{\"status\":\"pass\",\"ticks_per_session\":" + TICKS
                + ",\"warmups\":" + warmups + ",\"samples\":" + samples
                + ",\"sessions_per_sample\":" + batch
                + ",\"java_runtime_version\":" + jsonString(System.getProperty("java.runtime.version"))
                + ",\"os_name\":" + jsonString(System.getProperty("os.name"))
                + ",\"os_version\":" + jsonString(System.getProperty("os.version"))
                + ",\"available_processors\":" + Runtime.getRuntime().availableProcessors()
                + ",\"thread_cpu_supported\":" + THREADS.isCurrentThreadCpuTimeSupported()
                + ",\"allocation_supported\":" + (ALLOCATIONS != null && ALLOCATIONS.isThreadAllocatedMemorySupported())
                + ",\"process_cpu_supported\":" + (PROCESS != null)
                + ",\"scenarios\":[" + canonical + "," + kill + "]}");
    }

    private static String measure(String name, String save, String expectedHash, ContentRegistry registry,
                                  int warmups, int samples, int batch) {
        long[] timings = new long[samples];
        long[] threadCpu = new long[samples];
        long[] processCpu = new long[samples];
        long[] allocatedBytes = new long[samples];
        long[] gcCollections = new long[samples];
        long[] gcTimeMillis = new long[samples];
        for (int iteration = -warmups; iteration < samples; iteration++) {
            SandboxSession[] sessions = new SandboxSession[batch];
            for (int i = 0; i < batch; i++) sessions[i] = SandboxSession.Companion.restore(save, registry);
            long gcCountBefore = gcCount();
            long gcTimeBefore = gcMillis();
            long processBefore = processCpuNanos();
            long threadBefore = threadCpuNanos();
            long allocationBefore = allocatedBytes();
            // Keep these original wall-clock boundaries unchanged: diagnostics are outside them.
            long started = System.nanoTime();
            for (SandboxSession session : sessions) session.step(TICKS);
            long elapsed = System.nanoTime() - started;
            long allocationDelta = delta(allocationBefore, allocatedBytes());
            long threadDelta = delta(threadBefore, threadCpuNanos());
            long processDelta = delta(processBefore, processCpuNanos());
            long gcCountDelta = delta(gcCountBefore, gcCount());
            long gcTimeDelta = delta(gcTimeBefore, gcMillis());
            // Each independent sample must execute the accepted scenario, not an empty/terminal path.
            for (SandboxSession session : sessions) {
                consumedHash = session.stableHash();
                if (!expectedHash.equals(consumedHash)) {
                    throw new IllegalStateException(name + " golden mismatch: " + consumedHash);
                }
            }
            if (iteration >= 0) {
                timings[iteration] = elapsed;
                threadCpu[iteration] = threadDelta;
                processCpu[iteration] = processDelta;
                allocatedBytes[iteration] = allocationDelta;
                gcCollections[iteration] = gcCountDelta;
                gcTimeMillis[iteration] = gcTimeDelta;
            }
        }
        return "{\"scenario\":\"" + name + "\",\"final_hash\":\"" + expectedHash
                + "\",\"median_batch_ns\":" + median(timings)
                + ",\"median_session_ns\":" + (median(timings) / batch)
                + ",\"minimum_batch_ns\":" + Arrays.stream(timings).min().orElseThrow()
                + ",\"p90_batch_ns\":" + percentile90(timings)
                + ",\"maximum_batch_ns\":" + Arrays.stream(timings).max().orElseThrow()
                + ",\"median_batch_thread_cpu_ns\":" + median(threadCpu)
                + ",\"median_batch_process_cpu_ns\":" + median(processCpu)
                + ",\"median_batch_allocated_bytes\":" + median(allocatedBytes)
                + ",\"measured_gc_collections\":" + availableSum(gcCollections)
                + ",\"measured_gc_time_ms\":" + availableSum(gcTimeMillis)
                + ",\"batch_samples_ns\":" + Arrays.toString(timings)
                + ",\"batch_thread_cpu_ns\":" + Arrays.toString(threadCpu)
                + ",\"batch_process_cpu_ns\":" + Arrays.toString(processCpu)
                + ",\"batch_allocated_bytes\":" + Arrays.toString(allocatedBytes)
                + ",\"batch_gc_collections\":" + Arrays.toString(gcCollections)
                + ",\"batch_gc_time_ms\":" + Arrays.toString(gcTimeMillis) + "}";
    }

    private static long threadCpuNanos() {
        return THREADS.isCurrentThreadCpuTimeSupported() ? THREADS.getCurrentThreadCpuTime() : -1;
    }

    private static long processCpuNanos() { return PROCESS == null ? -1 : PROCESS.getProcessCpuTime(); }

    @SuppressWarnings("deprecation") // getId keeps the shared harness source compatible with Java 17.
    private static long allocatedBytes() {
        return ALLOCATIONS != null && ALLOCATIONS.isThreadAllocatedMemorySupported()
                ? ALLOCATIONS.getThreadAllocatedBytes(Thread.currentThread().getId()) : -1;
    }

    private static long gcCount() {
        long result = 0;
        for (GarbageCollectorMXBean collector : COLLECTORS) {
            long count = collector.getCollectionCount();
            if (count < 0) return -1;
            result += count;
        }
        return result;
    }

    private static long gcMillis() {
        long result = 0;
        for (GarbageCollectorMXBean collector : COLLECTORS) {
            long millis = collector.getCollectionTime();
            if (millis < 0) return -1;
            result += millis;
        }
        return result;
    }

    static long delta(long before, long after) { return before < 0 || after < before ? -1 : after - before; }

    static long availableSum(long[] values) {
        long result = 0;
        for (long value : values) {
            if (value < 0) return -1;
            result += value;
        }
        return result;
    }

    static long percentile90(long[] source) {
        if (source.length == 0) throw new IllegalArgumentException("Percentile needs samples.");
        long[] sorted = source.clone();
        Arrays.sort(sorted);
        return sorted[(int) Math.ceil(sorted.length * 0.9) - 1];
    }

    static String jsonString(String value) {
        StringBuilder result = new StringBuilder("\"");
        for (char character : value.toCharArray()) {
            if (character == '"' || character == '\\') result.append('\\').append(character);
            else if (character < 0x20) result.append(String.format("\\u%04x", (int) character));
            else result.append(character);
        }
        return result.append('"').toString();
    }

    static String fixture(String emptySave, int x, int y) throws Exception {
        Properties properties = new Properties();
        properties.load(new StringReader(emptySave));
        properties.setProperty("pendingCommands", "build_tower|1|1||pulse:" + x + ":" + y);
        StringWriter writer = new StringWriter();
        properties.store(writer, "ENG-036 canonical command fixture");
        return writer.toString();
    }

    static long median(long[] source) {
        if (source.length == 0) throw new IllegalArgumentException("Median needs samples.");
        long[] sorted = source.clone();
        Arrays.sort(sorted);
        int middle = sorted.length / 2;
        // Avoid overflow while retaining a deterministic integer midpoint.
        return sorted.length % 2 == 1 ? sorted[middle]
                : sorted[middle - 1] + (sorted[middle] - sorted[middle - 1]) / 2;
    }

    private static int bounded(String value, int minimum, int maximum) {
        int parsed = Integer.parseInt(value);
        if (parsed < minimum || parsed > maximum) throw new IllegalArgumentException("Out-of-range count: " + parsed);
        return parsed;
    }

    private static void selfTest() throws Exception {
        long[] original = {9, 1, 5};
        if (median(original) != 5 || !Arrays.equals(original, new long[]{9, 1, 5})) throw new AssertionError("Odd median/immutability");
        if (median(new long[]{2, 8, 4, 6}) != 5) throw new AssertionError("Even median");
        if (median(new long[]{Long.MAX_VALUE - 2, Long.MAX_VALUE}) != Long.MAX_VALUE - 1) throw new AssertionError("Median overflow");
        if (percentile90(new long[]{10, 1, 8, 4, 2, 9, 5, 6, 3, 7}) != 9) throw new AssertionError("P90 nearest rank");
        if (delta(100, 150) != 50 || delta(-1, 150) != -1 || delta(100, 99) != -1) throw new AssertionError("Diagnostic availability");
        if (availableSum(new long[]{1, 2}) != 3 || availableSum(new long[]{1, -1}) != -1) throw new AssertionError("Unavailable diagnostic sum");
        if (!jsonString("a\"b\\c\n").equals("\"a\\\"b\\\\c\\u000a\"")) throw new AssertionError("JSON escaping");
        try { median(new long[]{}); throw new AssertionError("Empty median accepted"); }
        catch (IllegalArgumentException expected) { /* Required rejection. */ }
        try { bounded("0", 1, 10); throw new AssertionError("Invalid bound accepted"); }
        catch (IllegalArgumentException expected) { /* Required rejection. */ }
        Properties fixture = new Properties();
        fixture.load(new StringReader(fixture("saveVersion=7\nseed=7\n", 2, 2)));
        if (!"build_tower|1|1||pulse:2:2".equals(fixture.getProperty("pendingCommands"))) throw new AssertionError("Command fixture encoding");
        if (!"7".equals(fixture.getProperty("seed"))) throw new AssertionError("Fixture changed seed");
    }
}
