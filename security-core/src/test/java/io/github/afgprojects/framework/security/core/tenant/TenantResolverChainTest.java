package io.github.afgprojects.framework.security.core.tenant;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TenantResolverChain 测试
 */
@DisplayName("TenantResolverChain 测试")
class TenantResolverChainTest {

    private TenantResolverChain chain;

    @BeforeEach
    void setUp() {
        chain = new TenantResolverChain();
    }

    @Nested
    @DisplayName("构造函数")
    class ConstructorTests {

        @Test
        @DisplayName("无参构造函数应创建空链")
        void shouldCreateEmptyChain() {
            TenantResolverChain emptyChain = new TenantResolverChain();

            assertThat(emptyChain.isEmpty()).isTrue();
            assertThat(emptyChain.getResolvers()).isEmpty();
        }

        @Test
        @DisplayName("带解析器列表的构造函数应正确创建")
        void shouldCreateWithResolvers() {
            List<TenantResolver> resolvers = List.of(
                    new HeaderTenantResolver(),
                    createFixedResolver("token-tenant", 100)
            );

            TenantResolverChain chainWithResolvers = new TenantResolverChain(resolvers);

            assertThat(chainWithResolvers.isEmpty()).isFalse();
            assertThat(chainWithResolvers.getResolvers()).hasSize(2);
        }

        @Test
        @DisplayName("带解析器列表和验证器的构造函数应正确创建")
        void shouldCreateWithResolversAndValidator() {
            List<TenantResolver> resolvers = List.of(new HeaderTenantResolver());

            TenantResolverChain chainWithValidator = new TenantResolverChain(resolvers, null, false);

            assertThat(chainWithValidator.isFailIfUnresolved()).isFalse();
        }
    }

    @Nested
    @DisplayName("addResolver")
    class AddResolverTests {

        @Test
        @DisplayName("应添加解析器到链中")
        void shouldAddResolverToChain() {
            chain.addResolver(new HeaderTenantResolver());

            assertThat(chain.isEmpty()).isFalse();
            assertThat(chain.getResolvers()).hasSize(1);
        }

        @Test
        @DisplayName("应按优先级排序")
        void shouldSortByOrder() {
            TenantResolver lowPriority = createFixedResolver("default-tenant", 400);
            TenantResolver highPriority = createFixedResolver("token-tenant", 100);
            TenantResolver mediumPriority = createFixedResolver("header-tenant", 200);

            chain.addResolver(lowPriority);
            chain.addResolver(highPriority);
            chain.addResolver(mediumPriority);

            List<TenantResolver> resolvers = chain.getResolvers();
            assertThat(resolvers).hasSize(3);
            assertThat(resolvers.get(0).getOrder()).isEqualTo(100);
            assertThat(resolvers.get(1).getOrder()).isEqualTo(200);
            assertThat(resolvers.get(2).getOrder()).isEqualTo(400);
        }

        @Test
        @DisplayName("null 解析器应抛出 NullPointerException")
        void shouldThrowWhenAddingNullResolver() {
            assertThatThrownBy(() -> chain.addResolver(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("应支持链式调用")
        void shouldSupportChaining() {
            TenantResolverChain returned = chain.addResolver(new HeaderTenantResolver());

            assertThat(returned).isSameAs(chain);
        }
    }

    @Nested
    @DisplayName("addResolvers")
    class AddResolversTests {

        @Test
        @DisplayName("应添加多个解析器")
        void shouldAddMultipleResolvers() {
            List<TenantResolver> resolvers = List.of(
                    createFixedResolver("token-tenant", 100),
                    createFixedResolver("header-tenant", 200)
            );

            chain.addResolvers(resolvers);

            assertThat(chain.getResolvers()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("resolve")
    class ResolveTests {

        @Test
        @DisplayName("应返回第一个成功解析的租户上下文")
        void shouldReturnFirstResolvedContext() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Tenant-Id", "header-tenant");

            chain.addResolver(new HeaderTenantResolver());
            chain.addResolver(createFixedResolver("default-tenant", 400));

            TenantContext context = chain.resolve(request);

            assertThat(context).isNotNull();
            assertThat(context.getTenantId()).isEqualTo("header-tenant");
        }

        @Test
        @DisplayName("高优先级解析器返回 null 时应降级到低优先级")
        void shouldFallbackToLowerPriorityWhenHigherReturnsNull() {
            MockHttpServletRequest request = new MockHttpServletRequest();

            chain.addResolver(new HeaderTenantResolver()); // 无 header，返回 null
            chain.addResolver(createFixedResolver("default-tenant", 400));

            TenantContext context = chain.resolve(request);

            assertThat(context).isNotNull();
            assertThat(context.getTenantId()).isEqualTo("default-tenant");
        }

        @Test
        @DisplayName("所有解析器都返回 null 且 failIfUnresolved=true 时应抛出异常")
        void shouldThrowWhenAllResolversReturnNullAndFailIfUnresolved() {
            chain.addResolver(new HeaderTenantResolver());
            chain.setFailIfUnresolved(true);

            MockHttpServletRequest request = new MockHttpServletRequest();

            assertThatThrownBy(() -> chain.resolve(request))
                    .isInstanceOf(TenantException.class);
        }

        @Test
        @DisplayName("所有解析器都返回 null 且 failIfUnresolved=false 时应返回 null")
        void shouldReturnNullWhenAllResolversReturnNullAndNotFailIfUnresolved() {
            chain.addResolver(new HeaderTenantResolver());
            chain.setFailIfUnresolved(false);

            MockHttpServletRequest request = new MockHttpServletRequest();

            TenantContext context = chain.resolve(request);

            assertThat(context).isNull();
        }

        @Test
        @DisplayName("无解析器且 failIfUnresolved=true 时应抛出异常")
        void shouldThrowWhenNoResolversAndFailIfUnresolved() {
            chain.setFailIfUnresolved(true);

            MockHttpServletRequest request = new MockHttpServletRequest();

            assertThatThrownBy(() -> chain.resolve(request))
                    .isInstanceOf(TenantException.class);
        }

        @Test
        @DisplayName("无解析器且 failIfUnresolved=false 时应返回 null")
        void shouldReturnNullWhenNoResolversAndNotFailIfUnresolved() {
            chain.setFailIfUnresolved(false);

            MockHttpServletRequest request = new MockHttpServletRequest();

            TenantContext context = chain.resolve(request);

            assertThat(context).isNull();
        }

        @Test
        @DisplayName("解析器抛出非 TenantException 时应继续尝试下一个")
        void shouldContinueWhenResolverThrowsNonTenantException() {
            TenantResolver failingResolver = new TenantResolver() {
                @Override
                public TenantContext resolve(HttpServletRequest request) {
                    throw new RuntimeException("Resolver error");
                }
            };

            chain.addResolver(failingResolver);
            chain.addResolver(createFixedResolver("fallback-tenant", 200));
            chain.setFailIfUnresolved(true);

            MockHttpServletRequest request = new MockHttpServletRequest();

            TenantContext context = chain.resolve(request);

            assertThat(context).isNotNull();
            assertThat(context.getTenantId()).isEqualTo("fallback-tenant");
        }

        @Test
        @DisplayName("解析器抛出 TenantException 时应直接抛出")
        void shouldRethrowTenantException() {
            TenantResolver failingResolver = new TenantResolver() {
                @Override
                public TenantContext resolve(HttpServletRequest request) {
                    throw TenantException.disabled("tenant-001");
                }
            };

            chain.addResolver(failingResolver);

            MockHttpServletRequest request = new MockHttpServletRequest();

            assertThatThrownBy(() -> chain.resolve(request))
                    .isInstanceOf(TenantException.class);
        }
    }

    @Nested
    @DisplayName("getOrder")
    class GetOrderTests {

        @Test
        @DisplayName("应返回 Integer.MAX_VALUE 表示最低优先级")
        void shouldReturnMaxValue() {
            assertThat(chain.getOrder()).isEqualTo(Integer.MAX_VALUE);
        }
    }

    @Nested
    @DisplayName("isEmpty / clear")
    class IsEmptyClearTests {

        @Test
        @DisplayName("空链应 isEmpty 返回 true")
        void shouldBeEmpty() {
            assertThat(chain.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("添加解析器后应 isEmpty 返回 false")
        void shouldNotBeEmptyAfterAddingResolver() {
            chain.addResolver(new HeaderTenantResolver());

            assertThat(chain.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("clear 应清空所有解析器")
        void shouldClearAllResolvers() {
            chain.addResolver(new HeaderTenantResolver());
            chain.addResolver(createFixedResolver("token-tenant", 100));

            chain.clear();

            assertThat(chain.isEmpty()).isTrue();
            assertThat(chain.getResolvers()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getResolvers")
    class GetResolversTests {

        @Test
        @DisplayName("应返回解析器的拷贝列表")
        void shouldReturnCopyOfResolvers() {
            chain.addResolver(new HeaderTenantResolver());

            List<TenantResolver> resolvers = chain.getResolvers();

            // 修改返回的列表不应影响原始链
            resolvers.clear();

            assertThat(chain.getResolvers()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("failIfUnresolved")
    class FailIfUnresolvedTests {

        @Test
        @DisplayName("默认 failIfUnresolved 应为 true")
        void shouldDefaultToTrue() {
            TenantResolverChain newChain = new TenantResolverChain();

            assertThat(newChain.isFailIfUnresolved()).isTrue();
        }

        @Test
        @DisplayName("setFailIfUnresolved 应正确设置")
        void shouldSetFailIfUnresolved() {
            chain.setFailIfUnresolved(false);

            assertThat(chain.isFailIfUnresolved()).isFalse();

            chain.setFailIfUnresolved(true);

            assertThat(chain.isFailIfUnresolved()).isTrue();
        }
    }

    /**
     * 创建固定返回租户 ID 的解析器
     */
    private TenantResolver createFixedResolver(String tenantId, int order) {
        return new TenantResolver() {
            @Override
            public TenantContext resolve(HttpServletRequest request) {
                return new SimpleTenantContext(tenantId);
            }

            @Override
            public int getOrder() {
                return order;
            }
        };
    }
}
