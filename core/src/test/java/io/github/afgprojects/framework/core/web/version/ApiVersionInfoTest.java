package io.github.afgprojects.framework.core.web.version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ApiVersionInfo 测试
 */
@DisplayName("ApiVersionInfo 测试")
class ApiVersionInfoTest {

    @Nested
    @DisplayName("parseVersion 测试")
    class ParseVersionTests {

        @Test
        @DisplayName("应该正确解析 major.minor 格式")
        void shouldParseMajorMinorFormat() {
            // when
            int[] parts = ApiVersionInfo.parseVersion("1.0");

            // then
            assertThat(parts[0]).isEqualTo(1);
            assertThat(parts[1]).isEqualTo(0);
        }

        @Test
        @DisplayName("应该正确解析 major.minor.patch 格式")
        void shouldParseMajorMinorPatchFormat() {
            // when
            int[] parts = ApiVersionInfo.parseVersion("2.5.3");

            // then
            assertThat(parts[0]).isEqualTo(2);
            assertThat(parts[1]).isEqualTo(5);
        }

        @Test
        @DisplayName("应该解析零版本")
        void shouldParseZeroVersion() {
            // when
            int[] parts = ApiVersionInfo.parseVersion("0.0");

            // then
            assertThat(parts[0]).isZero();
            assertThat(parts[1]).isZero();
        }

        @Test
        @DisplayName("格式无效应该抛出异常")
        void shouldThrowOnInvalidFormat() {
            // when & then
            assertThatThrownBy(() -> ApiVersionInfo.parseVersion("1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid version format");
        }

        @Test
        @DisplayName("部分无效应该抛出异常")
        void shouldThrowOnInvalidPart() {
            // when & then
            assertThatThrownBy(() -> ApiVersionInfo.parseVersion("a.b"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("from 测试")
    class FromTests {

        @Test
        @DisplayName("应该从注解创建版本信息")
        void shouldCreateFromAnnotation() {
            // given
            ApiVersion annotation = createApiVersion("1.5", false, "1.0", "2.0", "use v2", "deprecated");

            // when
            ApiVersionInfo info = ApiVersionInfo.from(annotation);

            // then
            assertThat(info.value()).isEqualTo("1.5");
            assertThat(info.major()).isEqualTo(1);
            assertThat(info.minor()).isEqualTo(5);
            assertThat(info.deprecated()).isFalse();
            assertThat(info.since()).isEqualTo("1.0");
            assertThat(info.until()).isEqualTo("2.0");
            assertThat(info.replacement()).isEqualTo("use v2");
            assertThat(info.reason()).isEqualTo("deprecated");
        }

        @Test
        @DisplayName("应该处理空字符串属性")
        void shouldHandleEmptyStringProperties() {
            // given
            ApiVersion annotation = createApiVersion("2.0", false, "", "", "", "");

            // when
            ApiVersionInfo info = ApiVersionInfo.from(annotation);

            // then
            assertThat(info.since()).isNull();
            assertThat(info.until()).isNull();
            assertThat(info.replacement()).isNull();
            assertThat(info.reason()).isNull();
        }
    }

    @Nested
    @DisplayName("of 测试")
    class OfTests {

        @Test
        @DisplayName("应该创建简单版本信息")
        void shouldCreateSimpleVersionInfo() {
            // when
            ApiVersionInfo info = ApiVersionInfo.of("3.2");

            // then
            assertThat(info.value()).isEqualTo("3.2");
            assertThat(info.major()).isEqualTo(3);
            assertThat(info.minor()).isEqualTo(2);
            assertThat(info.deprecated()).isFalse();
            assertThat(info.since()).isNull();
            assertThat(info.until()).isNull();
        }
    }

    @Nested
    @DisplayName("isCompatibleWith 测试")
    class IsCompatibleWithTests {

        @Test
        @DisplayName("相同主版本号应该兼容")
        void shouldBeCompatibleWithSameMajor() {
            // given
            ApiVersionInfo info = ApiVersionInfo.of("1.5");

            // then
            assertThat(info.isCompatibleWith(1)).isTrue();
        }

        @Test
        @DisplayName("不同主版本号不应该兼容")
        void shouldNotBeCompatibleWithDifferentMajor() {
            // given
            ApiVersionInfo info = ApiVersionInfo.of("1.5");

            // then
            assertThat(info.isCompatibleWith(2)).isFalse();
            assertThat(info.isCompatibleWith(0)).isFalse();
        }
    }

    @Nested
    @DisplayName("isInRange 测试")
    class IsInRangeTests {

        @Test
        @DisplayName("无限制应该返回 true")
        void shouldReturnTrueWhenNoLimits() {
            // given
            ApiVersionInfo info = ApiVersionInfo.of("1.0");

            // then
            assertThat(info.isInRange(1)).isTrue();
            assertThat(info.isInRange(2)).isTrue();
            assertThat(info.isInRange(0)).isTrue();
        }

        @Test
        @DisplayName("since 限制应该生效")
        void shouldApplySinceLimit() {
            // given
            ApiVersionInfo info = new ApiVersionInfo("1.0", 1, 0, false, "2.0", null, null, null);

            // then
            assertThat(info.isInRange(2)).isTrue();
            assertThat(info.isInRange(1)).isFalse();
        }

        @Test
        @DisplayName("until 限制应该生效")
        void shouldApplyUntilLimit() {
            // given
            ApiVersionInfo info = new ApiVersionInfo("1.0", 1, 0, false, null, "2.0", null, null);

            // then
            assertThat(info.isInRange(1)).isTrue();
            assertThat(info.isInRange(2)).isFalse();
        }

        @Test
        @DisplayName("since 和 until 同时限制")
        void shouldApplyBothLimits() {
            // given
            ApiVersionInfo info = new ApiVersionInfo("1.5", 1, 5, false, "1.0", "2.0", null, null);

            // then
            assertThat(info.isInRange(1)).isTrue();
            assertThat(info.isInRange(0)).isFalse(); // 小于 since
            assertThat(info.isInRange(2)).isFalse(); // 大于等于 until
        }
    }

    @Nested
    @DisplayName("compareTo 测试")
    class CompareToTests {

        @Test
        @DisplayName("应该正确比较版本顺序")
        void shouldCompareVersionsCorrectly() {
            // given
            ApiVersionInfo v1 = ApiVersionInfo.of("1.0");
            ApiVersionInfo v2 = ApiVersionInfo.of("1.5");
            ApiVersionInfo v3 = ApiVersionInfo.of("2.0");

            // then
            assertThat(v1.compareTo(v2)).isNegative();
            assertThat(v2.compareTo(v1)).isPositive();
            assertThat(v1.compareTo(v3)).isNegative();
            assertThat(v3.compareTo(v1)).isPositive();
        }

        @Test
        @DisplayName("相同版本比较应该返回 0")
        void shouldReturnZeroForSameVersion() {
            // given
            ApiVersionInfo v1 = ApiVersionInfo.of("1.5");
            ApiVersionInfo v2 = ApiVersionInfo.of("1.5");

            // then
            assertThat(v1.compareTo(v2)).isZero();
        }
    }

    @Nested
    @DisplayName("isNewerThan 测试")
    class IsNewerThanTests {

        @Test
        @DisplayName("主版本号更大应该更新")
        void shouldBeNewerWithHigherMajor() {
            // given
            ApiVersionInfo info = ApiVersionInfo.of("2.0");

            // then
            assertThat(info.isNewerThan(1, 9)).isTrue();
            assertThat(info.isNewerThan(2, 1)).isFalse();
        }

        @Test
        @DisplayName("次版本号更大应该更新")
        void shouldBeNewerWithHigherMinor() {
            // given
            ApiVersionInfo info = ApiVersionInfo.of("1.5");

            // then
            assertThat(info.isNewerThan(1, 4)).isTrue();
            assertThat(info.isNewerThan(1, 5)).isFalse();
            assertThat(info.isNewerThan(1, 6)).isFalse();
        }
    }

    @Nested
    @DisplayName("buildDeprecationWarning 测试")
    class BuildDeprecationWarningTests {

        @Test
        @DisplayName("非废弃版本应该返回 null")
        void shouldReturnNullForNonDeprecated() {
            // given
            ApiVersionInfo info = ApiVersionInfo.of("1.0");

            // then
            assertThat(info.buildDeprecationWarning()).isNull();
        }

        @Test
        @DisplayName("废弃版本应该返回警告信息")
        void shouldReturnWarningForDeprecated() {
            // given
            ApiVersionInfo info = new ApiVersionInfo("1.0", 1, 0, true, null, null, null, null);

            // when
            String warning = info.buildDeprecationWarning();

            // then
            assertThat(warning).contains("1.0").contains("deprecated");
        }

        @Test
        @DisplayName("应该包含移除版本信息")
        void shouldIncludeRemovalVersion() {
            // given
            ApiVersionInfo info = new ApiVersionInfo("1.0", 1, 0, true, null, "2.0", null, null);

            // when
            String warning = info.buildDeprecationWarning();

            // then
            assertThat(warning).contains("removed in version 2.0");
        }

        @Test
        @DisplayName("应该包含替代方案")
        void shouldIncludeReplacement() {
            // given
            ApiVersionInfo info = new ApiVersionInfo("1.0", 1, 0, true, null, null, "use v2 API", null);

            // when
            String warning = info.buildDeprecationWarning();

            // then
            assertThat(warning).contains("Use use v2 API instead");
        }

        @Test
        @DisplayName("应该包含废弃原因")
        void shouldIncludeReason() {
            // given
            ApiVersionInfo info = new ApiVersionInfo("1.0", 1, 0, true, null, null, null, "security issue");

            // when
            String warning = info.buildDeprecationWarning();

            // then
            assertThat(warning).contains("Reason: security issue");
        }

        @Test
        @DisplayName("应该包含完整信息")
        void shouldIncludeAllInfo() {
            // given
            ApiVersionInfo info = new ApiVersionInfo("1.0", 1, 0, true, null, "2.0", "use v2", "performance");

            // when
            String warning = info.buildDeprecationWarning();

            // then
            assertThat(warning)
                    .contains("1.0")
                    .contains("deprecated")
                    .contains("2.0")
                    .contains("use v2")
                    .contains("performance");
        }
    }

    /**
     * 创建 ApiVersion 注解实例（使用匿名类模拟）
     */
    private ApiVersion createApiVersion(
            String value, boolean deprecated, String since, String until, String replacement, String reason) {
        return new ApiVersion() {
            @Override
            public String value() {
                return value;
            }

            @Override
            public boolean deprecated() {
                return deprecated;
            }

            @Override
            public String since() {
                return since;
            }

            @Override
            public String until() {
                return until;
            }

            @Override
            public String replacement() {
                return replacement;
            }

            @Override
            public String reason() {
                return reason;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return ApiVersion.class;
            }
        };
    }
}