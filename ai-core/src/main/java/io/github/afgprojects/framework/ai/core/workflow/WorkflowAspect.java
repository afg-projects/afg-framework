package io.github.afgprojects.framework.ai.core.workflow;

import io.github.afgprojects.framework.ai.core.api.workflow.definition.WorkflowDefinition;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.DagEngine;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.DagResult;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.api.exception.AiException;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Workflow;
import io.github.afgprojects.framework.ai.core.workflow.engine.DefaultExecutionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.Map;
import java.util.function.Function;

/**
 * @Workflow 注解的 AOP 切面，拦截标注了 @Workflow 的方法，自动执行指定的工作流。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class WorkflowAspect {

    private final DagEngine dagEngine;
    private final Function<String, WorkflowDefinition> workflowDefinitionResolver;

    @Around("@annotation(workflow)")
    public Object aroundWorkflow(ProceedingJoinPoint joinPoint, Workflow workflow) throws Throwable {
        log.debug("Workflow aspect: executing workflow {}", workflow.value());

        // 根据注解中的工作流定义ID查找工作流定义
        WorkflowDefinition workflowDefinition = workflowDefinitionResolver.apply(workflow.value());
        if (workflowDefinition == null) {
            throw new AiException("Workflow definition not found: " + workflow.value(),
                    AiException.ErrorCodes.CONFIG_MISSING);
        }

        // 从方法参数提取变量
        Object[] args = joinPoint.getArgs();
        Map<String, Object> variables = args.length > 0 && args[0] instanceof Map
                ? (Map<String, Object>) args[0]
                : Map.of();

        // 创建执行上下文
        ExecutionContext context = new DefaultExecutionContext(
                workflow.value(),
                null,
                null,
                variables
        );

        // 执行工作流
        DagResult result = dagEngine.execute(workflowDefinition, context);

        if (workflow.async()) {
            // 异步执行时返回原始方法结果
            return joinPoint.proceed();
        }

        return result;
    }
}
