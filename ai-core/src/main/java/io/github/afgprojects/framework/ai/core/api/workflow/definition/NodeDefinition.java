package io.github.afgprojects.framework.ai.core.api.workflow.definition;

import java.util.Map;
import java.util.Set;

public interface NodeDefinition {
    String getType();
    String getDisplayName();

    /**
     * Localized (Chinese) display name for the front-end canvas. Returns
     * {@code null} when not provided; consumers fall back to {@link #getDisplayName()}.
     */
    default String getDisplayNameZh() {
        return null;
    }

    String getCategory();
    Map<String, ParamSchema> getParamSchema();
    Map<String, OutputSchema> getOutputSchema();

    /**
     * Editor-facing metadata (icon / color / order) for the front-end canvas.
     * Returns empty metadata by default; node registrars override to supply
     * concrete values.
     */
    default EditorMeta getEditorMeta() {
        return EditorMeta.EMPTY;
    }

    default Set<String> getSourceAnchors() {
        return Set.of("output");
    }

    default Set<String> getTargetAnchors() {
        return Set.of("input");
    }
}
