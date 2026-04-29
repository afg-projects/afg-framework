package io.github.afgprojects.framework.data.jdbc.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 并发安全测试
 */
@DisplayName("并发安全测试")
@Tag("security")
class ConcurrencySecurityTest extends SecurityTestBase {

    @Nested
    @DisplayName("并发读取测试")
    class ConcurrentReadTests {

        @Test
        @DisplayName("并发读取应返回一致数据")
        void concurrentReadShouldReturnConsistentData() throws Exception {
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        var results = userProxy.findAll();
                        if (results.size() == 3) {
                            successCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            assertThat(successCount.get()).isEqualTo(threadCount);
        }
    }

    @Nested
    @DisplayName("并发写入测试")
    class ConcurrentWriteTests {

        @Test
        @DisplayName("并发写入应正确处理")
        void concurrentWriteShouldBeHandled() throws Exception {
            int threadCount = 5;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            List<Exception> exceptions = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        SecurityTestUser user = new SecurityTestUser();
                        user.setName("concurrent-user-" + index);
                        user.setEmail("concurrent" + index + "@test.com");
                        userProxy.insert(user);
                    } catch (Exception e) {
                        synchronized (exceptions) {
                            exceptions.add(e);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            // 验证所有写入都成功
            assertThat(userProxy.count()).isEqualTo(initialCount + threadCount);
        }
    }

    @Nested
    @DisplayName("并发更新测试")
    class ConcurrentUpdateTests {

        @Test
        @DisplayName("并发更新同一记录应正确处理")
        void concurrentUpdateSameRecord() throws Exception {
            // 先插入一条记录
            SecurityTestUser user = new SecurityTestUser();
            user.setName("update-test");
            user.setEmail("update@test.com");
            final SecurityTestUser insertedUser = userProxy.insert(user);
            final long userId = insertedUser.getId();

            int threadCount = 5;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        SecurityTestUser toUpdate = userProxy.findById(userId).orElseThrow();
                        toUpdate.setName("updated-" + index);
                        userProxy.update(toUpdate);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        // 并发更新可能失败，这是预期的
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            // 至少有一个成功
            assertThat(successCount.get()).isGreaterThanOrEqualTo(1);
        }
    }

    @Nested
    @DisplayName("批量操作安全测试")
    class BatchOperationTests {

        @Test
        @DisplayName("批量插入应正确处理")
        void batchInsertShouldWork() {
            List<SecurityTestUser> users = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                SecurityTestUser user = new SecurityTestUser();
                user.setName("batch-user-" + i);
                user.setEmail("batch" + i + "@test.com");
                users.add(user);
            }

            List<SecurityTestUser> inserted = userProxy.insertAll(users);

            assertThat(inserted).hasSize(100);
            assertThat(inserted).allMatch(u -> u.getId() != null);
        }

        @Test
        @DisplayName("批量删除应正确处理")
        void batchDeleteShouldWork() {
            // 先插入一些数据
            List<SecurityTestUser> users = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                SecurityTestUser user = new SecurityTestUser();
                user.setName("delete-test-" + i);
                user.setEmail("delete" + i + "@test.com");
                users.add(userProxy.insert(user));
            }

            // 批量删除
            userProxy.deleteAll(users);

            // 验证已删除
            for (SecurityTestUser u : users) {
                assertThat(userProxy.findById(u.getId())).isEmpty();
            }
        }
    }
}
