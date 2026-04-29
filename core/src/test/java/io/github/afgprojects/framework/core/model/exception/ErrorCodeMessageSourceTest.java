package io.github.afgprojects.framework.core.model.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

class ErrorCodeMessageSourceTest {

    private MessageSource messageSource;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource bundle = new ResourceBundleMessageSource();
        bundle.setBasename("messages");
        bundle.setDefaultEncoding("UTF-8");
        bundle.setFallbackToSystemLocale(false);
        messageSource = bundle;
        ErrorCodeMessageSource.setMessageSource(messageSource);
    }

    @Nested
    @DisplayName("getMessage 测试")
    class GetMessageTests {

        @Test
        @DisplayName("应返回中文消息")
        void shouldReturnChineseMessage_forLocaleChina() {
            String message = CommonErrorCode.PARAM_ERROR.getMessage(Locale.CHINA);

            assertThat(message).isEqualTo("参数错误");
        }

        @Test
        @DisplayName("应返回英文消息")
        void shouldReturnEnglishMessage_forLocaleEnglish() {
            String message = CommonErrorCode.PARAM_ERROR.getMessage(Locale.ENGLISH);

            assertThat(message).isEqualTo("Invalid parameter");
        }

        @Test
        @DisplayName("locale 为空时应返回默认消息")
        void shouldReturnDefaultMessage_forNullLocale() {
            String message = CommonErrorCode.PARAM_ERROR.getMessage((Locale) null);

            // 应回退到默认消息（中文）
            assertThat(message).isNotEmpty();
        }

        @Test
        @DisplayName("MessageSource 未初始化时应返回默认消息")
        void shouldReturnDefaultMessage_whenMessageSourceNotInitialized() {
            ErrorCodeMessageSource.setMessageSource(null);

            String message = CommonErrorCode.PARAM_ERROR.getMessage(Locale.ENGLISH);

            // 应返回枚举中的默认消息
            assertThat(message).isEqualTo("参数错误");
        }
    }

    @Nested
    @DisplayName("带参数的 getMessage 测试")
    class GetMessageWithArgsTests {

        @Test
        @DisplayName("应返回带参数的消息")
        void shouldReturnMessageWithArgs() {
            // 测试参数化消息
            String message = CommonErrorCode.NOT_FOUND.getMessage(new Object[]{"User"}, Locale.CHINA);

            // 由于消息中没有占位符，应返回基础消息
            assertThat(message).isEqualTo("资源不存在");
        }
    }

    @Nested
    @DisplayName("所有错误码测试")
    class AllErrorCodesTests {

        @Test
        @DisplayName("所有 CommonErrorCode 值应有中文消息")
        void allCommonErrorCodeValues_shouldHaveChineseMessages() {
            for (CommonErrorCode errorCode : CommonErrorCode.values()) {
                String message = errorCode.getMessage(Locale.CHINA);
                assertThat(message).as("错误码 %s 应有中文消息", errorCode.name())
                        .isNotEmpty();
            }
        }

        @Test
        @DisplayName("所有 CommonErrorCode 值应有英文消息")
        void allCommonErrorCodeValues_shouldHaveEnglishMessages() {
            for (CommonErrorCode errorCode : CommonErrorCode.values()) {
                String message = errorCode.getMessage(Locale.ENGLISH);
                assertThat(message).as("错误码 %s 应有英文消息", errorCode.name())
                        .isNotEmpty();
            }
        }
    }

    @Nested
    @DisplayName("静态方法测试")
    class StaticMethodTests {

        @Test
        @DisplayName("应通过静态方法获取消息")
        void shouldGetMessageViaStaticMethod() {
            String message = ErrorCodeMessageSource.getMessage(CommonErrorCode.UNAUTHORIZED, Locale.ENGLISH);

            assertThat(message).isEqualTo("Unauthorized or session expired");
        }

        @Test
        @DisplayName("应通过静态方法获取带参数的消息")
        void shouldGetMessageWithArgsViaStaticMethod() {
            String message = ErrorCodeMessageSource.getMessage(
                    CommonErrorCode.NOT_FOUND, new Object[]{"User"}, Locale.CHINA);

            assertThat(message).isEqualTo("资源不存在");
        }
    }
}
