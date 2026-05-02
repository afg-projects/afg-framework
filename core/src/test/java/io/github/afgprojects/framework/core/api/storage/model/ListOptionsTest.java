package io.github.afgprojects.framework.core.api.storage.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ListOptions 测试
 */
@DisplayName("ListOptions 测试")
class ListOptionsTest {

    @Nested
    @DisplayName("静态工厂方法测试")
    class StaticFactoryTests {

        @Test
        @DisplayName("应该创建默认选项")
        void shouldCreateDefaults() {
            ListOptions options = ListOptions.defaults();

            assertThat(options.prefix()).isNull();
            assertThat(options.delimiter()).isNull();
            assertThat(options.maxKeys()).isEqualTo(1000);
            assertThat(options.marker()).isNull();
        }

        @Test
        @DisplayName("应该创建带前缀的选项")
        void shouldCreateWithPrefix() {
            ListOptions options = ListOptions.withPrefix("images/");

            assertThat(options.prefix()).isEqualTo("images/");
            assertThat(options.delimiter()).isNull();
            assertThat(options.maxKeys()).isEqualTo(1000);
            assertThat(options.marker()).isNull();
        }

        @Test
        @DisplayName("应该创建带前缀和分隔符的选项")
        void shouldCreateWithPrefixAndDelimiter() {
            ListOptions options = ListOptions.withPrefixAndDelimiter("images/", "/");

            assertThat(options.prefix()).isEqualTo("images/");
            assertThat(options.delimiter()).isEqualTo("/");
            assertThat(options.maxKeys()).isEqualTo(1000);
            assertThat(options.marker()).isNull();
        }
    }

    @Nested
    @DisplayName("Builder 测试")
    class BuilderTests {

        @Test
        @DisplayName("应该使用 Builder 构建选项")
        void shouldBuildWithBuilder() {
            ListOptions options = ListOptions.builder()
                    .prefix("docs/")
                    .delimiter("/")
                    .maxKeys(500)
                    .marker("start-key")
                    .build();

            assertThat(options.prefix()).isEqualTo("docs/");
            assertThat(options.delimiter()).isEqualTo("/");
            assertThat(options.maxKeys()).isEqualTo(500);
            assertThat(options.marker()).isEqualTo("start-key");
        }

        @Test
        @DisplayName("应该支持部分设置")
        void shouldSupportPartialSettings() {
            ListOptions options = ListOptions.builder()
                    .maxKeys(100)
                    .build();

            assertThat(options.prefix()).isNull();
            assertThat(options.maxKeys()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("Record 特性测试")
    class RecordTests {

        @Test
        @DisplayName("应该正确实现 equals")
        void shouldImplementEquals() {
            ListOptions options1 = new ListOptions("test/", "/", 100, "marker");
            ListOptions options2 = new ListOptions("test/", "/", 100, "marker");

            assertThat(options1).isEqualTo(options2);
        }

        @Test
        @DisplayName("应该正确实现 hashCode")
        void shouldImplementHashCode() {
            ListOptions options1 = new ListOptions("test/", "/", 100, "marker");
            ListOptions options2 = new ListOptions("test/", "/", 100, "marker");

            assertThat(options1.hashCode()).isEqualTo(options2.hashCode());
        }

        @Test
        @DisplayName("应该生成正确的 toString")
        void shouldGenerateToString() {
            ListOptions options = new ListOptions("test/", "/", 100, "marker");

            assertThat(options.toString()).contains("test/");
            assertThat(options.toString()).contains("100");
        }
    }
}