package io.github.afgprojects.framework.security.impl.auth;

import io.github.afgprojects.framework.security.auth.oauth2.InMemoryOAuth2ClientService;
import io.github.afgprojects.framework.security.core.oauth2.model.ClientDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * InMemoryOAuth2ClientService 测试
 */
@DisplayName("InMemoryOAuth2ClientService 测试")
class InMemoryOAuth2ClientServiceTest {

    private InMemoryOAuth2ClientService clientService;

    private static final ClientDetails CONFIDENTIAL_CLIENT = new ClientDetails(
            "confidential-client",
            "secret-123",
            "Confidential Client",
            Set.of("http://localhost:8080/callback"),
            Set.of("read", "write"),
            Set.of("authorization_code", "refresh_token", "client_credentials"),
            false,
            Duration.ofHours(1),
            Duration.ofDays(7)
    );

    private static final ClientDetails PUBLIC_CLIENT = new ClientDetails(
            "public-client",
            null,
            "Public Client",
            Set.of("http://localhost:3000/callback"),
            Set.of("read"),
            Set.of("authorization_code", "refresh_token"),
            true,
            Duration.ofHours(1),
            Duration.ofDays(7)
    );

    @BeforeEach
    void setUp() {
        clientService = new InMemoryOAuth2ClientService();
    }

    @Nested
    @DisplayName("构造函数")
    class ConstructorTests {

        @Test
        @DisplayName("默认构造函数应创建空服务")
        void shouldCreateEmptyService() {
            InMemoryOAuth2ClientService service = new InMemoryOAuth2ClientService();

            assertThat(service.size()).isZero();
        }

        @Test
        @DisplayName("使用初始客户端列表构造应包含所有客户端")
        void shouldCreateServiceWithInitialClients() {
            InMemoryOAuth2ClientService service = new InMemoryOAuth2ClientService(
                    List.of(CONFIDENTIAL_CLIENT, PUBLIC_CLIENT)
            );

            assertThat(service.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("createTestInstance 应返回预配置的测试实例")
        void shouldCreateTestInstance() {
            InMemoryOAuth2ClientService service = InMemoryOAuth2ClientService.createTestInstance();

            assertThat(service.size()).isEqualTo(2);
            assertThat(service.exists("test-client")).isTrue();
            assertThat(service.exists("test-public-client")).isTrue();
        }
    }

    @Nested
    @DisplayName("saveClient 和 loadClientByClientId 方法")
    class SaveAndLoadTests {

        @Test
        @DisplayName("应能保存和加载客户端")
        void shouldSaveAndLoadClient() {
            clientService.saveClient(CONFIDENTIAL_CLIENT);

            ClientDetails loaded = clientService.loadClientByClientId("confidential-client");

            assertThat(loaded).isNotNull();
            assertThat(loaded.clientId()).isEqualTo("confidential-client");
            assertThat(loaded.clientName()).isEqualTo("Confidential Client");
        }

        @Test
        @DisplayName("加载不存在的客户端应返回 null")
        void shouldReturnNullForNonExistentClient() {
            ClientDetails loaded = clientService.loadClientByClientId("non-existent");

            assertThat(loaded).isNull();
        }

        @Test
        @DisplayName("保存相同 clientId 应覆盖")
        void shouldOverwriteWhenSavingSameClientId() {
            clientService.saveClient(CONFIDENTIAL_CLIENT);

            ClientDetails updated = new ClientDetails(
                    "confidential-client",
                    "new-secret",
                    "Updated Client",
                    Set.of("http://localhost:9090/callback"),
                    Set.of("read"),
                    Set.of("authorization_code"),
                    false,
                    Duration.ofHours(2),
                    Duration.ofDays(14)
            );
            clientService.saveClient(updated);

            ClientDetails loaded = clientService.loadClientByClientId("confidential-client");
            assertThat(loaded.clientName()).isEqualTo("Updated Client");
            assertThat(loaded.clientSecret()).isEqualTo("new-secret");
        }
    }

    @Nested
    @DisplayName("validateClientCredentials 方法")
    class ValidateCredentialsTests {

        @Test
        @DisplayName("机密客户端正确密钥应验证通过")
        void shouldValidateConfidentialClientWithCorrectSecret() {
            clientService.saveClient(CONFIDENTIAL_CLIENT);

            boolean valid = clientService.validateClientCredentials("confidential-client", "secret-123");

            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("机密客户端错误密钥应验证失败")
        void shouldFailValidationForWrongSecret() {
            clientService.saveClient(CONFIDENTIAL_CLIENT);

            boolean valid = clientService.validateClientCredentials("confidential-client", "wrong-secret");

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("机密客户端空密钥应验证失败")
        void shouldFailValidationForEmptySecret() {
            clientService.saveClient(CONFIDENTIAL_CLIENT);

            boolean valid = clientService.validateClientCredentials("confidential-client", "");

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("公共客户端不需要密钥验证")
        void shouldPassValidationForPublicClientWithoutSecret() {
            clientService.saveClient(PUBLIC_CLIENT);

            boolean valid = clientService.validateClientCredentials("public-client", null);

            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("不存在的客户端应验证失败")
        void shouldFailValidationForNonExistentClient() {
            boolean valid = clientService.validateClientCredentials("non-existent", "any-secret");

            assertThat(valid).isFalse();
        }
    }

    @Nested
    @DisplayName("requiresPkce 方法")
    class RequiresPkceTests {

        @Test
        @DisplayName("需要 PKCE 的客户端应返回 true")
        void shouldReturnTrueForPkceRequiredClient() {
            clientService.saveClient(PUBLIC_CLIENT);

            boolean requires = clientService.requiresPkce("public-client");

            assertThat(requires).isTrue();
        }

        @Test
        @DisplayName("不需要 PKCE 的客户端应返回 false")
        void shouldReturnFalseForNonPkceClient() {
            clientService.saveClient(CONFIDENTIAL_CLIENT);

            boolean requires = clientService.requiresPkce("confidential-client");

            assertThat(requires).isFalse();
        }

        @Test
        @DisplayName("不存在的客户端应返回 false")
        void shouldReturnFalseForNonExistentClient() {
            boolean requires = clientService.requiresPkce("non-existent");

            assertThat(requires).isFalse();
        }
    }

    @Nested
    @DisplayName("deleteClient 方法")
    class DeleteClientTests {

        @Test
        @DisplayName("应能删除已存在的客户端")
        void shouldDeleteExistingClient() {
            clientService.saveClient(CONFIDENTIAL_CLIENT);

            clientService.deleteClient("confidential-client");

            assertThat(clientService.exists("confidential-client")).isFalse();
            assertThat(clientService.size()).isZero();
        }

        @Test
        @DisplayName("删除不存在的客户端应无效果")
        void shouldDoNothingWhenDeletingNonExistentClient() {
            clientService.saveClient(CONFIDENTIAL_CLIENT);

            clientService.deleteClient("non-existent");

            assertThat(clientService.size()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("辅助方法")
    class HelperMethodTests {

        @Test
        @DisplayName("exists 应正确判断客户端是否存在")
        void shouldCheckExistence() {
            clientService.saveClient(CONFIDENTIAL_CLIENT);

            assertThat(clientService.exists("confidential-client")).isTrue();
            assertThat(clientService.exists("non-existent")).isFalse();
        }

        @Test
        @DisplayName("size 应返回正确的客户端数量")
        void shouldReturnCorrectSize() {
            assertThat(clientService.size()).isZero();

            clientService.saveClient(CONFIDENTIAL_CLIENT);
            assertThat(clientService.size()).isEqualTo(1);

            clientService.saveClient(PUBLIC_CLIENT);
            assertThat(clientService.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("clear 应清空所有客户端")
        void shouldClearAllClients() {
            clientService.saveClient(CONFIDENTIAL_CLIENT);
            clientService.saveClient(PUBLIC_CLIENT);

            clientService.clear();

            assertThat(clientService.size()).isZero();
        }
    }

    @Nested
    @DisplayName("ClientDetails 方法")
    class ClientDetailsTests {

        @Test
        @DisplayName("getClientType 应正确判断客户端类型")
        void shouldDetermineClientType() {
            assertThat(CONFIDENTIAL_CLIENT.getClientType()).isEqualTo(ClientDetails.ClientType.CONFIDENTIAL);
            assertThat(PUBLIC_CLIENT.getClientType()).isEqualTo(ClientDetails.ClientType.PUBLIC);
        }

        @Test
        @DisplayName("isRedirectUriAllowed 应正确判断重定向 URI")
        void shouldCheckRedirectUri() {
            assertThat(CONFIDENTIAL_CLIENT.isRedirectUriAllowed("http://localhost:8080/callback")).isTrue();
            assertThat(CONFIDENTIAL_CLIENT.isRedirectUriAllowed("http://evil.com/callback")).isFalse();
        }

        @Test
        @DisplayName("isGrantTypeAllowed 应正确判断授权类型")
        void shouldCheckGrantType() {
            assertThat(CONFIDENTIAL_CLIENT.isGrantTypeAllowed("authorization_code")).isTrue();
            assertThat(CONFIDENTIAL_CLIENT.isGrantTypeAllowed("implicit")).isFalse();
        }

        @Test
        @DisplayName("isScopeAllowed 应正确判断权限范围")
        void shouldCheckScope() {
            assertThat(CONFIDENTIAL_CLIENT.isScopeAllowed(Set.of("read"))).isTrue();
            assertThat(CONFIDENTIAL_CLIENT.isScopeAllowed(Set.of("read", "write"))).isTrue();
            assertThat(CONFIDENTIAL_CLIENT.isScopeAllowed(Set.of("read", "admin"))).isFalse();
        }
    }
}
