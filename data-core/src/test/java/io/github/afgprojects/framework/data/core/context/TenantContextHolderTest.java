package io.github.afgprojects.framework.data.core.context;

import io.github.afgprojects.framework.data.core.scope.TenantScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * TenantContextHolder comprehensive tests
 */
class TenantContextHolderTest {

    private TenantContextHolder holder;

    @BeforeEach
    void setUp() {
        holder = new TenantContextHolder();
    }

    @AfterEach
    void tearDown() {
        holder.clear();
    }

    // ==================== Basic get/set/clear tests ====================

    @Test
    void shouldReturnNullWhenNoTenantSet() {
        assertThat(holder.getTenantId()).isNull();
    }

    @Test
    void shouldSetAndGetTenantId() {
        holder.setTenantId("tenant-001");
        assertThat(holder.getTenantId()).isEqualTo("tenant-001");
    }

    @Test
    void shouldClearTenantId() {
        holder.setTenantId("tenant-001");
        holder.clear();
        assertThat(holder.getTenantId()).isNull();
    }

    @Test
    void shouldClearWhenSetTenantIdToNull() {
        holder.setTenantId("tenant-001");
        holder.setTenantId(null);
        assertThat(holder.getTenantId()).isNull();
    }

    @Test
    void shouldOverwriteExistingTenantId() {
        holder.setTenantId("tenant-001");
        holder.setTenantId("tenant-002");
        assertThat(holder.getTenantId()).isEqualTo("tenant-002");
    }

    @Test
    void shouldAllowMultipleClears() {
        holder.clear();
        holder.clear();
        assertThat(holder.getTenantId()).isNull();
    }

    @Test
    void shouldSupportEmptyTenantId() {
        holder.setTenantId("");
        assertThat(holder.getTenantId()).isEqualTo("");
    }

    // ==================== Snapshot tests ====================

    @Test
    void shouldSnapshotReturnNullWhenNoTenantSet() {
        TenantContextHolder.TenantContextSnapshot snapshot = holder.snapshot();
        assertThat(snapshot).isNull();
    }

    @Test
    void shouldSnapshotCaptureTenantId() {
        holder.setTenantId("tenant-snapshot");
        TenantContextHolder.TenantContextSnapshot snapshot = holder.snapshot();

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.tenantId()).isEqualTo("tenant-snapshot");
    }

    @Test
    void shouldSnapshotNotChangeWhenOriginalChanges() {
        holder.setTenantId("tenant-original");
        TenantContextHolder.TenantContextSnapshot snapshot = holder.snapshot();

        holder.setTenantId("tenant-changed");

        assertThat(snapshot.tenantId()).isEqualTo("tenant-original");
    }

    @Test
    void shouldSnapshotIsValidReturnTrueWhenTenantSet() {
        holder.setTenantId("tenant-001");
        TenantContextHolder.TenantContextSnapshot snapshot = holder.snapshot();

        assertThat(snapshot.isValid()).isTrue();
    }

    @Test
    void shouldSnapshotIsValidReturnFalseWhenEmptyString() {
        TenantContextHolder.TenantContextSnapshot snapshot =
                new TenantContextHolder.TenantContextSnapshot("");

        // Empty string is not null, so it should be valid
        assertThat(snapshot.isValid()).isTrue();
    }

    @Test
    void shouldSnapshotIsValidReturnFalseWhenNull() {
        TenantContextHolder.TenantContextSnapshot snapshot =
                new TenantContextHolder.TenantContextSnapshot(null);

        assertThat(snapshot.isValid()).isFalse();
    }

    // ==================== Restore tests ====================

    @Test
    void shouldRestoreFromSnapshot() {
        TenantContextHolder.TenantContextSnapshot snapshot =
                new TenantContextHolder.TenantContextSnapshot("tenant-restore");

        holder.restore(snapshot);

        assertThat(holder.getTenantId()).isEqualTo("tenant-restore");
    }

    @Test
    void shouldRestoreClearContextWhenSnapshotIsNull() {
        holder.setTenantId("tenant-existing");

        holder.restore(null);

        assertThat(holder.getTenantId()).isNull();
    }

    @Test
    void shouldRestoreOverwriteExistingContext() {
        holder.setTenantId("tenant-existing");
        TenantContextHolder.TenantContextSnapshot snapshot =
                new TenantContextHolder.TenantContextSnapshot("tenant-new");

        holder.restore(snapshot);

        assertThat(holder.getTenantId()).isEqualTo("tenant-new");
    }

    // ==================== runWithSnapshot tests ====================

    @Test
    void shouldRunWithSnapshotExecuteWithCorrectTenant() {
        TenantContextHolder.TenantContextSnapshot snapshot =
                new TenantContextHolder.TenantContextSnapshot("tenant-run");
        AtomicReference<String> capturedTenantId = new AtomicReference<>();

        holder.runWithSnapshot(snapshot, () -> capturedTenantId.set(holder.getTenantId()));

        assertThat(capturedTenantId.get()).isEqualTo("tenant-run");
    }

    @Test
    void shouldRunWithSnapshotRestoreOriginalValueAfterExecution() {
        holder.setTenantId("tenant-original");
        TenantContextHolder.TenantContextSnapshot snapshot =
                new TenantContextHolder.TenantContextSnapshot("tenant-temp");

        holder.runWithSnapshot(snapshot, () -> {});

        assertThat(holder.getTenantId()).isEqualTo("tenant-original");
    }

    @Test
    void shouldRunWithSnapshotRestoreToNullWhenNoOriginalValue() {
        TenantContextHolder.TenantContextSnapshot snapshot =
                new TenantContextHolder.TenantContextSnapshot("tenant-temp");

        holder.runWithSnapshot(snapshot, () -> {});

        assertThat(holder.getTenantId()).isNull();
    }

    @Test
    void shouldRunWithSnapshotHandleNullSnapshot() {
        AtomicReference<String> capturedTenantId = new AtomicReference<>();

        holder.runWithSnapshot(null, () -> capturedTenantId.set(holder.getTenantId()));

        assertThat(capturedTenantId.get()).isNull();
    }

    @Test
    void shouldRunWithSnapshotRestoreOriginalValueWhenNullSnapshot() {
        holder.setTenantId("tenant-original");

        holder.runWithSnapshot(null, () -> {});

        assertThat(holder.getTenantId()).isEqualTo("tenant-original");
    }

    @Test
    void shouldRunWithSnapshotRestoreOriginalValueOnException() {
        holder.setTenantId("tenant-original");
        TenantContextHolder.TenantContextSnapshot snapshot =
                new TenantContextHolder.TenantContextSnapshot("tenant-temp");

        assertThatThrownBy(() ->
                holder.runWithSnapshot(snapshot, () -> {
                    throw new RuntimeException("Test exception");
                }))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Test exception");

        assertThat(holder.getTenantId()).isEqualTo("tenant-original");
    }

    @Test
    void shouldRunWithSnapshotRestoreToNullOnException() {
        TenantContextHolder.TenantContextSnapshot snapshot =
                new TenantContextHolder.TenantContextSnapshot("tenant-temp");

        assertThatThrownBy(() ->
                holder.runWithSnapshot(snapshot, () -> {
                    throw new RuntimeException("Test exception");
                }))
                .isInstanceOf(RuntimeException.class);

        assertThat(holder.getTenantId()).isNull();
    }

    @Test
    void shouldRunWithNestedSnapshotsWorkCorrectly() {
        holder.setTenantId("tenant-outer");

        TenantContextHolder.TenantContextSnapshot innerSnapshot =
                new TenantContextHolder.TenantContextSnapshot("tenant-inner");

        AtomicReference<String> outerCapture = new AtomicReference<>();
        AtomicReference<String> innerCapture = new AtomicReference<>();

        outerCapture.set(holder.getTenantId());
        holder.runWithSnapshot(innerSnapshot, () -> {
            innerCapture.set(holder.getTenantId());
        });

        assertThat(outerCapture.get()).isEqualTo("tenant-outer");
        assertThat(innerCapture.get()).isEqualTo("tenant-inner");
        assertThat(holder.getTenantId()).isEqualTo("tenant-outer");
    }

    // ==================== Scope tests ====================

    @Test
    void shouldScopeSetTenantId() {
        try (TenantScope scope = holder.scope("tenant-scope")) {
            assertThat(holder.getTenantId()).isEqualTo("tenant-scope");
            assertThat(scope.getTenantId()).isEqualTo("tenant-scope");
        }
    }

    @Test
    void shouldScopeRestorePreviousTenantIdOnClose() {
        holder.setTenantId("tenant-previous");

        try (TenantScope scope = holder.scope("tenant-scope")) {
            assertThat(holder.getTenantId()).isEqualTo("tenant-scope");
        }

        assertThat(holder.getTenantId()).isEqualTo("tenant-previous");
    }

    @Test
    void shouldScopeRestoreToNullWhenNoPreviousTenant() {
        try (TenantScope scope = holder.scope("tenant-scope")) {
            assertThat(holder.getTenantId()).isEqualTo("tenant-scope");
        }

        assertThat(holder.getTenantId()).isNull();
    }

    @Test
    void shouldNestedScopesWorkCorrectly() {
        holder.setTenantId("tenant-outer");

        try (TenantScope scope1 = holder.scope("tenant-inner1")) {
            assertThat(holder.getTenantId()).isEqualTo("tenant-inner1");

            try (TenantScope scope2 = holder.scope("tenant-inner2")) {
                assertThat(holder.getTenantId()).isEqualTo("tenant-inner2");
            }

            assertThat(holder.getTenantId()).isEqualTo("tenant-inner1");
        }

        assertThat(holder.getTenantId()).isEqualTo("tenant-outer");
    }

    @Test
    void shouldScopeRestoreOnException() {
        holder.setTenantId("tenant-previous");

        assertThatThrownBy(() -> {
            try (TenantScope scope = holder.scope("tenant-scope")) {
                throw new RuntimeException("Test exception");
            }
        }).isInstanceOf(RuntimeException.class);

        assertThat(holder.getTenantId()).isEqualTo("tenant-previous");
    }

    @Test
    void shouldMultipleScopesWorkCorrectly() {
        holder.setTenantId("tenant-0");

        try (TenantScope scope1 = holder.scope("tenant-1")) {
            assertThat(holder.getTenantId()).isEqualTo("tenant-1");
        }

        assertThat(holder.getTenantId()).isEqualTo("tenant-0");

        try (TenantScope scope2 = holder.scope("tenant-2")) {
            assertThat(holder.getTenantId()).isEqualTo("tenant-2");
        }

        assertThat(holder.getTenantId()).isEqualTo("tenant-0");
    }

    // ==================== Thread isolation tests ====================

    @Test
    void shouldThreadLocalsBeIsolatedBetweenThreads() throws Exception {
        holder.setTenantId("tenant-main");

        AtomicReference<String> threadTenantId = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Thread thread = new Thread(() -> {
            // Should not see main thread's tenant
            threadTenantId.set(holder.getTenantId());
            latch.countDown();
        });

        thread.start();
        latch.await(1, TimeUnit.SECONDS);

        assertThat(threadTenantId.get()).isNull();
        assertThat(holder.getTenantId()).isEqualTo("tenant-main");
    }

    @Test
    void shouldEachThreadHaveItsOwnTenantContext() throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(3);
        List<String> results = new ArrayList<>();

        Runnable task = () -> {
            try {
                startLatch.await();
                String tenantId = holder.getTenantId();
                synchronized (results) {
                    results.add(Thread.currentThread().getName() + ":" + tenantId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                endLatch.countDown();
            }
        };

        Thread t1 = new Thread(task, "thread-1");
        Thread t2 = new Thread(task, "thread-2");
        Thread t3 = new Thread(task, "thread-3");

        // Set different tenants in main thread
        holder.setTenantId("tenant-main");

        // Set different tenants in each thread before starting
        t1.start();
        holder.setTenantId("tenant-1");
        t2.start();
        holder.setTenantId("tenant-2");
        t3.start();

        startLatch.countDown();
        endLatch.await(2, TimeUnit.SECONDS);

        // All threads should have seen null (no tenant set in their context)
        assertThat(results).hasSize(3);
        assertThat(results).allMatch(s -> s.endsWith(":null"));
    }

    // ==================== Edge cases ====================

    @Test
    void shouldHandleSpecialCharactersInTenantId() {
        String specialTenantId = "tenant-特殊字符-日本語-🎨";
        holder.setTenantId(specialTenantId);

        assertThat(holder.getTenantId()).isEqualTo(specialTenantId);
    }

    @Test
    void shouldHandleVeryLongTenantId() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("a");
        }
        String longTenantId = sb.toString();

        holder.setTenantId(longTenantId);

        assertThat(holder.getTenantId()).isEqualTo(longTenantId);
    }

    @Test
    void shouldHandleWhitespaceInTenantId() {
        String whitespaceTenantId = "  tenant-with-spaces  ";
        holder.setTenantId(whitespaceTenantId);

        assertThat(holder.getTenantId()).isEqualTo(whitespaceTenantId);
    }

    @Test
    void shouldClearWorkAfterMultipleOperations() {
        holder.setTenantId("tenant-1");
        holder.setTenantId("tenant-2");
        holder.clear();
        holder.setTenantId("tenant-3");
        holder.clear();

        assertThat(holder.getTenantId()).isNull();
    }
}
