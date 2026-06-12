package io.github.afgprojects.framework.security.core.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NoOpRefreshTokenStorage 测试
 */
@DisplayName("NoOpRefreshTokenStorage 测试")
class NoOpRefreshTokenStorageTest {

    private NoOpRefreshTokenStorage storage;

    @BeforeEach
    void setUp() {
        storage = new NoOpRefreshTokenStorage();
    }

    @Test
    @DisplayName("save 应不抛异常")
    void shouldNotThrowOnSave() {
        storage.save("token-id-1", "hash123", "user1", "tenant1", "client1", "device1", Instant.now().plusSeconds(3600));
    }

    @Test
    @DisplayName("findByTokenHash 应返回空")
    void shouldReturnEmptyOnFindByTokenHash() {
        assertThat(storage.findByTokenHash("hash123")).isEmpty();
    }

    @Test
    @DisplayName("findByTokenId 应返回空")
    void shouldReturnEmptyOnFindByTokenId() {
        assertThat(storage.findByTokenId("token-id-1")).isEmpty();
    }

    @Test
    @DisplayName("delete 应不抛异常")
    void shouldNotThrowOnDelete() {
        storage.delete("token-id-1");
    }

    @Test
    @DisplayName("deleteByUserId 应不抛异常")
    void shouldNotThrowOnDeleteByUserId() {
        storage.deleteByUserId("user1");
    }

    @Test
    @DisplayName("deleteExpired 应返回 0")
    void shouldReturnZeroOnDeleteExpired() {
        assertThat(storage.deleteExpired()).isZero();
    }
}
