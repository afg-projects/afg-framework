package io.github.afgprojects.framework.security.auth.security;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jspecify.annotations.NonNull;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import io.github.afgprojects.framework.security.core.security.PasswordValidator;

/**
 * 默认密码验证器实现。
 *
 * <p>支持密码强度校验和 BCrypt 加密。
 *
 * <p>密码强度校验规则：
 * <ul>
 *   <li>最小长度要求（默认8位）</li>
 *   <li>大写字母要求（默认需要）</li>
 *   <li>小写字母要求（默认需要）</li>
 *   <li>数字要求（默认需要）</li>
 *   <li>特殊字符要求（默认需要）</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class DefaultPasswordValidator implements PasswordValidator {

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");

    private final PasswordPolicy policy;
    private final PasswordEncoder passwordEncoder;

    /**
     * 使用默认密码策略创建验证器。
     */
    public DefaultPasswordValidator() {
        this(PasswordPolicy.DEFAULT);
    }

    /**
     * 使用自定义密码策略创建验证器。
     *
     * @param policy 密码策略
     */
    public DefaultPasswordValidator(@NonNull PasswordPolicy policy) {
        this.policy = policy;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * 使用自定义密码策略和密码编码器创建验证器。
     *
     * @param policy 密码策略
     * @param passwordEncoder 密码编码器
     */
    public DefaultPasswordValidator(@NonNull PasswordPolicy policy, @NonNull PasswordEncoder passwordEncoder) {
        this.policy = policy;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public ValidationResult validate(@NonNull String password) {
        List<String> errors = new ArrayList<>();

        // 检查最小长度
        if (password.length() < policy.minLength()) {
            errors.add("密码长度不能少于" + policy.minLength() + "位");
        }

        // 检查大写字母
        if (policy.requireUppercase() && !UPPERCASE_PATTERN.matcher(password).find()) {
            errors.add("密码必须包含大写字母");
        }

        // 检查小写字母
        if (policy.requireLowercase() && !LOWERCASE_PATTERN.matcher(password).find()) {
            errors.add("密码必须包含小写字母");
        }

        // 检查数字
        if (policy.requireDigit() && !DIGIT_PATTERN.matcher(password).find()) {
            errors.add("密码必须包含数字");
        }

        // 检查特殊字符
        if (policy.requireSpecialChar() && !SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            errors.add("密码必须包含特殊字符");
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    @Override
    public boolean matches(@NonNull String rawPassword, @NonNull String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Override
    public String encode(@NonNull String password) {
        return passwordEncoder.encode(password);
    }
}
