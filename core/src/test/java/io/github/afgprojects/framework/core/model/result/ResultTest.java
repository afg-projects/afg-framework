package io.github.afgprojects.framework.core.model.result;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ResultTest {

    @Nested
    @DisplayName("Static Factory Methods")
    class StaticFactoryTests {

        @Test
        @DisplayName("success(data) should return success result with data")
        void successWithData_shouldReturnSuccessResultWithData() {
            Result<String> result = Result.success("hello");

            assertThat(result.code()).isEqualTo(0);
            assertThat(result.message()).isEqualTo("success");
            assertThat(result.data()).isEqualTo("hello");
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("success(message, data) should return success result with message and data")
        void successWithMessageAndData_shouldReturnSuccessResult() {
            Result<String> result = Result.success("ok", "hello");

            assertThat(result.code()).isEqualTo(0);
            assertThat(result.message()).isEqualTo("ok");
            assertThat(result.data()).isEqualTo("hello");
        }

        @Test
        @DisplayName("fail(code, message) should return fail result with code and message")
        void failWithCodeAndMessage_shouldReturnFailResultWithCodeAndMessage() {
            Result<Void> result = Result.fail(10001, "custom error");

            assertThat(result.code()).isEqualTo(10001);
            assertThat(result.message()).isEqualTo("custom error");
            assertThat(result.isSuccess()).isFalse();
        }
    }

    @Nested
    @DisplayName("Instance Methods")
    class InstanceMethodTests {

        @Test
        @DisplayName("isSuccess should return true when code is 0")
        void isSuccess_shouldReturnTrueWhenCodeIs0() {
            Result<Void> result = new Result<>(0, "success", null, null, null);

            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("isSuccess should return false when code is not 0")
        void isSuccess_shouldReturnFalseWhenCodeIsNot0() {
            Result<Void> result = new Result<>(-1, "fail", null, null, null);

            assertThat(result.isSuccess()).isFalse();
        }
    }

    @Nested
    @DisplayName("Record Features")
    class RecordFeatureTests {

        @Test
        @DisplayName("should have correct equals and hashCode")
        void shouldHaveCorrectEqualsAndHashCode() {
            Result<String> result1 = new Result<>(0, "success", "data", "trace1", "req1");
            Result<String> result2 = new Result<>(0, "success", "data", "trace1", "req1");
            Result<String> result3 = new Result<>(1, "fail", "data", "trace1", "req1");

            assertThat(result1).isEqualTo(result2);
            assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
            assertThat(result1).isNotEqualTo(result3);
        }

        @Test
        @DisplayName("should have correct toString")
        void shouldHaveCorrectToString() {
            Result<String> result = new Result<>(0, "success", "data", "trace1", "req1");

            String str = result.toString();
            assertThat(str).contains("0");
            assertThat(str).contains("success");
            assertThat(str).contains("data");
        }
    }
}
