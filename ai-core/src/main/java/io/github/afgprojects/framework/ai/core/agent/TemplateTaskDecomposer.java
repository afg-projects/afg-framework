package io.github.afgprojects.framework.ai.core.agent;

import io.github.afgprojects.framework.ai.core.api.multiagent.decomposition.*;
import org.jspecify.annotations.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模板任务分解器
 *
 * <p>根据预注册的模板将任务分解为子任务。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class TemplateTaskDecomposer implements TaskDecomposer {

    private final Map<String, DecompositionTemplate> templates = new ConcurrentHashMap<>();

    /**
     * 注册分解模板
     */
    public void registerTemplate(
            String templateId,
            String taskPattern,
            List<SubTask> subTasks,
            Map<String, List<String>> dependencies
    ) {
        log.info("Registering decomposition template: {}", templateId);
        templates.put(templateId, new DecompositionTemplate(templateId, taskPattern, subTasks, dependencies));
    }

    @Override
    @NonNull
    public DecompositionResult decompose(@NonNull TaskDescription task, @NonNull DecompositionContext context) {
        log.debug("Decomposing task: {}", task.name());

        Optional<DecompositionTemplate> matched = findMatchingTemplate(task);

        if (matched.isPresent()) {
            log.info("Found matching template: {}", matched.get().templateId());
            return applyTemplate(matched.get(), task, context);
        }

        log.debug("No matching template found, returning single task");
        return DecompositionResult.singleTask(task);
    }

    @Override
    @NonNull
    public String getName() {
        return "template";
    }

    @Override
    public boolean supports(@NonNull TaskDescription task) {
        return findMatchingTemplate(task).isPresent();
    }

    private Optional<DecompositionTemplate> findMatchingTemplate(TaskDescription task) {
        return templates.values().stream()
                .filter(t -> t.matches(task))
                .findFirst();
    }

    private DecompositionResult applyTemplate(
            DecompositionTemplate template,
            TaskDescription task,
            DecompositionContext context
    ) {
        List<SubTask> subTasks = new ArrayList<>();
        for (SubTask templateTask : template.subTasks()) {
            subTasks.add(new SubTask(
                    UUID.randomUUID().toString(),
                    templateTask.name(),
                    templateTask.description(),
                    templateTask.type(),
                    templateTask.suggestedAgent(),
                    templateTask.parameters(),
                    templateTask.priority(),
                    templateTask.estimatedDuration()
            ));
        }

        return new DecompositionResult(
                subTasks,
                template.dependencies(),
                "template:" + template.templateId(),
                "Matched template: " + template.templateId()
        );
    }

    /**
     * 分解模板
     */
    private record DecompositionTemplate(
            String templateId,
            String taskPattern,
            List<SubTask> subTasks,
            Map<String, List<String>> dependencies
    ) {
        boolean matches(TaskDescription task) {
            return task.name().contains(taskPattern) ||
                   task.description().contains(taskPattern);
        }
    }
}