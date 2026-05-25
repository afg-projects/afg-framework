package io.github.afgprojects.framework.ai.chat.advisor;

import io.github.afgprojects.framework.ai.core.observability.AuditLogger;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.core.Ordered;

import java.util.HashMap;

/**
 * 审计日志 Advisor
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class AuditLoggingAdvisor implements BaseAdvisor {

    private static final Logger log = LoggerFactory.getLogger(AuditLoggingAdvisor.class);

    private final AuditLogger auditLogger;

    public AuditLoggingAdvisor(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @Override
    public @NonNull String getName() {
        return "audit_logging";
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 100;
    }

    @Override
    public @NonNull ChatClientRequest before(@NonNull ChatClientRequest request, @NonNull AdvisorChain chain) {
        var newContext = new HashMap<>(request.context());
        newContext.put("audit_start_time", System.currentTimeMillis());
        return ChatClientRequest.builder()
            .prompt(request.prompt())
            .context(newContext)
            .build();
    }

    @Override
    public @NonNull ChatClientResponse after(@NonNull ChatClientResponse response, @NonNull AdvisorChain chain) {
        var startTime = response.context().get("audit_start_time");
        long responseTimeMs = 0;
        if (startTime instanceof Long time) {
            responseTimeMs = System.currentTimeMillis() - time;
        }

        var userId = (String) response.context().get("userId");
        var chatResponse = response.chatResponse();
        String modelName = null;
        String requestContent = null;
        String responseContent = null;
        AuditLogger.AuditStatus status = AuditLogger.AuditStatus.SUCCESS;

        if (chatResponse != null) {
            if (chatResponse.getMetadata() != null) {
                modelName = chatResponse.getMetadata().getModel();
            }

            var result = chatResponse.getResult();
            if (result != null && result.getOutput() != null) {
                responseContent = result.getOutput().getText();
            }
        }

        requestContent = (String) response.context().get("audit_request_content");

        if (Boolean.TRUE.equals(response.context().get("safety_blocked"))) {
            status = AuditLogger.AuditStatus.REJECTED;
        }

        try {
            auditLogger.log(userId, "chat", modelName != null ? modelName : "unknown",
                requestContent, responseContent, status);
        } catch (Exception e) {
            log.error("Audit logging failed: {}", e.getMessage(), e);
        }

        return response;
    }
}