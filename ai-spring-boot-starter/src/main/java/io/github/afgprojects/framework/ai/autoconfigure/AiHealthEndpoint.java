package io.github.afgprojects.framework.ai.autoconfigure;

import io.github.afgprojects.framework.ai.core.resilience.CircuitBreaker;
import io.github.afgprojects.framework.ai.core.resilience.ResilienceExecutor;
import io.github.afgprojects.framework.ai.core.observability.MetricsCollector;
import io.github.afgprojects.framework.ai.core.observability.Tracer;
import io.github.afgprojects.framework.ai.core.observability.AuditLogger;
import io.github.afgprojects.framework.ai.core.security.ApiKeyManager;
import io.github.afgprojects.framework.ai.core.security.ContentSafetyChecker;
import io.github.afgprojects.framework.ai.core.security.PiiDetector;
import io.github.afgprojects.framework.ai.core.persistence.SessionStore;
import io.github.afgprojects.framework.ai.core.persistence.MessageHistoryStore;
import io.github.afgprojects.framework.ai.core.performance.Cache;
import io.github.afgprojects.framework.ai.core.performance.RateLimiter;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 模块健康检查端点
 *
 * <p>提供 AI 模块各组件的健康状态信息。
 *
 * <p>访问方式：
 * <ul>
 *   <li>GET /actuator/ai-health - 获取所有组件状态</li>
 *   <li>GET /actuator/ai-health/{component} - 获取指定组件状态</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Component
@Endpoint(id = "ai-health")
@ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
@ConditionalOnProperty(prefix = "afg.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiHealthEndpoint {

    private final AiConfigurationProperties properties;
    private final Map<String, Object> components = new HashMap<>();

    public AiHealthEndpoint(AiConfigurationProperties properties,
                            @org.springframework.beans.factory.annotation.Autowired(required = false) ResilienceExecutor resilienceExecutor,
                            @org.springframework.beans.factory.annotation.Autowired(required = false) MetricsCollector metricsCollector,
                            @org.springframework.beans.factory.annotation.Autowired(required = false) Tracer tracer,
                            @org.springframework.beans.factory.annotation.Autowired(required = false) AuditLogger auditLogger,
                            @org.springframework.beans.factory.annotation.Autowired(required = false) ApiKeyManager apiKeyManager,
                            @org.springframework.beans.factory.annotation.Autowired(required = false) ContentSafetyChecker contentSafetyChecker,
                            @org.springframework.beans.factory.annotation.Autowired(required = false) PiiDetector piiDetector,
                            @org.springframework.beans.factory.annotation.Autowired(required = false) SessionStore sessionStore,
                            @org.springframework.beans.factory.annotation.Autowired(required = false) MessageHistoryStore messageHistoryStore,
                            @org.springframework.beans.factory.annotation.Autowired(required = false) Cache<String, Object> cache,
                            @org.springframework.beans.factory.annotation.Autowired(required = false) RateLimiter rateLimiter) {
        this.properties = properties;

        // 注册各组件
        if (resilienceExecutor != null) {
            components.put("resilience", resilienceExecutor);
        }
        if (metricsCollector != null) {
            components.put("metrics", metricsCollector);
        }
        if (tracer != null) {
            components.put("tracer", tracer);
        }
        if (auditLogger != null) {
            components.put("audit", auditLogger);
        }
        if (apiKeyManager != null) {
            components.put("apiKeyManager", apiKeyManager);
        }
        if (contentSafetyChecker != null) {
            components.put("contentSafety", contentSafetyChecker);
        }
        if (piiDetector != null) {
            components.put("piiDetector", piiDetector);
        }
        if (sessionStore != null) {
            components.put("sessionStore", sessionStore);
        }
        if (messageHistoryStore != null) {
            components.put("messageHistory", messageHistoryStore);
        }
        if (cache != null) {
            components.put("cache", cache);
        }
        if (rateLimiter != null) {
            components.put("rateLimiter", rateLimiter);
        }
    }

    @ReadOperation
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();

        health.put("status", "UP");
        health.put("enabled", properties.isEnabled());
        health.put("components", getComponentStatuses());

        return health;
    }

    @ReadOperation
    public Map<String, Object> health(@Selector String component) {
        Map<String, Object> health = new HashMap<>();

        Object comp = components.get(component);
        if (comp == null) {
            health.put("status", "UNKNOWN");
            health.put("message", "Component not found: " + component);
        } else {
            health.put("status", "UP");
            health.put("name", component);
            health.put("details", getComponentDetails(comp));
        }

        return health;
    }

    private Map<String, Object> getComponentStatuses() {
        Map<String, Object> statuses = new HashMap<>();

        for (Map.Entry<String, Object> entry : components.entrySet()) {
            statuses.put(entry.getKey(), Map.of(
                    "status", "UP",
                    "type", entry.getValue().getClass().getSimpleName()
            ));
        }

        return statuses;
    }

    private Map<String, Object> getComponentDetails(Object component) {
        Map<String, Object> details = new HashMap<>();

        details.put("type", component.getClass().getSimpleName());

        if (component instanceof ResilienceExecutor) {
            ResilienceExecutor executor = (ResilienceExecutor) component;
            details.put("circuitBreakerState", executor.getCircuitBreaker().getState().name());
        } else if (component instanceof MetricsCollector) {
            MetricsCollector collector = (MetricsCollector) component;
            details.put("totalRequests", collector.getSummary().getTotalRequests());
        } else if (component instanceof Cache) {
            @SuppressWarnings("unchecked")
            Cache<String, Object> cache = (Cache<String, Object>) component;
            details.put("size", cache.size());
            details.put("hitRate", cache.getStats().getHitRate());
        } else if (component instanceof SessionStore) {
            SessionStore store = (SessionStore) component;
            details.put("totalSessions", store.getSessionHistory(null, null, null, Integer.MAX_VALUE).size());
        }

        return details;
    }
}