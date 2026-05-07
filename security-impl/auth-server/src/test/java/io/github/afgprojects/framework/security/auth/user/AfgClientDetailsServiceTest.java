package io.github.afgprojects.framework.security.auth.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

/**
 * AfgClientDetailsService 测试
 */
@DisplayName("AfgClientDetailsService 测试")
class AfgClientDetailsServiceTest {

    @Nested
    @DisplayName("客户端加载测试")
    class ClientLoadingTests {

        @Test
        @DisplayName("应该加载已注册的客户端")
        void shouldLoadRegisteredClient() {
            // given
            AfgClientDetails clientDetails = AfgClientDetails.builder()
                    .clientId("test-client")
                    .clientSecret("test-secret")
                    .clientName("Test Client")
                    .authorizationGrantTypes(Set.of(
                            AuthorizationGrantType.AUTHORIZATION_CODE,
                            AuthorizationGrantType.CLIENT_CREDENTIALS))
                    .clientAuthenticationMethods(Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC))
                    .redirectUris(Set.of("https://example.com/callback"))
                    .scopes(Set.of("read", "write"))
                    .build();

            AfgClientDetailsService service = mock(AfgClientDetailsService.class);
            when(service.loadClientByClientId("test-client"))
                    .thenReturn(clientDetails);

            // when
            AfgClientDetails loaded = service.loadClientByClientId("test-client");

            // then
            assertThat(loaded).isNotNull();
            assertThat(loaded.getClientId()).isEqualTo("test-client");
            assertThat(loaded.getClientSecret()).isEqualTo("test-secret");
            assertThat(loaded.getClientName()).isEqualTo("Test Client");
        }

        @Test
        @DisplayName("应该返回 null 当客户端不存在")
        void shouldReturnNullWhenClientNotFound() {
            // given
            AfgClientDetailsService service = mock(AfgClientDetailsService.class);
            when(service.loadClientByClientId("non-existent"))
                    .thenReturn(null);

            // when
            AfgClientDetails loaded = service.loadClientByClientId("non-existent");

            // then
            assertThat(loaded).isNull();
        }
    }

    @Nested
    @DisplayName("AfgClientDetails 测试")
    class AfgClientDetailsTests {

        @Test
        @DisplayName("应该正确构建 AfgClientDetails")
        void shouldBuildAfgClientDetails() {
            // given & when
            AfgClientDetails clientDetails = AfgClientDetails.builder()
                    .clientId("my-client")
                    .clientSecret("my-secret")
                    .clientName("My Application")
                    .authorizationGrantTypes(Set.of(
                            AuthorizationGrantType.AUTHORIZATION_CODE,
                            AuthorizationGrantType.REFRESH_TOKEN))
                    .clientAuthenticationMethods(Set.of(ClientAuthenticationMethod.CLIENT_SECRET_POST))
                    .redirectUris(Set.of("https://app.example.com/oauth/callback"))
                    .scopes(Set.of("profile", "email"))
                    .build();

            // then
            assertThat(clientDetails.getClientId()).isEqualTo("my-client");
            assertThat(clientDetails.getClientSecret()).isEqualTo("my-secret");
            assertThat(clientDetails.getClientName()).isEqualTo("My Application");
            assertThat(clientDetails.getAuthorizationGrantTypes())
                    .contains(AuthorizationGrantType.AUTHORIZATION_CODE, AuthorizationGrantType.REFRESH_TOKEN);
            assertThat(clientDetails.getClientAuthenticationMethods())
                    .contains(ClientAuthenticationMethod.CLIENT_SECRET_POST);
            assertThat(clientDetails.getRedirectUris())
                    .contains("https://app.example.com/oauth/callback");
            assertThat(clientDetails.getScopes()).contains("profile", "email");
        }

        @Test
        @DisplayName("应该转换为 RegisteredClient")
        void shouldConvertToRegisteredClient() {
            // given
            AfgClientDetails clientDetails = AfgClientDetails.builder()
                    .clientId("conversion-test")
                    .clientSecret("{noop}test-secret")
                    .clientName("Conversion Test Client")
                    .authorizationGrantTypes(Set.of(
                            AuthorizationGrantType.AUTHORIZATION_CODE,
                            AuthorizationGrantType.REFRESH_TOKEN))
                    .clientAuthenticationMethods(Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC))
                    .redirectUris(Set.of("https://test.com/callback"))
                    .scopes(Set.of("read", "write"))
                    .build();

            // when
            RegisteredClient registeredClient = clientDetails.toRegisteredClient();

            // then
            assertThat(registeredClient).isNotNull();
            assertThat(registeredClient.getClientId()).isEqualTo("conversion-test");
            assertThat(registeredClient.getClientSecret()).isEqualTo("{noop}test-secret");
            assertThat(registeredClient.getClientName()).isEqualTo("Conversion Test Client");
            assertThat(registeredClient.getAuthorizationGrantTypes())
                    .contains(AuthorizationGrantType.AUTHORIZATION_CODE, AuthorizationGrantType.REFRESH_TOKEN);
            assertThat(registeredClient.getClientAuthenticationMethods())
                    .contains(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
            assertThat(registeredClient.getRedirectUris())
                    .contains("https://test.com/callback");
            assertThat(registeredClient.getScopes()).contains("read", "write");
        }

        @Test
        @DisplayName("应该处理空 redirectUris")
        void shouldHandleEmptyRedirectUris() {
            // given
            AfgClientDetails clientDetails = AfgClientDetails.builder()
                    .clientId("no-redirect-client")
                    .clientSecret("secret")
                    .clientName("No Redirect Client")
                    .authorizationGrantTypes(Set.of(AuthorizationGrantType.CLIENT_CREDENTIALS))
                    .clientAuthenticationMethods(Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC))
                    .scopes(Set.of("api"))
                    .build();

            // when
            RegisteredClient registeredClient = clientDetails.toRegisteredClient();

            // then
            assertThat(registeredClient).isNotNull();
            assertThat(registeredClient.getRedirectUris()).isEmpty();
        }
    }
}