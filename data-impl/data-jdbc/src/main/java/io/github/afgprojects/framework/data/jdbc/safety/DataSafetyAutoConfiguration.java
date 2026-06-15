package io.github.afgprojects.framework.data.jdbc.safety;

import io.github.afgprojects.framework.data.core.safety.FullTableOperationChecker;
import io.github.afgprojects.framework.data.core.safety.NoOpFullTableOperationChecker;
import io.github.afgprojects.framework.data.jdbc.autoconfigure.DataManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 数据安全自动配置
 *
 * <p>注册 {@link FullTableOperationChecker} Bean，用于保护全表操作（条件更新/删除无条件时）。
 *
 * <p>配置属性前缀：{@code afg.data.safety}
 *
 * <p>当 {@code afg.data.safety.enabled=true}（默认）时注册 {@link DefaultFullTableOperationChecker}，
 * 当 {@code afg.data.safety.enabled=false} 时注册 {@link NoOpFullTableOperationChecker}。
 *
 * <p>必须在 {@link DataManagerAutoConfiguration} 之前执行，
 * 以确保 {@link FullTableOperationChecker} Bean 在创建 {@link io.github.afgprojects.framework.data.jdbc.JdbcDataManager} 时可用。
 */
@AutoConfiguration(before = DataManagerAutoConfiguration.class)
@EnableConfigurationProperties(DataSafetyProperties.class)
public class DataSafetyAutoConfiguration {

    /**
     * 创建全表操作检查器 Bean（当数据安全功能启用时）
     *
     * <p>当容器中不存在自定义 {@link FullTableOperationChecker} 实现时，
     * 使用 {@link DefaultFullTableOperationChecker}，根据配置的策略检查全表操作。
     *
     * @param properties 数据安全配置属性
     * @return DefaultFullTableOperationChecker 实例
     */
    @Bean
    @ConditionalOnMissingBean(FullTableOperationChecker.class)
    @ConditionalOnProperty(prefix = "afg.data.safety", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DefaultFullTableOperationChecker fullTableOperationChecker(DataSafetyProperties properties) {
        return new DefaultFullTableOperationChecker(properties);
    }

    /**
     * 创建 NoOp 全表操作检查器 Bean（当数据安全功能明确关闭时）
     *
     * <p>当 {@code afg.data.safety.enabled=false} 时使用，不做任何检查。
     *
     * @return NoOpFullTableOperationChecker 实例
     */
    @Bean
    @ConditionalOnMissingBean(FullTableOperationChecker.class)
    @ConditionalOnProperty(prefix = "afg.data.safety", name = "enabled", havingValue = "false")
    public NoOpFullTableOperationChecker noOpFullTableOperationChecker() {
        return new NoOpFullTableOperationChecker();
    }
}
