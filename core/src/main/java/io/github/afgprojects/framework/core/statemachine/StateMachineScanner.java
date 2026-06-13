package io.github.afgprojects.framework.core.statemachine;

import java.lang.reflect.Method;

import io.github.afgprojects.framework.core.api.statemachine.LocalStateMachineFactory;
import io.github.afgprojects.framework.core.api.statemachine.StateMachineFactory;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * 状态机扫描器
 * <p>
 * 在 Spring 上下文刷新完成后，扫描所有注册为 Bean 的枚举类中的 {@link StateMachine} 注解，
 * 解析 {@link Transition} 注解构建 {@link StateMachineDefinition}，
 * 并注册到 {@link StateMachineFactory}。
 * </p>
 * <p>
 * 扫描逻辑：
 * <ol>
 *   <li>遍历 Spring 容器中所有 Bean</li>
 *   <li>检查 Bean 的 Class 是否为枚举且带有 {@code @StateMachine} 注解</li>
 *   <li>解析枚举类中所有带 {@code @Transition} 注解的方法</li>
 *   <li>构建 {@link StateMachineDefinition} 并注册到工厂</li>
 * </ol>
 * </p>
 * <p>
 * 注意：用户需要将 {@code @StateMachine} 枚举注册为 Spring Bean（通过 {@code @Component} 或 {@code @Bean}），
 * 否则扫描器无法发现它。对于未注册为 Bean 的枚举，用户可以通过
 * {@link StateMachineFactory#register(StateMachineDefinition)} 手动注册。
 * </p>
 *
 * @since 1.0.0
 */
@Slf4j
public class StateMachineScanner implements ApplicationListener<ContextRefreshedEvent> {

    private final StateMachineFactory factory;

    /**
     * 构造函数
     *
     * @param factory 状态机工厂
     */
    public StateMachineScanner(StateMachineFactory factory) {
        this.factory = factory;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (factory instanceof LocalStateMachineFactory) {
            scanAndRegister(event);
        }
    }

    private void scanAndRegister(ContextRefreshedEvent event) {
        String[] beanNames = event.getApplicationContext().getBeanNamesForType(Enum.class);
        int count = 0;
        for (String beanName : beanNames) {
            Object bean = event.getApplicationContext().getBean(beanName);
            Class<?> beanClass = bean.getClass();

            // 检查是否为枚举类且带有 @StateMachine 注解
            if (!beanClass.isEnum()) {
                continue;
            }
            StateMachine stateMachineAnnotation = beanClass.getAnnotation(StateMachine.class);
            if (stateMachineAnnotation == null) {
                continue;
            }

            // 解析枚举类
            Enum<?>[] enumConstants = (Enum<?>[]) beanClass.getEnumConstants();

            // 构建定义
            String machineName = stateMachineAnnotation.name().isEmpty()
                    ? beanClass.getSimpleName()
                    : stateMachineAnnotation.name();

            @SuppressWarnings("rawtypes")
            StateMachineDefinition.Builder definitionBuilder = StateMachineDefinition.builder()
                    .name(machineName)
                    .entityType(stateMachineAnnotation.entity())
                    .stateTypeRaw(beanClass);

            // 为所有枚举常量注册状态
            for (Enum<?> constant : enumConstants) {
                definitionBuilder.stateRaw(constant);
            }

            // 解析 @Transition 注解
            for (Method method : beanClass.getDeclaredMethods()) {
                Transition transitionAnnotation = method.getAnnotation(Transition.class);
                if (transitionAnnotation == null) {
                    continue;
                }

                String eventName = transitionAnnotation.event().isEmpty()
                        ? method.getName()
                        : transitionAnnotation.event();

                // 解析目标状态
                Enum<?> toState = resolveEnumByName(enumConstants, transitionAnnotation.to());
                if (toState == null) {
                    log.warn("状态机 [{}] 的 @Transition 方法 [{}] 引用了不存在的目标状态: {}",
                            machineName, method.getName(), transitionAnnotation.to());
                    continue;
                }

                // 为每个 from 状态创建一个 TransitionDefinition
                for (String fromName : transitionAnnotation.from()) {
                    Enum<?> fromState = resolveEnumByName(enumConstants, fromName);
                    if (fromState == null) {
                        log.warn("状态机 [{}] 的 @Transition 方法 [{}] 引用了不存在的源状态: {}",
                                machineName, method.getName(), fromName);
                        continue;
                    }

                    TransitionDefinition transitionDef = new TransitionDefinition(
                            fromState,
                            toState,
                            eventName,
                            method.getName()
                    );
                    definitionBuilder.transitionRaw(transitionDef);
                }
            }

            StateMachineDefinition definition = definitionBuilder.build();
            factory.register(definition);
            count++;
        }

        if (count > 0) {
            log.info("扫描并注册了 {} 个状态机定义", count);
        }
    }

    /**
     * 根据名称查找枚举常量
     *
     * @param enumConstants 枚举常量数组
     * @param name          枚举常量名称
     * @return 匹配的枚举常量，如果不存在返回 null
     */
    private Enum<?> resolveEnumByName(Enum<?>[] enumConstants, String name) {
        for (Enum<?> constant : enumConstants) {
            if (constant.name().equals(name)) {
                return constant;
            }
        }
        return null;
    }
}
