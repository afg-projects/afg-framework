package io.github.afgprojects.framework.security.auth.casbin;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.afgprojects.framework.security.auth.casbin.enforcer.CasbinAfgEnforcer;
import io.github.afgprojects.framework.security.core.permission.AbacService;

/**
 * CasbinAbacService 测试。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class CasbinAbacServiceTest {

    @Mock
    private CasbinAfgEnforcer enforcer;

    private AbacService abacService;

    @BeforeEach
    void setUp() {
        abacService = new CasbinAbacService(enforcer);
    }

    @Nested
    @DisplayName("权限检查测试")
    class EnforceTests {

        @Test
        @DisplayName("应检查主体对资源的操作权限")
        void shouldEnforceWithoutDomain() {
            when(enforcer.enforce("user-001", "document-001", "read")).thenReturn(true);
            when(enforcer.enforce("user-001", "document-001", "delete")).thenReturn(false);

            assertThat(abacService.enforce("user-001", "document-001", "read")).isTrue();
            assertThat(abacService.enforce("user-001", "document-001", "delete")).isFalse();

            verify(enforcer).enforce("user-001", "document-001", "read");
            verify(enforcer).enforce("user-001", "document-001", "delete");
        }

        @Test
        @DisplayName("应检查主体在域内对资源的操作权限")
        void shouldEnforceWithDomain() {
            when(enforcer.enforce("user-001", "tenant-001", "document-001", "read")).thenReturn(true);
            when(enforcer.enforce("user-001", "tenant-002", "document-001", "read")).thenReturn(false);

            assertThat(abacService.enforce("user-001", "tenant-001", "document-001", "read")).isTrue();
            assertThat(abacService.enforce("user-001", "tenant-002", "document-001", "read")).isFalse();

            verify(enforcer).enforce("user-001", "tenant-001", "document-001", "read");
            verify(enforcer).enforce("user-001", "tenant-002", "document-001", "read");
        }

        @Test
        @DisplayName("null 主体应返回 false")
        void shouldReturnFalseForNullSubject() {
            when(enforcer.enforce(anyString(), anyString(), anyString())).thenReturn(false);

            // CasbinAfgEnforcer 内部处理 null 检查
            assertThat(abacService.enforce("user-001", "resource", "action")).isFalse();
        }
    }

    @Nested
    @DisplayName("策略添加测试")
    class AddPolicyTests {

        @Test
        @DisplayName("应添加策略（无域）")
        void shouldAddPolicyWithoutDomain() {
            doNothing().when(enforcer).addPolicy(anyString(), anyString(), anyString(), anyString());

            abacService.addPolicy("user-001", "document-001", "read");

            // 无域版本使用空字符串作为默认域
            verify(enforcer).addPolicy("user-001", "", "document-001", "read");
        }

        @Test
        @DisplayName("应添加策略（带域）")
        void shouldAddPolicyWithDomain() {
            doNothing().when(enforcer).addPolicy(anyString(), anyString(), anyString(), anyString());

            abacService.addPolicy("user-001", "tenant-001", "document-001", "read");

            verify(enforcer).addPolicy("user-001", "tenant-001", "document-001", "read");
        }

        @Test
        @DisplayName("应添加多条策略")
        void shouldAddMultiplePolicies() {
            doNothing().when(enforcer).addPolicy(anyString(), anyString(), anyString(), anyString());

            abacService.addPolicy("user-001", "document-001", "read");
            abacService.addPolicy("user-001", "document-001", "write");
            abacService.addPolicy("user-001", "tenant-001", "document-002", "delete");

            verify(enforcer, times(3)).addPolicy(anyString(), anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("策略移除测试")
    class RemovePolicyTests {

        @Test
        @DisplayName("应移除策略（无域）")
        void shouldRemovePolicyWithoutDomain() {
            doNothing().when(enforcer).removePolicy(anyString(), anyString(), anyString(), anyString());

            abacService.removePolicy("user-001", "document-001", "read");

            // 无域版本使用空字符串作为默认域
            verify(enforcer).removePolicy("user-001", "", "document-001", "read");
        }

        @Test
        @DisplayName("应移除策略（带域）")
        void shouldRemovePolicyWithDomain() {
            doNothing().when(enforcer).removePolicy(anyString(), anyString(), anyString(), anyString());

            abacService.removePolicy("user-001", "tenant-001", "document-001", "read");

            verify(enforcer).removePolicy("user-001", "tenant-001", "document-001", "read");
        }

        @Test
        @DisplayName("应移除多条策略")
        void shouldRemoveMultiplePolicies() {
            doNothing().when(enforcer).removePolicy(anyString(), anyString(), anyString(), anyString());

            abacService.removePolicy("user-001", "document-001", "read");
            abacService.removePolicy("user-001", "document-001", "write");
            abacService.removePolicy("user-001", "tenant-001", "document-002", "delete");

            verify(enforcer, times(3)).removePolicy(anyString(), anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("执行器访问测试")
    class EnforcerAccessTests {

        @Test
        @DisplayName("应返回底层执行器")
        void shouldReturnEnforcer() {
            CasbinAbacService casbinAbacService = (CasbinAbacService) abacService;

            assertThat(casbinAbacService.getEnforcer()).isEqualTo(enforcer);
        }
    }
}