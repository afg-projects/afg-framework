package io.github.afgprojects.framework.data.core.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tenant 包测试
 * <p>
 * 重点测试多租户上下文的线程隔离和并发场景
 */
@DisplayName("Tenant 包测试")
class TenantTest {

    private ThreadLocalTenantContext tenantContext;

    @BeforeEach
    void setUp() {
        tenantContext = new ThreadLocalTenantContext();
    }

    @AfterEach
    void tearDown() {
        tenantContext.clear();
    }

    // ==================== ThreadLocalTenantContext 测试 ====================

    @Nested
    @DisplayName("ThreadLocalTenantContext 测试")
    class ThreadLocalTenantContextTest {

        @Test
        @DisplayName("应正确设置和获取租户ID")
        void shouldSetAndGetTenantId() {
            assertThat(tenantContext.getTenantId()).isNull();

            tenantContext.setTenantId("tenant-001");
            assertThat(tenantContext.getTenantId()).isEqualTo("tenant-001");

            tenantContext.setTenantId("tenant-002");
            assertThat(tenantContext.getTenantId()).isEqualTo("tenant-002");
        }

        @Test
        @DisplayName("设置 null 应清除租户ID")
        void shouldClearTenantIdWhenSetNull() {
            tenantContext.setTenantId("tenant-001");
            assertThat(tenantContext.getTenantId()).isEqualTo("tenant-001");

            tenantContext.setTenantId(null);
            assertThat(tenantContext.getTenantId()).isNull();
        }

        @Test
        @DisplayName("clear 应清除租户ID和忽略标记")
        void shouldClearAllOnClear() {
            tenantContext.setTenantId("tenant-001");
            tenantContext.setIgnoreTenant(true);

            tenantContext.clear();

            assertThat(tenantContext.getTenantId()).isNull();
            assertThat(tenantContext.isIgnoreTenant()).isFalse();
        }

        @Test
        @DisplayName("应正确设置和获取忽略租户标记")
        void shouldSetAndGetIgnoreTenant() {
            assertThat(tenantContext.isIgnoreTenant()).isFalse();

            tenantContext.setIgnoreTenant(true);
            assertThat(tenantContext.isIgnoreTenant()).isTrue();

            tenantContext.setIgnoreTenant(false);
            assertThat(tenantContext.isIgnoreTenant()).isFalse();
        }

        @Test
        @DisplayName("runWithoutTenant 应临时忽略租户隔离")
        void shouldRunWithoutTenant() {
            tenantContext.setIgnoreTenant(false);

            // 在忽略租户的上下文中执行
            tenantContext.runWithoutTenant(() -> {
                assertThat(tenantContext.isIgnoreTenant()).isTrue();
            });

            // 执行后应恢复原状态
            assertThat(tenantContext.isIgnoreTenant()).isFalse();
        }

        @Test
        @DisplayName("runWithoutTenant 应恢复原有的忽略状态")
        void shouldRestoreOriginalIgnoreState() {
            tenantContext.setIgnoreTenant(true);

            tenantContext.runWithoutTenant(() -> {
                // 已经是忽略状态，执行中仍为 true
                assertThat(tenantContext.isIgnoreTenant()).isTrue();
            });

            // 应恢复原有的 true 状态
            assertThat(tenantContext.isIgnoreTenant()).isTrue();
        }

        @Test
        @DisplayName("runWithoutTenant 异常时应恢复状态")
        void shouldRestoreStateOnException() {
            tenantContext.setIgnoreTenant(false);

            try {
                tenantContext.runWithoutTenant(() -> {
                    throw new RuntimeException("Test exception");
                });
            } catch (RuntimeException e) {
                // 忽略异常
            }

            // 即使异常也应恢复状态
            assertThat(tenantContext.isIgnoreTenant()).isFalse();
        }

        @Test
        @DisplayName("不同线程应隔离租户ID")
        void shouldIsolateTenantIdBetweenThreads() throws InterruptedException {
            tenantContext.setTenantId("main-tenant");

            ExecutorService executor = Executors.newSingleThreadExecutor();
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> otherThreadTenantId = new AtomicReference<>();

            executor.submit(() -> {
                // 子线程设置不同的租户ID
                ThreadLocalTenantContext otherContext = new ThreadLocalTenantContext();
                otherContext.setTenantId("sub-tenant");
                otherThreadTenantId.set(otherContext.getTenantId());
                latch.countDown();
            });

            latch.await();
            executor.shutdown();

            // 主线程的租户ID不受影响
            assertThat(tenantContext.getTenantId()).isEqualTo("main-tenant");
            assertThat(otherThreadTenantId.get()).isEqualTo("sub-tenant");
        }

        @Test
        @DisplayName("多线程并发设置租户ID应互不干扰")
        void concurrentTenantIdSettingShouldNotInterfer() throws InterruptedException {
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(threadCount);
            AtomicReference<String>[] results = new AtomicReference[threadCount];

            for (int i = 0; i < threadCount; i++) {
                results[i] = new AtomicReference<>();
                final int threadIndex = i;
                executor.submit(() -> {
                    try {
                        startLatch.await(); // 等待所有线程就绪
                        ThreadLocalTenantContext ctx = new ThreadLocalTenantContext();
                        ctx.setTenantId("tenant-" + threadIndex);
                        results[threadIndex].set(ctx.getTenantId());
                    } catch (InterruptedException e) {
                        results[threadIndex].set("error");
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown(); // 启动所有线程
            endLatch.await(); // 等待所有线程完成
            executor.shutdown();

            // 每个线程应获取到自己设置的租户ID
            for (int i = 0; i < threadCount; i++) {
                assertThat(results[i].get()).isEqualTo("tenant-" + i);
            }
        }
    }

    // ==================== TenantAware 接口测试 ====================

    @Nested
    @DisplayName("TenantAware 接口测试")
    class TenantAwareTest {

        @Test
        @DisplayName("实现类应正确管理租户ID")
        void implementingClassShouldManageTenantId() {
            TestTenantEntity entity = new TestTenantEntity();
            assertThat(entity.getTenantId()).isNull();

            entity.setTenantId("tenant-001");
            assertThat(entity.getTenantId()).isEqualTo("tenant-001");

            entity.setTenantId(null);
            assertThat(entity.getTenantId()).isNull();
        }

        @Test
        @DisplayName("TenantAware 应作为接口标记")
        void tenantAwareShouldBeInterface() {
            assertThat(TenantAware.class).isInterface();
        }
    }

    // ==================== TenantContext 接口测试 ====================

    @Nested
    @DisplayName("TenantContext 接口测试")
    class TenantContextInterfaceTest {

        @Test
        @DisplayName("ThreadLocalTenantContext 应实现 TenantContext")
        void shouldImplementTenantContext() {
            assertThat(tenantContext).isInstanceOf(TenantContext.class);
        }

        @Test
        @DisplayName("TenantContext 应定义所有必要方法")
        void shouldDefineAllMethods() throws NoSuchMethodException {
            // 验证接口方法存在
            assertThat(TenantContext.class.getMethod("getTenantId")).isNotNull();
            assertThat(TenantContext.class.getMethod("setTenantId", String.class)).isNotNull();
            assertThat(TenantContext.class.getMethod("clear")).isNotNull();
            assertThat(TenantContext.class.getMethod("isIgnoreTenant")).isNotNull();
            assertThat(TenantContext.class.getMethod("setIgnoreTenant", boolean.class)).isNotNull();
            assertThat(TenantContext.class.getMethod("runWithoutTenant", Runnable.class)).isNotNull();
        }
    }

    // 测试用的 TenantAware 实现类
    private static class TestTenantEntity implements TenantAware {
        private String tenantId;

        @Override
        public String getTenantId() {
            return tenantId;
        }

        @Override
        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }
    }
}