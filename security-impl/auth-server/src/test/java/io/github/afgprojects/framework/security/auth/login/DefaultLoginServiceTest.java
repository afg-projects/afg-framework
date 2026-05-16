package io.github.afgprojects.framework.security.auth.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetailsService;
import io.github.afgprojects.framework.security.core.login.CaptchaService;
import io.github.afgprojects.framework.security.core.login.TokenService;
import io.github.afgprojects.framework.security.core.login.model.CaptchaRequest;
import io.github.afgprojects.framework.security.core.login.model.CaptchaResponse;
import io.github.afgprojects.framework.security.core.login.model.CaptchaType;
import io.github.afgprojects.framework.security.core.login.model.LoginRequest;
import io.github.afgprojects.framework.security.core.login.model.LoginResponse;
import io.github.afgprojects.framework.security.core.login.strategy.LoginStrategyFactory;
import io.github.afgprojects.framework.security.core.token.TokenValidationException;

/**
 * DefaultLoginService 测试
 */
@DisplayName("DefaultLoginService 测试")
@ExtendWith(MockitoExtension.class)
class DefaultLoginServiceTest {

    @Mock
    private LoginStrategyFactory strategyFactory;

    @Mock
    private AfgUserDetailsService userDetailsService;

    @Mock
    private TokenService tokenService;

    @Mock
    private CaptchaService captchaService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private DefaultLoginService loginService;

    @BeforeEach
    void setUp() {
        loginService = new DefaultLoginService(strategyFactory, userDetailsService, tokenService, captchaService);
    }

    @Nested
    @DisplayName("用户名密码登录测试")
    class UsernamePasswordLoginTests {

        @Test
        @DisplayName("用户名密码登录成功")
        void shouldLoginSuccessfullyWithUsernameAndPassword() {
            // given
            String username = "testuser";
            String password = "password123";
            String encodedPassword = "$2a$10$encoded";
            String accessToken = "access-token-123";
            String refreshToken = "refresh-token-456";

            AfgUserDetails userDetails = createMockUserDetails("user-1", username, encodedPassword, Set.of("USER"), Set.of("read:user"));
            LoginRequest request = LoginRequest.ofUsername(username, password);

            when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
            when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);
            when(tokenService.generateAccessToken(eq("user-1"), eq(username), any(), any(), any())).thenReturn(accessToken);
            when(tokenService.generateRefreshToken(eq("user-1"), any())).thenReturn(refreshToken);
            when(tokenService.getAccessTokenTtl()).thenReturn(7200L);

            // when
            LoginResponse response = loginService.login(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo(accessToken);
            assertThat(response.refreshToken()).isEqualTo(refreshToken);
            assertThat(response.userId()).isEqualTo("user-1");
            assertThat(response.username()).isEqualTo(username);
            assertThat(response.roles()).containsExactly("USER");
            assertThat(response.permissions()).containsExactly("read:user");
        }

        @Test
        @DisplayName("用户名密码登录失败 - 用户不存在")
        void shouldFailLoginWhenUserNotFound() {
            // given
            String username = "nonexistent";
            String password = "password123";
            LoginRequest request = LoginRequest.ofUsername(username, password);

            when(userDetailsService.loadUserByUsername(username))
                    .thenThrow(new UsernameNotFoundException("User not found"));

            // when & then
            assertThatThrownBy(() -> loginService.login(request))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("User not found");

            verify(tokenService, never()).generateAccessToken(anyString(), anyString(), any(), any(), any());
        }

        @Test
        @DisplayName("用户名密码登录失败 - 密码错误")
        void shouldFailLoginWhenPasswordIncorrect() {
            // given
            String username = "testuser";
            String password = "wrongpassword";
            String encodedPassword = "$2a$10$encoded";

            AfgUserDetails userDetails = createMockUserDetails("user-1", username, encodedPassword, Set.of("USER"), Set.of());
            LoginRequest request = LoginRequest.ofUsername(username, password);

            when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
            when(passwordEncoder.matches(password, encodedPassword)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> loginService.login(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("密码错误");

            verify(tokenService, never()).generateAccessToken(anyString(), anyString(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("用户名密码验证码登录测试")
    class UsernamePasswordCaptchaLoginTests {

        @Test
        @DisplayName("用户名密码验证码登录成功")
        void shouldLoginSuccessfullyWithUsernamePasswordAndCaptcha() {
            // given
            String username = "testuser";
            String password = "password123";
            String encodedPassword = "$2a$10$encoded";
            String captchaKey = "captcha-key-123";
            String captchaValue = "1234";
            String accessToken = "access-token-123";
            String refreshToken = "refresh-token-456";

            AfgUserDetails userDetails = createMockUserDetails("user-1", username, encodedPassword, Set.of("USER"), Set.of());
            LoginRequest request = new LoginRequest(
                    LoginRequest.LoginType.USERNAME,
                    username,
                    password,
                    null,
                    null,
                    captchaKey,
                    captchaValue,
                    null,
                    null,
                    null,
                    null,
                    null);

            when(captchaService.validate(captchaKey, captchaValue)).thenReturn(true);
            when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
            when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);
            when(tokenService.generateAccessToken(eq("user-1"), eq(username), any(), any(), any())).thenReturn(accessToken);
            when(tokenService.generateRefreshToken(eq("user-1"), any())).thenReturn(refreshToken);
            when(tokenService.getAccessTokenTtl()).thenReturn(7200L);

            // when
            LoginResponse response = loginService.login(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo(accessToken);
            verify(captchaService).validate(captchaKey, captchaValue);
        }

        @Test
        @DisplayName("验证码错误拒绝登录")
        void shouldRejectLoginWhenCaptchaIncorrect() {
            // given
            String username = "testuser";
            String password = "password123";
            String captchaKey = "captcha-key-123";
            String captchaValue = "wrong";

            LoginRequest request = new LoginRequest(
                    LoginRequest.LoginType.USERNAME,
                    username,
                    password,
                    null,
                    null,
                    captchaKey,
                    captchaValue,
                    null,
                    null,
                    null,
                    null,
                    null);

            when(captchaService.validate(captchaKey, captchaValue)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> loginService.login(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("验证码错误");

            verify(userDetailsService, never()).loadUserByUsername(anyString());
        }
    }

    @Nested
    @DisplayName("手机号验证码登录测试")
    class MobileCaptchaLoginTests {

        @Test
        @DisplayName("手机号验证码登录成功")
        void shouldLoginSuccessfullyWithMobileAndCaptcha() {
            // given
            String mobile = "13800138000";
            String captchaValue = "123456";
            String accessToken = "access-token-123";
            String refreshToken = "refresh-token-456";

            AfgUserDetails userDetails = createMockUserDetails("user-1", "mobileuser", "N/A", Set.of("USER"), Set.of());
            LoginRequest request = LoginRequest.ofMobile(mobile, captchaValue);

            // For mobile login, captchaKey is the mobile number
            when(captchaService.validate("sms:" + mobile, captchaValue)).thenReturn(true);
            when(userDetailsService.loadUserByMobile(mobile)).thenReturn(userDetails);
            when(tokenService.generateAccessToken(eq("user-1"), eq("mobileuser"), any(), any(), any())).thenReturn(accessToken);
            when(tokenService.generateRefreshToken(eq("user-1"), any())).thenReturn(refreshToken);
            when(tokenService.getAccessTokenTtl()).thenReturn(7200L);

            // when
            LoginResponse response = loginService.login(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo(accessToken);
            assertThat(response.userId()).isEqualTo("user-1");
        }
    }

    @Nested
    @DisplayName("邮箱验证码登录测试")
    class EmailCaptchaLoginTests {

        @Test
        @DisplayName("邮箱验证码登录成功")
        void shouldLoginSuccessfullyWithEmailAndCaptcha() {
            // given
            String email = "test@example.com";
            String captchaValue = "123456";
            String accessToken = "access-token-123";
            String refreshToken = "refresh-token-456";

            AfgUserDetails userDetails = createMockUserDetails("user-1", "emailuser", "N/A", Set.of("USER"), Set.of());
            LoginRequest request = LoginRequest.ofEmail(email, captchaValue);

            // For email login, captchaKey is the email address
            when(captchaService.validate("email:" + email, captchaValue)).thenReturn(true);
            when(userDetailsService.loadUserByEmail(email)).thenReturn(userDetails);
            when(tokenService.generateAccessToken(eq("user-1"), eq("emailuser"), any(), any(), any())).thenReturn(accessToken);
            when(tokenService.generateRefreshToken(eq("user-1"), any())).thenReturn(refreshToken);
            when(tokenService.getAccessTokenTtl()).thenReturn(7200L);

            // when
            LoginResponse response = loginService.login(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo(accessToken);
            assertThat(response.userId()).isEqualTo("user-1");
        }
    }

    @Nested
    @DisplayName("登出测试")
    class LogoutTests {

        @Test
        @DisplayName("登出使 Token 失效")
        void shouldInvalidateTokenOnLogout() {
            // given
            String token = "access-token-to-invalidate";

            // when
            loginService.logout(token);

            // then
            verify(tokenService).invalidateToken(token);
        }
    }

    @Nested
    @DisplayName("刷新 Token 测试")
    class RefreshTokenTests {

        @Test
        @DisplayName("刷新 Token 返回新的 Token")
        void shouldReturnNewTokenWhenRefreshTokenValid() {
            // given
            String oldRefreshToken = "old-refresh-token";
            String newAccessToken = "new-access-token";
            String newRefreshToken = "new-refresh-token";
            String userId = "user-1";
            String username = "testuser";

            AfgUserDetails userDetails = createMockUserDetails(userId, username, "N/A", Set.of("USER"), Set.of("read:user"));

            when(tokenService.validateRefreshToken(oldRefreshToken)).thenReturn(true);
            when(tokenService.extractUserId(oldRefreshToken)).thenReturn(userId);
            when(userDetailsService.loadUserByUserId(userId)).thenReturn(userDetails);
            when(tokenService.generateAccessToken(eq(userId), eq(username), any(), any(), any())).thenReturn(newAccessToken);
            when(tokenService.generateRefreshToken(eq(userId), any())).thenReturn(newRefreshToken);
            when(tokenService.getAccessTokenTtl()).thenReturn(7200L);

            // when
            LoginResponse response = loginService.refreshToken(oldRefreshToken);

            // then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo(newAccessToken);
            assertThat(response.refreshToken()).isEqualTo(newRefreshToken);
        }

        @Test
        @DisplayName("无效 Refresh Token 抛出异常")
        void shouldThrowExceptionWhenRefreshTokenInvalid() {
            // given
            String invalidRefreshToken = "invalid-refresh-token";

            when(tokenService.validateRefreshToken(invalidRefreshToken)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> loginService.refreshToken(invalidRefreshToken))
                    .isInstanceOf(TokenValidationException.class)
                    .hasMessageContaining("invalid");
        }
    }

    @Nested
    @DisplayName("验证码测试")
    class CaptchaTests {

        @Test
        @DisplayName("生成验证码")
        void shouldGenerateCaptcha() {
            // given
            CaptchaRequest request = CaptchaRequest.ofImage();
            CaptchaResponse expectedResponse = CaptchaResponse.builder()
                    .captchaKey("captcha-key-123")
                    .captchaImage("base64-image-data")
                    .captchaType(CaptchaType.IMAGE)
                    .expiresIn(300)
                    .build();

            when(captchaService.generate(request)).thenReturn(expectedResponse);

            // when
            CaptchaResponse response = loginService.generateCaptcha(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.captchaKey()).isEqualTo("captcha-key-123");
            assertThat(response.captchaImage()).isEqualTo("base64-image-data");
            assertThat(response.captchaType()).isEqualTo(CaptchaType.IMAGE);
        }

        @Test
        @DisplayName("验证验证码 - 正确")
        void shouldValidateCorrectCaptcha() {
            // given
            String captchaKey = "captcha-key-123";
            String captchaValue = "1234";

            when(captchaService.validate(captchaKey, captchaValue)).thenReturn(true);

            // when
            boolean result = loginService.validateCaptcha(captchaKey, captchaValue);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("验证验证码 - 错误")
        void shouldValidateIncorrectCaptcha() {
            // given
            String captchaKey = "captcha-key-123";
            String captchaValue = "wrong";

            when(captchaService.validate(captchaKey, captchaValue)).thenReturn(false);

            // when
            boolean result = loginService.validateCaptcha(captchaKey, captchaValue);

            // then
            assertThat(result).isFalse();
        }
    }

    /**
     * 创建模拟的 AfgUserDetails
     */
    private AfgUserDetails createMockUserDetails(
            String userId,
            String username,
            String password,
            Set<String> roles,
            Set<String> permissions) {

        return new AfgUserDetails() {
            @Override
            @NonNull
            public String getUserId() {
                return userId;
            }

            @Override
            @NonNull
            public String getUsername() {
                return username;
            }

            @Override
            @Nullable
            public String getTenantId() {
                return null;
            }

            @Override
            @NonNull
            public Set<String> getRoles() {
                return roles;
            }

            @Override
            @NonNull
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return permissions.stream()
                        .map(p -> (GrantedAuthority) () -> p)
                        .toList();
            }

            @Override
            public String getPassword() {
                return password;
            }

            @Override
            public boolean isAccountNonExpired() {
                return true;
            }

            @Override
            public boolean isAccountNonLocked() {
                return true;
            }

            @Override
            public boolean isCredentialsNonExpired() {
                return true;
            }

            @Override
            public boolean isEnabled() {
                return true;
            }
        };
    }
}
