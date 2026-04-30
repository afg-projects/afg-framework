package io.github.afgprojects.framework.core.api.storage.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StorageType 测试
 */
@DisplayName("StorageType 测试")
class StorageTypeTest {

    @Test
    @DisplayName("应该获取正确的 code")
    void shouldGetCorrectCode() {
        assertEquals("local", StorageType.LOCAL.getCode());
        assertEquals("minio", StorageType.MINIO.getCode());
        assertEquals("oss", StorageType.OSS.getCode());
        assertEquals("s3", StorageType.S3.getCode());
    }

    @Test
    @DisplayName("应该根据 code 获取 StorageType")
    void shouldGetStorageTypeFromCode() {
        assertEquals(StorageType.LOCAL, StorageType.fromCode("local"));
        assertEquals(StorageType.MINIO, StorageType.fromCode("minio"));
        assertEquals(StorageType.OSS, StorageType.fromCode("oss"));
        assertEquals(StorageType.S3, StorageType.fromCode("s3"));
    }

    @Test
    @DisplayName("应该忽略大小写获取 StorageType")
    void shouldIgnoreCaseWhenGettingFromCode() {
        assertEquals(StorageType.LOCAL, StorageType.fromCode("LOCAL"));
        assertEquals(StorageType.MINIO, StorageType.fromCode("MinIO"));
        assertEquals(StorageType.OSS, StorageType.fromCode("OSS"));
        assertEquals(StorageType.S3, StorageType.fromCode("S3"));
    }

    @Test
    @DisplayName("无效 code 应该返回 null")
    void shouldReturnNullForInvalidCode() {
        assertNull(StorageType.fromCode("invalid"));
        assertNull(StorageType.fromCode(""));
        assertNull(StorageType.fromCode(null));
    }

    @Test
    @DisplayName("应该包含所有存储类型")
    void shouldContainAllTypes() {
        StorageType[] types = StorageType.values();
        assertEquals(4, types.length);
    }
}
