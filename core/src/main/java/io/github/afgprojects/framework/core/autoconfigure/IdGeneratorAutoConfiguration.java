package io.github.afgprojects.framework.core.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.core.api.id.IdGenerator;
import io.github.afgprojects.framework.core.api.id.NoOpIdGenerator;
import io.github.afgprojects.framework.core.api.id.SnowflakeIdGenerator;
import io.github.afgprojects.framework.core.api.id.UuidIdGenerator;
import io.github.afgprojects.framework.core.config.AfgCoreProperties;

/**
 * ID 生成器自动配置
 * <p>
 * 根据 {@code afg.core.id-generator.type} 配置自动选择 ID 生成器实现：
 * <ul>
 *   <li>{@code SNOWFLAKE} — Twitter Snowflake 算法（默认）</li>
 *   <li>{@code UUID} — UUID 随机字符串</li>
 *   <li>{@code SEGMENT} — 号段模式（计划支持，当前降级为 NoOp）</li>
 * </ul>
 * </p>
 * <p>
 * 配置示例：
 * <pre>
 * afg:
 *   core:
 *     id-generator:
 *       enabled: true
 *       type: SNOWFLAKE
 *       snowflake:
 *         worker-id: 1
 *         datacenter-id: 1
 * </pre>
 * </p>
 *
 * @since 1.0.0
 */
@AutoConfiguration(after = AfgAutoConfiguration.class)
@ConditionalOnProperty(prefix = "afg.core.id-generator", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AfgCoreProperties.class)
public class IdGeneratorAutoConfiguration {

    /**
     * Snowflake ID 生成器
     * <p>
     * 当 type=SNOWFLAKE 时注册。基于 Twitter Snowflake 算法生成 64-bit 有序数值 ID。
     *
     * @param properties 核心配置属性
     * @return Snowflake ID 生成器实例
     */
    @Bean
    @ConditionalOnMissingBean(IdGenerator.class)
    @ConditionalOnProperty(
            prefix = "afg.core.id-generator",
            name = "type",
            havingValue = "SNOWFLAKE",
            matchIfMissing = true)
    public IdGenerator snowflakeIdGenerator(AfgCoreProperties properties) {
        AfgCoreProperties.IdGeneratorConfig.SnowflakeConfig snowflake = properties.getIdGenerator().getSnowflake();
        return new SnowflakeIdGenerator(
                snowflake.getWorkerId(),
                snowflake.getDatacenterId(),
                snowflake.getTwepoch(),
                snowflake.getMaxTolerateClockSkewMs());
    }

    /**
     * UUID ID 生成器
     * <p>
     * 当 type=UUID 时注册。基于 UUID.randomUUID() 生成字符串型 ID。
     *
     * @return UUID ID 生成器实例
     */
    @Bean
    @ConditionalOnMissingBean(IdGenerator.class)
    @ConditionalOnProperty(
            prefix = "afg.core.id-generator",
            name = "type",
            havingValue = "UUID")
    public IdGenerator uuidIdGenerator() {
        return new UuidIdGenerator();
    }

    /**
     * NoOp ID 生成器降级实现
     * <p>
     * 当 type=SEGMENT（暂不支持）或无其他 IdGenerator 实现时使用。
     * 使用内存自增计数器，不具备分布式安全性。
     *
     * @return NoOp ID 生成器实例
     */
    @Bean
    @ConditionalOnMissingBean(IdGenerator.class)
    public IdGenerator noOpIdGenerator() {
        return new NoOpIdGenerator();
    }
}
