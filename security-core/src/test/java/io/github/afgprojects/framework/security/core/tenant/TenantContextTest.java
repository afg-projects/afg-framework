package io.github.afgprojects.framework.security.core.tenant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TenantContext 接口默认方法测试
 */
@DisplayName("TenantContext 接口默认方法测试")
class TenantContextTest {

    @Nested
    @DisplayName("getTenantCode 默认方法")
    class GetTenantCodeTests {

        @Test
        @DisplayName("默认应返回 tenantId")
        void shouldReturnTenantIdByDefault() {
            TenantContext context = createContext("tenant-001", null, null);

            assertThat(context.getTenantCode()).isEqualTo("tenant-001");
        }
    }

    @Nested
    @DisplayName("getTenantName 默认方法")
    class GetTenantNameTests {

        @Test
        @DisplayName("默认应返回 null")
        void shouldReturnNullByDefault() {
            TenantContext context = createContext("tenant-001", null, null);

            assertThat(context.getTenantName()).isNull();
        }
    }

    @Nested
    @DisplayName("getAttributes 默认方法")
    class GetAttributesTests {

        @Test
        @DisplayName("默认应返回空 Map")
        void shouldReturnEmptyMapByDefault() {
            TenantContext context = createContext("tenant-001", null, null);

            assertThat(context.getAttributes()).isEmpty();
        }
    }

    @Nested
    @DisplayName("isDefault 默认方法")
    class IsDefaultTests {

        @Test
        @DisplayName("默认应返回 false")
        void shouldReturnFalseByDefault() {
            TenantContext context = createContext("tenant-001", null, null);

            assertThat(context.isDefault()).isFalse();
        }
    }

    @Nested
    @DisplayName("isValid 默认方法")
    class IsValidTests {

        @Test
        @DisplayName("默认应返回 true")
        void shouldReturnTrueByDefault() {
            TenantContext context = createContext("tenant-001", null, null);

            assertThat(context.isValid()).isTrue();
        }
    }

    /**
     * 创建 TenantContext 实例用于测试 default 方法
     */
    private TenantContext createContext(String tenantId, String tenantCode, String tenantName) {
        return new TenantContext() {
            @Override
            public String getTenantId() {
                return tenantId;
            }

            @Override
            public String getTenantCode() {
                return tenantCode != null ? tenantCode : TenantContext.super.getTenantCode();
            }

            @Override
            public String getTenantName() {
                return tenantName != null ? tenantName : TenantContext.super.getTenantName();
            }
        };
    }
}