package io.github.afgprojects.framework.core.model.version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ApiVersion 测试
 */
@DisplayName("ApiVersion 测试")
class ApiVersionTest {

    @Nested
    @DisplayName("parse 测试")
    class ParseTests {

        @Test
        @DisplayName("应该正确解析版本字符串")
        void shouldParseVersionString() {
            // when
            ApiVersion version = ApiVersion.parse("1.2.3");

            // then
            assertThat(version.major()).isEqualTo(1);
            assertThat(version.minor()).isEqualTo(2);
            assertThat(version.patch()).isEqualTo(3);
        }

        @Test
        @DisplayName("应该解析 0.0.0 版本")
        void shouldParseZeroVersion() {
            // when
            ApiVersion version = ApiVersion.parse("0.0.0");

            // then
            assertThat(version.major()).isZero();
            assertThat(version.minor()).isZero();
            assertThat(version.patch()).isZero();
        }

        @Test
        @DisplayName("应该解析大版本号")
        void shouldParseLargeVersionNumbers() {
            // when
            ApiVersion version = ApiVersion.parse("100.200.300");

            // then
            assertThat(version.major()).isEqualTo(100);
            assertThat(version.minor()).isEqualTo(200);
            assertThat(version.patch()).isEqualTo(300);
        }

        @Test
        @DisplayName("格式无效应该抛出异常")
        void shouldThrowOnInvalidFormat() {
            // when & then
            assertThatThrownBy(() -> ApiVersion.parse("1.2"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid version format");
        }

        @Test
        @DisplayName("部分无效应该抛出异常")
        void shouldThrowOnInvalidPart() {
            // when & then
            assertThatThrownBy(() -> ApiVersion.parse("1.a.3"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid version format");
        }

        @Test
        @DisplayName("空字符串应该抛出异常")
        void shouldThrowOnEmptyString() {
            // when & then
            assertThatThrownBy(() -> ApiVersion.parse("")).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("initial 测试")
    class InitialTests {

        @Test
        @DisplayName("应该创建初始版本")
        void shouldCreateInitialVersion() {
            // when
            ApiVersion version = ApiVersion.initial();

            // then
            assertThat(version.major()).isZero();
            assertThat(version.minor()).isZero();
            assertThat(version.patch()).isZero();
        }
    }

    @Nested
    @DisplayName("isNewerThan 测试")
    class IsNewerThanTests {

        @Test
        @DisplayName("主版本号更大应该更新")
        void shouldBeNewerWithHigherMajor() {
            // given
            ApiVersion v1 = new ApiVersion(2, 0, 0);
            ApiVersion v2 = new ApiVersion(1, 9, 9);

            // then
            assertThat(v1.isNewerThan(v2)).isTrue();
            assertThat(v2.isNewerThan(v1)).isFalse();
        }

        @Test
        @DisplayName("次版本号更大应该更新")
        void shouldBeNewerWithHigherMinor() {
            // given
            ApiVersion v1 = new ApiVersion(1, 2, 0);
            ApiVersion v2 = new ApiVersion(1, 1, 9);

            // then
            assertThat(v1.isNewerThan(v2)).isTrue();
        }

        @Test
        @DisplayName("补丁版本号更大应该更新")
        void shouldBeNewerWithHigherPatch() {
            // given
            ApiVersion v1 = new ApiVersion(1, 0, 2);
            ApiVersion v2 = new ApiVersion(1, 0, 1);

            // then
            assertThat(v1.isNewerThan(v2)).isTrue();
        }

        @Test
        @DisplayName("相同版本应该不是更新")
        void shouldNotBeNewerForSameVersion() {
            // given
            ApiVersion v1 = new ApiVersion(1, 2, 3);
            ApiVersion v2 = new ApiVersion(1, 2, 3);

            // then
            assertThat(v1.isNewerThan(v2)).isFalse();
        }
    }

    @Nested
    @DisplayName("isOlderThan 测试")
    class IsOlderThanTests {

        @Test
        @DisplayName("主版本号更小应该更旧")
        void shouldBeOlderWithLowerMajor() {
            // given
            ApiVersion v1 = new ApiVersion(1, 9, 9);
            ApiVersion v2 = new ApiVersion(2, 0, 0);

            // then
            assertThat(v1.isOlderThan(v2)).isTrue();
            assertThat(v2.isOlderThan(v1)).isFalse();
        }

        @Test
        @DisplayName("相同版本应该不是更旧")
        void shouldNotBeOlderForSameVersion() {
            // given
            ApiVersion v1 = new ApiVersion(1, 2, 3);
            ApiVersion v2 = new ApiVersion(1, 2, 3);

            // then
            assertThat(v1.isOlderThan(v2)).isFalse();
        }
    }

    @Nested
    @DisplayName("isCompatibleWith 测试")
    class IsCompatibleWithTests {

        @Test
        @DisplayName("相同主版本号应该兼容")
        void shouldBeCompatibleWithSameMajor() {
            // given
            ApiVersion v1 = new ApiVersion(1, 2, 3);
            ApiVersion v2 = new ApiVersion(1, 9, 9);

            // then
            assertThat(v1.isCompatibleWith(v2)).isTrue();
            assertThat(v2.isCompatibleWith(v1)).isTrue();
        }

        @Test
        @DisplayName("不同主版本号不应该兼容")
        void shouldNotBeCompatibleWithDifferentMajor() {
            // given
            ApiVersion v1 = new ApiVersion(1, 0, 0);
            ApiVersion v2 = new ApiVersion(2, 0, 0);

            // then
            assertThat(v1.isCompatibleWith(v2)).isFalse();
            assertThat(v2.isCompatibleWith(v1)).isFalse();
        }
    }

    @Nested
    @DisplayName("isMajorChange 测试")
    class IsMajorChangeTests {

        @Test
        @DisplayName("主版本号变更应该是主版本变更")
        void shouldBeMajorChangeWhenMajorDiffers() {
            // given
            ApiVersion v1 = new ApiVersion(1, 0, 0);
            ApiVersion v2 = new ApiVersion(2, 0, 0);

            // then
            assertThat(v1.isMajorChange(v2)).isTrue();
        }

        @Test
        @DisplayName("主版本号相同应该不是主版本变更")
        void shouldNotBeMajorChangeWhenMajorSame() {
            // given
            ApiVersion v1 = new ApiVersion(1, 2, 3);
            ApiVersion v2 = new ApiVersion(1, 9, 9);

            // then
            assertThat(v1.isMajorChange(v2)).isFalse();
        }
    }

    @Nested
    @DisplayName("isMinorChange 测试")
    class IsMinorChangeTests {

        @Test
        @DisplayName("次版本号变更应该是次版本变更")
        void shouldBeMinorChangeWhenMinorDiffers() {
            // given
            ApiVersion v1 = new ApiVersion(1, 2, 0);
            ApiVersion v2 = new ApiVersion(1, 3, 0);

            // then
            assertThat(v1.isMinorChange(v2)).isTrue();
        }

        @Test
        @DisplayName("主版本号不同应该不是次版本变更")
        void shouldNotBeMinorChangeWhenMajorDiffers() {
            // given
            ApiVersion v1 = new ApiVersion(1, 2, 0);
            ApiVersion v2 = new ApiVersion(2, 3, 0);

            // then
            assertThat(v1.isMinorChange(v2)).isFalse();
        }

        @Test
        @DisplayName("仅补丁变更应该不是次版本变更")
        void shouldNotBeMinorChangeWhenOnlyPatchDiffers() {
            // given
            ApiVersion v1 = new ApiVersion(1, 2, 3);
            ApiVersion v2 = new ApiVersion(1, 2, 4);

            // then
            assertThat(v1.isMinorChange(v2)).isFalse();
        }
    }

    @Nested
    @DisplayName("isPatchChange 测试")
    class IsPatchChangeTests {

        @Test
        @DisplayName("补丁版本号变更应该是补丁变更")
        void shouldBePatchChangeWhenPatchDiffers() {
            // given
            ApiVersion v1 = new ApiVersion(1, 2, 3);
            ApiVersion v2 = new ApiVersion(1, 2, 4);

            // then
            assertThat(v1.isPatchChange(v2)).isTrue();
        }

        @Test
        @DisplayName("主版本号不同应该不是补丁变更")
        void shouldNotBePatchChangeWhenMajorDiffers() {
            // given
            ApiVersion v1 = new ApiVersion(1, 2, 3);
            ApiVersion v2 = new ApiVersion(2, 2, 4);

            // then
            assertThat(v1.isPatchChange(v2)).isFalse();
        }

        @Test
        @DisplayName("次版本号不同应该不是补丁变更")
        void shouldNotBePatchChangeWhenMinorDiffers() {
            // given
            ApiVersion v1 = new ApiVersion(1, 2, 3);
            ApiVersion v2 = new ApiVersion(1, 3, 4);

            // then
            assertThat(v1.isPatchChange(v2)).isFalse();
        }
    }

    @Nested
    @DisplayName("compareTo 测试")
    class CompareToTests {

        @Test
        @DisplayName("应该正确比较版本顺序")
        void shouldCompareVersionsCorrectly() {
            // given
            ApiVersion v1 = new ApiVersion(1, 0, 0);
            ApiVersion v2 = new ApiVersion(1, 1, 0);
            ApiVersion v3 = new ApiVersion(1, 1, 1);
            ApiVersion v4 = new ApiVersion(2, 0, 0);

            // then
            assertThat(v1.compareTo(v2)).isNegative();
            assertThat(v2.compareTo(v1)).isPositive();
            assertThat(v2.compareTo(v3)).isNegative();
            assertThat(v3.compareTo(v4)).isNegative();
        }

        @Test
        @DisplayName("相同版本比较应该返回 0")
        void shouldReturnZeroForSameVersion() {
            // given
            ApiVersion v1 = new ApiVersion(1, 2, 3);
            ApiVersion v2 = new ApiVersion(1, 2, 3);

            // then
            assertThat(v1.compareTo(v2)).isZero();
        }
    }

    @Nested
    @DisplayName("toString 测试")
    class ToStringTests {

        @Test
        @DisplayName("应该返回正确的版本字符串")
        void shouldReturnCorrectVersionString() {
            // given
            ApiVersion version = new ApiVersion(1, 2, 3);

            // when
            String result = version.toString();

            // then
            assertThat(result).isEqualTo("1.2.3");
        }

        @Test
        @DisplayName("应该可以解析 toString 的结果")
        void shouldParseToStringResult() {
            // given
            ApiVersion original = new ApiVersion(2, 5, 10);

            // when
            ApiVersion parsed = ApiVersion.parse(original.toString());

            // then
            assertThat(parsed).isEqualTo(original);
        }
    }
}
