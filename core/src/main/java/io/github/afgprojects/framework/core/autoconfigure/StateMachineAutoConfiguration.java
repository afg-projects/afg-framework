package io.github.afgprojects.framework.core.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.core.api.statemachine.LocalStateMachineFactory;
import io.github.afgprojects.framework.core.api.statemachine.NoOpStateMachineFactory;
import io.github.afgprojects.framework.core.api.statemachine.StateMachineFactory;
import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import io.github.afgprojects.framework.core.statemachine.StateMachineScanner;

/**
 * 状态机自动配置
 * <p>
 * 自动配置轻量级状态机功能，包括：
 * <ul>
 *   <li>{@link LocalStateMachineFactory} — 本地内存状态机工厂（默认实现）</li>
 *   <li>{@link StateMachineScanner} — 启动时扫描 {@code @StateMachine} 注解的枚举</li>
 *   <li>{@link NoOpStateMachineFactory} — NoOp 降级实现（状态机功能禁用时）</li>
 * </ul>
 * </p>
 * <p>
 * 配置示例：
 * <pre>
 * afg:
 *   core:
 *     state-machine:
 *       enabled: true
 *       strict-mode: true
 * </pre>
 * </p>
 *
 * @since 1.0.0
 */
@AutoConfiguration(after = AfgAutoConfiguration.class)
@ConditionalOnProperty(prefix = "afg.core.state-machine", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AfgCoreProperties.class)
public class StateMachineAutoConfiguration {

    /**
     * 本地内存状态机工厂
     * <p>
     * 使用 ConcurrentHashMap 存储状态机定义，支持按状态枚举类型和按名称查找。
     * 默认使用严格模式（非法转换抛异常）。
     *
     * @param properties 核心配置属性（从中获取状态机配置）
     * @return 本地状态机工厂实例
     */
    @Bean
    @ConditionalOnMissingBean(StateMachineFactory.class)
    public StateMachineFactory localStateMachineFactory(AfgCoreProperties properties) {
        boolean strictMode = properties.getStateMachine().isStrictMode();
        return new LocalStateMachineFactory(strictMode);
    }

    /**
     * 状态机扫描器
     * <p>
     * 在 Spring 上下文刷新完成后，扫描所有 {@code @StateMachine} 注解的枚举，
     * 解析 {@code @Transition} 注解构建状态机定义，注册到工厂。
     *
     * @param factory 状态机工厂
     * @return 状态机扫描器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public StateMachineScanner stateMachineScanner(StateMachineFactory factory) {
        return new StateMachineScanner(factory);
    }

    /**
     * NoOp 状态机工厂降级实现
     * <p>
     * 当没有其他 {@link StateMachineFactory} 实现时使用。
     * 所有操作均为空操作：getDefinition 返回 null，register 静默忽略。
     * <p>
     * 注意：此 Bean 仅在 {@link LocalStateMachineFactory} 也不满足条件时才会被创建，
     * 正常情况下 {@link LocalStateMachineFactory} 作为默认实现已足够。
     *
     * @return NoOp 状态机工厂实例
     */
    @Bean
    @ConditionalOnMissingBean(StateMachineFactory.class)
    public StateMachineFactory noOpStateMachineFactory() {
        return new NoOpStateMachineFactory();
    }
}
