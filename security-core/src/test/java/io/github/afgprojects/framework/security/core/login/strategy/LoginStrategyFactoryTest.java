package io.github.afgprojects.framework.security.core.login.strategy;

import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;
import io.github.afgprojects.framework.security.core.login.model.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LoginStrategyFactory 测试
 */
@DisplayName("LoginStrategyFactory 测试")
class LoginStrategyFactoryTest {

    private LoginStrategyFactory factory;

    @BeforeEach
    void setUp() {
        factory = new LoginStrategyFactory();
    }

    @Nested
    @DisplayName("register")
    class RegisterTests {

        @Test
        @DisplayName("应注册登录策略")
        void shouldRegisterStrategy() {
            LoginStrategy strategy = createMockStrategy("USERNAME");

            factory.register(strategy);

            assertThat(factory.isRegistered("USERNAME")).isTrue();
            assertThat(factory.getRegisteredTypes()).contains("USERNAME");
        }

        @Test
        @DisplayName("应覆盖重复注册的策略")
        void shouldOverwriteDuplicateRegistration() {
            LoginStrategy strategy1 = createMockStrategy("USERNAME");
            LoginStrategy strategy2 = createMockStrategy("USERNAME");

            factory.register(strategy1);
            factory.register(strategy2);

            assertThat(factory.getStrategy("USERNAME")).contains(strategy2);
        }
    }

    @Nested
    @DisplayName("registerAll")
    class RegisterAllTests {

        @Test
        @DisplayName("应批量注册策略")
        void shouldRegisterAllStrategies() {
            List<LoginStrategy> strategies = List.of(
                    createMockStrategy("USERNAME"),
                    createMockStrategy("MOBILE"),
                    createMockStrategy("EMAIL")
            );

            factory.registerAll(strategies);

            assertThat(factory.isRegistered("USERNAME")).isTrue();
            assertThat(factory.isRegistered("MOBILE")).isTrue();
            assertThat(factory.isRegistered("EMAIL")).isTrue();
        }

        @Test
        @DisplayName("null 列表不应抛出异常")
        void shouldNotThrowWhenListIsNull() {
            factory.registerAll(null);

            assertThat(factory.getRegisteredTypes()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getStrategy")
    class GetStrategyTests {

        @Test
        @DisplayName("应根据登录类型获取策略")
        void shouldGetStrategyByLoginType() {
            LoginStrategy strategy = createMockStrategy("USERNAME");
            factory.register(strategy);

            assertThat(factory.getStrategy("USERNAME")).contains(strategy);
        }

        @Test
        @DisplayName("不存在的类型应返回 empty")
        void shouldReturnEmptyForNonExistentType() {
            assertThat(factory.getStrategy("NONEXISTENT")).isEmpty();
        }

        @Test
        @DisplayName("应根据请求获取策略")
        void shouldGetStrategyByRequest() {
            LoginStrategy usernameStrategy = createMockStrategy("USERNAME");
            LoginStrategy mobileStrategy = createMockStrategy("MOBILE");

            factory.register(usernameStrategy);
            factory.register(mobileStrategy);

            LoginRequest usernameRequest = LoginRequest.ofUsername("admin", "password");
            assertThat(factory.getStrategy(usernameRequest)).contains(usernameStrategy);

            LoginRequest mobileRequest = LoginRequest.ofMobile("13800138000", "123456");
            assertThat(factory.getStrategy(mobileRequest)).contains(mobileStrategy);
        }

        @Test
        @DisplayName("无匹配策略时应返回 empty")
        void shouldReturnEmptyWhenNoMatchingStrategy() {
            LoginRequest request = LoginRequest.ofUsername("admin", "password");

            assertThat(factory.getStrategy(request)).isEmpty();
        }
    }

    @Nested
    @DisplayName("isRegistered")
    class IsRegisteredTests {

        @Test
        @DisplayName("已注册的类型应返回 true")
        void shouldReturnTrueForRegisteredType() {
            factory.register(createMockStrategy("USERNAME"));

            assertThat(factory.isRegistered("USERNAME")).isTrue();
        }

        @Test
        @DisplayName("未注册的类型应返回 false")
        void shouldReturnFalseForUnregisteredType() {
            assertThat(factory.isRegistered("USERNAME")).isFalse();
        }
    }

    @Nested
    @DisplayName("getRegisteredTypes")
    class GetRegisteredTypesTests {

        @Test
        @DisplayName("应返回所有已注册的类型")
        void shouldReturnAllRegisteredTypes() {
            factory.register(createMockStrategy("USERNAME"));
            factory.register(createMockStrategy("MOBILE"));
            factory.register(createMockStrategy("EMAIL"));

            assertThat(factory.getRegisteredTypes()).containsExactlyInAnyOrder("USERNAME", "MOBILE", "EMAIL");
        }

        @Test
        @DisplayName("空工厂应返回空集合")
        void shouldReturnEmptyCollectionForEmptyFactory() {
            assertThat(factory.getRegisteredTypes()).isEmpty();
        }
    }

    /**
     * 创建模拟登录策略
     */
    private LoginStrategy createMockStrategy(String loginType) {
        return new LoginStrategy() {
            @Override
            public String getLoginType() {
                return loginType;
            }

            @Override
            public AfgUserDetails authenticate(LoginRequest request) {
                // 不需要真实实现，只用于测试工厂注册
                return new AfgUserDetails() {
                    @Override
                    public String getUserId() {
                        return "user-001";
                    }

                    @Override
                    public String getUsername() {
                        return "admin";
                    }

                    @Override
                    public String getTenantId() {
                        return null;
                    }

                    @Override
                    public Set<String> getRoles() {
                        return Set.of("ADMIN");
                    }

                    @Override
                    public Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
                        return List.of();
                    }

                    @Override
                    public String getPassword() {
                        return null;
                    }
                };
            }
        };
    }
}