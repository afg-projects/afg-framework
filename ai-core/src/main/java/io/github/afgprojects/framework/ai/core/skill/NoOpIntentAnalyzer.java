package io.github.afgprojects.framework.ai.core.skill;

import io.github.afgprojects.framework.ai.core.api.skill.*;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

/**
 * No-op implementation of {@link IntentAnalyzer} that returns empty analysis results.
 *
 * <p>Used as a fallback when no {@link io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient}
 * is available, ensuring the skill system can still function without an LLM backend.
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class NoOpIntentAnalyzer implements IntentAnalyzer {

    @Override
    @NonNull
    public IntentResult analyze(@NonNull String input, @NonNull SkillContext context) {
        log.debug("NoOpIntentAnalyzer: analyze input '{}' - no chat client available", input);
        return IntentResult.empty();
    }

    @Override
    @NonNull
    public IntentResult analyze(@NonNull String input) {
        log.debug("NoOpIntentAnalyzer: analyze input '{}' - no chat client available", input);
        return IntentResult.empty();
    }
}
