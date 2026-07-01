package io.github.afgprojects.framework.ai.core.workflow.node.output;

import io.github.afgprojects.framework.ai.core.api.workflow.definition.ParamType;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Log output node - logs data as workflow output.
 *
 * <p>Logs the provided data at a specified log level. Useful for debugging
 * workflows and recording intermediate state.</p>
 *
 * <p>Parameters are declared on {@link Params} so the node is self-describing.</p>
 */
@Slf4j
public class LogOutputNode extends AbstractWorkflowNode<LogOutputNode.Params> {

    public static final String TYPE = "log-output";

    /** Strongly-typed parameters for {@link LogOutputNode}. */
    public record Params(
            @Param(displayName = "Message", description = "The message to log", required = true)
            String message,
            @Param(displayName = "Level", description = "Log level",
                    type = ParamType.ENUM,
                    enumValues = {"DEBUG", "INFO", "WARN", "ERROR"},
                    defaultValue = "INFO")
            String level,
            @Param(displayName = "Data", description = "Additional data to include in the log")
            Object data
    ) {
        /** Effective level, defaulting to INFO. */
        public String effectiveLevel() {
            return level == null || level.isBlank() ? "INFO" : level.toUpperCase();
        }
    }

    /** Output descriptor for {@link LogOutputNode}. */
    public record Output(
            @Out(description = "Whether logged") boolean logged,
            @Out(description = "Log level") String level
    ) {}

    public LogOutputNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        String message = params.message();
        String level = params.effectiveLevel();
        Object data = params.data();

        String logMessage = data != null
                ? message + " | data: " + data
                : message;

        switch (level) {
            case "DEBUG" -> log.debug("LogOutputNode [{}]: {}", getNodeId(), logMessage);
            case "WARN" -> log.warn("LogOutputNode [{}]: {}", getNodeId(), logMessage);
            case "ERROR" -> log.error("LogOutputNode [{}]: {}", getNodeId(), logMessage);
            default -> log.info("LogOutputNode [{}]: {}", getNodeId(), logMessage);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("logged", true);
        result.put("level", level);
        return result;
    }
}
