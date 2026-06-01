package io.github.afgprojects.framework.ai.workflow.dsl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.ai.core.workflow.dsl.DslConverter;
import io.github.afgprojects.framework.ai.core.workflow.definition.WorkflowDefinition;

/**
 * Default implementation of DslConverter that composes JsonDslParser and JsonDslGenerator.
 */
public class DefaultDslConverter implements DslConverter {

    private final JsonDslParser parser;
    private final JsonDslGenerator generator;

    public DefaultDslConverter() {
        this(new ObjectMapper());
    }

    public DefaultDslConverter(ObjectMapper objectMapper) {
        this.parser = new JsonDslParser(objectMapper);
        this.generator = new JsonDslGenerator(objectMapper);
    }

    @Override
    public String toJson(WorkflowDefinition workflow) {
        return generator.generate(workflow);
    }

    @Override
    public WorkflowDefinition fromJson(String json) {
        return parser.parse(json);
    }
}
