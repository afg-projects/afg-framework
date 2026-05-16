package io.github.afgprojects.framework.security.auth.tenant.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.github.afgprojects.framework.security.auth.tenant.validator.TenantValidator;
import io.github.afgprojects.framework.security.core.tenant.TenantContext;
import io.github.afgprojects.framework.security.core.tenant.TenantException;
import io.github.afgprojects.framework.security.core.tenant.TenantResolver;
import jakarta.servlet.http.HttpServletRequest;

/**
 * TenantResolverChain 测试。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantResolverChainTest {

    @Mock
    private TenantValidator validator;

    @Mock
    private HttpServletRequest request;

    private TenantResolverChain chain;

    @BeforeEach
    void setUp() {
        chain = new TenantResolverChain(Collections.emptyList(), validator, false);
    }

    @Nested
    @DisplayName("解析器排序测试")
    class ResolverOrderTests {

        @Test
        @DisplayName("应按 Order 值升序排列解析器")
        void shouldSortResolversByOrder() {
            // Given
            TenantResolver lowPriorityResolver = mock(TenantResolver.class);
            when(lowPriorityResolver.getOrder()).thenReturn(200);
            when(lowPriorityResolver.resolve(any())).thenReturn(null);

            TenantResolver highPriorityResolver = mock(TenantResolver.class);
            when(highPriorityResolver.getOrder()).thenReturn(50);
            when(highPriorityResolver.resolve(any())).thenReturn(createTestContext("tenant-001"));

            TenantResolver mediumPriorityResolver = mock(TenantResolver.class);
            when(mediumPriorityResolver.getOrder()).thenReturn(100);
            when(mediumPriorityResolver.resolve(any())).thenReturn(null);

            List<TenantResolver> resolvers = Arrays.asList(
                    lowPriorityResolver,
                    highPriorityResolver,
                    mediumPriorityResolver
            );

            chain = new TenantResolverChain(resolvers, validator, false);

            // When
            TenantContext result = chain.resolve(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTenantId()).isEqualTo("tenant-001");

            // 验证调用顺序：先调用高优先级（order=50），找到后不再调用其他解析器
            verify(highPriorityResolver).resolve(any());
            verify(mediumPriorityResolver, never()).resolve(any());
            verify(lowPriorityResolver, never()).resolve(any());
        }

        @Test
        @DisplayName("相同 Order 值的解析器按列表顺序执行")
        void shouldPreserveOrderForSamePriority() {
            // Given
            TenantResolver firstResolver = mock(TenantResolver.class);
            when(firstResolver.getOrder()).thenReturn(100);
            when(firstResolver.resolve(any())).thenReturn(null);

            TenantResolver secondResolver = mock(TenantResolver.class);
            when(secondResolver.getOrder()).thenReturn(100);
            when(secondResolver.resolve(any())).thenReturn(createTestContext("tenant-001"));

            List<TenantResolver> resolvers = Arrays.asList(firstResolver, secondResolver);
            chain = new TenantResolverChain(resolvers, validator, false);

            // When
            TenantContext result = chain.resolve(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTenantId()).isEqualTo("tenant-001");

            verify(firstResolver).resolve(any());
            verify(secondResolver).resolve(any());
        }
    }

    @Nested
    @DisplayName("解析流程测试")
    class ResolutionFlowTests {

        @Test
        @DisplayName("应调用每个解析器直到找到租户")
        void shouldCallResolversUntilFound() {
            // Given
            TenantResolver resolver1 = mock(TenantResolver.class);
            when(resolver1.getOrder()).thenReturn(100);
            when(resolver1.resolve(any())).thenReturn(null);

            TenantResolver resolver2 = mock(TenantResolver.class);
            when(resolver2.getOrder()).thenReturn(100);
            when(resolver2.resolve(any())).thenReturn(createTestContext("tenant-001"));

            TenantResolver resolver3 = mock(TenantResolver.class);
            when(resolver3.getOrder()).thenReturn(100);
            when(resolver3.resolve(any())).thenReturn(createTestContext("tenant-002"));

            List<TenantResolver> resolvers = Arrays.asList(resolver1, resolver2, resolver3);
            chain = new TenantResolverChain(resolvers, validator, false);

            // When
            TenantContext result = chain.resolve(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTenantId()).isEqualTo("tenant-001");

            verify(resolver1).resolve(any());
            verify(resolver2).resolve(any());
            verify(resolver3, never()).resolve(any()); // 找到后不再调用
        }

        @Test
        @DisplayName("所有解析器都返回 null 时应返回 null")
        void shouldReturnNullWhenAllResolversReturnNull() {
            // Given
            TenantResolver resolver1 = mock(TenantResolver.class);
            when(resolver1.getOrder()).thenReturn(100);
            when(resolver1.resolve(any())).thenReturn(null);

            TenantResolver resolver2 = mock(TenantResolver.class);
            when(resolver2.getOrder()).thenReturn(100);
            when(resolver2.resolve(any())).thenReturn(null);

            List<TenantResolver> resolvers = Arrays.asList(resolver1, resolver2);
            chain = new TenantResolverChain(resolvers, validator, false);

            // When
            TenantContext result = chain.resolve(request);

            // Then
            assertThat(result).isNull();

            verify(resolver1).resolve(any());
            verify(resolver2).resolve(any());
        }

        @Test
        @DisplayName("空解析器列表应返回 null")
        void shouldReturnNullForEmptyResolverList() {
            // Given
            chain = new TenantResolverChain(Collections.emptyList(), validator, false);

            // When
            TenantContext result = chain.resolve(request);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("验证测试")
    class ValidationTests {

        @Test
        @DisplayName("解析成功后应调用验证器")
        void shouldValidateAfterResolution() {
            // Given
            TenantContext context = createTestContext("tenant-001");
            TenantResolver resolver = mock(TenantResolver.class);
            when(resolver.getOrder()).thenReturn(100);
            when(resolver.resolve(any())).thenReturn(context);

            List<TenantResolver> resolvers = Collections.singletonList(resolver);
            chain = new TenantResolverChain(resolvers, validator, false);

            // When
            chain.resolve(request);

            // Then
            verify(validator).validate("tenant-001");
        }

        @Test
        @DisplayName("验证失败时应抛出异常")
        void shouldThrowExceptionOnValidationFailure() {
            // Given
            TenantContext context = createTestContext("tenant-001");
            TenantResolver resolver = mock(TenantResolver.class);
            when(resolver.getOrder()).thenReturn(100);
            when(resolver.resolve(any())).thenReturn(context);

            List<TenantResolver> resolvers = Collections.singletonList(resolver);
            chain = new TenantResolverChain(resolvers, validator, false);

            doThrow(TenantException.disabled("tenant-001"))
                    .when(validator).validate("tenant-001");

            // When & Then
            assertThatThrownBy(() -> chain.resolve(request))
                    .isInstanceOf(TenantException.class)
                    .hasMessageContaining("租户已禁用");
        }

        @Test
        @DisplayName("解析失败时不应调用验证器")
        void shouldNotValidateWhenResolutionFails() {
            // Given
            TenantResolver resolver = mock(TenantResolver.class);
            when(resolver.getOrder()).thenReturn(100);
            when(resolver.resolve(any())).thenReturn(null);

            List<TenantResolver> resolvers = Collections.singletonList(resolver);
            chain = new TenantResolverChain(resolvers, validator, false);

            // When
            chain.resolve(request);

            // Then
            verify(validator, never()).validate(any());
        }
    }

    @Nested
    @DisplayName("failIfUnresolved 配置测试")
    class FailIfUnresolvedTests {

        @Test
        @DisplayName("failIfUnresolved=true 时应抛出异常")
        void shouldThrowExceptionWhenFailIfUnresolvedIsTrue() {
            // Given
            TenantResolver resolver = mock(TenantResolver.class);
            when(resolver.getOrder()).thenReturn(100);
            when(resolver.resolve(any())).thenReturn(null);

            List<TenantResolver> resolvers = Collections.singletonList(resolver);
            chain = new TenantResolverChain(resolvers, validator, true);

            // When & Then
            assertThatThrownBy(() -> chain.resolve(request))
                    .isInstanceOf(TenantException.class)
                    .hasMessageContaining("无法解析租户");
        }

        @Test
        @DisplayName("failIfUnresolved=false 时应返回 null")
        void shouldReturnNullWhenFailIfUnresolvedIsFalse() {
            // Given
            TenantResolver resolver = mock(TenantResolver.class);
            when(resolver.getOrder()).thenReturn(100);
            when(resolver.resolve(any())).thenReturn(null);

            List<TenantResolver> resolvers = Collections.singletonList(resolver);
            chain = new TenantResolverChain(resolvers, validator, false);

            // When
            TenantContext result = chain.resolve(request);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("NoOp 验证器测试")
    class NoOpValidatorTests {

        @Test
        @DisplayName("使用 NoOp 验证器时应跳过验证")
        void shouldSkipValidationWithNoOpValidator() {
            // Given
            TenantContext context = createTestContext("tenant-001");
            TenantResolver resolver = mock(TenantResolver.class);
            when(resolver.getOrder()).thenReturn(100);
            when(resolver.resolve(any())).thenReturn(context);

            TenantValidator noOpValidator = (tenantId) -> {}; // NoOp implementation

            List<TenantResolver> resolvers = Collections.singletonList(resolver);
            chain = new TenantResolverChain(resolvers, noOpValidator, false);

            // When
            TenantContext result = chain.resolve(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTenantId()).isEqualTo("tenant-001");
        }
    }

    // ========== 测试辅助方法 ==========

    private TenantContext createTestContext(String tenantId) {
        return new TenantContext() {
            @Override
            public String getTenantId() {
                return tenantId;
            }
        };
    }
}