package io.github.afgprojects.framework.core.web.context;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 请求上下文
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestContext {

    private String traceId;
    private String requestId;
    private Long userId;
    private String username;
    private Long tenantId;
    private String clientIp;
    private String source;
    private LocalDateTime requestTime;
    private String requestPath;
    private String requestMethod;
    private Map<String, Object> attributes;

    /**
     * 获取属性映射（空安全）
     *
     * @return 属性映射
     */
    public Map<String, Object> getAttributes() {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        return attributes;
    }

    /**
     * 设置属性映射（空安全）
     *
     * @param attributes 属性映射
     */
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes != null ? attributes : new HashMap<>();
    }

    /**
     * 获取属性
     *
     * @param key 属性键
     * @return 属性值
     */
    public Object getAttribute(String key) {
        return getAttributes().get(key);
    }

    /**
     * 设置属性
     *
     * @param key   属性键
     * @param value 属性值
     */
    public void setAttribute(String key, Object value) {
        getAttributes().put(key, value);
    }
}
