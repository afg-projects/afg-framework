package io.github.afgprojects.framework.security.core.storage;

import io.github.afgprojects.framework.security.core.security.model.DeviceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NoOpDeviceStorage 测试
 */
@DisplayName("NoOpDeviceStorage 测试")
class NoOpDeviceStorageTest {

    private NoOpDeviceStorage storage;

    @BeforeEach
    void setUp() {
        storage = new NoOpDeviceStorage();
    }

    @Test
    @DisplayName("save 应不抛异常")
    void shouldNotThrowOnSave() {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setDeviceId("device1");
        deviceInfo.setUserId("user1");

        storage.save(deviceInfo);
    }

    @Test
    @DisplayName("findById 应返回空")
    void shouldReturnEmptyOnFindById() {
        assertThat(storage.findById("device1")).isEmpty();
    }

    @Test
    @DisplayName("findByUserId 应返回空列表")
    void shouldReturnEmptyListOnFindByUserId() {
        assertThat(storage.findByUserId("user1")).isEmpty();
    }

    @Test
    @DisplayName("countActiveByUserId 应返回 0")
    void shouldReturnZeroOnCountActive() {
        assertThat(storage.countActiveByUserId("user1")).isZero();
    }

    @Test
    @DisplayName("delete 应不抛异常")
    void shouldNotThrowOnDelete() {
        storage.delete("device1");
    }

    @Test
    @DisplayName("deleteByUserId 应不抛异常")
    void shouldNotThrowOnDeleteByUserId() {
        storage.deleteByUserId("user1");
    }

    @Test
    @DisplayName("updateActiveStatus 应不抛异常")
    void shouldNotThrowOnUpdateActiveStatus() {
        storage.updateActiveStatus("device1", true);
    }
}
