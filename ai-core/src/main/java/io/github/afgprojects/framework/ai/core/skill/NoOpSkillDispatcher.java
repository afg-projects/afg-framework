package io.github.afgprojects.framework.ai.core.skill;

import io.github.afgprojects.framework.ai.core.api.skill.*;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

/**
 * No-op implementation of {@link SkillDispatcher} that returns not-matched results.
 *
 * <p>Used as a fallback when no {@link io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient}
 * is available, ensuring the skill system can still function without an LLM backend.
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class NoOpSkillDispatcher implements SkillDispatcher {

    private final IntentAnalyzer intentAnalyzer;
    private final SkillRegistry skillRegistry;

    public NoOpSkillDispatcher(@NonNull IntentAnalyzer intentAnalyzer, @NonNull SkillRegistry skillRegistry) {
        this.intentAnalyzer = intentAnalyzer;
        this.skillRegistry = skillRegistry;
    }

    @Override
    @NonNull
    public SkillRoutingResult dispatch(@NonNull String input) {
        log.debug("NoOpSkillDispatcher: dispatch input '{}' - no chat client available", input);
        return SkillRoutingResult.notMatched("No chat client available, skill dispatch is disabled");
    }

    @Override
    @NonNull
    public IntentAnalyzer getIntentAnalyzer() {
        return intentAnalyzer;
    }

    @Override
    @NonNull
    public SkillRegistry getRegistry() {
        return skillRegistry;
    }
}
