package io.github.afgprojects.framework.core.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.github.afgprojects.framework.core.support.BaseUnitTest;

/**
 * CacheAspect 单元测试
 */
@DisplayName("CacheAspect 单元测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CacheAspectTest extends BaseUnitTest {

    @Mock
    private DefaultCacheManager cacheManager;

    @Mock
    private AfgCache<Object> cache;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private CacheAspect cacheAspect;

    @BeforeEach
    void setUp() {
        cacheAspect = new CacheAspect(cacheManager);
    }

    @Nested
    @DisplayName("@Cached 注解测试")
    class CachedTests {

        @Test
        @DisplayName("缓存命中时应该直接返回缓存值")
        void shouldReturnCachedValueWhenHit() throws Throwable {
            // given
            Cached annotation = createCachedAnnotation("test-cache", "#id", "", 60, TimeUnit.SECONDS, "", "", false);
            when(cacheManager.getCache("test-cache")).thenReturn(cache);
            when(cache.get("123")).thenReturn("cached-value");
            setupJoinPoint("testMethod", new Object[]{"123"});

            // when
            Object result = cacheAspect.aroundCached(joinPoint, annotation);

            // then
            assertThat(result).isEqualTo("cached-value");
            verify(joinPoint, never()).proceed();
        }

        @Test
        @DisplayName("缓存未命中时应该执行方法并缓存结果")
        void shouldExecuteAndCacheWhenMiss() throws Throwable {
            // given
            Cached annotation = createCachedAnnotation("test-cache", "#id", "", 60, TimeUnit.SECONDS, "", "", false);
            when(cacheManager.getCache("test-cache")).thenReturn(cache);
            when(cache.get("123")).thenReturn(null);
            setupJoinPoint("testMethod", new Object[]{"123"});
            when(joinPoint.proceed()).thenReturn("method-result");

            // when
            Object result = cacheAspect.aroundCached(joinPoint, annotation);

            // then
            assertThat(result).isEqualTo("method-result");
            verify(joinPoint).proceed();
            verify(cache).put(anyString(), any(), anyLong());
        }

        @Test
        @DisplayName("条件不满足时应该跳过缓存")
        void shouldSkipCacheWhenConditionNotMet() throws Throwable {
            // given
            Cached annotation = createCachedAnnotation("test-cache", "#id", "", 60, TimeUnit.SECONDS, "false", "", false);
            when(cacheManager.getCache("test-cache")).thenReturn(cache);
            setupJoinPoint("testMethod", new Object[]{"123"});
            when(joinPoint.proceed()).thenReturn("method-result");

            // when
            Object result = cacheAspect.aroundCached(joinPoint, annotation);

            // then
            assertThat(result).isEqualTo("method-result");
            verify(cache, never()).get(anyString());
        }

        @Test
        @DisplayName("unless 条件满足时应该不缓存结果")
        void shouldNotCacheWhenUnlessMet() throws Throwable {
            // given
            Cached annotation = createCachedAnnotation("test-cache", "#id", "", 60, TimeUnit.SECONDS, "", "#result == 'skip'", false);
            when(cacheManager.getCache("test-cache")).thenReturn(cache);
            when(cache.get("123")).thenReturn(null);
            setupJoinPoint("testMethod", new Object[]{"123"});
            when(joinPoint.proceed()).thenReturn("skip");

            // when
            Object result = cacheAspect.aroundCached(joinPoint, annotation);

            // then
            assertThat(result).isEqualTo("skip");
            verify(cache, never()).put(anyString(), any(), anyLong());
        }

        @Test
        @DisplayName("应该缓存 null 值当 cacheNull 为 true")
        void shouldCacheNullWhenCacheNullIsTrue() throws Throwable {
            // given
            Cached annotation = createCachedAnnotation("test-cache", "#id", "", 60, TimeUnit.SECONDS, "", "", true);
            when(cacheManager.getCache("test-cache")).thenReturn(cache);
            when(cache.get("123")).thenReturn(null);
            setupJoinPoint("testMethod", new Object[]{"123"});
            when(joinPoint.proceed()).thenReturn(null);

            // when
            Object result = cacheAspect.aroundCached(joinPoint, annotation);

            // then
            assertThat(result).isNull();
            verify(cache).put(anyString(), any(), anyLong());
        }
    }

    @Nested
    @DisplayName("@CacheEvict 注解测试")
    class CacheEvictTests {

        @Test
        @DisplayName("应该清除指定键")
        void shouldEvictKey() throws Throwable {
            // given
            CacheEvict annotation = createCacheEvictAnnotation("test-cache", "#id", "", false, false, "");
            when(cacheManager.getCache("test-cache")).thenReturn(cache);
            setupJoinPoint("testMethod", new Object[]{"123"});
            when(joinPoint.proceed()).thenReturn("result");

            // when
            Object result = cacheAspect.aroundCacheEvict(joinPoint, annotation);

            // then
            assertThat(result).isEqualTo("result");
            verify(cache).evict(anyString());
        }

        @Test
        @DisplayName("应该清除所有条目")
        void shouldEvictAllEntries() throws Throwable {
            // given
            CacheEvict annotation = createCacheEvictAnnotation("test-cache", "", "", true, false, "");
            when(cacheManager.getCache("test-cache")).thenReturn(cache);
            setupJoinPoint("testMethod", new Object[]{});
            when(joinPoint.proceed()).thenReturn("result");

            // when
            Object result = cacheAspect.aroundCacheEvict(joinPoint, annotation);

            // then
            assertThat(result).isEqualTo("result");
            verify(cache).clear();
        }

        @Test
        @DisplayName("方法执行前应该清除缓存")
        void shouldEvictBeforeInvocation() throws Throwable {
            // given
            CacheEvict annotation = createCacheEvictAnnotation("test-cache", "#id", "", false, true, "");
            when(cacheManager.getCache("test-cache")).thenReturn(cache);
            setupJoinPoint("testMethod", new Object[]{"123"});
            when(joinPoint.proceed()).thenReturn("result");

            // when
            Object result = cacheAspect.aroundCacheEvict(joinPoint, annotation);

            // then
            assertThat(result).isEqualTo("result");
            verify(cache).evict(anyString());
        }

        @Test
        @DisplayName("条件不满足时应该跳过清除")
        void shouldSkipEvictWhenConditionNotMet() throws Throwable {
            // given
            CacheEvict annotation = createCacheEvictAnnotation("test-cache", "#id", "", false, false, "false");
            when(cacheManager.getCache("test-cache")).thenReturn(cache);
            setupJoinPoint("testMethod", new Object[]{"123"});
            when(joinPoint.proceed()).thenReturn("result");

            // when
            Object result = cacheAspect.aroundCacheEvict(joinPoint, annotation);

            // then
            assertThat(result).isEqualTo("result");
            verify(cache, never()).evict(anyString());
        }
    }

    @Nested
    @DisplayName("@CachePut 注解测试")
    class CachePutTests {

        @Test
        @DisplayName("应该更新缓存")
        void shouldPutCache() throws Throwable {
            // given
            CachePut annotation = createCachePutAnnotation("test-cache", "#id", "", 60, TimeUnit.SECONDS, "", "", false);
            when(cacheManager.getCache("test-cache")).thenReturn(cache);
            setupJoinPoint("testMethod", new Object[]{"123"});
            when(joinPoint.proceed()).thenReturn("result");

            // when
            Object result = cacheAspect.aroundCachePut(joinPoint, annotation);

            // then
            assertThat(result).isEqualTo("result");
            verify(cache).put(anyString(), any(), anyLong());
        }

        @Test
        @DisplayName("条件不满足时应该跳过更新")
        void shouldSkipPutWhenConditionNotMet() throws Throwable {
            // given
            CachePut annotation = createCachePutAnnotation("test-cache", "#id", "", 60, TimeUnit.SECONDS, "false", "", false);
            when(cacheManager.getCache("test-cache")).thenReturn(cache);
            setupJoinPoint("testMethod", new Object[]{"123"});
            when(joinPoint.proceed()).thenReturn("result");

            // when
            Object result = cacheAspect.aroundCachePut(joinPoint, annotation);

            // then
            assertThat(result).isEqualTo("result");
            verify(cache, never()).put(anyString(), any(), anyLong());
        }

        @Test
        @DisplayName("unless 条件满足时应该跳过更新")
        void shouldSkipPutWhenUnlessMet() throws Throwable {
            // given
            CachePut annotation = createCachePutAnnotation("test-cache", "#id", "", 60, TimeUnit.SECONDS, "", "#result == 'skip'", false);
            when(cacheManager.getCache("test-cache")).thenReturn(cache);
            setupJoinPoint("testMethod", new Object[]{"123"});
            when(joinPoint.proceed()).thenReturn("skip");

            // when
            Object result = cacheAspect.aroundCachePut(joinPoint, annotation);

            // then
            assertThat(result).isEqualTo("skip");
            verify(cache, never()).put(anyString(), any(), anyLong());
        }
    }

    // Helper methods to create annotation mocks
    private Cached createCachedAnnotation(String cacheName, String key, String keyPrefix,
                                          long ttl, TimeUnit timeUnit, String condition, String unless, boolean cacheNull) {
        return new Cached() {
            @Override public String cacheName() { return cacheName; }
            @Override public String key() { return key; }
            @Override public String keyPrefix() { return keyPrefix; }
            @Override public long ttl() { return ttl; }
            @Override public TimeUnit timeUnit() { return timeUnit; }
            @Override public String condition() { return condition; }
            @Override public String unless() { return unless; }
            @Override public boolean cacheNull() { return cacheNull; }
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return Cached.class; }
        };
    }

    private CacheEvict createCacheEvictAnnotation(String cacheName, String key, String keyPrefix,
                                                   boolean allEntries, boolean beforeInvocation, String condition) {
        return new CacheEvict() {
            @Override public String cacheName() { return cacheName; }
            @Override public String key() { return key; }
            @Override public String keyPrefix() { return keyPrefix; }
            @Override public boolean allEntries() { return allEntries; }
            @Override public boolean beforeInvocation() { return beforeInvocation; }
            @Override public String condition() { return condition; }
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return CacheEvict.class; }
        };
    }

    private CachePut createCachePutAnnotation(String cacheName, String key, String keyPrefix,
                                               long ttl, TimeUnit timeUnit, String condition, String unless, boolean cacheNull) {
        return new CachePut() {
            @Override public String cacheName() { return cacheName; }
            @Override public String key() { return key; }
            @Override public String keyPrefix() { return keyPrefix; }
            @Override public long ttl() { return ttl; }
            @Override public TimeUnit timeUnit() { return timeUnit; }
            @Override public String condition() { return condition; }
            @Override public String unless() { return unless; }
            @Override public boolean cacheNull() { return cacheNull; }
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return CachePut.class; }
        };
    }

    private void setupJoinPoint(String methodName, Object[] args) throws NoSuchMethodException {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(args);

        Method method;
        if (args.length > 0) {
            method = TestService.class.getMethod(methodName, String.class);
        } else {
            method = TestService.class.getMethod(methodName);
        }
        when(methodSignature.getMethod()).thenReturn(method);

        java.lang.reflect.Parameter[] parameters = method.getParameters();
        when(methodSignature.getParameterNames()).thenReturn(
            java.util.Arrays.stream(parameters).map(java.lang.reflect.Parameter::getName).toArray(String[]::new)
        );
    }

    // Test service for method reflection
    public static class TestService {
        public void testMethod(String id) {}
        public void testMethod() {}
    }
}
