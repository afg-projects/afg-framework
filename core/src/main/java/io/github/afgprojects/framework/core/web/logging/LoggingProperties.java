package io.github.afgprojects.framework.core.web.logging;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 日志配置属性
 * <p>
 * 支持以下配置：
 * <ul>
 *   <li>结构化日志输出（JSON 格式）</li>
 *   <li>MDC 字段配置</li>
 *   <li>日志文件滚动策略</li>
 *   <li>异步日志队列配置</li>
 *   <li>敏感信息脱敏</li>
 * </ul>
 */
@Data
@ConfigurationProperties(prefix = "afg.logging")
public class LoggingProperties {

    private final Mdc mdc = new Mdc();
    private final Structured structured = new Structured();
    private final File file = new File();
    private final Async async = new Async();

    /**
     * 是否启用敏感信息脱敏
     * <p>
     * 启用后，日志中的敏感字段（如密码、手机号、身份证号等）会被自动脱敏
     */
    private boolean maskSensitive = true;

    /**
     * MDC 配置
     * <p>
     * 支持的字段：traceId, tenantId, userId, username, clientIp, requestPath, requestMethod, requestId
     * <p>
     * 注意：traceId 由 Micrometer Tracing 自动注入 MDC（当启用追踪时），
     * 但也可以在此配置以便在日志中统一格式化。
     */
    @Data
    public static class Mdc {
        private boolean enabled = true;
        private String[] fields = {"traceId", "tenantId", "userId", "requestPath"};
    }

    /**
     * 结构化日志配置
     */
    @Data
    public static class Structured {
        private boolean enabled;
        private boolean prettyPrint;
    }

    /**
     * 日志文件配置
     */
    @Data
    public static class File {
        /**
         * 日志文件路径
         */
        private String path = "./logs";

        /**
         * 单个日志文件最大大小
         * <p>
         * 支持单位：KB, MB, GB
         */
        private String maxSize = "100MB";

        /**
         * 历史日志文件保留天数
         */
        private int maxHistory = 30;

        /**
         * 所有日志文件总大小上限
         * <p>
         * 支持单位：KB, MB, GB
         */
        private String totalSizeCap = "10GB";
    }

    /**
     * 异步日志配置
     */
    @Data
    public static class Async {
        /**
         * 异步日志队列大小
         */
        private int queueSize = 512;

        /**
         * 当队列剩余容量小于此阈值时，开始丢弃 TRACE、DEBUG、INFO 级别日志
         */
        private int discardingThreshold = 0;

        /**
         * 是否包含调用者信息（类名、方法名、行号）
         * <p>
         * 注意：开启会影响性能
         */
        private boolean includeCallerData = true;

        /**
         * 队列满时是否阻塞
         * <p>
         * true: 队列满时不阻塞，直接丢弃日志（推荐生产环境）
         * false: 队列满时阻塞等待（保证日志不丢失，但可能影响性能）
         */
        private boolean neverBlock = true;
    }
}
