package io.github.afgprojects.framework.security.auth.oauth2;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.security.core.oauth2.model.ClientDetails;

/**
 * InMemoryOAuth2ClientService 测试。
 *
 * @author afg-projects
 * @since 1.0.0
 */
class InMemoryOAuth2ClientServiceTest {

    private InMemoryOAuth2ClientService clientService;

    @BeforeEach
    void setUp() {
        clientService = new InMemoryOAuth2ClientService();
    }

    @Nested
    @DisplayName("客户端管理测试")
    class ClientManagementTests {

        @Test
        @DisplayName("应保存客户端")
        void shouldSaveClient() {
            // Given
            ClientDetails client = createConfidentialClient();

            // When
            clientService.saveClient(client);

            // Then
            assertThat(clientService.exists("client-001")).isTrue();
            assertThat(clientService.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("应加载客户端")
        void shouldLoadClient() {
            // Given
            ClientDetails client = createConfidentialClient();
            clientService.saveClient(client);

            // When
            ClientDetails loaded = clientService.loadClientByClientId("client-001");

            // Then
            assertThat(loaded).isNotNull();
            assertThat(loaded.clientId()).isEqualTo("client-001");
            assertThat(loaded.clientName()).isEqualTo("Test Client");
        }

        @Test
        @DisplayName("不存在的客户端应返回 null")
        void shouldReturnNullForNonExistentClient() {
            // When
            ClientDetails loaded = clientService.loadClientByClientId("unknown");

            // Then
            assertThat(loaded).isNull();
        }

        @Test
        @DisplayName("应删除客户端")
        void shouldDeleteClient() {
            // Given
            ClientDetails client = createConfidentialClient();
            clientService.saveClient(client);

            // When
            clientService.deleteClient("client-001");

            // Then
            assertThat(clientService.exists("client-001")).isFalse();
        }

        @Test
        @DisplayName("应清空所有客户端")
        void shouldClearAllClients() {
            // Given
            clientService.saveClient(createConfidentialClient());
            clientService.saveClient(createPublicClient(false));

            // When
            clientService.clear();

            // Then
            assertThat(clientService.size()).isZero();
        }
    }

    @Nested
    @DisplayName("客户端凭证验证测试")
    class CredentialValidationTests {

        @Test
        @DisplayName("机密客户端正确凭证应验证通过")
        void shouldValidateCorrectCredentialsForConfidentialClient() {
            // Given
            ClientDetails client = createConfidentialClient();
            clientService.saveClient(client);

            // When
            boolean valid = clientService.validateClientCredentials("client-001", "secret-123");

            // Then
            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("机密客户端错误凭证应验证失败")
        void shouldFailValidationForWrongCredentials() {
            // Given
            ClientDetails client = createConfidentialClient();
            clientService.saveClient(client);

            // When
            boolean valid = clientService.validateClientCredentials("client-001", "wrong-secret");

            // Then
            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("机密客户端空密钥应验证失败")
        void shouldFailValidationForEmptySecret() {
            // Given
            ClientDetails client = createConfidentialClient();
            clientService.saveClient(client);

            // When
            boolean valid = clientService.validateClientCredentials("client-001", null);

            // Then
            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("公共客户端不需要密钥验证")
        void shouldNotRequireSecretForPublicClient() {
            // Given
            ClientDetails client = createPublicClient(false);
            clientService.saveClient(client);

            // When
            boolean valid = clientService.validateClientCredentials("client-002", null);

            // Then
            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("不存在的客户端应验证失败")
        void shouldFailValidationForNonExistentClient() {
            // When
            boolean valid = clientService.validateClientCredentials("unknown", "secret");

            // Then
            assertThat(valid).isFalse();
        }
    }

    @Nested
    @DisplayName("PKCE 需求检查测试")
    class PkceRequirementTests {

        @Test
        @DisplayName("需要 PKCE 的客户端应返回 true")
        void shouldReturnTrueWhenPkceRequired() {
            // Given
            ClientDetails client = createPublicClient(true);
            clientService.saveClient(client);

            // When
            boolean requires = clientService.requiresPkce("client-002");

            // Then
            assertThat(requires).isTrue();
        }

        @Test
        @DisplayName("不需要 PKCE 的客户端应返回 false")
        void shouldReturnFalseWhenPkceNotRequired() {
            // Given
            ClientDetails client = createConfidentialClient();
            clientService.saveClient(client);

            // When
            boolean requires = clientService.requiresPkce("client-001");

            // Then
            assertThat(requires).isFalse();
        }

        @Test
        @DisplayName("不存在的客户端应返回 false")
        void shouldReturnFalseForNonExistentClient() {
            // When
            boolean requires = clientService.requiresPkce("unknown");

            // Then
            assertThat(requires).isFalse();
        }
    }

    @Nested
    @DisplayName("测试实例工厂测试")
    class TestInstanceTests {

        @Test
        @DisplayName("应创建预配置的测试实例")
        void shouldCreatePreconfiguredTestInstance() {
            // When
            InMemoryOAuth2ClientService testService = InMemoryOAuth2ClientService.createTestInstance();

            // Then
            assertThat(testService.size()).isEqualTo(2);
            assertThat(testService.exists("test-client")).isTrue();
            assertThat(testService.exists("test-public-client")).isTrue();
        }
    }

    // ==================== 辅助方法 ====================

    private ClientDetails createConfidentialClient() {
        return new ClientDetails(
                "client-001",
                "secret-123",
                "Test Client",
                Set.of("https://example.com/callback"),
                Set.of("read", "write"),
                Set.of("authorization_code", "refresh_token", "client_credentials"),
                false,
                Duration.ofHours(1),
                Duration.ofDays(7));
    }

    private ClientDetails createPublicClient(boolean requirePkce) {
        return new ClientDetails(
                "client-002",
                null,
                "Public Client",
                Set.of("https://example.com/callback"),
                Set.of("read"),
                Set.of("authorization_code", "refresh_token"),
                requirePkce,
                Duration.ofHours(1),
                Duration.ofDays(7));
    }
}