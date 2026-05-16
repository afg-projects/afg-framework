package io.github.afgprojects.framework.security.auth.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetailsService;
import io.github.afgprojects.framework.security.core.audit.LoginLogService;
import io.github.afgprojects.framework.security.core.audit.model.LoginLog;
import io.github.afgprojects.framework.security.core.login.CaptchaService;
import io.github.afgprojects.framework.security.core.login.TokenService;
import io.github.afgprojects.framework.security.core.login.model.LoginRequest;
import io.github.afgprojects.framework.security.core.login.model.LoginResponse;
import io.github.afgprojects.framework.security.core.login.strategy.LoginStrategy;
import io.github.afgprojects.framework.security.core.login.strategy.LoginStrategyFactory;
import io.github.afgprojects.framework.security.core.security.DeviceLimiter;
import io.github.afgprojects.framework.security.core.security.IpRestrictionChecker;
import io.github.afgprojects.framework.security.core.security.LoginFailureTracker;
import io.github.afgprojects.framework.security.core.security.PasswordValidator;

/**
 * DefaultLoginService 安全策略集成测试
 */
@DisplayName("DefaultLoginService 安全策略集成测试")
@ExtendWith(MockitoExtension.class)
class DefaultLoginServiceSecurityTest {

    @Mock
    private LoginStrategyFactory strategyFactory;

    @Mock
    private AfgUserDetailsService userDetailsService;

    @Mock
    private TokenService tokenService;

    @Mock
    private CaptchaService captchaService;

    @Mock
    private LoginFailureTracker loginFailureTracker;

    @Mock
    private PasswordValidator passwordValidator;

    @Mock
    private IpRestrictionChecker ipRestrictionChecker;

    @Mock
    private DeviceLimiter deviceLimiter;

    @Mock
    private LoginLogService loginLogService;

    @Mock
    private LoginStrategy loginStrategy;

    private DefaultLoginService loginService;

    @BeforeEach
    void setUp() {
        loginService = new DefaultLoginService(
                strategyFactory,
                userDetailsService,
                tokenService,
                captchaService,
                loginFailureTracker,
                passwordValidator,
                ipRestrictionChecker,
                deviceLimiter,
                loginLogService);
    }

    @Nested
    @DisplayName("IP 限制检查测试")
    class IpRestrictionTests {

        @Test
        @DisplayName("IP 在黑名单中应拒绝登录")
        void shouldRejectLoginWhenIpIsBlacklisted() {
            // given
            String ip = "192.168.1.100";
            String username = "testuser";
            LoginRequest request = createLoginRequest(username, "password", ip, null, null);

            when(ipRestrictionChecker.isAllowed(eq(ip), any(), any())).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> loginService.login(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("IP 已被限制");

            verify(loginStrategy, never()).authenticate(any());
            verify(loginLogService).recordLogin(any(LoginLog.class));
        }

        @Test
        @DisplayName("IP 不在限制列表中应允许登录")
        void shouldAllowLoginWhenIpIsAllowed() {
            // given
            String ip = "192.168.1.100";
            String username = "testuser";
            String userId = "user-1";
            AfgUserDetails userDetails = createMockUserDetails(userId, username, Set.of("USER"));

            LoginRequest request = createLoginRequest(username, "password", ip, null, null);

            when(strategyFactory.getStrategy(request)).thenReturn(java.util.Optional.of(loginStrategy));
            when(ipRestrictionChecker.isAllowed(eq(ip), any(), any())).thenReturn(true);
            when(loginStrategy.authenticate(request)).thenReturn(userDetails);
            when(loginFailureTracker.isLocked(eq(userId), any())).thenReturn(false);
            when(tokenService.generateAccessToken(anyString(), anyString(), any(), any(), any())).thenReturn("access-token");
            when(tokenService.generateRefreshToken(anyString(), any())).thenReturn("refresh-token");
            when(tokenService.getAccessTokenTtl()).thenReturn(7200L);

            // when
            LoginResponse response = loginService.login(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo("access-token");
            verify(ipRestrictionChecker).isAllowed(eq(ip), any(), any());
        }
    }

    @Nested
    @DisplayName("账户锁定检查测试")
    class AccountLockTests {

        @Test
        @DisplayName("账户被锁定应拒绝登录")
        void shouldRejectLoginWhenAccountIsLocked() {
            // given
            String ip = "192.168.1.100";
            String username = "testuser";
            String userId = "user-1";
            AfgUserDetails userDetails = createMockUserDetails(userId, username, Set.of("USER"));

            LoginRequest request = createLoginRequest(username, "password", ip, null, null);

            when(strategyFactory.getStrategy(request)).thenReturn(java.util.Optional.of(loginStrategy));
            when(ipRestrictionChecker.isAllowed(eq(ip), any(), any())).thenReturn(true);
            when(loginStrategy.authenticate(request)).thenReturn(userDetails);
            when(loginFailureTracker.isLocked(eq(userId), any())).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> loginService.login(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("账户已被锁定");

            verify(tokenService, never()).generateAccessToken(anyString(), anyString(), any(), any(), any());
        }

        @Test
        @DisplayName("账户未锁定应允许登录")
        void shouldAllowLoginWhenAccountIsNotLocked() {
            // given
            String ip = "192.168.1.100";
            String username = "testuser";
            String userId = "user-1";
            AfgUserDetails userDetails = createMockUserDetails(userId, username, Set.of("USER"));

            LoginRequest request = createLoginRequest(username, "password", ip, null, null);

            when(strategyFactory.getStrategy(request)).thenReturn(java.util.Optional.of(loginStrategy));
            when(ipRestrictionChecker.isAllowed(eq(ip), any(), any())).thenReturn(true);
            when(loginStrategy.authenticate(request)).thenReturn(userDetails);
            when(loginFailureTracker.isLocked(eq(userId), any())).thenReturn(false);
            when(tokenService.generateAccessToken(anyString(), anyString(), any(), any(), any())).thenReturn("access-token");
            when(tokenService.generateRefreshToken(anyString(), any())).thenReturn("refresh-token");
            when(tokenService.getAccessTokenTtl()).thenReturn(7200L);

            // when
            LoginResponse response = loginService.login(request);

            // then
            assertThat(response).isNotNull();
            verify(loginFailureTracker).isLocked(eq(userId), any());
        }
    }

    @Nested
    @DisplayName("设备注册测试")
    class DeviceRegistrationTests {

        @Test
        @DisplayName("登录成功后应注册设备")
        void shouldRegisterDeviceOnSuccessfulLogin() {
            // given
            String ip = "192.168.1.100";
            String username = "testuser";
            String userId = "user-1";
            String deviceId = "device-001";
            String deviceName = "iPhone 15";
            AfgUserDetails userDetails = createMockUserDetails(userId, username, Set.of("USER"));

            LoginRequest request = createLoginRequest(username, "password", ip, deviceId, deviceName);

            when(strategyFactory.getStrategy(request)).thenReturn(java.util.Optional.of(loginStrategy));
            when(ipRestrictionChecker.isAllowed(eq(ip), any(), any())).thenReturn(true);
            when(loginStrategy.authenticate(request)).thenReturn(userDetails);
            when(loginFailureTracker.isLocked(eq(userId), any())).thenReturn(false);
            when(deviceLimiter.registerDevice(eq(userId), any(), eq(deviceId), eq(deviceName), any(), eq(ip))).thenReturn(true);
            when(tokenService.generateAccessToken(anyString(), anyString(), any(), any(), any())).thenReturn("access-token");
            when(tokenService.generateRefreshToken(anyString(), any())).thenReturn("refresh-token");
            when(tokenService.getAccessTokenTtl()).thenReturn(7200L);

            // when
            LoginResponse response = loginService.login(request);

            // then
            assertThat(response).isNotNull();
            verify(deviceLimiter).registerDevice(eq(userId), any(), eq(deviceId), eq(deviceName), any(), eq(ip));
        }

        @Test
        @DisplayName("无设备 ID 时不注册设备")
        void shouldNotRegisterDeviceWhenDeviceIdIsNull() {
            // given
            String ip = "192.168.1.100";
            String username = "testuser";
            String userId = "user-1";
            AfgUserDetails userDetails = createMockUserDetails(userId, username, Set.of("USER"));

            LoginRequest request = createLoginRequest(username, "password", ip, null, null);

            when(strategyFactory.getStrategy(request)).thenReturn(java.util.Optional.of(loginStrategy));
            when(ipRestrictionChecker.isAllowed(eq(ip), any(), any())).thenReturn(true);
            when(loginStrategy.authenticate(request)).thenReturn(userDetails);
            when(loginFailureTracker.isLocked(eq(userId), any())).thenReturn(false);
            when(tokenService.generateAccessToken(anyString(), anyString(), any(), any(), any())).thenReturn("access-token");
            when(tokenService.generateRefreshToken(anyString(), any())).thenReturn("refresh-token");
            when(tokenService.getAccessTokenTtl()).thenReturn(7200L);

            // when
            LoginResponse response = loginService.login(request);

            // then
            assertThat(response).isNotNull();
            verify(deviceLimiter, never()).registerDevice(anyString(), any(), anyString(), any(), any(), anyString());
        }
    }

    @Nested
    @DisplayName("登录失败追踪测试")
    class LoginFailureTrackingTests {

        @Test
        @DisplayName("登录失败应记录失败")
        void shouldRecordFailureOnLoginFailure() {
            // given
            String ip = "192.168.1.100";
            String username = "testuser";
            LoginRequest request = createLoginRequest(username, "password", ip, null, null);

            when(strategyFactory.getStrategy(request)).thenReturn(java.util.Optional.of(loginStrategy));
            when(ipRestrictionChecker.isAllowed(eq(ip), any(), any())).thenReturn(true);
            when(loginStrategy.authenticate(request)).thenThrow(new RuntimeException("认证失败"));

            // when & then
            assertThatThrownBy(() -> loginService.login(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("认证失败");

            verify(loginFailureTracker, never()).recordFailure(anyString(), any(), anyString());
            verify(loginLogService).recordLogin(any(LoginLog.class));
        }

        @Test
        @DisplayName("登录成功应清除失败记录")
        void shouldResetFailureOnSuccessfulLogin() {
            // given
            String ip = "192.168.1.100";
            String username = "testuser";
            String userId = "user-1";
            AfgUserDetails userDetails = createMockUserDetails(userId, username, Set.of("USER"));

            LoginRequest request = createLoginRequest(username, "password", ip, null, null);

            when(strategyFactory.getStrategy(request)).thenReturn(java.util.Optional.of(loginStrategy));
            when(ipRestrictionChecker.isAllowed(eq(ip), any(), any())).thenReturn(true);
            when(loginStrategy.authenticate(request)).thenReturn(userDetails);
            when(loginFailureTracker.isLocked(eq(userId), any())).thenReturn(false);
            when(tokenService.generateAccessToken(anyString(), anyString(), any(), any(), any())).thenReturn("access-token");
            when(tokenService.generateRefreshToken(anyString(), any())).thenReturn("refresh-token");
            when(tokenService.getAccessTokenTtl()).thenReturn(7200L);

            // when
            loginService.login(request);

            // then
            verify(loginFailureTracker).reset(eq(userId), any());
        }
    }

    @Nested
    @DisplayName("登录日志记录测试")
    class LoginLogTests {

        @Test
        @DisplayName("登录成功应记录成功日志")
        void shouldRecordSuccessLogOnSuccessfulLogin() {
            // given
            String ip = "192.168.1.100";
            String username = "testuser";
            String userId = "user-1";
            AfgUserDetails userDetails = createMockUserDetails(userId, username, Set.of("USER"));

            LoginRequest request = createLoginRequest(username, "password", ip, null, null);

            when(strategyFactory.getStrategy(request)).thenReturn(java.util.Optional.of(loginStrategy));
            when(ipRestrictionChecker.isAllowed(eq(ip), any(), any())).thenReturn(true);
            when(loginStrategy.authenticate(request)).thenReturn(userDetails);
            when(loginFailureTracker.isLocked(eq(userId), any())).thenReturn(false);
            when(tokenService.generateAccessToken(anyString(), anyString(), any(), any(), any())).thenReturn("access-token");
            when(tokenService.generateRefreshToken(anyString(), any())).thenReturn("refresh-token");
            when(tokenService.getAccessTokenTtl()).thenReturn(7200L);

            // when
            loginService.login(request);

            // then
            verify(loginLogService).recordLogin(any(LoginLog.class));
        }

        @Test
        @DisplayName("登录失败应记录失败日志")
        void shouldRecordFailureLogOnLoginFailure() {
            // given
            String ip = "192.168.1.100";
            String username = "testuser";
            LoginRequest request = createLoginRequest(username, "password", ip, null, null);

            when(strategyFactory.getStrategy(request)).thenReturn(java.util.Optional.of(loginStrategy));
            when(ipRestrictionChecker.isAllowed(eq(ip), any(), any())).thenReturn(true);
            when(loginStrategy.authenticate(request)).thenThrow(new RuntimeException("认证失败"));

            // when & then
            assertThatThrownBy(() -> loginService.login(request))
                    .isInstanceOf(RuntimeException.class);

            verify(loginLogService).recordLogin(any(LoginLog.class));
        }
    }

    @Nested
    @DisplayName("登出日志测试")
    class LogoutLogTests {

        @Test
        @DisplayName("登出应使 Token 失效")
        void shouldInvalidateTokenOnLogout() {
            // given
            String token = "access-token";
            String userId = "user-1";

            when(tokenService.extractUserId(token)).thenReturn(userId);

            // when
            loginService.logout(token);

            // then
            verify(tokenService).invalidateToken(token);
        }

        @Test
        @DisplayName("登出应记录登出日志")
        void shouldRecordLogoutLog() {
            // given
            String token = "access-token";
            String userId = "user-1";
            String tenantId = "tenant-001";

            when(tokenService.extractUserId(token)).thenReturn(userId);
            when(tokenService.extractTenantId(token)).thenReturn(tenantId);

            // when
            loginService.logout(token);

            // then
            verify(loginLogService).recordLogout(eq(userId), eq(tenantId), any());
        }
    }

    @Nested
    @DisplayName("可选依赖测试")
    class OptionalDependencyTests {

        @Test
        @DisplayName("安全策略为 null 时仍可正常工作")
        void shouldWorkWhenSecurityStrategiesAreNull() {
            // given
            DefaultLoginService serviceWithoutSecurity = new DefaultLoginService(
                    strategyFactory,
                    userDetailsService,
                    tokenService,
                    captchaService,
                    null, // loginFailureTracker
                    null, // passwordValidator
                    null, // ipRestrictionChecker
                    null, // deviceLimiter
                    null  // loginLogService
            );

            String ip = "192.168.1.100";
            String username = "testuser";
            String userId = "user-1";
            AfgUserDetails userDetails = createMockUserDetails(userId, username, Set.of("USER"));

            LoginRequest request = createLoginRequest(username, "password", ip, null, null);

            when(strategyFactory.getStrategy(request)).thenReturn(java.util.Optional.of(loginStrategy));
            when(loginStrategy.authenticate(request)).thenReturn(userDetails);
            when(tokenService.generateAccessToken(anyString(), anyString(), any(), any(), any())).thenReturn("access-token");
            when(tokenService.generateRefreshToken(anyString(), any())).thenReturn("refresh-token");
            when(tokenService.getAccessTokenTtl()).thenReturn(7200L);

            // when
            LoginResponse response = serviceWithoutSecurity.login(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo("access-token");
        }
    }

    /**
     * 创建登录请求
     */
    private LoginRequest createLoginRequest(String username, String password, String ip, String deviceId, String deviceName) {
        return new LoginRequest(
                LoginRequest.LoginType.USERNAME,
                username,
                password,
                null,
                null,
                null,
                null,
                null,
                deviceId,
                deviceName,
                null,
                ip,
                null);
    }

    /**
     * 创建模拟的 AfgUserDetails
     */
    private AfgUserDetails createMockUserDetails(String userId, String username, Set<String> roles) {
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
                return roles.stream()
                        .map(r -> (GrantedAuthority) () -> "ROLE_" + r)
                        .toList();
            }

            @Override
            public String getPassword() {
                return "encoded-password";
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
