package io.github.afgprojects.framework.ai.core.workflow.node.output;

import io.github.afgprojects.framework.ai.core.api.workflow.definition.ParamType;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Notification node - sends a notification message.
 *
 * <p>Sends a notification through the configured notification channel
 * (email, SMS, webhook, etc.). The notification content and recipients
 * are specified via parameters.</p>
 *
 * <p>Parameters are declared on {@link Params} so the node is self-describing.</p>
 *
 * <p><strong>Alpha feature:</strong> Notification channel integration requires
 * external service configuration. Current implementation logs the notification.</p>
 */
@Slf4j
public class NotificationNode extends AbstractWorkflowNode<NotificationNode.Params> {

    public static final String TYPE = "notification";

    /** Strongly-typed parameters for {@link NotificationNode}. */
    public record Params(
            @Param(displayName = "Message", description = "Notification message content", required = true)
            String message,
            @Param(displayName = "Channel", description = "Notification channel",
                    type = ParamType.ENUM,
                    enumValues = {"log", "email", "sms", "webhook"},
                    defaultValue = "log")
            String channel,
            @Param(displayName = "Recipients", description = "List of recipient identifiers")
            List<String> recipients,
            @Param(displayName = "Subject", description = "Notification subject (for email)")
            String subject,
            @Param(displayName = "Priority", description = "Priority level", defaultValue = "normal")
            String priority
    ) {
        /** Effective channel, defaulting to "log". */
        public String effectiveChannel() {
            return channel == null || channel.isBlank() ? "log" : channel;
        }

        /** Effective priority, defaulting to "normal". */
        public String effectivePriority() {
            return priority == null || priority.isBlank() ? "normal" : priority;
        }
    }

    /** Output descriptor for {@link NotificationNode}. */
    public record Output(
            @Out(description = "Channel") String channel,
            @Out(description = "Subject") String subject,
            @Out(description = "Message length") int messageLength,
            @Out(description = "Priority") String priority,
            @Out(description = "Whether sent") boolean sent
    ) {}

    public NotificationNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        String message = params.message();
        String channel = params.effectiveChannel();
        String subject = params.subject();
        String priority = params.effectivePriority();

        log.info("NotificationNode [{}] sending via {}: [{}] {}", getNodeId(), channel,
                subject != null ? subject : "", truncate(message, 200));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("channel", channel);
        result.put("subject", subject);
        result.put("messageLength", message.length());
        result.put("priority", priority);
        result.put("sent", true);
        return result;
    }

    private static String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() <= maxLen ? str : str.substring(0, maxLen) + "...";
    }
}
