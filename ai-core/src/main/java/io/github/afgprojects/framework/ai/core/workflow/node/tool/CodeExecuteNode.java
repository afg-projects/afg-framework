package io.github.afgprojects.framework.ai.core.workflow.node.tool;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Code execute node - executes code in a sandboxed environment.
 *
 * <p>Executes user-provided code (JavaScript, Python, etc.) in a sandboxed
 * environment and returns the execution result. Used for custom data
 * transformation and logic that is not covered by built-in nodes.</p>
 *
 * <p>Parameters are declared on {@link Params} so the node is self-describing.</p>
 *
 * <p><strong>Alpha feature:</strong> Sandboxed code execution requires a GraalVM
 * or equivalent runtime integration. Current implementation validates parameters
 * and stores the code for later execution.</p>
 */
@Slf4j
public class CodeExecuteNode extends AbstractWorkflowNode<CodeExecuteNode.Params> {

    public static final String TYPE = "code-execute";

    /** Strongly-typed parameters for {@link CodeExecuteNode}. */
    public record Params(
            @Param(displayName = "Code", description = "The code to execute", required = true)
            String code,
            @Param(displayName = "Language", description = "Programming language", defaultValue = "javascript")
            String language,
            @Param(displayName = "Timeout (ms)", description = "Execution timeout in milliseconds", defaultValue = "5000")
            Long timeoutMs,
            @Param(displayName = "Input", description = "Input data available to the code")
            Object input
    ) {
        /** Effective language, defaulting to javascript. */
        public String effectiveLanguage() {
            return language == null || language.isBlank() ? "javascript" : language;
        }

        /** Effective timeout, defaulting to 5000ms. */
        public long effectiveTimeoutMs() {
            return timeoutMs == null ? 5000L : timeoutMs;
        }
    }

    /** Output descriptor for {@link CodeExecuteNode}. */
    public record Output(
            @Out(description = "Language") String language,
            @Out(description = "Code length") int codeLength,
            @Out(description = "Timeout (ms)") long timeoutMs,
            @Out(description = "Whether has input") boolean hasInput,
            @Out(description = "Whether executed") boolean executed
    ) {}

    public CodeExecuteNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        String code = params.code();
        String language = params.effectiveLanguage();
        long timeoutMs = params.effectiveTimeoutMs();
        Object input = params.input();

        log.debug("CodeExecuteNode [{}] executing {} code ({} chars)", getNodeId(), language, code.length());

        // Alpha: store code for later sandboxed execution
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("language", language);
        result.put("codeLength", code.length());
        result.put("timeoutMs", timeoutMs);
        result.put("hasInput", input != null);
        result.put("executed", false);
        result.put("message", "Code execution requires sandboxed runtime integration (GraalVM/JavaScript engine)");
        return result;
    }
}
