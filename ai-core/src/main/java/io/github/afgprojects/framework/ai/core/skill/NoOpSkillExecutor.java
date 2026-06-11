package io.github.afgprojects.framework.ai.core.skill;

import io.github.afgprojects.framework.ai.core.api.chat.AiChatResponse;
import io.github.afgprojects.framework.ai.core.api.skill.*;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * No-op implementation of {@link SkillExecutor} that returns empty/failure results.
 *
 * <p>Used as a fallback when no {@link io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient}
 * is available, ensuring the skill system can still function without an LLM backend.
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class NoOpSkillExecutor implements SkillExecutor {

    private final SkillRegistry skillRegistry;

    public NoOpSkillExecutor(@NonNull SkillRegistry skillRegistry) {
        this.skillRegistry = skillRegistry;
    }

    @Override
    @NonNull
    public SkillResult execute(@NonNull String name, @NonNull Map<String, Object> inputs) {
        log.debug("NoOpSkillExecutor: execute skill '{}' - no chat client available", name);
        return SkillResult.failure(name, "No chat client available, skill execution is disabled");
    }

    @Override
    @NonNull
    public SkillResult execute(@NonNull SkillContext context) {
        log.debug("NoOpSkillExecutor: execute skill '{}' from context - no chat client available", context.getSkillName());
        return SkillResult.failure(context.getSkillName(), "No chat client available, skill execution is disabled");
    }

    @Override
    @NonNull
    public Flux<SkillResult> executeStream(@NonNull SkillContext context) {
        log.debug("NoOpSkillExecutor: executeStream skill '{}' - no chat client available", context.getSkillName());
        return Flux.just(SkillResult.failure(context.getSkillName(), "No chat client available, skill execution is disabled"));
    }

    @Override
    @NonNull
    public AiChatResponse executeRaw(@NonNull SkillContext context) {
        log.debug("NoOpSkillExecutor: executeRaw skill '{}' - no chat client available", context.getSkillName());
        return AiChatResponse.of("No chat client available, skill execution is disabled");
    }

    @Override
    @NonNull
    public String renderPrompt(@NonNull String prompt, @NonNull SkillContext context) {
        return prompt;
    }

    @Override
    @NonNull
    public SkillRegistry getRegistry() {
        return skillRegistry;
    }

    @Override
    @NonNull
    public List<String> getToolNames(@NonNull SkillDefinition definition) {
        return List.of();
    }
}
