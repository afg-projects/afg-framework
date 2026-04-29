package io.github.afgprojects.framework.core.cache.benchmark;

import io.github.afgprojects.framework.core.cache.AfgCache;
import io.github.afgprojects.framework.core.cache.CacheConfig;
import io.github.afgprojects.framework.core.cache.CacheProperties;
import io.github.afgprojects.framework.core.cache.DefaultCacheManager;
import io.github.afgprojects.framework.core.cache.LocalCache;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/**
 * 缓存管理器性能基准测试
 * <p>
 * 测试场景：
 * <ul>
 *   <li>缓存命中（本地缓存）</li>
 *   <li>缓存未命中（本地缓存）</li>
 *   <li>缓存写入（本地缓存）</li>
 *   <li>缓存删除（本地缓存）</li>
 *   <li>getOrLoad 加载模式</li>
 * </ul>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class CacheManagerBenchmark {

    /**
     * 本地缓存状态
     */
    @State(Scope.Thread)
    public static class LocalCacheState {
        LocalCache<String> cache;
        DefaultCacheManager cacheManager;

        // 预热的键
        static final String HOT_KEY = "hot-key";
        static final String HOT_VALUE = "hot-value";

        // 冷键（未预热）
        static final String COLD_KEY = "cold-key";

        // 测试用的值
        static final String TEST_VALUE = "test-value";

        @Setup(Level.Trial)
        public void setup() {
            CacheProperties properties = new CacheProperties();
            properties.setType(CacheProperties.CacheType.LOCAL);
            CacheConfig config = CacheConfig.defaultConfig()
                    .maximumSize(10000)
                    .recordStats(true);

            cache = new LocalCache<>("benchmark-cache", config);
            cacheManager = new DefaultCacheManager(properties);

            // 预热缓存
            cache.put(HOT_KEY, HOT_VALUE);
            cache.put("key1", "value1");
            cache.put("key2", "value2");
            cache.put("key3", "value3");
            cache.put("key4", "value4");
            cache.put("key5", "value5");
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            cache.clear();
            cacheManager.destroy();
        }
    }

    /**
     * 大量数据缓存状态
     */
    @State(Scope.Thread)
    public static class LargeCacheState {
        LocalCache<String> cache;

        @Setup(Level.Trial)
        public void setup() {
            CacheConfig config = CacheConfig.defaultConfig()
                    .maximumSize(100000)
                    .recordStats(true);

            cache = new LocalCache<>("large-cache", config);

            // 预热 1000 条数据
            for (int i = 0; i < 1000; i++) {
                cache.put("key-" + i, "value-" + i);
            }
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            cache.clear();
        }
    }

    // ==================== 缓存命中测试 ====================

    /**
     * 测试缓存命中 - 单次获取
     */
    @Benchmark
    public String cacheHitSingle(LocalCacheState state) {
        return state.cache.get(LocalCacheState.HOT_KEY);
    }

    /**
     * 测试缓存命中 - 多次获取
     */
    @Benchmark
    public String[] cacheHitMultiple(LocalCacheState state) {
        String[] values = new String[5];
        values[0] = state.cache.get("key1");
        values[1] = state.cache.get("key2");
        values[2] = state.cache.get("key3");
        values[3] = state.cache.get("key4");
        values[4] = state.cache.get("key5");
        return values;
    }

    // ==================== 缓存未命中测试 ====================

    /**
     * 测试缓存未命中
     */
    @Benchmark
    public String cacheMiss(LocalCacheState state) {
        return state.cache.get(LocalCacheState.COLD_KEY);
    }

    // ==================== 缓存写入测试 ====================

    /**
     * 测试缓存写入 - 单次
     */
    @Benchmark
    public void cachePutSingle(LocalCacheState state) {
        state.cache.put("put-key", LocalCacheState.TEST_VALUE);
    }

    /**
     * 测试缓存写入 - 多次
     */
    @Benchmark
    public void cachePutMultiple(LocalCacheState state) {
        state.cache.put("put-key-1", LocalCacheState.TEST_VALUE);
        state.cache.put("put-key-2", LocalCacheState.TEST_VALUE);
        state.cache.put("put-key-3", LocalCacheState.TEST_VALUE);
        state.cache.put("put-key-4", LocalCacheState.TEST_VALUE);
        state.cache.put("put-key-5", LocalCacheState.TEST_VALUE);
    }

    /**
     * 测试缓存写入 - 指定 TTL
     */
    @Benchmark
    public void cachePutWithTtl(LocalCacheState state) {
        state.cache.put("ttl-key", LocalCacheState.TEST_VALUE, 60000);
    }

    // ==================== 缓存删除测试 ====================

    /**
     * 测试缓存删除
     */
    @Benchmark
    public void cacheEvict(LocalCacheState state) {
        state.cache.evict("evict-key");
    }

    /**
     * 测试缓存清空
     */
    @Benchmark
    public void cacheClear(LargeCacheState state) {
        state.cache.clear();
    }

    // ==================== putIfAbsent 测试 ====================

    /**
     * 测试 putIfAbsent - 已存在
     */
    @Benchmark
    public String putIfAbsentExisting(LocalCacheState state) {
        return state.cache.putIfAbsent(LocalCacheState.HOT_KEY, LocalCacheState.TEST_VALUE);
    }

    /**
     * 测试 putIfAbsent - 不存在
     */
    @Benchmark
    public String putIfAbsentNew(LocalCacheState state) {
        return state.cache.putIfAbsent("new-key", LocalCacheState.TEST_VALUE);
    }

    // ==================== getOrLoad 测试 ====================

    /**
     * 测试 getOrLoad - 缓存命中（加载器不执行）
     */
    @Benchmark
    public String getOrLoadHit(LocalCacheState state) {
        return state.cache.getOrLoad(LocalCacheState.HOT_KEY, () -> {
            // 此加载器在缓存命中时不会执行
            return "loaded-value";
        });
    }

    /**
     * 测试 getOrLoad - 缓存未命中（加载器执行）
     */
    @Benchmark
    public String getOrLoadMiss(LocalCacheState state) {
        return state.cache.getOrLoad(LocalCacheState.COLD_KEY, () -> {
            return "loaded-value";
        });
    }

    // ==================== containsKey 测试 ====================

    /**
     * 测试 containsKey - 存在
     */
    @Benchmark
    public boolean containsKeyExists(LocalCacheState state) {
        return state.cache.containsKey(LocalCacheState.HOT_KEY);
    }

    /**
     * 测试 containsKey - 不存在
     */
    @Benchmark
    public boolean containsKeyNotExists(LocalCacheState state) {
        return state.cache.containsKey(LocalCacheState.COLD_KEY);
    }

    // ==================== size 测试 ====================

    /**
     * 测试 size 获取
     */
    @Benchmark
    public long cacheSize(LargeCacheState state) {
        return state.cache.size();
    }

    // ==================== CacheManager 测试 ====================

    /**
     * 测试从 CacheManager 获取缓存
     */
    @Benchmark
    public AfgCache<String> getCacheFromManager(LocalCacheState state) {
        return state.cacheManager.getCache("benchmark-cache");
    }

    /**
     * 测试创建新缓存
     */
    @Benchmark
    public AfgCache<String> createNewCache(LocalCacheState state) {
        return state.cacheManager.getCache("new-cache-" + Thread.currentThread().getId());
    }

    /**
     * 运行基准测试
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(CacheManagerBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}