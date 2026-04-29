package io.github.afgprojects.framework.data.core.entity;

import io.github.afgprojects.framework.data.core.context.AuditContext;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Auditable 接口测试
 */
@DisplayName("Auditable 接口测试")
class AuditableTest {

    @Nested
    @DisplayName("接口方法测试")
    class InterfaceMethodTests {

        @Test
        @DisplayName("onCreate 方法应该接收 AuditContext")
        void onCreateShouldReceiveAuditContext() {
            // Given
            StringBuilder log = new StringBuilder();
            Auditable auditable = new Auditable() {
                @Override
                public void onCreate(AuditContext context) {
                    log.append("onCreate called with userId=" + context.getCurrentUserId());
                }

                @Override
                public void onUpdate(AuditContext context) {
                    // Not used in this test
                }
            };
            AuditContext context = createMockAuditContext("user123");

            // When
            auditable.onCreate(context);

            // Then
            assertThat(log.toString()).contains("onCreate called with userId=user123");
        }

        @Test
        @DisplayName("onUpdate 方法应该接收 AuditContext")
        void onUpdateShouldReceiveAuditContext() {
            // Given
            StringBuilder log = new StringBuilder();
            Auditable auditable = new Auditable() {
                @Override
                public void onCreate(AuditContext context) {
                    // Not used in this test
                }

                @Override
                public void onUpdate(AuditContext context) {
                    log.append("onUpdate called with userId=" + context.getCurrentUserId());
                }
            };
            AuditContext context = createMockAuditContext("user456");

            // When
            auditable.onUpdate(context);

            // Then
            assertThat(log.toString()).contains("onUpdate called with userId=user456");
        }
    }

    @Nested
    @DisplayName("审计字段填充测试")
    class AuditFieldFillingTests {

        @Test
        @DisplayName("onCreate 应该填充 createTime 和 createBy")
        void onCreateShouldFillCreateTimeAndCreateBy() {
            // Given
            TestAuditableEntity entity = new TestAuditableEntity();
            Instant createTime = Instant.parse("2024-06-15T10:30:00Z");
            AuditContext context = createMockAuditContextWithTime("user123", createTime);

            // When
            entity.onCreate(context);

            // Then
            assertThat(entity.getCreateBy()).isEqualTo("user123");
            assertThat(entity.getCreateTime()).isNotNull();
        }

        @Test
        @DisplayName("onUpdate 应该填充 updateTime 和 updateBy")
        void onUpdateShouldFillUpdateTimeAndUpdateBy() {
            // Given
            TestAuditableEntity entity = new TestAuditableEntity();
            Instant updateTime = Instant.parse("2024-06-15T11:30:00Z");
            AuditContext context = createMockAuditContextWithTime("user456", updateTime);

            // When
            entity.onUpdate(context);

            // Then
            assertThat(entity.getUpdateBy()).isEqualTo("user456");
            assertThat(entity.getUpdateTime()).isNotNull();
        }

        @Test
        @DisplayName("多次更新应该覆盖 updateBy")
        void multipleUpdatesShouldOverrideUpdateBy() {
            // Given
            TestAuditableEntity entity = new TestAuditableEntity();
            AuditContext context1 = createMockAuditContext("user1");
            AuditContext context2 = createMockAuditContext("user2");

            // When
            entity.onUpdate(context1);
            entity.onUpdate(context2);

            // Then
            assertThat(entity.getUpdateBy()).isEqualTo("user2");
        }
    }

    @Nested
    @DisplayName("AuditContext 数据访问测试")
    class AuditContextDataAccessTests {

        @Test
        @DisplayName("应该能获取当前用户 ID")
        void shouldGetCurrentUserId() {
            // Given
            final String[] capturedUserId = new String[1];
            Auditable auditable = new Auditable() {
                @Override
                public void onCreate(AuditContext context) {
                    capturedUserId[0] = context.getCurrentUserId();
                }

                @Override
                public void onUpdate(AuditContext context) {
                    // Not used in this test
                }
            };
            AuditContext context = createMockAuditContext("testUser");

            // When
            auditable.onCreate(context);

            // Then
            assertThat(capturedUserId[0]).isEqualTo("testUser");
        }

        @Test
        @DisplayName("应该能获取当前用户名")
        void shouldGetCurrentUsername() {
            // Given
            final String[] capturedUsername = new String[1];
            Auditable auditable = new Auditable() {
                @Override
                public void onCreate(AuditContext context) {
                    capturedUsername[0] = context.getCurrentUsername();
                }

                @Override
                public void onUpdate(AuditContext context) {
                    // Not used in this test
                }
            };
            AuditContext context = mock(AuditContext.class);
            when(context.getCurrentUsername()).thenReturn("Test User Name");

            // When
            auditable.onCreate(context);

            // Then
            assertThat(capturedUsername[0]).isEqualTo("Test User Name");
        }

        @Test
        @DisplayName("应该能获取当前时间")
        void shouldGetCurrentTime() {
            // Given
            final Instant[] capturedTime = new Instant[1];
            Auditable auditable = new Auditable() {
                @Override
                public void onCreate(AuditContext context) {
                    capturedTime[0] = context.getCurrentTime();
                }

                @Override
                public void onUpdate(AuditContext context) {
                    // Not used in this test
                }
            };
            Instant expectedTime = Instant.parse("2024-06-15T12:00:00Z");
            AuditContext context = mock(AuditContext.class);
            when(context.getCurrentTime()).thenReturn(expectedTime);

            // When
            auditable.onCreate(context);

            // Then
            assertThat(capturedTime[0]).isEqualTo(expectedTime);
        }

        @Test
        @DisplayName("应该能获取租户 ID")
        void shouldGetTenantId() {
            // Given
            final String[] capturedTenantId = new String[1];
            Auditable auditable = new Auditable() {
                @Override
                public void onCreate(AuditContext context) {
                    capturedTenantId[0] = context.getTenantId();
                }

                @Override
                public void onUpdate(AuditContext context) {
                    // Not used in this test
                }
            };
            AuditContext context = mock(AuditContext.class);
            when(context.getTenantId()).thenReturn("tenant-001");

            // When
            auditable.onCreate(context);

            // Then
            assertThat(capturedTenantId[0]).isEqualTo("tenant-001");
        }
    }

    @Nested
    @DisplayName("空值处理测试")
    class NullValueHandlingTests {

        @Test
        @DisplayName("userId 为 null 时应该正确处理")
        void shouldHandleNullUserId() {
            // Given
            TestAuditableEntity entity = new TestAuditableEntity();
            AuditContext context = mock(AuditContext.class);
            when(context.getCurrentUserId()).thenReturn(null);
            when(context.getCurrentTime()).thenReturn(Instant.now());

            // When
            entity.onCreate(context);

            // Then
            assertThat(entity.getCreateBy()).isNull();
        }

        @Test
        @DisplayName("username 为 null 时应该正确处理")
        void shouldHandleNullUsername() {
            // Given
            final String[] capturedUsername = new String[1];
            Auditable auditable = new Auditable() {
                @Override
                public void onCreate(AuditContext context) {
                    capturedUsername[0] = context.getCurrentUsername();
                }

                @Override
                public void onUpdate(AuditContext context) {
                    // Not used in this test
                }
            };
            AuditContext context = mock(AuditContext.class);
            when(context.getCurrentUsername()).thenReturn(null);

            // When
            auditable.onCreate(context);

            // Then
            assertThat(capturedUsername[0]).isNull();
        }

        @Test
        @DisplayName("tenantId 为 null 时应该正确处理")
        void shouldHandleNullTenantId() {
            // Given
            final String[] capturedTenantId = new String[1];
            Auditable auditable = new Auditable() {
                @Override
                public void onCreate(AuditContext context) {
                    capturedTenantId[0] = context.getTenantId();
                }

                @Override
                public void onUpdate(AuditContext context) {
                    // Not used in this test
                }
            };
            AuditContext context = mock(AuditContext.class);
            when(context.getTenantId()).thenReturn(null);

            // When
            auditable.onCreate(context);

            // Then
            assertThat(capturedTenantId[0]).isNull();
        }
    }

    @Nested
    @DisplayName("业务场景测试")
    class BusinessScenarioTests {

        @Test
        @DisplayName("完整创建审计流程")
        void completeCreateAuditFlow() {
            // Given
            TestAuditableEntity entity = new TestAuditableEntity();
            Instant createTime = Instant.parse("2024-01-01T00:00:00Z");
            AuditContext context = createFullMockAuditContext("admin", "Administrator", createTime, "tenant-001");

            // When
            entity.onCreate(context);

            // Then
            assertThat(entity.getCreateBy()).isEqualTo("admin");
            assertThat(entity.getCreateTime()).isNotNull();
        }

        @Test
        @DisplayName("完整更新审计流程")
        void completeUpdateAuditFlow() {
            // Given
            TestAuditableEntity entity = new TestAuditableEntity();
            // 先创建
            AuditContext createContext = createFullMockAuditContext("creator", "Creator",
                    Instant.parse("2024-01-01T00:00:00Z"), "tenant-001");
            entity.onCreate(createContext);

            // 再更新
            Instant updateTime = Instant.parse("2024-06-15T10:30:00Z");
            AuditContext updateContext = createFullMockAuditContext("modifier", "Modifier", updateTime, "tenant-001");

            // When
            entity.onUpdate(updateContext);

            // Then
            assertThat(entity.getCreateBy()).isEqualTo("creator"); // 创建人不变
            assertThat(entity.getUpdateBy()).isEqualTo("modifier"); // 更新人改变
            assertThat(entity.getUpdateTime()).isNotNull();
        }

        @Test
        @DisplayName("审计回调不应该影响实体 ID")
        void auditShouldNotAffectId() {
            // Given
            TestAuditableEntity entity = new TestAuditableEntity();
            entity.setId(100L);
            AuditContext context = createMockAuditContext("user");

            // When
            entity.onCreate(context);

            // Then
            assertThat(entity.getId()).isEqualTo(100L); // ID 不变
        }
    }

    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("onCreate 抛出异常应该正确传播")
        void onCreateExceptionShouldPropagate() {
            // Given
            Auditable auditable = new Auditable() {
                @Override
                public void onCreate(AuditContext context) {
                    throw new RuntimeException("Create audit failed");
                }

                @Override
                public void onUpdate(AuditContext context) {
                    // Not used in this test
                }
            };
            AuditContext context = createMockAuditContext("user");

            // When & Then
            assertThatCode(() -> auditable.onCreate(context))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Create audit failed");
        }

        @Test
        @DisplayName("onUpdate 抛出异常应该正确传播")
        void onUpdateExceptionShouldPropagate() {
            // Given
            Auditable auditable = new Auditable() {
                @Override
                public void onCreate(AuditContext context) {
                    // Not used in this test
                }

                @Override
                public void onUpdate(AuditContext context) {
                    throw new IllegalStateException("Update audit failed");
                }
            };
            AuditContext context = createMockAuditContext("user");

            // When & Then
            assertThatCode(() -> auditable.onUpdate(context))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Update audit failed");
        }
    }

    // Helper methods

    private AuditContext createMockAuditContext(String userId) {
        AuditContext context = mock(AuditContext.class);
        when(context.getCurrentUserId()).thenReturn(userId);
        when(context.getCurrentTime()).thenReturn(Instant.now());
        return context;
    }

    private AuditContext createMockAuditContextWithTime(String userId, Instant time) {
        AuditContext context = mock(AuditContext.class);
        when(context.getCurrentUserId()).thenReturn(userId);
        when(context.getCurrentTime()).thenReturn(time);
        return context;
    }

    private AuditContext createFullMockAuditContext(String userId, String username, Instant time, String tenantId) {
        AuditContext context = mock(AuditContext.class);
        when(context.getCurrentUserId()).thenReturn(userId);
        when(context.getCurrentUsername()).thenReturn(username);
        when(context.getCurrentTime()).thenReturn(time);
        when(context.getTenantId()).thenReturn(tenantId);
        return context;
    }

    /**
     * 测试可审计实体
     */
    static class TestAuditableEntity extends BaseEntity<Long> implements Auditable {

        private @Nullable String createBy;
        private @Nullable String updateBy;

        @Override
        public void onCreate(AuditContext context) {
            this.createBy = context.getCurrentUserId();
            this.createTime = LocalDateTime.now();
        }

        @Override
        public void onUpdate(AuditContext context) {
            this.updateBy = context.getCurrentUserId();
            this.updateTime = LocalDateTime.now();
        }

        public @Nullable String getCreateBy() {
            return createBy;
        }

        public @Nullable String getUpdateBy() {
            return updateBy;
        }
    }
}
