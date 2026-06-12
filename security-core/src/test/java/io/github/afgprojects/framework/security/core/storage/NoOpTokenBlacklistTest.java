package io.github.afgprojects.framework.security.core.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NoOpTokenBlacklist 测试
 */
@DisplayName("NoOpTokenBlacklist 测试")
class NoOpTokenBlacklistTest {

    private NoOpTokenBlacklist blacklist;

    @BeforeEach
    void setUp() {
        blacklist = new NoOpTokenBlacklist();
    }

    @Test
    @DisplayName("addToBlacklist 应不抛异常")
    void shouldNotThrowOnAddToBlacklist() {
        blacklist.addToBlacklist("hash123", "user1", "logout", Duration.ofHours(2));
    }

    @Test
    @DisplayName("isBlacklisted 应返回 false")
    void shouldReturnFalseOnIsBlacklisted() {
        blacklist.addToBlacklist("hash123", "user1", "logout", Duration.ofHours(2));

        assertThat(blacklist.isBlacklisted("hash123")).isFalse();
    }

    @Test
    @DisplayName("blacklistAllUserTokens 应不抛异常")
    void shouldNotThrowOnBlacklistAllUserTokens() {
        blacklist.blacklistAllUserTokens("user1", Duration.ofHours(2));
    }
}
