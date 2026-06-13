package io.github.afgprojects.framework.ai.core.workflow.node.output;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Notification node - sends a notification message.
 *
 * <p>Sends a notification through the configured notification channel
 * (email, SMS, webhook, etc.). The notification content and recipients
 * are specified via parameters.</p>
 *
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code message} (required) - notification message content</li>
 *   <li>{@code channel} (optional) - notification channel (email/sms/webhook), defaults to "log"</li>
 *   <li>{@code recipients} (optional) - list of recipient identifiers</li>
 *   <li>{@code subject} (optional) - notification subject (for email)</li>
 *   <li>{@code priority} (optional) - priority level (low/normal/high/urgent), defaults to "normal"</li>
 * </ul>
 *
 * <p><strong>Alpha feature:</strong> Notification channel integration requires
 * external service configuration. Current implementation logs the notification.</p>
 */
@Slf4j
public class NotificationNode extends AbstractWorkflowNode {

    public static final String TYPE = "notification";

    public NotificationNode(String nodeId) {
        super(nodeId, TYPE);
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Map<String, Object> params) {
        String message = getRequiredParam(params, "message");
        String channel = getParam(params, "channel", "log");
        String subject = getParam(params, "subject", null);
        String priority = getParam(params, "priority", "normal");

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

    private static String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() <= maxLen ? str : str.substring(0, maxLen) + "...";
    }
}
