package io.github.afgprojects.framework.data.jdbc;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.github.afgprojects.framework.data.core.exception.OptimisticLockException;
import io.github.afgprojects.framework.data.jdbc.entity.TestVersionedItem;
import io.github.afgprojects.framework.data.jdbc.test.BaseDataTest;

/**
 * JdbcDataManager 乐观锁集成测试
 * <p>
 * 使用 PostgreSQL Testcontainers + Liquibase 迁移。
 * 单线程测试使用 @Transactional 自动回滚。
 * 并发测试不使用事务，需要显式清理。
 * </p>
 */
class JdbcDataManagerOptimisticLockTest extends BaseDataTest {

    @Nested
    @DisplayName("版本号管理")
    class VersionManagement {

        @Test
        @DisplayName("should start with version 0 when create new versioned entity")
        void shouldStartWithVersion0_whenCreateNewVersionedEntity() {
            TestVersionedItem item = TestVersionedItem.create("version-test", 100);

            TestVersionedItem saved = dataManager.save(TestVersionedItem.class, item);

            assertThat(saved.getVersion()).isEqualTo(0);
        }

        @Test
        @DisplayName("should increment version when update versioned entity")
        void shouldIncrementVersion_whenUpdateVersionedEntity() {
            TestVersionedItem item = dataManager.save(TestVersionedItem.class, TestVersionedItem.create("inc-test", 50));
            assertThat(item.getVersion()).isEqualTo(0);

            item.setStock(40);
            TestVersionedItem updated1 = dataManager.save(TestVersionedItem.class, item);
            assertThat(updated1.getVersion()).isEqualTo(1);

            item.setStock(30);
            TestVersionedItem updated2 = dataManager.save(TestVersionedItem.class, item);
            assertThat(updated2.getVersion()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("并发冲突检测")
    class ConcurrentConflictDetection {

        private String testItemId;

        @BeforeEach
        void setUp() {
            // 在事务中创建测试数据，确保提交后对其他线程可见
            testItemId = dataManager.executeInTransaction(() -> {
                TestVersionedItem item = dataManager.save(TestVersionedItem.class,
                    TestVersionedItem.create("concurrent-test", 100));
                return item.getId();
            });
        }

        @AfterEach
        void tearDown() {
            // 清理测试数据
            if (testItemId != null) {
                dataManager.entity(TestVersionedItem.class).deleteById(testItemId);
            }
        }

        @Test
        @DisplayName("should throw OptimisticLockException when concurrent update with same version")
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        void shouldThrowOptimisticLockException_whenConcurrentUpdateWithSameVersion() throws Exception {
            String itemId = testItemId;

            // 先读取实体，两个线程将使用相同的旧版本对象
            TestVersionedItem staleItem = dataManager.findById(TestVersionedItem.class, itemId).orElseThrow();
            assertThat(staleItem.getVersion()).isEqualTo(0);

            // 线程1：使用旧版本实体更新成功
            dataManager.executeInTransaction(() -> {
                TestVersionedItem item1 = dataManager.findById(TestVersionedItem.class, itemId).orElseThrow();
                item1.setStock(80);
                dataManager.save(TestVersionedItem.class, item1);
                return null;
            });

            // 验证线程1更新后版本已递增
            TestVersionedItem afterFirstUpdate = dataManager.findById(TestVersionedItem.class, itemId).orElseThrow();
            assertThat(afterFirstUpdate.getVersion()).isEqualTo(1);

            // 线程2：使用旧版本实体（version=0）更新，应该抛出乐观锁异常
            AtomicReference<Exception> thread2Exception = new AtomicReference<>();
            try {
                staleItem.setStock(60);
                dataManager.save(TestVersionedItem.class, staleItem);
            } catch (Exception e) {
                thread2Exception.set(e);
            }

            // 验证：应该抛出乐观锁异常
            assertThat(thread2Exception.get())
                .isInstanceOf(OptimisticLockException.class);

            OptimisticLockException ex = (OptimisticLockException) thread2Exception.get();
            assertThat(ex.getEntityId()).isEqualTo(itemId);
            assertThat(ex.getExpectedVersion()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("重试成功场景")
    class RetrySuccess {

        @Test
        @DisplayName("should succeed when retry with fresh version after conflict")
        void shouldSucceed_whenRetryWithFreshVersionAfterConflict() {
            // 准备：创建实体
            TestVersionedItem item = dataManager.save(TestVersionedItem.class, TestVersionedItem.create("retry-test", 100));
            String itemId = item.getId();

            // 第一次更新成功
            TestVersionedItem item1 = dataManager.findById(TestVersionedItem.class, itemId).orElseThrow();
            item1.setStock(80);
            dataManager.save(TestVersionedItem.class, item1);

            // 模拟重试：重新读取最新版本后更新
            TestVersionedItem freshItem = dataManager.findById(TestVersionedItem.class, itemId).orElseThrow();
            assertThat(freshItem.getVersion()).isEqualTo(1); // 版本已更新

            freshItem.setStock(60);
            TestVersionedItem updated = dataManager.save(TestVersionedItem.class, freshItem);

            assertThat(updated.getVersion()).isEqualTo(2);
            assertThat(updated.getStock()).isEqualTo(60);
        }
    }
}
