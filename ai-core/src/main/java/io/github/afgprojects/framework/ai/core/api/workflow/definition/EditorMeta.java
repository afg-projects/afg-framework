package io.github.afgprojects.framework.ai.core.api.workflow.definition;

/**
 * Editor-facing metadata for a workflow node type, used by the front-end
 * workflow canvas to render the node panel and canvas nodes (icon, color,
 * in-category sort order). All fields are optional; consumers fall back to
 * category-level defaults when a field is absent.
 *
 * @author afg-projects
 * @since 1.0.0
 */
public record EditorMeta(String icon, String color, Integer order) {

    /** Empty metadata sentinel (all fields null). */
    public static final EditorMeta EMPTY = new EditorMeta(null, null, null);

    public static EditorMeta of(String icon, String color) {
        return new EditorMeta(icon, color, null);
    }

    public static EditorMeta of(String icon, String color, int order) {
        return new EditorMeta(icon, color, order);
    }
}
