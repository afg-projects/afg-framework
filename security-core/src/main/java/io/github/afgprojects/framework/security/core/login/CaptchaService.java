package io.github.afgprojects.framework.security.core.login;

import io.github.afgprojects.framework.security.core.login.model.CaptchaRequest;
import io.github.afgprojects.framework.security.core.login.model.CaptchaResponse;
import org.jspecify.annotations.NonNull;

/**
 * 验证码服务接口。
 *
 * <p>定义验证码生成、验证和删除的标准接口。
 *
 * <p>实现类可以基于内存、Redis 或其他存储机制。
 *
 * @since 1.0.0
 */
public interface CaptchaService {

    /**
     * 生成验证码。
     *
     * <p>根据验证码类型生成相应的验证码：
     * <ul>
     *   <li>图形验证码：生成随机字符并渲染为图片</li>
     *   <li>短信验证码：生成随机数字并发送到手机</li>
     *   <li>邮箱验证码：生成随机数字并发送到邮箱</li>
     * </ul>
     *
     * @param request 验证码请求，永不为 null
     * @return 验证码响应，包含验证码 key 和相关信息，永不为 null
     */
    @NonNull
    CaptchaResponse generate(@NonNull CaptchaRequest request);

    /**
     * 验证验证码。
     *
     * <p>验证成功后，验证码将被自动删除（一次性使用）。
     *
     * @param captchaKey 验证码 key，永不为 null
     * @param captchaValue 验证码值，永不为 null
     * @return 如果验证码正确则返回 true，否则返回 false
     */
    boolean validate(@NonNull String captchaKey, @NonNull String captchaValue);

    /**
     * 删除验证码。
     *
     * <p>手动删除验证码，通常用于清理过期或无效的验证码。
     *
     * @param captchaKey 验证码 key，永不为 null
     */
    void delete(@NonNull String captchaKey);
}
