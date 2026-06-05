package io.github.afgprojects.framework.security.core.oauth2.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ClientDetails 测试
 */
@DisplayName("ClientDetails 测试")
class ClientDetailsTest {

    private ClientDetails createConfidentialClient() {
        return new ClientDetails(
                "my-client",
                "my-secret",
                "My Application",
                Set.of("https://app.example.com/callback"),
                Set.of("read", "write"),
                Set.of("authorization_code", "refresh_token"),
                true,
                Duration.ofHours(2),
                Duration.ofDays(7)
        );
    }

    private ClientDetails createPublicClient() {
        return new ClientDetails(
                "spa-client",
                null,
                "SPA Application",
                Set.of("https://spa.example.com/callback"),
                Set.of("read"),
                Set.of("authorization_code"),
                true,
                Duration.ofHours(1),
                Duration.ofDays(1)
        );
    }

    private ClientDetails createEmptySecretClient() {
        return new ClientDetails(
                "empty-secret-client",
                "",
                "Empty Secret Client",
                Set.of("https://app.example.com/callback"),
                Set.of("read"),
                Set.of("client_credentials"),
                false,
                Duration.ofHours(1),
                Duration.ofDays(1)
        );
    }

    @Nested
    @DisplayName("getClientType")
    class GetClientTypeTests {

        @Test
        @DisplayName("有密钥的客户端应为 CONFIDENTIAL")
        void shouldReturnConfidentialWhenSecretExists() {
            ClientDetails client = createConfidentialClient();

            assertThat(client.getClientType()).isEqualTo(ClientDetails.ClientType.CONFIDENTIAL);
        }

        @Test
        @DisplayName("无密钥的客户端应为 PUBLIC")
        void shouldReturnPublicWhenSecretIsNull() {
            ClientDetails client = createPublicClient();

            assertThat(client.getClientType()).isEqualTo(ClientDetails.ClientType.PUBLIC);
        }

        @Test
        @DisplayName("空密钥的客户端应为 PUBLIC")
        void shouldReturnPublicWhenSecretIsEmpty() {
            ClientDetails client = createEmptySecretClient();

            assertThat(client.getClientType()).isEqualTo(ClientDetails.ClientType.PUBLIC);
        }
    }

    @Nested
    @DisplayName("isRedirectUriAllowed")
    class IsRedirectUriAllowedTests {

        @Test
        @DisplayName("允许的重定向 URI 应返回 true")
        void shouldReturnTrueForAllowedRedirectUri() {
            ClientDetails client = createConfidentialClient();

            assertThat(client.isRedirectUriAllowed("https://app.example.com/callback")).isTrue();
        }

        @Test
        @DisplayName("不允许的重定向 URI 应返回 false")
        void shouldReturnFalseForDisallowedRedirectUri() {
            ClientDetails client = createConfidentialClient();

            assertThat(client.isRedirectUriAllowed("https://evil.example.com/callback")).isFalse();
        }
    }

    @Nested
    @DisplayName("isGrantTypeAllowed")
    class IsGrantTypeAllowedTests {

        @Test
        @DisplayName("允许的授权类型应返回 true")
        void shouldReturnTrueForAllowedGrantType() {
            ClientDetails client = createConfidentialClient();

            assertThat(client.isGrantTypeAllowed("authorization_code")).isTrue();
            assertThat(client.isGrantTypeAllowed("refresh_token")).isTrue();
        }

        @Test
        @DisplayName("不允许的授权类型应返回 false")
        void shouldReturnFalseForDisallowedGrantType() {
            ClientDetails client = createConfidentialClient();

            assertThat(client.isGrantTypeAllowed("client_credentials")).isFalse();
        }
    }

    @Nested
    @DisplayName("isScopeAllowed")
    class IsScopeAllowedTests {

        @Test
        @DisplayName("请求范围全部允许时应返回 true")
        void shouldReturnTrueWhenAllScopesAllowed() {
            ClientDetails client = createConfidentialClient();

            assertThat(client.isScopeAllowed(Set.of("read"))).isTrue();
            assertThat(client.isScopeAllowed(Set.of("read", "write"))).isTrue();
        }

        @Test
        @DisplayName("请求范围部分不允许时应返回 false")
        void shouldReturnFalseWhenSomeScopesNotAllowed() {
            ClientDetails client = createConfidentialClient();

            assertThat(client.isScopeAllowed(Set.of("read", "write", "admin"))).isFalse();
        }

        @Test
        @DisplayName("空范围请求应返回 true")
        void shouldReturnTrueForEmptyScopeRequest() {
            ClientDetails client = createConfidentialClient();

            assertThat(client.isScopeAllowed(Set.of())).isTrue();
        }
    }

    @Nested
    @DisplayName("record 特性")
    class RecordTests {

        @Test
        @DisplayName("应正确实现 equals 和 hashCode")
        void shouldImplementEqualsAndHashCode() {
            ClientDetails client1 = createConfidentialClient();
            ClientDetails client2 = createConfidentialClient();

            assertThat(client1).isEqualTo(client2);
            assertThat(client1).hasSameHashCodeAs(client2);
        }

        @Test
        @DisplayName("应正确实现 toString")
        void shouldImplementToString() {
            ClientDetails client = createConfidentialClient();

            String str = client.toString();

            assertThat(str).contains("ClientDetails");
            assertThat(str).contains("my-client");
            assertThat(str).contains("My Application");
        }
    }
}
