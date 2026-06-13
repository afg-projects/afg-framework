package io.github.afgprojects.framework.ai.core.workflow.node.tool;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
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
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code code} (required) - the code to execute</li>
 *   <li>{@code language} (optional) - programming language, defaults to "javascript"</li>
 *   <li>{@code timeoutMs} (optional) - execution timeout, defaults to 5000</li>
 *   <li>{@code input} (optional) - input data available to the code</li>
 * </ul>
 *
 * <p><strong>Alpha feature:</strong> Sandboxed code execution requires a GraalVM
 * or equivalent runtime integration. Current implementation validates parameters
 * and stores the code for later execution.</p>
 */
@Slf4j
public class CodeExecuteNode extends AbstractWorkflowNode {

    public static final String TYPE = "code-execute";

    public CodeExecuteNode(String nodeId) {
        super(nodeId, TYPE);
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Map<String, Object> params) {
        String code = getRequiredParam(params, "code");
        String language = getParam(params, "language", "javascript");
        long timeoutMs = getLongParam(params, "timeoutMs", 5000L);
        Object input = params.get("input");

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

    private String getRequiredParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Required parameter '" + key + "' is missing");
        }
        return value.toString();
    }

    private String getParam(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private long getLongParam(Map<String, Object> params, String key, long defaultValue) {
        Object value = params.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number num) return num.longValue();
        return Long.parseLong(value.toString());
    }
}
