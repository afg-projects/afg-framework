package io.github.afgprojects.framework.security.auth.login.social;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetailsService;
import io.github.afgprojects.framework.security.core.login.model.LoginRequest;
import io.github.afgprojects.framework.security.core.login.strategy.LoginStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.client.RestClient;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AbstractSocialLoginStrategy 测试
 *
 * <p>验证模板方法模式的核心流程：extractCode -> exchangeToken -> getUserInfo -> mapToSystemUser。
 * 使用具体的测试子类替代 mock。
 */
@DisplayName("AbstractSocialLoginStrategy 测试")
class AbstractSocialLoginStrategyTest {

    /**
     * 测试用的 AfgUserDetails 实现。
     */
    private static class TestUserDetails implements AfgUserDetails {

        private final String userId;
        private final String username;
        private final String tenantId;
        private final Set<String> roles;

        TestUserDetails(String userId, String username, String tenantId, Set<String> roles) {
            this.userId = userId;
            this.username = username;
            this.tenantId = tenantId;
            this.roles = roles;
        }

        @Override
        public String getUserId() { return userId; }

        @Override
        public String getUsername() { return username; }

        @Override
        public String getPassword() { return ""; }

        @Override
        public String getTenantId() { return tenantId; }

        @Override
        public Set<String> getRoles() { return roles; }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() { return Collections.emptySet(); }
    }

    /**
     * 测试用的 AfgUserDetailsService 实现。
     * 使用内存映射存储用户，不使用 mock。
     */
    private static class TestUserDetailsService implements AfgUserDetailsService {

        private final java.util.Map<String, AfgUserDetails> usersByOpenId = new java.util.HashMap<>();
        private final java.util.Map<String, AfgUserDetails> usersByUsername = new java.util.HashMap<>();

        void addUser(String openId, AfgUserDetails user) {
            usersByOpenId.put(openId, user);
            usersByUsername.put(user.getUsername(), user);
        }

        @Override
        public AfgUserDetails loadUserByUsername(String username) {
            AfgUserDetails user = usersByUsername.get(username);
            if (user == null) {
                throw new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found: " + username);
            }
            return user;
        }

        @Override
        public AfgUserDetails loadUserByUserId(String userId) {
            return usersByUsername.values().stream()
                    .filter(u -> u.getUserId().equals(userId))
                    .findFirst()
                    .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found: " + userId));
        }

        @Override
        public AfgUserDetails loadUserBySocialOpenId(String openId, String source) {
            AfgUserDetails user = usersByOpenId.get(source + ":" + openId);
            if (user == null) {
                throw new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found for openId: " + openId);
            }
            return user;
        }
    }

    /**
     * 成功的社交登录策略测试子类。
     * 模拟完整的 OAuth2 流程。
     */
    private static class SuccessfulSocialStrategy extends AbstractSocialLoginStrategy {

        private final SocialTokenResponse tokenResponse;
        private final SocialUserInfo userInfo;

        SuccessfulSocialStrategy(AfgUserDetailsService userDetailsService,
                                  RestClient restClient,
                                  SocialTokenResponse tokenResponse,
                                  SocialUserInfo userInfo) {
            super(userDetailsService, restClient);
            this.tokenResponse = tokenResponse;
            this.userInfo = userInfo;
        }

        @Override
        public String getLoginType() { return "WECHAT"; }

        @Override
        protected String getStrategyName() { return "测试社交平台"; }

        @Override
        protected SocialTokenResponse exchangeToken(String code, String redirectUri) {
            return tokenResponse;
        }

        @Override
        protected SocialUserInfo getUserInfo(SocialTokenResponse tokenResponse) {
            return userInfo;
        }
    }

    /**
     * Token 交换失败的社交登录策略。
     */
    private static class TokenExchangeFailedStrategy extends AbstractSocialLoginStrategy {

        TokenExchangeFailedStrategy(AfgUserDetailsService userDetailsService, RestClient restClient) {
            super(userDetailsService, restClient);
        }

        @Override
        public String getLoginType() { return "DINGTALK"; }

        @Override
        protected String getStrategyName() { return "Token失败平台"; }

        @Override
        protected SocialTokenResponse exchangeToken(String code, String redirectUri) {
            SocialTokenResponse response = new SocialTokenResponse();
            // access_token 为空，模拟授权失败
            return response;
        }

        @Override
        protected SocialUserInfo getUserInfo(SocialTokenResponse tokenResponse) {
            return SocialUserInfo.builder().openId("open-id").source("test").build();
        }
    }

    /**
     * 获取用户信息失败的社交登录策略。
     */
    private static class UserInfoFailedStrategy extends AbstractSocialLoginStrategy {

        UserInfoFailedStrategy(AfgUserDetailsService userDetailsService, RestClient restClient) {
            super(userDetailsService, restClient);
        }

        @Override
        public String getLoginType() { return "FEISHU"; }

        @Override
        protected String getStrategyName() { return "用户信息失败平台"; }

        @Override
        protected SocialTokenResponse exchangeToken(String code, String redirectUri) {
            SocialTokenResponse response = new SocialTokenResponse();
            response.setAccessToken("valid-access-token");
            response.setOpenId("open-id-123");
            return response;
        }

        @Override
        protected SocialUserInfo getUserInfo(SocialTokenResponse tokenResponse) {
            // openId 为 null，模拟获取用户信息失败
            return SocialUserInfo.builder().source("test").build();
        }
    }

    private TestUserDetailsService userDetailsService;
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        userDetailsService = new TestUserDetailsService();
        restClient = RestClient.create();
    }

    @Nested
    @DisplayName("extractCode 操作")
    class ExtractCodeTests {

        @Test
        @DisplayName("extra 为 null 时应抛出 BusinessException")
        void shouldThrowWhenExtraIsNull() {
            SocialTokenResponse tokenResponse = new SocialTokenResponse();
            tokenResponse.setAccessToken("token");
            tokenResponse.setOpenId("open-id");
            SocialUserInfo userInfo = SocialUserInfo.builder().openId("open-id").source("test").build();

            LoginStrategy strategy = new SuccessfulSocialStrategy(
                    userDetailsService, restClient, tokenResponse, userInfo);

            LoginRequest request = new LoginRequest(
                    LoginRequest.LoginType.WECHAT, null, null, null, null,
                    null, null, null, null, null, null, null, null);

            assertThatThrownBy(() -> strategy.authenticate(request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("extra 中缺少 code 字段应抛出 BusinessException")
        void shouldThrowWhenCodeMissing() {
            SocialTokenResponse tokenResponse = new SocialTokenResponse();
            tokenResponse.setAccessToken("token");
            tokenResponse.setOpenId("open-id");
            SocialUserInfo userInfo = SocialUserInfo.builder().openId("open-id").source("test").build();

            LoginStrategy strategy = new SuccessfulSocialStrategy(
                    userDetailsService, restClient, tokenResponse, userInfo);

            // extra 中没有 code 字段
            LoginRequest request = new LoginRequest(
                    LoginRequest.LoginType.WECHAT, null, null, null, null,
                    null, null, null, null, null, null, null, "{\"redirectUri\":\"https://callback\"}");

            assertThatThrownBy(() -> strategy.authenticate(request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("authenticate 完整流程")
    class AuthenticateTests {

        @Test
        @DisplayName("完整的社交登录流程应成功")
        void shouldAuthenticateSuccessfully() {
            // 准备测试数据
            AfgUserDetails testUser = new TestUserDetails("user-1", "testuser", "tenant-1", Set.of("USER"));
            userDetailsService.addUser("wechat:open-id-123", testUser);

            SocialTokenResponse tokenResponse = new SocialTokenResponse();
            tokenResponse.setAccessToken("valid-access-token");
            tokenResponse.setOpenId("open-id-123");

            SocialUserInfo userInfo = SocialUserInfo.builder()
                    .openId("open-id-123")
                    .nickname("Test User")
                    .source("wechat")
                    .build();

            LoginStrategy strategy = new SuccessfulSocialStrategy(
                    userDetailsService, restClient, tokenResponse, userInfo);

            LoginRequest request = new LoginRequest(
                    LoginRequest.LoginType.WECHAT, null, null, null, null,
                    null, null, null, null, null, null, null,
                    "{\"code\":\"auth-code-123\",\"redirectUri\":\"https://callback\"}");

            AfgUserDetails result = strategy.authenticate(request);

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo("user-1");
            assertThat(result.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Token 交换失败应抛出 BusinessException")
        void shouldThrowWhenTokenExchangeFails() {
            LoginStrategy strategy = new TokenExchangeFailedStrategy(userDetailsService, restClient);

            LoginRequest request = new LoginRequest(
                    LoginRequest.LoginType.DINGTALK, null, null, null, null,
                    null, null, null, null, null, null, null,
                    "{\"code\":\"auth-code-123\"}");

            assertThatThrownBy(() -> strategy.authenticate(request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("获取用户信息失败时应从 tokenResponse 获取 openId")
        void shouldFallbackToTokenResponseOpenId() {
            // 用户信息中 openId 为 null，但 tokenResponse 中有
            AfgUserDetails testUser = new TestUserDetails("user-2", "feishu-user", null, Set.of("USER"));
            userDetailsService.addUser("test:open-id-from-token", testUser);

            SocialTokenResponse tokenResponse = new SocialTokenResponse();
            tokenResponse.setAccessToken("valid-access-token");
            tokenResponse.setOpenId("open-id-from-token");

            SocialUserInfo userInfo = SocialUserInfo.builder()
                    .source("test")
                    .build();  // openId 为 null

            // 自定义策略：在 getUserInfo 返回后设置 openId
            AbstractSocialLoginStrategy strategy = new AbstractSocialLoginStrategy(userDetailsService, restClient) {
                @Override
                public String getLoginType() { return "TEST"; }

                @Override
                protected String getStrategyName() { return "测试"; }

                @Override
                protected SocialTokenResponse exchangeToken(String code, String redirectUri) {
                    return tokenResponse;
                }

                @Override
                protected SocialUserInfo getUserInfo(SocialTokenResponse tr) {
                    // 返回无 openId 的 userInfo，触发 fallback
                    return userInfo;
                }
            };

            LoginRequest request = new LoginRequest(
                    LoginRequest.LoginType.THIRD_PARTY, null, null, null, null,
                    null, null, null, null, null, null, null,
                    "{\"code\":\"auth-code\"}");

            // tokenResponse 中有 openId，应被使用
            AfgUserDetails result = strategy.authenticate(request);
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo("user-2");
        }

        @Test
        @DisplayName("用户未绑定系统账号应抛出 BusinessException")
        void shouldThrowWhenUserNotBound() {
            SocialTokenResponse tokenResponse = new SocialTokenResponse();
            tokenResponse.setAccessToken("valid-access-token");
            tokenResponse.setOpenId("unbound-open-id");

            SocialUserInfo userInfo = SocialUserInfo.builder()
                    .openId("unbound-open-id")
                    .nickname("Unbound User")
                    .source("wechat")
                    .build();

            LoginStrategy strategy = new SuccessfulSocialStrategy(
                    userDetailsService, restClient, tokenResponse, userInfo);

            LoginRequest request = new LoginRequest(
                    LoginRequest.LoginType.WECHAT, null, null, null, null,
                    null, null, null, null, null, null, null,
                    "{\"code\":\"auth-code\"}");

            assertThatThrownBy(() -> strategy.authenticate(request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("getLoginType 操作")
    class GetLoginTypeTests {

        @Test
        @DisplayName("应返回子类定义的登录类型")
        void shouldReturnCorrectLoginType() {
            SocialTokenResponse tokenResponse = new SocialTokenResponse();
            tokenResponse.setAccessToken("token");
            SocialUserInfo userInfo = SocialUserInfo.builder().openId("id").source("test").build();

            LoginStrategy strategy = new SuccessfulSocialStrategy(
                    userDetailsService, restClient, tokenResponse, userInfo);

            assertThat(strategy.getLoginType()).isEqualTo("WECHAT");
        }
    }

    @Nested
    @DisplayName("supports 操作")
    class SupportsTests {

        @Test
        @DisplayName("应支持匹配的 LoginType")
        void shouldSupportMatchingLoginType() {
            SocialTokenResponse tokenResponse = new SocialTokenResponse();
            tokenResponse.setAccessToken("token");
            SocialUserInfo userInfo = SocialUserInfo.builder().openId("id").source("test").build();

            LoginStrategy strategy = new SuccessfulSocialStrategy(
                    userDetailsService, restClient, tokenResponse, userInfo);

            LoginRequest request = new LoginRequest(
                    LoginRequest.LoginType.WECHAT, null, null, null, null,
                    null, null, null, null, null, null, null, null);

            assertThat(strategy.supports(request)).isTrue();
        }

        @Test
        @DisplayName("不应支持不匹配的 LoginType")
        void shouldNotSupportNonMatchingLoginType() {
            SocialTokenResponse tokenResponse = new SocialTokenResponse();
            tokenResponse.setAccessToken("token");
            SocialUserInfo userInfo = SocialUserInfo.builder().openId("id").source("test").build();

            LoginStrategy strategy = new SuccessfulSocialStrategy(
                    userDetailsService, restClient, tokenResponse, userInfo);

            LoginRequest request = new LoginRequest(
                    LoginRequest.LoginType.DINGTALK, null, null, null, null,
                    null, null, null, null, null, null, null, null);

            assertThat(strategy.supports(request)).isFalse();
        }
    }

    @Nested
    @DisplayName("extractJsonField 操作")
    class ExtractJsonFieldTests {

        @Test
        @DisplayName("应正确提取 JSON 字段值")
        void shouldExtractFieldValue() {
            SocialTokenResponse tokenResponse = new SocialTokenResponse();
            tokenResponse.setAccessToken("token");
            tokenResponse.setOpenId("id");
            SocialUserInfo userInfo = SocialUserInfo.builder().openId("id").source("test").build();

            AbstractSocialLoginStrategy strategy = new SuccessfulSocialStrategy(
                    userDetailsService, restClient, tokenResponse, userInfo);

            String result = strategy.extractJsonField("{\"code\":\"abc123\",\"redirectUri\":\"https://callback\"}", "code");

            assertThat(result).isEqualTo("abc123");
        }

        @Test
        @DisplayName("字段不存在应返回 null")
        void shouldReturnNullForMissingField() {
            SocialTokenResponse tokenResponse = new SocialTokenResponse();
            tokenResponse.setAccessToken("token");
            tokenResponse.setOpenId("id");
            SocialUserInfo userInfo = SocialUserInfo.builder().openId("id").source("test").build();

            AbstractSocialLoginStrategy strategy = new SuccessfulSocialStrategy(
                    userDetailsService, restClient, tokenResponse, userInfo);

            String result = strategy.extractJsonField("{\"code\":\"abc123\"}", "nonexistent");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("空 JSON 应返回 null")
        void shouldReturnNullForEmptyJson() {
            SocialTokenResponse tokenResponse = new SocialTokenResponse();
            tokenResponse.setAccessToken("token");
            tokenResponse.setOpenId("id");
            SocialUserInfo userInfo = SocialUserInfo.builder().openId("id").source("test").build();

            AbstractSocialLoginStrategy strategy = new SuccessfulSocialStrategy(
                    userDetailsService, restClient, tokenResponse, userInfo);

            String result = strategy.extractJsonField("{}", "code");

            assertThat(result).isNull();
        }
    }
}
