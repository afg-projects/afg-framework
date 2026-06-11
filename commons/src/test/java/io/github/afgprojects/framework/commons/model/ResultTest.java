package io.github.afgprojects.framework.commons.model;

import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.commons.exception.ErrorCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Result 测试
 */
@DisplayName("Result 测试")
class ResultTest {

    @Nested
    @DisplayName("success(data) 方法")
    class SuccessDataTests {

        @Test
        @DisplayName("应返回 code=0, message=success, data=传入数据")
        void shouldReturnSuccessResultWithData() {
            Result<String> result = Result.success("hello");

            assertThat(result.code()).isEqualTo(0);
            assertThat(result.message()).isEqualTo("success");
            assertThat(result.data()).isEqualTo("hello");
            assertThat(result.traceId()).isNull();
            assertThat(result.requestId()).isNull();
        }

        @Test
        @DisplayName("data 为 null 时应正常返回")
        void shouldReturnSuccessResultWithNullData() {
            Result<Object> result = Result.success(null);

            assertThat(result.code()).isEqualTo(0);
            assertThat(result.message()).isEqualTo("success");
            assertThat(result.data()).isNull();
        }
    }

    @Nested
    @DisplayName("success() 无参方法")
    class SuccessNoArgTests {

        @Test
        @DisplayName("应返回 code=0, message=success, data=null")
        void shouldReturnSuccessResultWithNoData() {
            Result<Object> result = Result.success();

            assertThat(result.code()).isEqualTo(0);
            assertThat(result.message()).isEqualTo("success");
            assertThat(result.data()).isNull();
        }
    }

    @Nested
    @DisplayName("success(message, data) 方法")
    class SuccessMessageDataTests {

        @Test
        @DisplayName("应返回自定义消息和数据")
        void shouldReturnSuccessResultWithCustomMessage() {
            Result<Integer> result = Result.success("操作完成", 42);

            assertThat(result.code()).isEqualTo(0);
            assertThat(result.message()).isEqualTo("操作完成");
            assertThat(result.data()).isEqualTo(42);
        }
    }

    @Nested
    @DisplayName("fail(code, message) 方法")
    class FailTests {

        @Test
        @DisplayName("应返回错误码和消息，data 为 null")
        void shouldReturnFailResult() {
            Result<Object> result = Result.fail(10001, "参数错误");

            assertThat(result.code()).isEqualTo(10001);
            assertThat(result.message()).isEqualTo("参数错误");
            assertThat(result.data()).isNull();
            assertThat(result.traceId()).isNull();
            assertThat(result.requestId()).isNull();
        }
    }

    @Nested
    @DisplayName("fail(ErrorCode) 方法")
    class FailErrorCodeTests {

        @Test
        @DisplayName("应从 ErrorCode 提取 code 和 message")
        void shouldReturnFailResultFromErrorCode() {
            Result<Object> result = Result.fail(CommonErrorCode.NOT_FOUND);

            assertThat(result.code()).isEqualTo(CommonErrorCode.NOT_FOUND.getCode());
            assertThat(result.message()).isEqualTo(CommonErrorCode.NOT_FOUND.getMessage());
            assertThat(result.data()).isNull();
        }
    }

    @Nested
    @DisplayName("fail(ErrorCode, message) 方法")
    class FailErrorCodeMessageTests {

        @Test
        @DisplayName("应使用 ErrorCode 的 code 和自定义 message")
        void shouldReturnFailResultWithCustomMessage() {
            Result<Object> result = Result.fail(CommonErrorCode.ENTITY_NOT_FOUND, "用户 123 不存在");

            assertThat(result.code()).isEqualTo(CommonErrorCode.ENTITY_NOT_FOUND.getCode());
            assertThat(result.message()).isEqualTo("用户 123 不存在");
            assertThat(result.data()).isNull();
        }
    }

    @Nested
    @DisplayName("isSuccess() 方法")
    class IsSuccessTests {

        @Test
        @DisplayName("code=0 时应返回 true")
        void shouldReturnTrueWhenCodeIsZero() {
            Result<String> result = Result.success("ok");
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("code 非 0 时应返回 false")
        void shouldReturnFalseWhenCodeIsNonZero() {
            Result<Object> result = Result.fail(10001, "错误");
            assertThat(result.isSuccess()).isFalse();
        }
    }

    @Nested
    @DisplayName("record 完整构造器")
    class FullConstructorTests {

        @Test
        @DisplayName("应正确保留所有字段")
        void shouldRetainAllFields() {
            Result<String> result = new Result<>(1, "msg", "data", "trace-1", "req-1");

            assertThat(result.code()).isEqualTo(1);
            assertThat(result.message()).isEqualTo("msg");
            assertThat(result.data()).isEqualTo("data");
            assertThat(result.traceId()).isEqualTo("trace-1");
            assertThat(result.requestId()).isEqualTo("req-1");
        }
    }
}
