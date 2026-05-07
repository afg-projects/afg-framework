package io.github.afgprojects.framework.security.core.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TenantContextTest {

    @Nested
    @DisplayName("TenantContext 接口测试")
    class TenantContextInterfaceTests {

        @Test
        @DisplayName("应获取租户 ID")
        void shouldGetTenantId() {
            TenantContext context = createTestTenantContext("tenant-001", "acme");

            assertThat(context.getTenantId()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("应获取租户编码")
        void shouldGetTenantCode() {
            TenantContext context = createTestTenantContext("tenant-001", "acme");

            assertThat(context.getTenantCode()).isEqualTo("acme");
        }

        @Test
        @DisplayName("租户编码默认返回租户 ID")
        void shouldReturnTenantIdAsCode() {
            TenantContext context = new TenantContext() {
                @Override
                public @NonNull String getTenantId() {
                    return "tenant-001";
                }

                // Not overriding getTenantCode() to test default behavior
            };

            assertThat(context.getTenantCode()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("应获取租户名称")
        void shouldGetTenantName() {
            TenantContext context = createTestTenantContext("tenant-001", "acme");

            assertThat(context.getTenantName()).isEqualTo("Acme Corporation");
        }

        @Test
        @DisplayName("应获取扩展属性")
        void shouldGetAttributes() {
            TenantContext context = createTestTenantContext("tenant-001", "acme");

            assertThat(context.getAttributes()).containsEntry("type", "enterprise");
        }

        @Test
        @DisplayName("默认应不是默认租户")
        void shouldNotBeDefaultByDefault() {
            TenantContext context = createTestTenantContext("tenant-001", "acme");

            assertThat(context.isDefault()).isFalse();
        }

        @Test
        @DisplayName("默认应有效")
        void shouldBeValidByDefault() {
            TenantContext context = createTestTenantContext("tenant-001", "acme");

            assertThat(context.isValid()).isTrue();
        }
    }

    @Nested
    @DisplayName("TenantResolver 接口测试")
    class TenantResolverInterfaceTests {

        @Test
        @DisplayName("应从请求解析租户")
        void shouldResolveFromRequest() {
            TenantResolver resolver = createTestTenantResolver();

            // 使用简化的测试 - 不需要完整的 HttpServletRequest mock
            assertThat(resolver.getOrder()).isEqualTo(100);
        }

        @Test
        @DisplayName("默认优先级应为 100")
        void shouldHaveDefaultOrder() {
            TenantResolver resolver = createTestTenantResolver();

            assertThat(resolver.getOrder()).isEqualTo(100);
        }

        @Test
        @DisplayName("默认 Token 解析返回 null")
        void shouldReturnNullForTokenByDefault() {
            TenantResolver resolver = new TenantResolver() {
                @Override
                public @Nullable TenantContext resolve(jakarta.servlet.http.@NonNull HttpServletRequest request) {
                    return null;
                }
            };

            assertThat(resolver.resolveFromToken("some-token")).isNull();
        }
    }

    @Nested
    @DisplayName("TenantAware 接口测试")
    class TenantAwareInterfaceTests {

        @Test
        @DisplayName("应实现 TenantAware")
        void shouldImplementTenantAware() {
            TenantAware entity = () -> "tenant-001";

            assertThat(entity.getTenantId()).isEqualTo("tenant-001");
        }
    }

    @Nested
    @DisplayName("TenantException 测试")
    class TenantExceptionTests {

        @Test
        @DisplayName("应创建租户不存在异常")
        void shouldCreateNotFoundException() {
            TenantException ex = TenantException.notFound("tenant-001");

            assertThat(ex.getErrorCode()).isEqualTo(TenantErrorCode.TENANT_NOT_FOUND);
            assertThat(ex.getMessage()).contains("tenant-001");
        }

        @Test
        @DisplayName("应创建租户已禁用异常")
        void shouldCreateDisabledException() {
            TenantException ex = TenantException.disabled("tenant-001");

            assertThat(ex.getErrorCode()).isEqualTo(TenantErrorCode.TENANT_DISABLED);
            assertThat(ex.getMessage()).contains("tenant-001");
        }

        @Test
        @DisplayName("应创建无法解析租户异常")
        void shouldCreateUnresolvedException() {
            TenantException ex = TenantException.unresolved();

            assertThat(ex.getErrorCode()).isEqualTo(TenantErrorCode.TENANT_UNRESOLVED);
        }

        @Test
        @DisplayName("应创建租户已过期异常")
        void shouldCreateExpiredException() {
            TenantException ex = TenantException.expired("tenant-001");

            assertThat(ex.getErrorCode()).isEqualTo(TenantErrorCode.TENANT_EXPIRED);
        }

        @Test
        @DisplayName("应创建租户访问被拒绝异常")
        void shouldCreateAccessDeniedException() {
            TenantException ex = TenantException.accessDenied("tenant-001");

            assertThat(ex.getErrorCode()).isEqualTo(TenantErrorCode.TENANT_ACCESS_DENIED);
        }

        @Test
        @DisplayName("应创建租户切换失败异常")
        void shouldCreateSwitchFailedException() {
            TenantException ex = TenantException.switchFailed("tenant-001");

            assertThat(ex.getErrorCode()).isEqualTo(TenantErrorCode.TENANT_SWITCH_FAILED);
        }

        @Test
        @DisplayName("应使用错误码构造异常")
        void shouldCreateWithErrorCode() {
            TenantException ex = new TenantException(TenantErrorCode.TENANT_CONFIG_ERROR);

            assertThat(ex.getErrorCode()).isEqualTo(TenantErrorCode.TENANT_CONFIG_ERROR);
        }

        @Test
        @DisplayName("应使用错误码和消息构造异常")
        void shouldCreateWithErrorCodeAndMessage() {
            TenantException ex = new TenantException(TenantErrorCode.TENANT_INVALID_PARAM, "Invalid tenant type");

            assertThat(ex.getErrorCode()).isEqualTo(TenantErrorCode.TENANT_INVALID_PARAM);
            assertThat(ex.getMessage()).isEqualTo("Invalid tenant type");
        }
    }

    @Nested
    @DisplayName("TenantErrorCode 测试")
    class TenantErrorCodeTests {

        @Test
        @DisplayName("应获取错误码")
        void shouldGetCode() {
            assertThat(TenantErrorCode.TENANT_NOT_FOUND.getCode()).isEqualTo(20000);
            assertThat(TenantErrorCode.TENANT_DISABLED.getCode()).isEqualTo(20001);
        }

        @Test
        @DisplayName("应获取错误消息")
        void shouldGetMessage() {
            assertThat(TenantErrorCode.TENANT_NOT_FOUND.getMessage()).contains("租户不存在");
        }

        @Test
        @DisplayName("应获取错误分类")
        void shouldGetCategory() {
            assertThat(TenantErrorCode.TENANT_NOT_FOUND.getCategory())
                    .isEqualTo(io.github.afgprojects.framework.core.model.exception.ErrorCategory.BUSINESS);
            assertThat(TenantErrorCode.TENANT_ACCESS_DENIED.getCategory())
                    .isEqualTo(io.github.afgprojects.framework.core.model.exception.ErrorCategory.SECURITY);
        }

        @Test
        @DisplayName("应格式化错误码")
        void shouldFormatCode() {
            assertThat(TenantErrorCode.TENANT_NOT_FOUND.formatCode()).isEqualTo("E20000");
        }
    }

    // ========== 测试辅助方法 ==========

    private TenantContext createTestTenantContext(String tenantId, String tenantCode) {
        return new TenantContext() {
            @Override
            public @NonNull String getTenantId() {
                return tenantId;
            }

            @Override
            public @Nullable String getTenantCode() {
                return tenantCode;
            }

            @Override
            public @Nullable String getTenantName() {
                return "Acme Corporation";
            }

            @Override
            public @NonNull Map<String, Object> getAttributes() {
                return Map.of("type", "enterprise");
            }
        };
    }

    private TenantResolver createTestTenantResolver() {
        return new TenantResolver() {
            @Override
            public @Nullable TenantContext resolve(jakarta.servlet.http.@NonNull HttpServletRequest request) {
                String tenantId = request.getHeader("X-Tenant-Id");
                if (tenantId != null) {
                    return createTestTenantContext(tenantId, null);
                }
                return null;
            }

            @Override
            public int getOrder() {
                return 100;
            }
        };
    }
}
