package io.github.afgprojects.framework.security.resource.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.afgprojects.framework.security.core.tenant.TenantContext;
import io.github.afgprojects.framework.security.core.tenant.TenantException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * TenantResolverChain 测试类。
 *
 * @since 1.0.0
 */
class TenantResolverChainTest {

    private TenantResolverChain chain;

    @BeforeEach
    void setUp() {
        chain = new TenantResolverChain();
    }

    @Nested
    @DisplayName("resolve(HttpServletRequest)")
    class ResolveFromRequest {

        @Test
        @DisplayName("应该按优先级顺序遍历解析器")
        void shouldResolveByPriority() {
            // 设置解析器优先级
            HeaderTenantResolver headerResolver = new HeaderTenantResolver();
            headerResolver.setOrder(300); // 较低优先级

            TokenTenantResolver tokenResolver = new TokenTenantResolver();
            tokenResolver.setOrder(100); // 较高优先级

            chain.addResolver(tokenResolver);
            chain.addResolver(headerResolver);

            assertThat(chain.getResolvers()).hasSize(2);
            assertThat(chain.getResolvers().get(0)).isInstanceOf(TokenTenantResolver.class);
            assertThat(chain.getResolvers().get(1)).isInstanceOf(HeaderTenantResolver.class);
        }

        @Test
        @DisplayName("当解析器返回租户上下文时应停止遍历")
        void shouldStopOnSuccessfulResolution() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-Tenant-Id")).thenReturn("tenant-123");

            HeaderTenantResolver resolver = new HeaderTenantResolver();
            chain.addResolver(resolver);

            TenantContext context = chain.resolve(request);

            assertThat(context).isNotNull();
            assertThat(context.getTenantId()).isEqualTo("tenant-123");
        }

        @Test
        @DisplayName("当所有解析器都无法解析时应抛出异常")
        void shouldThrowExceptionWhenUnresolved() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-Tenant-Id")).thenReturn(null);

            chain.addResolver(new HeaderTenantResolver());
            chain.setFailIfUnresolved(true);

            assertThat(chain.isFailIfUnresolved()).isTrue();
        }

        @Test
        @DisplayName("当配置不抛出异常时应返回 null")
        void shouldReturnNullWhenFailIfUnresolvedIsFalse() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-Tenant-Id")).thenReturn(null);

            chain.addResolver(new HeaderTenantResolver());
            chain.setFailIfUnresolved(false);

            TenantContext context = chain.resolve(request);

            assertThat(context).isNull();
        }
    }

    @Nested
    @DisplayName("addResolver()")
    class AddResolver {

        @Test
        @DisplayName("添加解析器应自动排序")
        void shouldSortOnAdd() {
            HeaderTenantResolver headerResolver = new HeaderTenantResolver();
            headerResolver.setOrder(300);

            TokenTenantResolver tokenResolver = new TokenTenantResolver();
            tokenResolver.setOrder(100);

            // 先添加低优先级
            chain.addResolver(headerResolver);
            chain.addResolver(tokenResolver);

            // 应自动排序为高优先级在前
            assertThat(chain.getResolvers().get(0)).isInstanceOf(TokenTenantResolver.class);
        }
    }

    @Nested
    @DisplayName("isEmpty() and clear()")
    class EmptyAndClear {

        @Test
        @DisplayName("空链应返回 true")
        void shouldReturnTrueForEmptyChain() {
            assertThat(chain.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("添加解析器后应返回 false")
        void shouldReturnFalseAfterAddingResolver() {
            chain.addResolver(new HeaderTenantResolver());
            assertThat(chain.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("清空后应返回 true")
        void shouldReturnTrueAfterClear() {
            chain.addResolver(new HeaderTenantResolver());
            chain.clear();
            assertThat(chain.isEmpty()).isTrue();
        }
    }
}