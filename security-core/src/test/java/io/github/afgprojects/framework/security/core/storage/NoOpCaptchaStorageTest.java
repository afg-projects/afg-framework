package io.github.afgprojects.framework.security.core.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NoOpCaptchaStorage 测试
 */
@DisplayName("NoOpCaptchaStorage 测试")
class NoOpCaptchaStorageTest {

    private NoOpCaptchaStorage storage;

    @BeforeEach
    void setUp() {
        storage = new NoOpCaptchaStorage();
    }

    @Test
    @DisplayName("save 应不抛异常")
    void shouldNotThrowOnSave() {
        storage.save("key1", "value1", Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("get 应返回 null")
    void shouldReturnNullOnGet() {
        storage.save("key1", "value1", Duration.ofMinutes(5));

        assertThat(storage.get("key1")).isNull();
    }

    @Test
    @DisplayName("delete 应不抛异常")
    void shouldNotThrowOnDelete() {
        storage.delete("key1");
    }

    @Test
    @DisplayName("exists 应返回 false")
    void shouldReturnFalseOnExists() {
        storage.save("key1", "value1", Duration.ofMinutes(5));

        assertThat(storage.exists("key1")).isFalse();
    }
}
