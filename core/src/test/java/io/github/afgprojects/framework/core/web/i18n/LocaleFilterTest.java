package io.github.afgprojects.framework.core.web.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LocaleFilterTest {

    private LocaleFilter localeFilter;
    private Locale originalDefaultLocale;

    @BeforeEach
    void setUp() {
        localeFilter = new LocaleFilter();
        originalDefaultLocale = Locale.getDefault();
    }

    @AfterEach
    void tearDown() {
        Locale.setDefault(originalDefaultLocale);
        LocaleContextHolder.resetLocaleContext();
    }

    @Nested
    @DisplayName("resolveLocale 测试")
    class ResolveLocaleTests {

        @Test
        @DisplayName("应解析 zh-CN")
        void shouldParseChineseSimple() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Accept-Language", "zh-CN");

            Locale locale = localeFilter.resolveLocale(request);

            assertThat(locale.getLanguage()).isEqualTo("zh");
            assertThat(locale.getCountry()).isEqualTo("CN");
        }

        @Test
        @DisplayName("应解析 en-US")
        void shouldParseEnglishUS() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Accept-Language", "en-US");

            Locale locale = localeFilter.resolveLocale(request);

            assertThat(locale.getLanguage()).isEqualTo("en");
            assertThat(locale.getCountry()).isEqualTo("US");
        }

        @Test
        @DisplayName("应解析纯语言代码 en")
        void shouldParseLanguageOnly() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Accept-Language", "en");

            Locale locale = localeFilter.resolveLocale(request);

            assertThat(locale.getLanguage()).isEqualTo("en");
            assertThat(locale.getCountry()).isEmpty();
        }

        @Test
        @DisplayName("应处理带质量因子的 Accept-Language")
        void shouldHandleQualityFactor() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");

            Locale locale = localeFilter.resolveLocale(request);

            assertThat(locale.getLanguage()).isEqualTo("zh");
            assertThat(locale.getCountry()).isEqualTo("CN");
        }

        @Test
        @DisplayName("应处理下划线分隔符")
        void shouldHandleUnderscoreSeparator() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Accept-Language", "zh_CN");

            Locale locale = localeFilter.resolveLocale(request);

            assertThat(locale.getLanguage()).isEqualTo("zh");
            assertThat(locale.getCountry()).isEqualTo("CN");
        }

        @Test
        @DisplayName("Accept-Language 为空时应返回默认 Locale")
        void shouldReturnDefaultLocaleWhenNoHeader() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            Locale.setDefault(Locale.ENGLISH);

            Locale locale = localeFilter.resolveLocale(request);

            assertThat(locale).isEqualTo(Locale.ENGLISH);
        }

        @Test
        @DisplayName("Accept-Language 为空白时应返回默认 Locale")
        void shouldReturnDefaultLocaleWhenBlankHeader() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Accept-Language", "   ");
            Locale.setDefault(Locale.ENGLISH);

            Locale locale = localeFilter.resolveLocale(request);

            assertThat(locale).isEqualTo(Locale.ENGLISH);
        }
    }

    @Nested
    @DisplayName("doFilter 测试")
    class DoFilterTests {

        @Test
        @DisplayName("应设置 LocaleContextHolder")
        void shouldSetLocaleContextHolder() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Accept-Language", "en-US");
            MockHttpServletResponse response = new MockHttpServletResponse();

            localeFilter.doFilter(request, response, (req, res) -> {
                // 验证 Locale 已设置
                Locale locale = LocaleContextHolder.getLocale();
                assertThat(locale.getLanguage()).isEqualTo("en");
                assertThat(locale.getCountry()).isEqualTo("US");
            });
        }

        @Test
        @DisplayName("过滤器执行后应清除 LocaleContextHolder")
        void shouldClearLocaleContextHolderAfterFilter() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Accept-Language", "zh-CN");
            MockHttpServletResponse response = new MockHttpServletResponse();

            localeFilter.doFilter(request, response, (req, res) -> {});

            // 过滤器执行后 LocaleContext 应被清除
            assertThat(LocaleContextHolder.getLocaleContext()).isNull();
        }
    }
}
