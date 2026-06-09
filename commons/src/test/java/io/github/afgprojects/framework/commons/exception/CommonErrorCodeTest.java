package io.github.afgprojects.framework.commons.exception;

import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CommonErrorCode 测试
 */
@DisplayName("CommonErrorCode 测试")
class CommonErrorCodeTest {

    @Nested
    @DisplayName("通用错误码 (10001-10099)")
    class GeneralErrorTests {

        @Test
        @DisplayName("FAIL 应有正确的 code、message、category")
        void shouldHaveCorrectFailProperties() {
            assertThat(CommonErrorCode.FAIL.getCode()).isEqualTo(10001);
            assertThat(CommonErrorCode.FAIL.getMessage()).isEqualTo("操作失败");
            assertThat(CommonErrorCode.FAIL.getCategory()).isEqualTo(ErrorCategory.BUSINESS);
        }

        @Test
        @DisplayName("PARAM_ERROR 应有正确的 code、message、category")
        void shouldHaveCorrectParamErrorProperties() {
            assertThat(CommonErrorCode.PARAM_ERROR.getCode()).isEqualTo(10002);
            assertThat(CommonErrorCode.PARAM_ERROR.getMessage()).isEqualTo("参数错误");
            assertThat(CommonErrorCode.PARAM_ERROR.getCategory()).isEqualTo(ErrorCategory.BUSINESS);
        }

        @Test
        @DisplayName("PARAM_MISSING 应有正确的 code")
        void shouldHaveCorrectParamMissingCode() {
            assertThat(CommonErrorCode.PARAM_MISSING.getCode()).isEqualTo(10003);
        }

        @Test
        @DisplayName("PARAM_FORMAT_ERROR 应有正确的 code")
        void shouldHaveCorrectParamFormatErrorCode() {
            assertThat(CommonErrorCode.PARAM_FORMAT_ERROR.getCode()).isEqualTo(10004);
        }
    }

    @Nested
    @DisplayName("认证授权错误码 (10400-10499)")
    class AuthErrorTests {

        @Test
        @DisplayName("UNAUTHORIZED 应属于 SECURITY 分类")
        void shouldBeSecurityCategory() {
            assertThat(CommonErrorCode.UNAUTHORIZED.getCode()).isEqualTo(10400);
            assertThat(CommonErrorCode.UNAUTHORIZED.getCategory()).isEqualTo(ErrorCategory.SECURITY);
        }

        @Test
        @DisplayName("TOKEN_EXPIRED 应属于 SECURITY 分类")
        void tokenExpiredShouldBeSecurityCategory() {
            assertThat(CommonErrorCode.TOKEN_EXPIRED.getCode()).isEqualTo(10401);
            assertThat(CommonErrorCode.TOKEN_EXPIRED.getCategory()).isEqualTo(ErrorCategory.SECURITY);
        }

        @Test
        @DisplayName("TOKEN_INVALID 应属于 SECURITY 分类")
        void tokenInvalidShouldBeSecurityCategory() {
            assertThat(CommonErrorCode.TOKEN_INVALID.getCode()).isEqualTo(10402);
            assertThat(CommonErrorCode.TOKEN_INVALID.getCategory()).isEqualTo(ErrorCategory.SECURITY);
        }

        @Test
        @DisplayName("FORBIDDEN 应属于 SECURITY 分类")
        void forbiddenShouldBeSecurityCategory() {
            assertThat(CommonErrorCode.FORBIDDEN.getCode()).isEqualTo(10403);
            assertThat(CommonErrorCode.FORBIDDEN.getCategory()).isEqualTo(ErrorCategory.SECURITY);
        }

        @Test
        @DisplayName("PERMISSION_DENIED 应属于 SECURITY 分类")
        void permissionDeniedShouldBeSecurityCategory() {
            assertThat(CommonErrorCode.PERMISSION_DENIED.getCode()).isEqualTo(10404);
            assertThat(CommonErrorCode.PERMISSION_DENIED.getCategory()).isEqualTo(ErrorCategory.SECURITY);
        }

        @Test
        @DisplayName("ACCOUNT_DISABLED 应属于 SECURITY 分类")
        void accountDisabledShouldBeSecurityCategory() {
            assertThat(CommonErrorCode.ACCOUNT_DISABLED.getCode()).isEqualTo(10405);
            assertThat(CommonErrorCode.ACCOUNT_DISABLED.getCategory()).isEqualTo(ErrorCategory.SECURITY);
        }

        @Test
        @DisplayName("ACCOUNT_LOCKED 应属于 SECURITY 分类")
        void accountLockedShouldBeSecurityCategory() {
            assertThat(CommonErrorCode.ACCOUNT_LOCKED.getCode()).isEqualTo(10406);
            assertThat(CommonErrorCode.ACCOUNT_LOCKED.getCategory()).isEqualTo(ErrorCategory.SECURITY);
        }
    }

    @Nested
    @DisplayName("系统错误码 (19000-19999)")
    class SystemErrorTests {

        @Test
        @DisplayName("SYSTEM_ERROR 应属于 SYSTEM 分类")
        void shouldBeSystemCategory() {
            assertThat(CommonErrorCode.SYSTEM_ERROR.getCode()).isEqualTo(19000);
            assertThat(CommonErrorCode.SYSTEM_ERROR.getCategory()).isEqualTo(ErrorCategory.SYSTEM);
        }

        @Test
        @DisplayName("INTERNAL_ERROR 应属于 SYSTEM 分类")
        void internalErrorShouldBeSystemCategory() {
            assertThat(CommonErrorCode.INTERNAL_ERROR.getCode()).isEqualTo(19001);
            assertThat(CommonErrorCode.INTERNAL_ERROR.getCategory()).isEqualTo(ErrorCategory.SYSTEM);
        }

        @Test
        @DisplayName("SERVICE_UNAVAILABLE 应属于 SYSTEM 分类")
        void serviceUnavailableShouldBeSystemCategory() {
            assertThat(CommonErrorCode.SERVICE_UNAVAILABLE.getCode()).isEqualTo(19002);
            assertThat(CommonErrorCode.SERVICE_UNAVAILABLE.getCategory()).isEqualTo(ErrorCategory.SYSTEM);
        }
    }

    @Nested
    @DisplayName("网络错误码 (14000-14999)")
    class NetworkErrorTests {

        @Test
        @DisplayName("CLIENT_REQUEST_FAILED 应属于 NETWORK 分类")
        void shouldBeNetworkCategory() {
            assertThat(CommonErrorCode.CLIENT_REQUEST_FAILED.getCode()).isEqualTo(14000);
            assertThat(CommonErrorCode.CLIENT_REQUEST_FAILED.getCategory()).isEqualTo(ErrorCategory.NETWORK);
        }

        @Test
        @DisplayName("CLIENT_TIMEOUT 应属于 NETWORK 分类")
        void clientTimeoutShouldBeNetworkCategory() {
            assertThat(CommonErrorCode.CLIENT_TIMEOUT.getCode()).isEqualTo(14001);
            assertThat(CommonErrorCode.CLIENT_TIMEOUT.getCategory()).isEqualTo(ErrorCategory.NETWORK);
        }

        @Test
        @DisplayName("REQUEST_TIMEOUT 应属于 NETWORK 分类")
        void requestTimeoutShouldBeNetworkCategory() {
            assertThat(CommonErrorCode.REQUEST_TIMEOUT.getCode()).isEqualTo(10202);
            assertThat(CommonErrorCode.REQUEST_TIMEOUT.getCategory()).isEqualTo(ErrorCategory.NETWORK);
        }
    }

    @Nested
    @DisplayName("数据层错误码 (11000-11999)")
    class DataErrorTests {

        @Test
        @DisplayName("ENTITY_NOT_FOUND 应有正确的属性")
        void shouldHaveCorrectEntityNotFoundProperties() {
            assertThat(CommonErrorCode.ENTITY_NOT_FOUND.getCode()).isEqualTo(11000);
            assertThat(CommonErrorCode.ENTITY_NOT_FOUND.getMessage()).isEqualTo("实体不存在");
            assertThat(CommonErrorCode.ENTITY_NOT_FOUND.getCategory()).isEqualTo(ErrorCategory.BUSINESS);
        }

        @Test
        @DisplayName("OPTIMISTIC_LOCK_ERROR 应属于 BUSINESS 分类")
        void optimisticLockShouldBeBusinessCategory() {
            assertThat(CommonErrorCode.OPTIMISTIC_LOCK_ERROR.getCode()).isEqualTo(11007);
            assertThat(CommonErrorCode.OPTIMISTIC_LOCK_ERROR.getCategory()).isEqualTo(ErrorCategory.BUSINESS);
        }
    }

    @Nested
    @DisplayName("ErrorCode 接口方法")
    class ErrorCodeInterfaceTests {

        @Test
        @DisplayName("formatCode() 应返回 E 前缀的错误码字符串")
        void shouldReturnFormattedCode() {
            assertThat(CommonErrorCode.FAIL.formatCode()).isEqualTo("E10001");
            assertThat(CommonErrorCode.UNAUTHORIZED.formatCode()).isEqualTo("E10400");
        }

        @Test
        @DisplayName("getMessage(Locale) 默认返回 getMessage()")
        void shouldReturnDefaultMessageForLocale() {
            assertThat(CommonErrorCode.FAIL.getMessage(Locale.CHINA)).isEqualTo("操作失败");
            assertThat(CommonErrorCode.FAIL.getMessage((Locale) null)).isEqualTo("操作失败");
        }

        @Test
        @DisplayName("getMessage(Object[], Locale) 默认返回 getMessage()")
        void shouldReturnDefaultMessageForArgsAndLocale() {
            assertThat(CommonErrorCode.NOT_FOUND.getMessage(new Object[]{"test"}, null)).isEqualTo("资源不存在");
        }
    }

    @Nested
    @DisplayName("枚举完整性")
    class EnumCompletenessTests {

        @Test
        @DisplayName("所有错误码应在 10000-19999 范围内")
        void allCodesShouldBeInRange() {
            for (CommonErrorCode errorCode : CommonErrorCode.values()) {
                assertThat(errorCode.getCode())
                        .as("Error code for %s should be in range 10000-19999", errorCode.name())
                        .isBetween(10000, 19999);
            }
        }

        @Test
        @DisplayName("所有错误码的 category 不应为 null")
        void allCategoriesShouldNotBeNull() {
            for (CommonErrorCode errorCode : CommonErrorCode.values()) {
                assertThat(errorCode.getCategory())
                        .as("Category for %s should not be null", errorCode.name())
                        .isNotNull();
            }
        }

        @Test
        @DisplayName("所有错误码的 message 不应为 null")
        void allMessagesShouldNotBeNull() {
            for (CommonErrorCode errorCode : CommonErrorCode.values()) {
                assertThat(errorCode.getMessage())
                        .as("Message for %s should not be null", errorCode.name())
                        .isNotNull()
                        .isNotEmpty();
            }
        }
    }
}
