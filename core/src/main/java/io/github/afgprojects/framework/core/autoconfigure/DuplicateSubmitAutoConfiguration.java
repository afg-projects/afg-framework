package io.github.afgprojects.framework.core.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.core.api.duplicatesubmit.DuplicateSubmitChecker;
import io.github.afgprojects.framework.core.api.duplicatesubmit.LocalDuplicateSubmitChecker;
import io.github.afgprojects.framework.core.api.duplicatesubmit.NoOpDuplicateSubmitChecker;
import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import io.github.afgprojects.framework.core.duplicatesubmit.DuplicateSubmitAspect;

/**
 * 防重复提交自动配置
 * <p>
 * 自动配置防重复提交功能，支持多种存储后端。
 * 默认使用本地内存去重（{@link LocalDuplicateSubmitChecker}），仅对单实例有效。
 * 引入 afg-redis 模块后可自动升级为 Redis 分布式去重。
 * </p>
 * <p>
 * 配置示例：
 * <pre>
 * afg:
 *   core:
 *     duplicate-submit:
 *       enabled: true
 *       key-prefix: "myapp:duplicate-submit"
 *       default-interval: 3000
 *       annotations:
 *         enabled: true
 * </pre>
 * </p>
 *
 * @since 1.0.0
 */
@AutoConfiguration(after = AfgAutoConfiguration.class)
@ConditionalOnProperty(prefix = "afg.core.duplicate-submit", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AfgCoreProperties.class)
public class DuplicateSubmitAutoConfiguration {

    /**
     * 本地内存重复提交检查器
     * <p>
     * 当没有 Redis 等分布式去重后端时，提供基于 Caffeine 的本地去重。
     * 仅对单实例有效，多实例部署时需引入 afg-redis 模块。
     *
     * @return 本地重复提交检查器实例
     */
    @Bean
    @ConditionalOnMissingBean(DuplicateSubmitChecker.class)
    public DuplicateSubmitChecker localDuplicateSubmitChecker() {
        return new LocalDuplicateSubmitChecker();
    }

    /**
     * NoOp 重复提交检查器降级实现
     * <p>
     * 当不需要任何去重检查时使用。所有去重检查总是成功（总是允许请求通过）。
     * <p>
     * 注意：此 Bean 仅在 {@link LocalDuplicateSubmitChecker} 也不满足条件时才会被创建，
     * 正常情况下 {@link LocalDuplicateSubmitChecker} 作为默认实现已足够。
     *
     * @return NoOp 重复提交检查器实例
     */
    @Bean
    @ConditionalOnMissingBean(DuplicateSubmitChecker.class)
    public DuplicateSubmitChecker noOpDuplicateSubmitChecker() {
        return new NoOpDuplicateSubmitChecker();
    }

    /**
     * 配置防重复提交切面
     * <p>
     * 当 afg.core.duplicate-submit.annotations.enabled=true 且存在 DuplicateSubmitChecker bean 时启用
     * </p>
     *
     * @param checker   重复提交检查器
     * @param properties 核心配置属性
     * @return 防重复提交切面实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(DuplicateSubmitChecker.class)
    @ConditionalOnProperty(
            prefix = "afg.core.duplicate-submit.annotations",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public DuplicateSubmitAspect duplicateSubmitAspect(DuplicateSubmitChecker checker,
                                                        AfgCoreProperties properties) {
        return new DuplicateSubmitAspect(checker, properties);
    }
}
