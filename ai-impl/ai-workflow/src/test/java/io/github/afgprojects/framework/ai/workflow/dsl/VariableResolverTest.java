package io.github.afgprojects.framework.ai.workflow.dsl;

import io.github.afgprojects.framework.ai.core.workflow.engine.NodeOutput;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VariableResolverTest {

    private final DefaultVariableResolver resolver = new DefaultVariableResolver();

    @Test
    void resolveSimpleVariable() {
        Map<String, NodeOutput> outputs = Map.of(
            "start_1", NodeOutput.of(Map.of("user_input", "hello world"))
        );

        Object result = resolver.resolve("${start_1.user_input}", outputs);

        assertEquals("hello world", result);
    }

    @Test
    void renderTemplateWithMultipleVariables() {
        Map<String, NodeOutput> outputs = Map.of(
            "start_1", NodeOutput.of(Map.of("user_input", "What is AI?")),
            "llm_1", NodeOutput.of(Map.of("result", "AI is artificial intelligence"))
        );

        String template = "User asked: ${start_1.user_input}\nAnswer: ${llm_1.result}";
        String rendered = resolver.renderTemplate(template, outputs);

        assertEquals("User asked: What is AI?\nAnswer: AI is artificial intelligence", rendered);
    }

    @Test
    void missingVariableReturnsNull() {
        Map<String, NodeOutput> outputs = Map.of(
            "start_1", NodeOutput.of(Map.of("user_input", "hello"))
        );

        Object result = resolver.resolve("${missing_node.key}", outputs);

        assertNull(result);
    }

    @Test
    void renderTemplateWithMissingVariableLeavesUnresolved() {
        Map<String, NodeOutput> outputs = Map.of(
            "start_1", NodeOutput.of(Map.of("user_input", "hello"))
        );

        String template = "Input: ${start_1.user_input}, Missing: ${missing.key}";
        String rendered = resolver.renderTemplate(template, outputs);

        assertEquals("Input: hello, Missing: ${missing.key}", rendered);
    }

    @Test
    void resolveWithNoVariableSyntax() {
        Map<String, NodeOutput> outputs = Map.of();

        Object result = resolver.resolve("plain text", outputs);

        assertNull(result);
    }

    @Test
    void renderTemplateWithNoVariables() {
        Map<String, NodeOutput> outputs = Map.of();

        String template = "plain text with no variables";
        String rendered = resolver.renderTemplate(template, outputs);

        assertEquals("plain text with no variables", rendered);
    }
}
