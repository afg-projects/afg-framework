package io.github.afgprojects.framework.ai.core.skill;

import io.github.afgprojects.framework.ai.core.api.skill.SkillContext;
import io.github.afgprojects.framework.ai.core.api.skill.SkillDispatcher;
import io.github.afgprojects.framework.ai.core.api.skill.SkillResult;
import io.github.afgprojects.framework.ai.core.skill.annotation.Skill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @Skill 注解的 AOP 切面，拦截标注了 @Skill 的方法，
 * 构建 {@link SkillContext} 并调用 {@link SkillDispatcher} 执行。
 *
 * <p>拦截逻辑：
 * <ol>
 *   <li>从注解获取 Skill 名称</li>
 *   <li>从方法参数提取输入</li>
 *   <li>构建 SkillContext</li>
 *   <li>直接调用原方法（@Skill 方法本身就是业务实现）</li>
 * </ol>
 *
 * <p>注意：@Skill 标注的方法是 Skill 的业务实现，方法体会正常执行。
 * AOP 切面负责记录执行日志和构建 SkillContext 上下文信息。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class SkillAspect {

    private final @org.jspecify.annotations.Nullable SkillDispatcher skillDispatcher;

    @Around("@annotation(skillAnnotation)")
    public Object aroundSkill(ProceedingJoinPoint joinPoint, Skill skillAnnotation) throws Throwable {
        String skillName = skillAnnotation.name();
        log.debug("SkillAspect: intercepting skill '{}' on method {}", skillName, joinPoint.getSignature().getName());

        long startTime = System.nanoTime();

        // 构建上下文信息
        Map<String, Object> inputs = extractInputs(joinPoint);
        SkillContext context = SkillContext.builder(skillName)
                .inputs(inputs)
                .build();

        // 执行原方法（@Skill 方法本身就是业务实现）
        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;

            if (error != null) {
                log.warn("Skill '{}' execution failed: {} ({}ms)", skillName, error.getMessage(), durationMs);
            } else {
                log.debug("Skill '{}' executed successfully ({}ms)", skillName, durationMs);
            }
        }
    }

    /**
     * 从方法参数提取输入 Map。
     */
    private Map<String, Object> extractInputs(ProceedingJoinPoint joinPoint) {
        Map<String, Object> inputs = new HashMap<>();
        Object[] args = joinPoint.getArgs();

        if (args.length == 0) {
            return inputs;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] parameterNames = signature.getParameterNames();

        for (int i = 0; i < args.length; i++) {
            // 跳过 SkillContext 类型的参数
            if (args[i] instanceof SkillContext) {
                continue;
            }

            String paramName = (parameterNames != null && i < parameterNames.length)
                    ? parameterNames[i]
                    : "arg" + i;
            inputs.put(paramName, args[i]);
        }

        return inputs;
    }
}
