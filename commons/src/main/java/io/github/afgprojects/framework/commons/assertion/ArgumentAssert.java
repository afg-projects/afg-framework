package io.github.afgprojects.framework.commons.assertion;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;

/**
 * 参数断言工具。
 * <p>断言失败时抛出 {@link BusinessException}，用于方法入口参数校验。
 *
 * <p>使用示例：
 * <pre>{@code
 * public void createUser(User user) {
 *     ArgumentAssert.notNull(user, "用户不能为空");
 *     ArgumentAssert.notEmpty(user.getUsername(), "用户名不能为空");
 *     ArgumentAssert.isTrue(user.getAge() >= 0, "年龄不能为负数");
 * }
 * }</pre>
 */
public final class ArgumentAssert {

    private ArgumentAssert() {
        // 工具类禁止实例化
    }

    /**
     * 断言对象不为 null
     *
     * @param object  待检查对象
     * @param message 失败消息
     * @throws BusinessException 如果 object 为 null
     */
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw BusinessException.of(CommonErrorCode.PARAM_MISSING, message);
        }
    }

    /**
     * 断言字符串不为空（非 null 且非空白）
     *
     * @param text    待检查字符串
     * @param message 失败消息
     * @throws BusinessException 如果 text 为 null 或空白
     */
    public static void notEmpty(String text, String message) {
        if (text == null || text.isBlank()) {
            throw BusinessException.of(CommonErrorCode.PARAM_MISSING, message);
        }
    }

    /**
     * 断言条件为 true
     *
     * @param condition 条件表达式
     * @param message   失败消息
     * @throws BusinessException 如果 condition 为 false
     */
    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw BusinessException.of(CommonErrorCode.PARAM_ERROR, message);
        }
    }

    /**
     * 断言状态合法（用于对象状态校验，区别于参数校验）
     *
     * @param condition 条件表达式
     * @param message   失败消息
     * @throws BusinessException 如果 condition 为 false
     */
    public static void state(boolean condition, String message) {
        if (!condition) {
            throw BusinessException.of(CommonErrorCode.FAIL, message);
        }
    }
}
