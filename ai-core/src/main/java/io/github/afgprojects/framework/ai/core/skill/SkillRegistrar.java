package io.github.afgprojects.framework.ai.core.skill;

import io.github.afgprojects.framework.ai.core.api.skill.SkillDefinition;
import io.github.afgprojects.framework.ai.core.api.skill.SkillRegistry;
import io.github.afgprojects.framework.ai.core.skill.annotation.Skill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.*;

/**
 * @Skill 注解扫描注册器。
 * <p>
 * 在 Spring 上下文刷新完成后，扫描所有标注了 {@link Skill} 注解的方法，
 * 将其转换为 {@link SkillDefinition} 并注册到 {@link SkillRegistry}。
 *
 * @author afg-projects
 * @since 1.0.0
 * @see Skill
 * @see SkillRegistry
 */
@Slf4j
@RequiredArgsConstructor
public class SkillRegistrar {

    private final SkillRegistry skillRegistry;

    /**
     * 监听上下文刷新事件，扫描并注册所有 @Skill 方法。
     *
     * @param event 上下文刷新事件
     */
    @EventListener
    public void onContextRefreshed(ContextRefreshedEvent event) {
        ApplicationContext context = event.getApplicationContext();
        Map<String, Object> beans = context.getBeansWithAnnotation(org.springframework.stereotype.Component.class);

        // Also scan @Service, @Controller beans
        Map<String, Object> serviceBeans = context.getBeansWithAnnotation(org.springframework.stereotype.Service.class);
        Map<String, Object> allBeans = new HashMap<>(beans);
        allBeans.putAll(serviceBeans);

        int registered = 0;
        for (Map.Entry<String, Object> entry : allBeans.entrySet()) {
            registered += scanAndRegister(entry.getValue());
        }

        log.info("SkillRegistrar: scanned and registered {} skill(s) from @Skill annotations", registered);
    }

    /**
     * 扫描单个 Bean 中的 @Skill 注解方法并注册。
     *
     * @param bean Spring Bean 实例
     * @return 注册的 Skill 数量
     */
    int scanAndRegister(Object bean) {
        int count = 0;
        Method[] methods = bean.getClass().getDeclaredMethods();

        for (Method method : methods) {
            Skill skillAnnotation = AnnotationUtils.findAnnotation(method, Skill.class);
            if (skillAnnotation == null) {
                continue;
            }

            SkillDefinition definition = createDefinition(skillAnnotation, method);
            skillRegistry.register(definition);
            count++;

            log.debug("Registered @Skill method: {} -> {}", method.getName(), definition.name());
        }

        return count;
    }

    /**
     * 从 @Skill 注解和方法信息创建 SkillDefinition。
     *
     * @param annotation @Skill 注解实例
     * @param method     标注方法
     * @return SkillDefinition
     */
    SkillDefinition createDefinition(Skill annotation, Method method) {
        // 从方法参数构建输入参数定义
        List<SkillDefinition.InputParameter> inputs = buildInputParameters(method);

        // 从 intentKeywords 构建元数据
        Map<String, Object> metadata = new HashMap<>();
        String[] keywords = annotation.intentKeywords();
        if (keywords.length > 0) {
            metadata.put("intentKeywords", List.of(keywords));
        }
        String category = annotation.category();
        if (!category.isEmpty()) {
            metadata.put("category", category);
        }
        metadata.put("beanMethod", method.getDeclaringClass().getName() + "#" + method.getName());

        return new SkillDefinition(
                annotation.name(),
                annotation.description().isEmpty() ? method.getName() : annotation.description(),
                buildPromptFromMethod(method),
                inputs.isEmpty() ? null : inputs,
                null,
                null,
                metadata.isEmpty() ? null : metadata
        );
    }

    /**
     * 从方法参数构建输入参数定义列表。
     */
    private List<SkillDefinition.InputParameter> buildInputParameters(Method method) {
        java.lang.reflect.Parameter[] parameters = method.getParameters();
        if (parameters.length == 0) {
            return List.of();
        }

        List<SkillDefinition.InputParameter> inputs = new ArrayList<>();
        for (java.lang.reflect.Parameter param : parameters) {
            // 跳过 SkillContext 类型的参数
            if (io.github.afgprojects.framework.ai.core.api.skill.SkillContext.class.isAssignableFrom(param.getType())) {
                continue;
            }

            SkillDefinition.ParameterType paramType = mapParameterType(param.getType());

            inputs.add(new SkillDefinition.InputParameter(
                    param.getName(),
                    null,
                    paramType,
                    true,
                    null,
                    null
            ));
        }

        return inputs;
    }

    /**
     * 从方法签名生成默认提示词模板。
     */
    private String buildPromptFromMethod(Method method) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Execute skill '").append(method.getName()).append("'");
        prompt.append(" with the following inputs: ");

        java.lang.reflect.Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                prompt.append(", ");
            }
            prompt.append("{{").append(parameters[i].getName()).append("}}");
        }

        return prompt.toString();
    }

    /**
     * 将 Java 类型映射到 SkillDefinition.ParameterType。
     */
    private SkillDefinition.ParameterType mapParameterType(Class<?> type) {
        if (type == String.class) {
            return SkillDefinition.ParameterType.STRING;
        } else if (type == Integer.class || type == int.class) {
            return SkillDefinition.ParameterType.INTEGER;
        } else if (type == Double.class || type == double.class
                || type == Float.class || type == float.class) {
            return SkillDefinition.ParameterType.NUMBER;
        } else if (type == Boolean.class || type == boolean.class) {
            return SkillDefinition.ParameterType.BOOLEAN;
        } else if (type.isArray() || Collection.class.isAssignableFrom(type)) {
            return SkillDefinition.ParameterType.ARRAY;
        } else if (Map.class.isAssignableFrom(type)) {
            return SkillDefinition.ParameterType.OBJECT;
        }
        return SkillDefinition.ParameterType.STRING;
    }
}
