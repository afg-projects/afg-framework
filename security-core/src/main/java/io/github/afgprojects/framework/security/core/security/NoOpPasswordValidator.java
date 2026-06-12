package io.github.afgprojects.framework.security.core.security;

/**
 * NoOp 密码验证降级实现。
 * <p>
 * 总是验证通过，密码匹配总是返回 false。
 *
 * @since 1.0.0
 */
public class NoOpPasswordValidator implements PasswordValidator {

    @Override
    public ValidationResult validate(String password) {
        return ValidationResult.success();
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return false;
    }

    @Override
    public String encode(String password) {
        return password;
    }
}
