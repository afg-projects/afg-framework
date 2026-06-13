package io.github.afgprojects.framework.core.api.notification;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知模型
 * <p>
 * 统一的通知数据结构，支持多种渠道和模板变量。
 *
 * <pre>{@code
 * Notification notification = Notification.builder()
 *     .to("user-123")
 *     .channel(NotificationChannel.EMAIL)
 *     .template("welcome")
 *     .variable("username", "张三")
 *     .build();
 * notificationService.send(notification);
 * }</pre>
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    /**
     * 接收者标识（用户 ID、邮箱、手机号等）
     */
    private String to;

    /**
     * 抄送（多接收者以逗号分隔）
     */
    private String cc;

    /**
     * 通知主题
     */
    private String subject;

    /**
     * 通知内容（与 template 二选一）
     */
    private String content;

    /**
     * 通知渠道
     */
    private NotificationChannel channel;

    /**
     * 模板名称（与 content 二选一）
     */
    private String template;

    /**
     * 模板变量
     */
    @Builder.Default
    private Map<String, Object> variables = new HashMap<>();

    /**
     * 优先级（数值越大优先级越高）
     */
    @Builder.Default
    private int priority = 0;

    /**
     * 扩展元数据
     */
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    /**
     * 添加模板变量
     *
     * @param key   变量名
     * @param value 变量值
     * @return 当前 Notification 实例（支持链式调用）
     */
    public Notification variable(String key, Object value) {
        if (this.variables == null) {
            this.variables = new HashMap<>();
        }
        this.variables.put(key, value);
        return this;
    }

    /**
     * 添加扩展元数据
     *
     * @param key   元数据键
     * @param value 元数据值
     * @return 当前 Notification 实例（支持链式调用）
     */
    public Notification meta(String key, String value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
        return this;
    }
}
