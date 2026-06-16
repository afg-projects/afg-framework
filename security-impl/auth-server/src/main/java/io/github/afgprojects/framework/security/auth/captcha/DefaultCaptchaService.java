package io.github.afgprojects.framework.security.auth.captcha;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import javax.imageio.ImageIO;

import org.jspecify.annotations.NonNull;

import io.github.afgprojects.framework.security.core.login.CaptchaService;
import io.github.afgprojects.framework.security.core.login.model.CaptchaRequest;
import io.github.afgprojects.framework.security.core.login.model.CaptchaResponse;
import io.github.afgprojects.framework.security.core.login.model.CaptchaType;
import io.github.afgprojects.framework.security.core.storage.AfgCaptchaStorage;

/**
 * 默认验证码服务实现。
 *
 * <p>提供图形验证码、短信验证码、邮箱验证码的生成和验证功能。
 *
 * <p>特性：
 * <ul>
 *   <li>图形验证码：生成 4 位字母数字验证码图片，key 为随机 UUID</li>
 *   <li>短信验证码：生成 6 位数字验证码，key 为 "sms:" + 手机号</li>
 *   <li>邮箱验证码：生成 6 位数字验证码，key 为 "email:" + 邮箱</li>
 *   <li>排除易混淆字符（0O1lI）</li>
 *   <li>图形验证码生成 Base64 编码的 PNG 图片</li>
 *   <li>验证时不区分大小写</li>
 *   <li>验证成功后自动删除验证码</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class DefaultCaptchaService implements CaptchaService {

    /**
     * 验证码字符集（排除易混淆字符：0O1lI）
     */
    private static final String CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    /**
     * 数字字符集（短信/邮箱验证码使用）
     */
    private static final String DIGIT_CHARS = "0123456789";

    /**
     * 图形验证码长度
     */
    private static final int IMAGE_CAPTCHA_LENGTH = 4;

    /**
     * 短信/邮箱验证码长度
     */
    private static final int CODE_CAPTCHA_LENGTH = 6;

    /**
     * 图片宽度
     */
    private static final int IMAGE_WIDTH = 120;

    /**
     * 图片高度
     */
    private static final int IMAGE_HEIGHT = 40;

    /**
     * 验证码过期时间（秒）
     */
    private static final long DEFAULT_EXPIRES_IN = 300;

    /**
     * 验证码存储
     */
    private final AfgCaptchaStorage captchaStorage;

    /**
     * 随机数生成器
     */
    private final Random random = new Random();

    /**
     * 构造函数。
     *
     * @param captchaStorage 验证码存储
     */
    public DefaultCaptchaService(@NonNull AfgCaptchaStorage captchaStorage) {
        this.captchaStorage = captchaStorage;
    }

    @Override
    @NonNull
    public CaptchaResponse generate(@NonNull CaptchaRequest request) {
        CaptchaType captchaType = request.captchaType();

        return switch (captchaType) {
            case IMAGE -> generateImageCaptcha(request);
            case SMS -> generateCodeCaptcha(request, CaptchaType.SMS, "sms:");
            case EMAIL -> generateCodeCaptcha(request, CaptchaType.EMAIL, "email:");
        };
    }

    /**
     * 生成图形验证码。
     */
    @NonNull
    private CaptchaResponse generateImageCaptcha(CaptchaRequest request) {
        String captchaKey = UUID.randomUUID().toString().replace("-", "");
        String captchaValue = generateRandomValue(CAPTCHA_CHARS, IMAGE_CAPTCHA_LENGTH);
        String captchaImage = generateCaptchaImage(captchaValue);

        Duration ttl = Duration.ofSeconds(DEFAULT_EXPIRES_IN);
        captchaStorage.save(captchaKey, captchaValue, ttl);

        return CaptchaResponse.builder()
                .captchaKey(captchaKey)
                .captchaImage(captchaImage)
                .captchaType(CaptchaType.IMAGE)
                .expiresIn(DEFAULT_EXPIRES_IN)
                .build();
    }

    /**
     * 生成短信/邮箱验证码。
     *
     * @param request 验证码请求
     * @param captchaType 验证码类型
     * @param keyPrefix key 前缀（"sms:" 或 "email:"）
     */
    @NonNull
    private CaptchaResponse generateCodeCaptcha(CaptchaRequest request, CaptchaType captchaType, String keyPrefix) {
        String target = request.target();
        if (target == null || target.isEmpty()) {
            throw new IllegalArgumentException("短信/邮箱验证码必须提供 target（手机号或邮箱）");
        }

        String captchaKey = keyPrefix + target;
        String captchaValue = generateRandomValue(DIGIT_CHARS, CODE_CAPTCHA_LENGTH);

        Duration ttl = Duration.ofSeconds(DEFAULT_EXPIRES_IN);
        captchaStorage.save(captchaKey, captchaValue, ttl);

        return CaptchaResponse.builder()
                .captchaKey(captchaKey)
                .captchaImage(null)
                .captchaType(captchaType)
                .expiresIn(DEFAULT_EXPIRES_IN)
                .build();
    }

    /**
     * 生成随机验证码值。
     *
     * @param charSet 字符集
     * @param length 长度
     * @return 随机字符串
     */
    private String generateRandomValue(String charSet, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(charSet.length());
            sb.append(charSet.charAt(index));
        }
        return sb.toString();
    }

    @Override
    public boolean validate(@NonNull String captchaKey, @NonNull String captchaValue) {
        // 从存储获取验证码
        String storedValue = captchaStorage.get(captchaKey);

        // 验证码不存在或已过期
        if (storedValue == null) {
            return false;
        }

        // 验证码匹配（不区分大小写）
        boolean isValid = storedValue.equalsIgnoreCase(captchaValue);

        // 验证成功后删除验证码
        if (isValid) {
            captchaStorage.delete(captchaKey);
        }

        return isValid;
    }

    @Override
    public void delete(@NonNull String captchaKey) {
        captchaStorage.delete(captchaKey);
    }

    /**
     * 生成验证码图片。
     *
     * <p>生成 Base64 编码的 PNG 图片。
     *
     * @param captchaValue 验证码值
     * @return Base64 编码的图片
     */
    private String generateCaptchaImage(String captchaValue) {
        // 创建图片
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        try {
            // 设置背景色
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

            // 设置字体
            g.setFont(new Font("Arial", Font.BOLD, 24));

            // 绘制验证码字符
            int x = 15;
            for (int i = 0; i < captchaValue.length(); i++) {
                // 随机颜色
                g.setColor(new Color(random.nextInt(100), random.nextInt(100), random.nextInt(100)));
                // 随机旋转角度
                double angle = (random.nextDouble() - 0.5) * 0.4;
                g.rotate(angle, x + 10, 25);
                g.drawString(String.valueOf(captchaValue.charAt(i)), x, 28);
                g.rotate(-angle, x + 10, 25);
                x += 25;
            }

            // 添加干扰线
            for (int i = 0; i < 4; i++) {
                g.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
                g.drawLine(
                        random.nextInt(IMAGE_WIDTH),
                        random.nextInt(IMAGE_HEIGHT),
                        random.nextInt(IMAGE_WIDTH),
                        random.nextInt(IMAGE_HEIGHT));
            }

            // 添加噪点
            for (int i = 0; i < 50; i++) {
                g.setColor(new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
                g.fillRect(random.nextInt(IMAGE_WIDTH), random.nextInt(IMAGE_HEIGHT), 2, 2);
            }
        } finally {
            g.dispose();
        }

        // 转换为 Base64
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate captcha image", e);
        }
    }
}
