package io.github.afgprojects.framework.ai.core.workflow.node;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.afgprojects.framework.ai.core.api.workflow.definition.ParamSchema;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.ParamType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Binds a runtime parameter map to a node's strongly-typed params record,
 * driven by the node's declared {@link ParamSchema}.
 *
 * <p>Responsibilities, in order:</p>
 * <ol>
 *   <li>required check — absent required param with no default →
 *       {@link ParamBindingException}</li>
 *   <li>default fill — absent param with a declared default uses the default</li>
 *   <li>enum check — ENUM-typed value must be one of
 *       {@link ParamSchema#enumValues()}</li>
 *   <li>type conversion — delegate to Jackson {@code convertValue} to assemble
 *       the target record, which handles nested maps / lists / primitives</li>
 * </ol>
 *
 * <p>Binding failures carry nodeId + paramName so the engine can surface a
 * precisely-located error rather than an opaque {@link ClassCastException}.</p>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class ParamBinder {

    private final ObjectMapper objectMapper;

    public ParamBinder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParamBinder() {
        this(new ObjectMapper());
    }

    /**
     * Bind {@code params} to a target record of type {@code P}.
     *
     * @param params     raw runtime parameters (never null; caller passes {@code Map.of()} for empty)
     * @param schema     the node's parameter schema, keyed by param name
     * @param targetType the params record class
     * @param nodeId     the node id, for error attribution
     * @return a populated {@code P} instance
     * @throws ParamBindingException if a required param is missing or a value
     *                               cannot be converted / is an illegal enum
     */
    public <P> P bind(Map<String, Object> params,
                      Map<String, ParamSchema> schema,
                      Class<P> targetType,
                      String nodeId) {
        Map<String, Object> resolved = new LinkedHashMap<>(params);

        // 1+2: required check & default fill, against the declared schema.
        for (Map.Entry<String, ParamSchema> entry : schema.entrySet()) {
            String name = entry.getKey();
            ParamSchema ps = entry.getValue();
            boolean present = resolved.containsKey(name) && resolved.get(name) != null;
            if (!present) {
                if (ps.required() && ps.defaultValue() == null) {
                    throw new ParamBindingException(nodeId, name, "required parameter is missing");
                }
                if (ps.defaultValue() != null) {
                    resolved.put(name, ps.defaultValue());
                }
            }
        }

        // 3: enum validation up front (Jackson would accept any string).
        for (Map.Entry<String, ParamSchema> entry : schema.entrySet()) {
            ParamSchema ps = entry.getValue();
            if (ps.type() != ParamType.ENUM) {
                continue;
            }
            Object value = resolved.get(entry.getKey());
            if (value == null) {
                continue;
            }
            Set<String> allowed = ps.enumValues() == null ? Set.of()
                    : Set.of(ps.enumValues().split(","));
            if (!allowed.isEmpty() && !allowed.contains(value.toString())) {
                throw new ParamBindingException(nodeId, entry.getKey(),
                        "value '" + value + "' is not one of " + allowed);
            }
        }

        // 4: assemble the record via Jackson.
        try {
            return objectMapper.convertValue(resolved, targetType);
        } catch (IllegalArgumentException e) {
            // Find the first schema param whose value likely failed, for a
            // located message; fall back to a generic conversion error.
            String suspect = resolved.keySet().stream()
                    .filter(schema::containsKey)
                    .findFirst()
                    .orElse("unknown");
            throw new ParamBindingException(nodeId, suspect,
                    "cannot convert to " + targetType.getSimpleName(), e);
        }
    }
}
