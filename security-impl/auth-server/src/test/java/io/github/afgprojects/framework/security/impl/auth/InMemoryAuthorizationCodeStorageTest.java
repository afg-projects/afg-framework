package io.github.afgprojects.framework.security.impl.auth;

import io.github.afgprojects.framework.security.auth.oauth2.InMemoryAuthorizationCodeStorage;
import io.github.afgprojects.framework.security.core.oauth2.model.AuthorizationCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * InMemoryAuthorizationCodeStorage 测试
 */
@DisplayName("InMemoryAuthorizationCodeStorage 测试")
class InMemoryAuthorizationCodeStorageTest {

    private InMemoryAuthorizationCodeStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryAuthorizationCodeStorage();
    }

    private AuthorizationCode createAuthCode(String code, Instant expiresAt) {
        return new AuthorizationCode(
                code,
                "client-001",
                "user-001",
                "http://localhost:8080/callback",
                Set.of("read", "write"),
                null,
                null,
                expiresAt,
                Instant.now()
        );
    }

    private AuthorizationCode createAuthCodeWithPkce(String code, Instant expiresAt) {
        return new AuthorizationCode(
                code,
                "client-001",
                "user-001",
                "http://localhost:8080/callback",
                Set.of("read"),
                "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk",
                "S256",
                expiresAt,
                Instant.now()
        );
    }

    @Nested
    @DisplayName("save 和 findByCode 方法")
    class SaveAndFindTests {

        @Test
        @DisplayName("应能保存和查找授权码")
        void shouldSaveAndFindAuthCode() {
            Instant expiresAt = Instant.now().plusSeconds(300);
            AuthorizationCode authCode = createAuthCode("code-001", expiresAt);

            storage.save(authCode);

            AuthorizationCode found = storage.findByCode("code-001");
            assertThat(found).isNotNull();
            assertThat(found.code()).isEqualTo("code-001");
            assertThat(found.clientId()).isEqualTo("client-001");
            assertThat(found.userId()).isEqualTo("user-001");
        }

        @Test
        @DisplayName("查找不存在的授权码应返回 null")
        void shouldReturnNullForNonExistentCode() {
            AuthorizationCode found = storage.findByCode("non-existent");

            assertThat(found).isNull();
        }

        @Test
        @DisplayName("过期的授权码应返回 null 并被自动删除")
        void shouldReturnNullForExpiredCode() {
            Instant expiredAt = Instant.now().minusSeconds(60);
            AuthorizationCode authCode = createAuthCode("expired-code", expiredAt);

            storage.save(authCode);

            AuthorizationCode found = storage.findByCode("expired-code");
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("应能保存多个授权码")
        void shouldSaveMultipleAuthCodes() {
            Instant expiresAt = Instant.now().plusSeconds(300);
            storage.save(createAuthCode("code-001", expiresAt));
            storage.save(createAuthCode("code-002", expiresAt));

            assertThat(storage.size()).isEqualTo(2);
            assertThat(storage.findByCode("code-001")).isNotNull();
            assertThat(storage.findByCode("code-002")).isNotNull();
        }
    }

    @Nested
    @DisplayName("delete 方法")
    class DeleteTests {

        @Test
        @DisplayName("应能删除已存在的授权码")
        void shouldDeleteExistingCode() {
            Instant expiresAt = Instant.now().plusSeconds(300);
            storage.save(createAuthCode("code-001", expiresAt));

            storage.delete("code-001");

            assertThat(storage.findByCode("code-001")).isNull();
            assertThat(storage.size()).isZero();
        }

        @Test
        @DisplayName("删除不存在的授权码应无效果")
        void shouldDoNothingWhenDeletingNonExistentCode() {
            storage.delete("non-existent");

            assertThat(storage.size()).isZero();
        }
    }

    @Nested
    @DisplayName("deleteByUserId 方法")
    class DeleteByUserIdTests {

        @Test
        @DisplayName("应能删除指定用户的所有授权码")
        void shouldDeleteAllCodesForUser() {
            Instant expiresAt = Instant.now().plusSeconds(300);
            AuthorizationCode code1 = createAuthCode("code-001", expiresAt);
            AuthorizationCode code2 = createAuthCode("code-002", expiresAt);
            AuthorizationCode code3 = new AuthorizationCode(
                    "code-003", "client-002", "user-002",
                    "http://localhost:9090/callback", Set.of("read"),
                    null, null, expiresAt, Instant.now()
            );
            storage.save(code1);
            storage.save(code2);
            storage.save(code3);

            storage.deleteByUserId("user-001");

            assertThat(storage.findByCode("code-001")).isNull();
            assertThat(storage.findByCode("code-002")).isNull();
            assertThat(storage.findByCode("code-003")).isNotNull();
        }

        @Test
        @DisplayName("删除不存在的用户应无效果")
        void shouldDoNothingForNonExistentUser() {
            Instant expiresAt = Instant.now().plusSeconds(300);
            storage.save(createAuthCode("code-001", expiresAt));

            storage.deleteByUserId("non-existent-user");

            assertThat(storage.size()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("deleteExpired 方法")
    class DeleteExpiredTests {

        @Test
        @DisplayName("应能清理过期的授权码")
        void shouldDeleteExpiredCodes() {
            Instant expiredAt = Instant.now().minusSeconds(60);
            Instant validAt = Instant.now().plusSeconds(300);
            storage.save(createAuthCode("expired-1", expiredAt));
            storage.save(createAuthCode("expired-2", expiredAt));
            storage.save(createAuthCode("valid-1", validAt));

            int count = storage.deleteExpired();

            assertThat(count).isEqualTo(2);
            assertThat(storage.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("没有过期授权码时应返回 0")
        void shouldReturnZeroWhenNoExpiredCodes() {
            Instant validAt = Instant.now().plusSeconds(300);
            storage.save(createAuthCode("valid-1", validAt));

            int count = storage.deleteExpired();

            assertThat(count).isZero();
        }

        @Test
        @DisplayName("空存储应返回 0")
        void shouldReturnZeroWhenEmpty() {
            int count = storage.deleteExpired();

            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("辅助方法")
    class HelperMethodTests {

        @Test
        @DisplayName("size 应返回正确的授权码数量")
        void shouldReturnCorrectSize() {
            Instant expiresAt = Instant.now().plusSeconds(300);
            assertThat(storage.size()).isZero();

            storage.save(createAuthCode("code-001", expiresAt));
            assertThat(storage.size()).isEqualTo(1);

            storage.save(createAuthCode("code-002", expiresAt));
            assertThat(storage.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("clear 应清空所有授权码")
        void shouldClearAllCodes() {
            Instant expiresAt = Instant.now().plusSeconds(300);
            storage.save(createAuthCode("code-001", expiresAt));
            storage.save(createAuthCode("code-002", expiresAt));

            storage.clear();

            assertThat(storage.size()).isZero();
        }
    }

    @Nested
    @DisplayName("AuthorizationCode 模型方法")
    class AuthorizationCodeModelTests {

        @Test
        @DisplayName("isPkce 应正确判断是否使用 PKCE")
        void shouldDeterminePkce() {
            Instant expiresAt = Instant.now().plusSeconds(300);
            AuthorizationCode withoutPkce = createAuthCode("code-1", expiresAt);
            AuthorizationCode withPkce = createAuthCodeWithPkce("code-2", expiresAt);

            assertThat(withoutPkce.isPkce()).isFalse();
            assertThat(withPkce.isPkce()).isTrue();
        }

        @Test
        @DisplayName("isExpired 应正确判断是否过期")
        void shouldDetermineExpired() {
            AuthorizationCode expired = createAuthCode("code-1", Instant.now().minusSeconds(1));
            AuthorizationCode valid = createAuthCode("code-2", Instant.now().plusSeconds(300));

            assertThat(expired.isExpired()).isTrue();
            assertThat(valid.isExpired()).isFalse();
        }

        @Test
        @DisplayName("isValid 应与 isExpired 相反")
        void shouldDetermineValid() {
            AuthorizationCode expired = createAuthCode("code-1", Instant.now().minusSeconds(1));
            AuthorizationCode valid = createAuthCode("code-2", Instant.now().plusSeconds(300));

            assertThat(expired.isValid()).isFalse();
            assertThat(valid.isValid()).isTrue();
        }
    }
}
