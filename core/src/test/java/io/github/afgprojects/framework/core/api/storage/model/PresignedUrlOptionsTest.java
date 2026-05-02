package io.github.afgprojects.framework.core.api.storage.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * PresignedUrlOptions 测试
 */
@DisplayName("PresignedUrlOptions 测试")
class PresignedUrlOptionsTest {

    @Nested
    @DisplayName("静态工厂方法测试")
    class StaticFactoryTests {

        @Test
        @DisplayName("应该创建 GET 请求选项")
        void shouldCreateForGet() {
            PresignedUrlOptions options = PresignedUrlOptions.forGet();

            assertThat(options.method()).isEqualTo("GET");
            assertThat(options.contentType()).isNull();
            assertThat(options.expiration()).isAfter(Instant.now());
        }

        @Test
        @DisplayName("应该创建带自定义过期时间的 GET 请求选项")
        void shouldCreateForGetWithCustomExpiration() {
            Instant expiration = Instant.now().plusSeconds(7200);
            PresignedUrlOptions options = PresignedUrlOptions.forGet(expiration);

            assertThat(options.method()).isEqualTo("GET");
            assertThat(options.expiration()).isEqualTo(expiration);
            assertThat(options.contentType()).isNull();
        }

        @Test
        @DisplayName("应该创建 PUT 请求选项")
        void shouldCreateForPut() {
            PresignedUrlOptions options = PresignedUrlOptions.forPut("image/jpeg");

            assertThat(options.method()).isEqualTo("PUT");
            assertThat(options.contentType()).isEqualTo("image/jpeg");
            assertThat(options.expiration()).isAfter(Instant.now());
        }

        @Test
        @DisplayName("应该创建带自定义过期时间的 PUT 请求选项")
        void shouldCreateForPutWithCustomExpiration() {
            Instant expiration = Instant.now().plusSeconds(1800);
            PresignedUrlOptions options = PresignedUrlOptions.forPut(expiration, "application/pdf");

            assertThat(options.method()).isEqualTo("PUT");
            assertThat(options.expiration()).isEqualTo(expiration);
            assertThat(options.contentType()).isEqualTo("application/pdf");
        }
    }

    @Nested
    @DisplayName("Builder 测试")
    class BuilderTests {

        @Test
        @DisplayName("应该使用 Builder 构建选项")
        void shouldBuildWithBuilder() {
            Instant expiration = Instant.now().plusSeconds(3600);
            PresignedUrlOptions options = PresignedUrlOptions.builder()
                    .expiration(expiration)
                    .method("POST")
                    .contentType("application/json")
                    .build();

            assertThat(options.expiration()).isEqualTo(expiration);
            assertThat(options.method()).isEqualTo("POST");
            assertThat(options.contentType()).isEqualTo("application/json");
        }

        @Test
        @DisplayName("应该支持 expirationSeconds")
        void shouldSupportExpirationSeconds() {
            PresignedUrlOptions options = PresignedUrlOptions.builder()
                    .expirationSeconds(1800)
                    .build();

            assertThat(options.expiration()).isAfter(Instant.now().plusSeconds(1700));
            assertThat(options.expiration()).isBefore(Instant.now().plusSeconds(1900));
        }

        @Test
        @DisplayName("应该有默认值")
        void shouldHaveDefaultValues() {
            PresignedUrlOptions options = PresignedUrlOptions.builder().build();

            assertThat(options.method()).isEqualTo("GET");
            assertThat(options.expiration()).isAfter(Instant.now());
        }
    }

    @Nested
    @DisplayName("Record 特性测试")
    class RecordTests {

        @Test
        @DisplayName("应该正确实现 equals")
        void shouldImplementEquals() {
            Instant expiration = Instant.now().plusSeconds(3600);
            PresignedUrlOptions options1 = new PresignedUrlOptions(expiration, "GET", null);
            PresignedUrlOptions options2 = new PresignedUrlOptions(expiration, "GET", null);

            assertThat(options1).isEqualTo(options2);
        }

        @Test
        @DisplayName("应该正确实现 hashCode")
        void shouldImplementHashCode() {
            Instant expiration = Instant.now().plusSeconds(3600);
            PresignedUrlOptions options1 = new PresignedUrlOptions(expiration, "GET", null);
            PresignedUrlOptions options2 = new PresignedUrlOptions(expiration, "GET", null);

            assertThat(options1.hashCode()).isEqualTo(options2.hashCode());
        }
    }
}