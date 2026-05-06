package io.github.afgprojects.framework.core.web.version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.HttpServletRequest;

/**
 * ApiVersionRequestCondition 测试
 */
@DisplayName("ApiVersionRequestCondition 测试")
@ExtendWith(MockitoExtension.class)
class ApiVersionRequestConditionTest {

    @Mock
    private HttpServletRequest request;

    private ApiVersionProperties properties;
    private ApiVersionInfo versionInfo;
    private ApiVersionRequestCondition condition;

    @BeforeEach
    void setUp() {
        properties = new ApiVersionProperties();
        versionInfo = ApiVersionInfo.of("1.0");
        condition = new ApiVersionRequestCondition(versionInfo, properties);
    }

    @Nested
    @DisplayName("构造方法测试")
    class ConstructorTests {

        @Test
        @DisplayName("应该正确初始化")
        void shouldInitializeCorrectly() {
            // then
            assertThat(condition.getVersionInfo()).isEqualTo(versionInfo);
        }
    }

    @Nested
    @DisplayName("combine 测试")
    class CombineTests {

        @Test
        @DisplayName("应该返回另一个条件（方法级优先）")
        void shouldReturnOtherCondition() {
            // given
            ApiVersionInfo otherVersionInfo = ApiVersionInfo.of("2.0");
            ApiVersionRequestCondition other = new ApiVersionRequestCondition(otherVersionInfo, properties);

            // when
            ApiVersionRequestCondition result = condition.combine(other);

            // then
            assertThat(result.getVersionInfo()).isEqualTo(otherVersionInfo);
        }
    }

    @Nested
    @DisplayName("getMatchingCondition 测试")
    class GetMatchingConditionTests {

        @Test
        @DisplayName("版本匹配应该返回自身")
        void shouldReturnSelfWhenVersionMatches() {
            // given - 请求版本 1.x，API 版本 1.0，主版本号相同应该匹配
            when(request.getHeader("X-API-Version")).thenReturn("1.5");

            // when
            ApiVersionRequestCondition result = condition.getMatchingCondition(request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getVersionInfo().value()).isEqualTo("1.0");
        }

        @Test
        @DisplayName("版本不匹配应该返回 null")
        void shouldReturnNullWhenVersionDoesNotMatch() {
            // given - 请求版本 2.x，API 版本 1.0，主版本号不同不应该匹配
            when(request.getHeader("X-API-Version")).thenReturn("2.0");

            // when
            ApiVersionRequestCondition result = condition.getMatchingCondition(request);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("无版本信息时应该使用默认版本")
        void shouldUseDefaultVersionWhenNoVersionInfo() {
            // given - 无版本头，使用默认版本 1.0.0
            // 需要模拟 request.getRequestURI() 用于 URL 解析策略
            when(request.getHeader("X-API-Version")).thenReturn(null);
            when(request.getRequestURI()).thenReturn("/api/users");
            when(request.getParameter("version")).thenReturn(null);

            // when
            ApiVersionRequestCondition result = condition.getMatchingCondition(request);

            // then - 默认版本 1.0.0 应该匹配 API 版本 1.0（主版本号相同）
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("compareTo 测试")
    class CompareToTests {

        @Test
        @DisplayName("应该优先匹配更高版本")
        void shouldPreferHigherVersion() {
            // given
            ApiVersionInfo otherVersionInfo = ApiVersionInfo.of("2.0");
            ApiVersionRequestCondition other = new ApiVersionRequestCondition(otherVersionInfo, properties);

            // when
            // 1.0 vs 2.0 -> 1.0 < 2.0 -> 返回负数
            int result = condition.compareTo(other, request);

            // then
            assertThat(result).isNegative(); // 1.0 < 2.0，返回负数
        }

        @Test
        @DisplayName("相同版本应该返回 0")
        void shouldReturnZeroForSameVersion() {
            // given
            ApiVersionRequestCondition other = new ApiVersionRequestCondition(versionInfo, properties);

            // when
            int result = condition.compareTo(other, request);

            // then
            assertThat(result).isZero();
        }
    }

    @Nested
    @DisplayName("equals 和 hashCode 测试")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("相同版本应该相等")
        void shouldBeEqualForSameVersion() {
            // given
            ApiVersionRequestCondition other = new ApiVersionRequestCondition(versionInfo, properties);

            // then
            assertThat(condition).isEqualTo(other);
            assertThat(condition.hashCode()).isEqualTo(other.hashCode());
        }

        @Test
        @DisplayName("不同版本不应该相等")
        void shouldNotBeEqualForDifferentVersion() {
            // given
            ApiVersionInfo otherVersionInfo = ApiVersionInfo.of("2.0");
            ApiVersionRequestCondition other = new ApiVersionRequestCondition(otherVersionInfo, properties);

            // then
            assertThat(condition).isNotEqualTo(other);
        }
    }

    @Nested
    @DisplayName("toString 测试")
    class ToStringTests {

        @Test
        @DisplayName("应该包含版本信息")
        void shouldIncludeVersionInfo() {
            // when
            String str = condition.toString();

            // then
            assertThat(str).contains("1.0");
        }
    }
}