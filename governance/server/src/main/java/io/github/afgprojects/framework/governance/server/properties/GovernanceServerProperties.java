package io.github.afgprojects.framework.governance.server.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 服务治理模块配置属性
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "afg.governance.server")
public class GovernanceServerProperties {

    /**
     * 是否启用服务治理服务端
     */
    private boolean enabled = true;

    /**
     * gRPC 服务端口。
     *
     * <p>设置此值将覆盖 {@code spring.grpc.server.port} 配置。如果不设置（null），
     * 则使用 {@code spring.grpc.server.port} 的值（默认 9090）。
     */
    private Integer port;

    /**
     * 配置变更通知是否启用
     */
    private boolean enableConfigNotification = true;

    /**
     * 配置历史保留天数
     */
    private int historyRetentionDays = 90;

    /**
     * 心跳配置
     */
    private Heartbeat heartbeat = new Heartbeat();

    /**
     * 推送配置
     */
    private Push push = new Push();

    /**
     * 负载均衡配置
     */
    private Loadbalance loadbalance = new Loadbalance();

    /**
     * 灰度发布配置
     */
    private GrayRelease grayRelease = new GrayRelease();

    /**
     * 心跳配置
     */
    @Data
    public static class Heartbeat {
        /**
         * 心跳间隔（毫秒）
         */
        private long interval = 10000;

        /**
         * 心跳超时时间（毫秒）
         */
        private long timeout = 30000;

        /**
         * 实例驱逐时间（毫秒）
         */
        private long eviction = 90000;
    }

    /**
     * 推送配置
     */
    @Data
    public static class Push {
        /**
         * 重试次数
         */
        private int retryTimes = 3;

        /**
         * 重试间隔（毫秒）
         */
        private long retryInterval = 5000;
    }

    /**
     * 负载均衡配置
     */
    @Data
    public static class Loadbalance {
        /**
         * 默认策略
         */
        private String defaultStrategy = "round_robin";
    }

    /**
     * 灰度发布配置
     */
    @Data
    public static class GrayRelease {
        /**
         * 默认灰度比例（百分比）
         */
        private int defaultPercentage = 10;
    }
}
