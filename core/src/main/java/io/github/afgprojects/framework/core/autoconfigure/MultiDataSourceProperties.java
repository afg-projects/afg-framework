package io.github.afgprojects.framework.core.autoconfigure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import lombok.Data;

/**
 * 多数据源配置属性
 *
 * <p>配置多个数据源及其相关属性
 */
@Data
public class MultiDataSourceProperties {

    /**
     * 是否启用多数据源
     */
    private boolean enabled = false;

    /**
     * 主数据源名称（默认数据源）
     */
    private String primary = "master";

    /**
     * 严格模式
     * <p>
     * true: 未匹配到数据源时抛异常
     * false: 未匹配到数据源时使用主数据源
     */
    private boolean strict = false;

    /**
     * 数据源配置映射
     * <p>
     * key: 数据源名称
     * value: 数据源配置
     */
    private Map<String, DataSourceConfig> datasources = new HashMap<>();

    /**
     * 读写分离配置
     */
    private ReadWriteSeparationConfig readWriteSeparation = new ReadWriteSeparationConfig();

    /**
     * 单个数据源配置
     */
    @Data
    public static class DataSourceConfig {

        /**
         * 数据库连接 URL
         */
        private @Nullable String url;

        /**
         * 用户名
         */
        private @Nullable String username;

        /**
         * 密码
         */
        private @Nullable String password;

        /**
         * 驱动类名
         */
        private @Nullable String driverClassName;

        /**
         * 是否懒加载
         */
        private boolean lazyInit = false;

        /**
         * 连接池配置
         */
        private Map<String, Object> poolConfig = new HashMap<>();
    }

    /**
     * 读写分离配置
     */
    @Data
    public static class ReadWriteSeparationConfig {

        /**
         * 是否启用读写分离
         */
        private boolean enabled = false;

        /**
         * 读数据源列表
         * <p>
         * 读操作会轮询这些数据源
         */
        private List<String> readDatasources = new ArrayList<>();

        /**
         * 写数据源（默认为主数据源）
         */
        private @Nullable String writeDatasource;

        /**
         * 负载均衡配置
         */
        private LoadBalanceConfig loadBalance = new LoadBalanceConfig();
    }

    /**
     * 负载均衡配置
     */
    @Data
    public static class LoadBalanceConfig {

        /**
         * 负载均衡策略
         * <p>
         * ROUND_ROBIN: 轮询
         * WEIGHTED: 权重
         * LEAST_CONNECTIONS: 最少连接
         */
        private LoadBalanceStrategyType strategy = LoadBalanceStrategyType.ROUND_ROBIN;

        /**
         * 健康检查间隔（毫秒）
         * <p>
         * 默认30秒
         */
        private long healthCheckInterval = 30000L;

        /**
         * 是否启用健康检查
         */
        private boolean healthCheckEnabled = true;

        /**
         * 数据源权重配置
         * <p>
         * key: 数据源名称
         * value: 权重值（仅对 WEIGHTED 策略有效）
         */
        private Map<String, Integer> weights = new HashMap<>();
    }

    /**
     * 负载均衡策略类型
     */
    public enum LoadBalanceStrategyType {
        ROUND_ROBIN,
        WEIGHTED,
        LEAST_CONNECTIONS
    }
}