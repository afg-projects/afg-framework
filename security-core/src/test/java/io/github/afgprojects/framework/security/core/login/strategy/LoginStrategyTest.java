package io.github.afgprojects.framework.security.core.login.strategy;

import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;
import io.github.afgprojects.framework.security.core.login.model.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LoginStrategy 接口默认方法测试
 */
@DisplayName("LoginStrategy 接口默认方法测试")
class LoginStrategyTest {

    @Nested
    @DisplayName("supports 默认方法")
    class SupportsTests {

        @Test
        @DisplayName("loginType 匹配时应返回 true")
        void shouldReturnTrueWhenLoginTypeMatches() {
            LoginStrategy strategy = createStrategy("USERNAME");

            LoginRequest request = LoginRequest.ofUsername("admin", "password");

            assertThat(strategy.supports(request)).isTrue();
        }

        @Test
        @DisplayName("loginType 不匹配时应返回 false")
        void shouldReturnFalseWhenLoginTypeDoesNotMatch() {
            LoginStrategy strategy = createStrategy("USERNAME");

            LoginRequest request = LoginRequest.ofMobile("13800138000", "123456");

            assertThat(strategy.supports(request)).isFalse();
        }

        @Test
        @DisplayName("MOBILE 策略应支持 MOBILE 请求")
        void shouldSupportMobileRequest() {
            LoginStrategy strategy = createStrategy("MOBILE");

            LoginRequest request = LoginRequest.ofMobile("13800138000", "123456");

            assertThat(strategy.supports(request)).isTrue();
        }

        @Test
        @DisplayName("EMAIL 策略应支持 EMAIL 请求")
        void shouldSupportEmailRequest() {
            LoginStrategy strategy = createStrategy("EMAIL");

            LoginRequest request = LoginRequest.ofEmail("test@example.com", "123456");

            assertThat(strategy.supports(request)).isTrue();
        }
    }

    /**
     * 创建简单策略实例用于测试 default 方法
     */
    private LoginStrategy createStrategy(String loginType) {
        return new LoginStrategy() {
            @Override
            public String getLoginType() {
                return loginType;
            }

            @Override
            public AfgUserDetails authenticate(LoginRequest request) {
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
                        return Set.of();
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