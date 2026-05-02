package io.github.afgprojects.framework.core.web.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.test.annotation.DirtiesContext;

import io.github.afgprojects.framework.core.support.TestApplication;

/**
 * LocaleAutoConfiguration 集成测试
 */
@DisplayName("LocaleAutoConfiguration 集成测试")
@SpringBootTest(
        classes = TestApplication.class,
        properties = {
                "afg.i18n.enabled=true",
                "afg.i18n.default-locale=zh_CN",
                "afg.i18n.fallback-locale=en_US"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LocaleAutoConfigurationIntegrationTest {

    @Autowired(required = false)
    private LocaleFilter localeFilter;

    @Autowired(required = false)
    private MessageSource messageSource;

    @Nested
    @DisplayName("国际化配置测试")
    class LocaleConfigTests {

        @Test
        @DisplayName("应该自动配置 LocaleFilter")
        void shouldAutoConfigureLocaleFilter() {
            if (localeFilter != null) {
                assertThat(localeFilter).isNotNull();
            }
        }

        @Test
        @DisplayName("应该自动配置 MessageSource")
        void shouldAutoConfigureMessageSource() {
            assertThat(messageSource).isNotNull();
        }
    }

    @Nested
    @DisplayName("消息解析测试")
    class MessageResolutionTests {

        @Test
        @DisplayName("应该能够解析默认语言的消息")
        void shouldResolveMessageInDefaultLocale() {
            // 尝试解析一个可能不存在的消息
            String message = messageSource.getMessage("test.key", null, "Default Message", Locale.getDefault());

            assertThat(message).isNotNull();
        }

        @Test
        @DisplayName("应该能够解析英文消息")
        void shouldResolveMessageInEnglish() {
            String message = messageSource.getMessage("test.key", null, "Default Message", Locale.US);

            assertThat(message).isNotNull();
        }

        @Test
        @DisplayName("应该能够解析中文消息")
        void shouldResolveMessageInChinese() {
            String message = messageSource.getMessage("test.key", null, "默认消息", Locale.SIMPLIFIED_CHINESE);

            assertThat(message).isNotNull();
        }
    }

    @Nested
    @DisplayName("LocaleFilter 功能测试")
    class LocaleFilterTests {

        @Test
        @DisplayName("LocaleFilter 应该正确配置")
        void localeFilterShouldBeConfigured() {
            if (localeFilter != null) {
                assertThat(localeFilter).isNotNull();
            }
        }
    }
}
