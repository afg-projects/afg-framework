package io.github.afgprojects.framework.core.model.exception;

import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.commons.exception.ErrorCategory;
import io.github.afgprojects.framework.commons.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorCode")
class ErrorCodeTest {

    @Nested
    @DisplayName("interface default methods")
    class InterfaceDefaultMethods {

        @Test
        @DisplayName("should format code as E{number}")
        void shouldFormatCodeAsENumber() {
            assertThat(CommonErrorCode.FAIL.formatCode()).isEqualTo("E10001");
            assertThat(CommonErrorCode.UNAUTHORIZED.formatCode()).isEqualTo("E10400");
        }

        @Test
        @DisplayName("should return default category as BUSINESS")
        void shouldReturnDefaultCategoryAsBusiness() {
            // Custom implementation without overriding getCategory
            ErrorCode custom = new ErrorCode() {
                @Override
                public int getCode() {
                    return 99999;
                }

                @Override
                public String getMessage() {
                    return "Custom error";
                }
            };

            assertThat(custom.getCategory()).isEqualTo(ErrorCategory.BUSINESS);
        }

        @Test
        @DisplayName("should return getMessage when locale is null")
        void shouldReturnGetMessageWhenLocaleIsNull() {
            assertThat(CommonErrorCode.FAIL.getMessage(null)).isEqualTo("操作失败");
        }

        @Test
        @DisplayName("should return getMessage when locale is provided but no i18n")
        void shouldReturnGetMessageWhenLocaleProvidedButNoI18n() {
            assertThat(CommonErrorCode.FAIL.getMessage(java.util.Locale.CHINA)).isEqualTo("操作失败");
        }

        @Test
        @DisplayName("should return getMessage when args and locale are provided but no i18n")
        void shouldReturnGetMessageWhenArgsAndLocaleProvidedButNoI18n() {
            assertThat(CommonErrorCode.FAIL.getMessage(new Object[]{"arg1"}, null)).isEqualTo("操作失败");
        }
    }

    @Nested
    @DisplayName("CommonErrorCode enum")
    class CommonErrorCodeEnum {

        @Test
        @DisplayName("should implement ErrorCode interface")
        void shouldImplementErrorCodeInterface() {
            assertThat(CommonErrorCode.FAIL).isInstanceOf(ErrorCode.class);
        }

        @Test
        @DisplayName("should have correct code and message")
        void shouldHaveCorrectCodeAndMessage() {
            assertThat(CommonErrorCode.FAIL.getCode()).isEqualTo(10001);
            assertThat(CommonErrorCode.FAIL.getMessage()).isEqualTo("操作失败");
        }

        @Test
        @DisplayName("should have correct category")
        void shouldHaveCorrectCategory() {
            assertThat(CommonErrorCode.FAIL.getCategory()).isEqualTo(ErrorCategory.BUSINESS);
            assertThat(CommonErrorCode.UNAUTHORIZED.getCategory()).isEqualTo(ErrorCategory.SECURITY);
            assertThat(CommonErrorCode.SYSTEM_ERROR.getCategory()).isEqualTo(ErrorCategory.SYSTEM);
            assertThat(CommonErrorCode.REQUEST_TIMEOUT.getCategory()).isEqualTo(ErrorCategory.NETWORK);
        }

        @Test
        @DisplayName("should have unique codes")
        void shouldHaveUniqueCodes() {
            long uniqueCount = java.util.Arrays.stream(CommonErrorCode.values())
                    .map(ErrorCode::getCode)
                    .distinct()
                    .count();

            assertThat(uniqueCount).isEqualTo(CommonErrorCode.values().length);
        }
    }

    @Nested
    @DisplayName("ErrorCategory")
    class ErrorCategoryTest {

        @Test
        @DisplayName("should have BUSINESS prefix B")
        void shouldHaveBusinessPrefixB() {
            assertThat(ErrorCategory.BUSINESS.getPrefix()).isEqualTo("B");
        }

        @Test
        @DisplayName("should have SYSTEM prefix S")
        void shouldHaveSystemPrefixS() {
            assertThat(ErrorCategory.SYSTEM.getPrefix()).isEqualTo("S");
        }

        @Test
        @DisplayName("should have NETWORK prefix N")
        void shouldHaveNetworkPrefixN() {
            assertThat(ErrorCategory.NETWORK.getPrefix()).isEqualTo("N");
        }

        @Test
        @DisplayName("should have SECURITY prefix A")
        void shouldHaveSecurityPrefixA() {
            assertThat(ErrorCategory.SECURITY.getPrefix()).isEqualTo("A");
        }
    }
}