package io.github.afgprojects.framework.security.core.security;

import java.util.List;

/**
 * 密码验证器接口。
 *
 * <p>提供密码强度验证和密码加密匹配功能。
 *
 * <p>实现类可以支持不同的密码策略，如：
 * <ul>
 *   <li>最小长度要求</li>
 *   <li>复杂度要求（大小写、数字、特殊字符）</li>
 *   <li>密码历史检查</li>
 *   <li>常见弱密码检查</li>
 * </ul>
 *
 * @since 1.0.0
 */
public interface PasswordValidator {

    /**
     * 验证密码强度。
     *
     * <p>检查密码是否符合安全策略要求。
     *
     * @param password 待验证的密码，永不为 null
     * @return 验证结果，包含是否通过和错误信息
     */
    ValidationResult validate(String password);

    /**
     * 检查原始密码与加密密码是否匹配。
     *
     * @param rawPassword 原始密码，永不为 null
     * @param encodedPassword 加密后的密码，永不为 null
     * @return 如果匹配则返回 true
     */
    boolean matches(String rawPassword, String encodedPassword);

    /**
     * 加密密码。
     *
     * @param password 原始密码，永不为 null
     * @return 加密后的密码
     */
    String encode(String password);

    /**
     * 密码验证结果。
     *
     * @param valid 是否通过验证
     * @param errors 错误信息列表，验证通过时为空列表
     */
    record ValidationResult(boolean valid, List<String> errors) {

        /**
         * 创建验证通过的结果。
         *
         * @return 验证通过的结果
         */
        public static ValidationResult success() {
            return new ValidationResult(true, List.of());
        }

        /**
         * 创建验证失败的结果。
         *
         * @param errors 错误信息列表
         * @return 验证失败的结果
         */
        public static ValidationResult failure(List<String> errors) {
            return new ValidationResult(false, errors);
        }

        /**
         * 创建验证失败的结果（单个错误）。
         *
         * @param error 错误信息
         * @return 验证失败的结果
         */
        public static ValidationResult failure(String error) {
            return new ValidationResult(false, List.of(error));
        }
    }
}
