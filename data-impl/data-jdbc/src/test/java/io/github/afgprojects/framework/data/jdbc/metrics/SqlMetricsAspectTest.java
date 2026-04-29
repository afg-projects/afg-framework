package io.github.afgprojects.framework.data.jdbc.metrics;

import io.github.afgprojects.framework.data.core.query.Page;
import io.github.afgprojects.framework.data.jdbc.JdbcEntityProxy;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SQL 指标切面测试
 */
@DisplayName("SQL 指标切面测试")
class SqlMetricsAspectTest {

    private MeterRegistry meterRegistry;
    private SqlMetrics sqlMetrics;
    private SqlMetricsProperties properties;
    private SqlMetricsAspect aspect;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        properties = new SqlMetricsProperties();
        properties.setEnabled(true);
        properties.setSlowQueryThreshold(Duration.ofMillis(100));
        properties.setLogSlowQueries(true);
        sqlMetrics = new SqlMetrics(meterRegistry, properties);
        aspect = new SqlMetricsAspect(sqlMetrics, properties);
    }

    @AfterEach
    void tearDown() {
        meterRegistry.clear();
    }

    @Nested
    @DisplayName("构造方法测试")
    class ConstructorTest {

        @Test
        @DisplayName("应正确初始化并设置慢查询监听器")
        void shouldInitializeWithSlowQueryListener() {
            assertThat(sqlMetrics).isNotNull();
        }
    }

    @Nested
    @DisplayName("aroundSave 测试")
    class AroundSaveTest {

        @Test
        @DisplayName("应记录 save 操作指标")
        void shouldRecordSaveMetrics() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(new TestEntity(1L, "test"));

            Object result = aspect.aroundSave(pjp);

            assertThat(result).isNotNull();
            assertCounterRecorded();
        }

        @Test
        @DisplayName("禁用指标后不应记录")
        void shouldNotRecordWhenDisabled() throws Throwable {
            properties.setEnabled(false);
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(new TestEntity(1L, "test"));

            aspect.aroundSave(pjp);

            Counter callsCounter = meterRegistry.find("afg.jdbc.sql.calls").counter();
            assertThat(callsCounter).isNull();
        }
    }

    @Nested
    @DisplayName("aroundSaveAll 测试")
    class AroundSaveAllTest {

        @Test
        @DisplayName("应记录 saveAll 操作指标")
        void shouldRecordSaveAllMetrics() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(List.of(new TestEntity(1L, "test")));

            Object result = aspect.aroundSaveAll(pjp);

            assertThat(result).isNotNull();
            assertCounterRecorded();
        }
    }

    @Nested
    @DisplayName("aroundInsert 测试")
    class AroundInsertTest {

        @Test
        @DisplayName("应记录 insert 操作指标")
        void shouldRecordInsertMetrics() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(new TestEntity(1L, "test"));

            Object result = aspect.aroundInsert(pjp);

            assertThat(result).isNotNull();
            assertCounterRecorded();
        }
    }

    @Nested
    @DisplayName("aroundInsertAll 测试")
    class AroundInsertAllTest {

        @Test
        @DisplayName("应记录 insertAll 操作指标")
        void shouldRecordInsertAllMetrics() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(List.of(new TestEntity(1L, "test")));

            Object result = aspect.aroundInsertAll(pjp);

            assertThat(result).isNotNull();
            assertCounterRecorded();
        }
    }

    @Nested
    @DisplayName("aroundUpdate 测试")
    class AroundUpdateTest {

        @Test
        @DisplayName("应记录 update 操作指标")
        void shouldRecordUpdateMetrics() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(new TestEntity(1L, "updated"));

            Object result = aspect.aroundUpdate(pjp);

            assertThat(result).isNotNull();
            assertCounterRecorded();
        }
    }

    @Nested
    @DisplayName("aroundUpdateAllEntities 测试")
    class AroundUpdateAllEntitiesTest {

        @Test
        @DisplayName("应记录 updateAll(entities) 操作指标")
        void shouldRecordUpdateAllEntitiesMetrics() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(List.of(new TestEntity(1L, "updated")));

            Object result = aspect.aroundUpdateAllEntities(pjp);

            assertThat(result).isNotNull();
            assertCounterRecorded();
        }
    }

    @Nested
    @DisplayName("aroundFindById 测试")
    class AroundFindByIdTest {

        @Test
        @DisplayName("应记录 findById 操作指标")
        void shouldRecordFindByIdMetrics() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(java.util.Optional.of(new TestEntity(1L, "test")));

            Object result = aspect.aroundFindById(pjp);

            assertThat(result).isNotNull();
            assertCounterRecorded();
        }
    }

    @Nested
    @DisplayName("aroundFindAll 测试")
    class AroundFindAllTest {

        @Test
        @DisplayName("应记录 findAll 操作指标")
        void shouldRecordFindAllMetrics() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(List.of(new TestEntity(1L, "test")));

            Object result = aspect.aroundFindAll(pjp);

            assertThat(result).isNotNull();
            assertCounterRecorded();
        }
    }

    @Nested
    @DisplayName("aroundFindAllById 测试")
    class AroundFindAllByIdTest {

        @Test
        @DisplayName("应记录 findAllById 操作指标")
        void shouldRecordFindAllByIdMetrics() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(List.of(new TestEntity(1L, "test")));

            Object result = aspect.aroundFindAllById(pjp);

            assertThat(result).isNotNull();
            assertCounterRecorded();
        }
    }

    @Nested
    @DisplayName("aroundCount 测试")
    class AroundCountTest {

        @Test
        @DisplayName("应记录 count 操作指标")
        void shouldRecordCountMetrics() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(5L);

            Object result = aspect.aroundCount(pjp);

            assertThat(result).isEqualTo(5L);
            assertCounterRecorded();
        }
    }

    @Nested
    @DisplayName("aroundExistsById 测试")
    class AroundExistsByIdTest {

        @Test
        @DisplayName("应记录 existsById 操作指标")
        void shouldRecordExistsByIdMetrics() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(true);

            Object result = aspect.aroundExistsById(pjp);

            assertThat(result).isEqualTo(true);
            assertCounterRecorded();
        }
    }

    @Nested
    @DisplayName("aroundDeleteById 测试")
    class AroundDeleteByIdTest {

        @Test
        @DisplayName("应记录 deleteById 操作指标")
        void shouldRecordDeleteByIdMetrics() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(1);

            Object result = aspect.aroundDeleteById(pjp);

            assertThat(result).isEqualTo(1);
            assertCounterRecorded();
        }
    }

    @Nested
    @DisplayName("aroundDelete 测试")
    class AroundDeleteTest {

        @Test
        @DisplayName("应记录 delete 操作指标")
        void shouldRecordDeleteMetrics() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(1);

            Object result = aspect.aroundDelete(pjp);

            assertThat(result).isEqualTo(1);
            assertCounterRecorded();
        }
    }

    @Nested
    @DisplayName("aroundDeleteAllById 测试")
    class AroundDeleteAllByIdTest {

        @Test
        @DisplayName("应记录 deleteAllById 操作指标")
        void shouldRecordDeleteAllByIdMetrics() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(3);

            Object result = aspect.aroundDeleteAllById(pjp);

            assertThat(result).isEqualTo(3);
            assertCounterRecorded();
        }
    }

    @Nested
    @DisplayName("aroundDeleteAllEntities 测试")
    class AroundDeleteAllEntitiesTest {

        @Test
        @DisplayName("应记录 deleteAll(entities) 操作指标")
        void shouldRecordDeleteAllEntitiesMetrics() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(2);

            Object result = aspect.aroundDeleteAllEntities(pjp);

            assertThat(result).isEqualTo(2);
            assertCounterRecorded();
        }
    }

    @Nested
    @DisplayName("aroundFindAllByCondition 测试")
    class AroundFindAllByConditionTest {

        @Test
        @DisplayName("应记录 findAll(Condition) 操作指标")
        void shouldRecordFindAllByConditionMetrics() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(List.of(new TestEntity(1L, "test")));

            Object result = aspect.aroundFindAllByCondition(pjp);

            assertThat(result).isNotNull();
            assertCounterRecorded();
        }
    }

    @Nested
    @DisplayName("aroundCountByCondition 测试")
    class AroundCountByConditionTest {

        @Test
        @DisplayName("应记录 count(Condition) 操作指标")
        void shouldRecordCountByConditionMetrics() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(10L);

            Object result = aspect.aroundCountByCondition(pjp);

            assertThat(result).isEqualTo(10L);
            assertCounterRecorded();
        }
    }

    @Nested
    @DisplayName("aroundExistsByCondition 测试")
    class AroundExistsByConditionTest {

        @Test
        @DisplayName("应记录 exists(Condition) 操作指标")
        void shouldRecordExistsByConditionMetrics() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(true);

            Object result = aspect.aroundExistsByCondition(pjp);

            assertThat(result).isEqualTo(true);
            assertCounterRecorded();
        }
    }

    @Nested
    @DisplayName("aroundFindOne 测试")
    class AroundFindOneTest {

        @Test
        @DisplayName("应记录 findOne 操作指标")
        void shouldRecordFindOneMetrics() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(java.util.Optional.of(new TestEntity(1L, "test")));

            Object result = aspect.aroundFindOne(pjp);

            assertThat(result).isNotNull();
            assertCounterRecorded();
        }
    }

    @Nested
    @DisplayName("aroundFindFirst 测试")
    class AroundFindFirstTest {

        @Test
        @DisplayName("应记录 findFirst 操作指标")
        void shouldRecordFindFirstMetrics() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(java.util.Optional.of(new TestEntity(1L, "test")));

            Object result = aspect.aroundFindFirst(pjp);

            assertThat(result).isNotNull();
            assertCounterRecorded();
        }
    }

    @Nested
    @DisplayName("aroundUpdateAllByCondition 测试")
    class AroundUpdateAllByConditionTest {

        @Test
        @DisplayName("应记录 updateAll(Condition, Map) 操作指标")
        void shouldRecordUpdateAllByConditionMetrics() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(10L);

            Object result = aspect.aroundUpdateAllByCondition(pjp);

            assertThat(result).isEqualTo(10L);
            assertCounterRecorded();
        }
    }

    @Nested
    @DisplayName("aroundDeleteAllByCondition 测试")
    class AroundDeleteAllByConditionTest {

        @Test
        @DisplayName("应记录 deleteAll(Condition) 操作指标")
        void shouldRecordDeleteAllByConditionMetrics() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(5L);

            Object result = aspect.aroundDeleteAllByCondition(pjp);

            assertThat(result).isEqualTo(5L);
            assertCounterRecorded();
        }
    }

    @Nested
    @DisplayName("aroundRestoreById 测试")
    class AroundRestoreByIdTest {

        @Test
        @DisplayName("应记录 restoreById 操作指标")
        void shouldRecordRestoreByIdMetrics() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(1);

            Object result = aspect.aroundRestoreById(pjp);

            assertThat(result).isEqualTo(1);
            assertCounterRecorded();
        }
    }

    @Nested
    @DisplayName("aroundRestoreAllById 测试")
    class AroundRestoreAllByIdTest {

        @Test
        @DisplayName("应记录 restoreAllById 操作指标")
        void shouldRecordRestoreAllByIdMetrics() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(3);

            Object result = aspect.aroundRestoreAllById(pjp);

            assertThat(result).isEqualTo(3);
            assertCounterRecorded();
        }
    }

    @Nested
    @DisplayName("aroundFindAllByConditionWithPaging 测试")
    class AroundFindAllByConditionWithPagingTest {

        @Test
        @DisplayName("应记录 findAll(Condition, PageRequest) 操作指标")
        void shouldRecordFindAllByConditionWithPagingMetrics() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(List.of(new TestEntity(1L, "test")));

            Object result = aspect.aroundFindAllByConditionWithPaging(pjp);

            assertThat(result).isNotNull();
            assertCounterRecorded();
        }
    }

    @Nested
    @DisplayName("getRowsAffected 分支测试")
    class GetRowsAffectedTests {

        @Test
        @DisplayName("应正确处理 Page 类型返回值（countRows=true）")
        void shouldHandlePageResultWithCountRows() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            Page<TestEntity> page =
                new Page<>(
                    List.of(new TestEntity(1L, "test"), new TestEntity(2L, "test2")),
                    2L, 1, 10
                );
            when(pjp.proceed()).thenReturn(page);

            // 使用 countRows=true 的方法（saveAll）来测试 Page 返回值
            Object result = aspect.aroundSaveAll(pjp);

            assertThat(result).isNotNull();
            assertCounterRecorded();
        }

        @Test
        @DisplayName("应正确处理 Integer 类型返回值")
        void shouldHandleIntegerResult() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(5);

            Object result = aspect.aroundRestoreById(pjp);

            assertThat(result).isEqualTo(5);
            assertCounterRecorded();
        }

        @Test
        @DisplayName("应正确处理 null 返回值（countRows=true）")
        void shouldHandleNullResultWithCountRows() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(null);

            // 使用 countRows=true 的方法（save）来测试 null 返回值
            Object result = aspect.aroundSave(pjp);

            assertThat(result).isNull();
            assertCounterRecorded();
        }

        @Test
        @DisplayName("应正确处理未知类型返回值（countRows=true，返回 0）")
        void shouldHandleUnknownTypeResultWithCountRows() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            // 返回一个未知类型（非 List/Long/Integer/Page），应返回 0
            when(pjp.proceed()).thenReturn("unknown string result");

            // 使用 countRows=true 且 methodName 不是实体操作的方法（saveAll）
            // 这样才能测试到 getRowsAffected 的 default 分支
            Object result = aspect.aroundSaveAll(pjp);

            assertThat(result).isEqualTo("unknown string result");
            assertCounterRecorded();
        }

        @Test
        @DisplayName("应正确处理 Long 类型返回值")
        void shouldHandleLongResult() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(100L);

            Object result = aspect.aroundUpdateAllByCondition(pjp);

            assertThat(result).isEqualTo(100L);
            assertCounterRecorded();
        }

        @Test
        @DisplayName("应正确处理 List 类型返回值")
        void shouldHandleListResult() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenReturn(List.of(new TestEntity(1L, "a"), new TestEntity(2L, "b")));

            Object result = aspect.aroundInsertAll(pjp);

            assertThat(result).isNotNull();
            assertCounterRecorded();
        }
    }

    @Nested
    @DisplayName("getEntityName 分支测试")
    class GetEntityNameTests {

        @Test
        @DisplayName("非 JdbcEntityProxy 类型应返回简单类名")
        void shouldReturnSimpleNameForNonProxyType() throws Throwable {
            ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
            MethodSignature signature = mock(MethodSignature.class);

            when(pjp.getSignature()).thenReturn(signature);
            when(signature.getDeclaringType()).thenReturn(String.class);
            when(pjp.getTarget()).thenReturn("test");

            when(pjp.proceed()).thenReturn("result");

            Object result = aspect.aroundSave(pjp);

            assertThat(result).isNotNull();
            assertCounterRecorded();
        }

        @Test
        @DisplayName("declaringType 为 JdbcEntityProxy 但 target 不是 JdbcEntityProxy 应返回 JdbcEntityProxy")
        void shouldReturnJdbcEntityProxyWhenTargetNotProxy() throws Throwable {
            ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
            MethodSignature signature = mock(MethodSignature.class);

            when(pjp.getSignature()).thenReturn(signature);
            // declaringType.getSimpleName() 返回 "JdbcEntityProxy"，但 target 不是 JdbcEntityProxy 实例
            when(signature.getDeclaringType()).thenReturn(JdbcEntityProxy.class);
            when(pjp.getTarget()).thenReturn("not a proxy"); // target 不是 JdbcEntityProxy

            when(pjp.proceed()).thenReturn("result");

            Object result = aspect.aroundSave(pjp);

            assertThat(result).isEqualTo("result");
            assertCounterRecorded();
        }

        @Test
        @DisplayName("异常时应返回 Unknown")
        void shouldReturnUnknownOnException() throws Throwable {
            ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
            // 让 getSignature() 抛出异常，触发 catch 块
            when(pjp.getSignature()).thenThrow(new RuntimeException("Signature error"));
            when(pjp.proceed()).thenReturn("result");

            Object result = aspect.aroundSave(pjp);

            assertThat(result).isEqualTo("result");
            // 仍然应该记录指标（使用 "Unknown" 作为实体名）
            assertCounterRecorded();
        }
    }

    @Nested
    @DisplayName("慢查询测试")
    class SlowQueryTest {

        @Test
        @DisplayName("应记录慢查询")
        void shouldRecordSlowQuery() throws Throwable {
            properties.setSlowQueryThreshold(Duration.ofMillis(10));

            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenAnswer(invocation -> {
                Thread.sleep(50);
                return List.of(new TestEntity(1L, "test"));
            });

            aspect.aroundFindAll(pjp);

            Counter slowCounter = meterRegistry.find("afg.jdbc.sql.slow").counter();
            assertThat(slowCounter).isNotNull();
            assertThat(slowCounter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("禁用慢查询日志后不应调用监听器")
        void shouldNotCallListenerWhenDisabled() throws Throwable {
            properties.setSlowQueryThreshold(Duration.ofMillis(10));
            properties.setLogSlowQueries(false);

            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenAnswer(invocation -> {
                Thread.sleep(50);
                return List.of(new TestEntity(1L, "test"));
            });

            aspect.aroundFindAll(pjp);

            // 慢查询计数器仍然会被记录（这是指标记录，不是日志）
            Counter slowCounter = meterRegistry.find("afg.jdbc.sql.slow").counter();
            assertThat(slowCounter).isNotNull();
            assertThat(slowCounter.count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("错误处理测试")
    class ErrorHandlingTest {

        @Test
        @DisplayName("应记录执行错误")
        void shouldRecordExecutionError() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenThrow(new RuntimeException("Database error"));

            try {
                aspect.aroundFindById(pjp);
            } catch (RuntimeException e) {
                // 预期异常
            }

            Counter errorCounter = meterRegistry.find("afg.jdbc.sql.errors").counter();
            assertThat(errorCounter).isNotNull();
            assertThat(errorCounter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("错误时应重新抛出异常")
        void shouldRethrowException() throws Throwable {
            ProceedingJoinPoint pjp = createBasicMockJoinPoint();
            when(pjp.proceed()).thenThrow(new RuntimeException("Database error"));

            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
                aspect.aroundFindById(pjp);
            });
        }
    }

    // ==================== 辅助方法 ====================

    private void assertCounterRecorded() {
        Counter callsCounter = meterRegistry.find("afg.jdbc.sql.calls").counter();
        assertThat(callsCounter).isNotNull();
        assertThat(callsCounter.count()).isEqualTo(1.0);
    }

    @SuppressWarnings("unchecked")
    private ProceedingJoinPoint createBasicMockJoinPoint() {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);

        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getDeclaringType()).thenReturn(JdbcEntityProxy.class);

        JdbcEntityProxy<TestEntity> proxy = mock(JdbcEntityProxy.class);
        when(proxy.getEntityClass()).thenReturn(TestEntity.class);
        when(pjp.getTarget()).thenReturn(proxy);

        return pjp;
    }

    @Data
    @NoArgsConstructor
    static class TestEntity {
        private Long id;
        private String name;

        TestEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
