package io.github.afgprojects.framework.integration.governance.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置中心客户端属性
 *
 * @author afg-projects
 */
@Data
@ConfigurationProperties(prefix = "afg.governance.client.config")
public class GovernanceConfigProperties {

    /**
     * 是否启用配置中心
     */
    private boolean enabled = true;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 环境
     */
    private String environment = "dev";

    /**
     * 是否启用配置订阅
     */
    private boolean enableSubscribe = true;

    /**
     * 是否启用配置自动上报
     */
    private boolean autoRegister = false;

    /**
     * 自动上报配置前缀过滤（支持多个前缀）
     */
    private List<String> autoRegisterPrefixes = new ArrayList<>();

    /**
     * 是否覆盖服务端已有配置
     */
    private boolean autoRegisterOverwrite = true;

    /**
     * 配置项中文显示名映射
     */
    private Map<String, String> displayNames = new HashMap<>();

    // ==================== 重连配置 ====================

    /**
     * 重连间隔（毫秒）
     */
    private int reconnectIntervalMs = 5000;

    /**
     * 最大重连间隔（毫秒）
     */
    private int maxReconnectIntervalMs = 60000;

    /**
     * 最大重连次数（0表示无限）
     */
    private int maxReconnectAttempts = 0;

    /**
     * 请求重试次数
     */
    private int retryCount = 3;

    /**
     * 重试间隔（毫秒）
     */
    private int retryIntervalMs = 1000;

    /**
     * 获取自动上报前缀列表
     */
    public List<String> getEffectiveAutoRegisterPrefixes() {
        if (!autoRegisterPrefixes.isEmpty()) {
            return autoRegisterPrefixes;
        }
        return List.of();
    }
}