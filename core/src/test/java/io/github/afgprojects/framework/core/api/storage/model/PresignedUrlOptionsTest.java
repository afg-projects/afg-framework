package io.github.afgprojects.framework.core.api.storage.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PresignedUrlOptions 测试
 */
@DisplayName("PresignedUrlOptions 测试")
class PresignedUrlOptionsTest {

    @Test
    @DisplayName("应该创建 GET 请求的默认选项")
    void shouldCreateDefaultForGet() {
        PresignedUrlOptions options = PresignedUrlOptions.forGet();

        assertEquals("GET", options.method());
        assertNull(options.contentType());
        assertNotNull(options.expiration());
    }

    @Test
    @DisplayName("应该创建带自定义过期时间的 GET 选项")
    void shouldCreateForGetWithCustomExpiration() {
        Instant expiration = Instant.now().plusSeconds(7200);
        PresignedUrlOptions options = PresignedUrlOptions.forGet(expiration);

        assertEquals("GET", options.method());
        assertEquals(expiration, options.expiration());
    }

    @Test
    @DisplayName("应该创建 PUT 请求的选项")
    void shouldCreateForPut() {
        PresignedUrlOptions options = PresignedUrlOptions.forPut("image/png");

        assertEquals("PUT", options.method());
        assertEquals("image/png", options.contentType());
    }

    @Test
    @DisplayName("应该创建带自定义过期时间的 PUT 选项")
    void shouldCreateForPutWithCustomExpiration() {
        Instant expiration = Instant.now().plusSeconds(1800);
        PresignedUrlOptions options = PresignedUrlOptions.forPut(expiration, "application/json");

        assertEquals("PUT", options.method());
        assertEquals("application/json", options.contentType());
        assertEquals(expiration, options.expiration());
    }

    @Test
    @DisplayName("应该使用 Builder 创建选项")
    void shouldCreateWithBuilder() {
        Instant expiration = Instant.now().plusSeconds(3600);
        PresignedUrlOptions options = PresignedUrlOptions.builder()
                .expiration(expiration)
                .method("POST")
                .contentType("text/plain")
                .build();

        assertEquals(expiration, options.expiration());
        assertEquals("POST", options.method());
        assertEquals("text/plain", options.contentType());
    }

    @Test
    @DisplayName("Builder 应该支持 expirationSeconds")
    void builderShouldSupportExpirationSeconds() {
        PresignedUrlOptions options = PresignedUrlOptions.builder()
                .expirationSeconds(1800)
                .method("GET")
                .build();

        assertNotNull(options.expiration());
        assertTrue(options.expiration().isAfter(Instant.now()));
    }
}
