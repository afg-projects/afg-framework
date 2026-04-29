package io.github.afgprojects.framework.core.model.result;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import io.github.afgprojects.framework.core.model.exception.CommonErrorCode;
import io.github.afgprojects.framework.core.web.context.AfgRequestContextHolder;
import io.github.afgprojects.framework.core.web.context.RequestContext;

class ResultsTest {

    @BeforeEach
    void setUp() {
        // Setup Spring RequestContextHolder
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestContext context = new RequestContext();
        context.setTraceId("test-trace-id");
        context.setRequestId("test-request-id");
        AfgRequestContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        AfgRequestContextHolder.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Nested
    @DisplayName("Success Tests")
    class SuccessTests {

        @Test
        @DisplayName("success() should return success result with trace info")
        void success_shouldReturnSuccessResultWithTraceInfo() {
            Result<String> result = Results.success("data");

            assertThat(result.code()).isEqualTo(0);
            assertThat(result.message()).isEqualTo("success");
            assertThat(result.data()).isEqualTo("data");
            assertThat(result.traceId()).isEqualTo("test-trace-id");
            assertThat(result.requestId()).isEqualTo("test-request-id");
        }

        @Test
        @DisplayName("success(message, data) should return success result with custom message")
        void successWithMessage_shouldReturnSuccessResultWithCustomMessage() {
            Result<String> result = Results.success("操作成功", "data");

            assertThat(result.message()).isEqualTo("操作成功");
        }
    }

    @Nested
    @DisplayName("Fail Tests")
    class FailTests {

        @Test
        @DisplayName("fail(ErrorCode) should return fail result with error code info")
        void fail_withErrorCode_shouldReturnFailResultWithErrorCodeInfo() {
            Result<Void> result = Results.fail(CommonErrorCode.PARAM_ERROR);

            assertThat(result.code()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode());
            assertThat(result.message()).isEqualTo(CommonErrorCode.PARAM_ERROR.getMessage());
            assertThat(result.traceId()).isEqualTo("test-trace-id");
        }

        @Test
        @DisplayName("fail(code, message) should return fail result with code and message")
        void fail_withCodeAndMessage_shouldReturnFailResultWithCodeAndMessage() {
            Result<Void> result = Results.fail(10001, "自定义错误");

            assertThat(result.code()).isEqualTo(10001);
            assertThat(result.message()).isEqualTo("自定义错误");
        }

        @Test
        @DisplayName("fail(ErrorCode, message) should return fail result with custom message")
        void fail_withErrorCodeAndMessage_shouldReturnFailResultWithCustomMessage() {
            Result<Void> result = Results.fail(CommonErrorCode.PARAM_ERROR, "用户名不能为空");

            assertThat(result.code()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode());
            assertThat(result.message()).isEqualTo("用户名不能为空");
        }
    }

    @Nested
    @DisplayName("Page Tests")
    class PageTests {

        @Test
        @DisplayName("page() should return success result with PageData")
        void page_shouldReturnSuccessResultWithPageData() {
            List<String> records = List.of("a", "b", "c");

            Result<PageData<String>> result = Results.page(records, 100, 2, 10);

            assertThat(result.code()).isEqualTo(0);
            assertThat(result.data().records()).containsExactly("a", "b", "c");
            assertThat(result.data().total()).isEqualTo(100);
            assertThat(result.data().page()).isEqualTo(2);
            assertThat(result.data().size()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("No Context Tests")
    class NoContextTests {

        @Test
        @DisplayName("should handle missing RequestContext gracefully")
        void shouldHandleMissingRequestContext() {
            AfgRequestContextHolder.clear();

            Result<String> result = Results.success("data");

            assertThat(result.code()).isEqualTo(0);
            assertThat(result.traceId()).isNull();
            assertThat(result.requestId()).isNull();
        }
    }
}
