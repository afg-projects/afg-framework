package io.github.afgprojects.framework.commons.assertion;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ArgumentAssert 测试")
class ArgumentAssertTest {

    @Nested
    @DisplayName("notNull() 方法")
    class NotNullTests {

        @Test
        @DisplayName("对象不为 null 时应正常通过")
        void shouldPassWhenObjectIsNotNull() {
            ArgumentAssert.notNull("hello", "不应为空");
            ArgumentAssert.notNull(42, "不应为空");
        }

        @Test
        @DisplayName("对象为 null 时应抛出 BusinessException(PARAM_MISSING)")
        void shouldThrowWhenObjectIsNull() {
            assertThatThrownBy(() -> ArgumentAssert.notNull(null, "参数不能为空"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(CommonErrorCode.PARAM_MISSING);
                        assertThat(be.getMessage()).isEqualTo("参数不能为空");
                    });
        }
    }

    @Nested
    @DisplayName("notEmpty() 方法")
    class NotEmptyTests {

        @Test
        @DisplayName("字符串不为空时应正常通过")
        void shouldPassWhenStringIsNotEmpty() {
            ArgumentAssert.notEmpty("hello", "不应为空");
        }

        @Test
        @DisplayName("字符串为 null 时应抛出异常")
        void shouldThrowWhenStringIsNull() {
            assertThatThrownBy(() -> ArgumentAssert.notEmpty(null, "用户名不能为空"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(CommonErrorCode.PARAM_MISSING));
        }

        @Test
        @DisplayName("字符串为空白时应抛出异常")
        void shouldThrowWhenStringIsBlank() {
            assertThatThrownBy(() -> ArgumentAssert.notEmpty("   ", "用户名不能为空"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("空字符串应抛出异常")
        void shouldThrowWhenStringIsEmpty() {
            assertThatThrownBy(() -> ArgumentAssert.notEmpty("", "用户名不能为空"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("isTrue() 方法")
    class IsTrueTests {

        @Test
        @DisplayName("条件为 true 时应正常通过")
        void shouldPassWhenConditionIsTrue() {
            ArgumentAssert.isTrue(true, "条件不满足");
            ArgumentAssert.isTrue(1 > 0, "条件不满足");
        }

        @Test
        @DisplayName("条件为 false 时应抛出 BusinessException(PARAM_ERROR)")
        void shouldThrowWhenConditionIsFalse() {
            assertThatThrownBy(() -> ArgumentAssert.isTrue(false, "年龄不能为负数"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(CommonErrorCode.PARAM_ERROR));
        }
    }

    @Nested
    @DisplayName("state() 方法")
    class StateTests {

        @Test
        @DisplayName("状态合法时应正常通过")
        void shouldPassWhenStateIsValid() {
            ArgumentAssert.state(true, "状态不合法");
        }

        @Test
        @DisplayName("状态不合法时应抛出 BusinessException(FAIL)")
        void shouldThrowWhenStateIsInvalid() {
            assertThatThrownBy(() -> ArgumentAssert.state(false, "订单状态不允许此操作"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(CommonErrorCode.FAIL));
        }
    }
}