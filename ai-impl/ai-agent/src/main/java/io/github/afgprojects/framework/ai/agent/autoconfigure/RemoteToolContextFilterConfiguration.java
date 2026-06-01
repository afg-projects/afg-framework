package io.github.afgprojects.framework.ai.agent.autoconfigure;

import io.github.afgprojects.framework.ai.core.tool.RemoteToolContextHolder;
import io.github.afgprojects.framework.ai.core.tool.ToolContext;
import io.github.afgprojects.framework.ai.core.tool.remote.ToolContextHeaders;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.*;

/**
 * 远程工具上下文过滤器配置。
 *
 * <p>保存原始 HTTP 请求头，用于透传认证信息到远程工具服务。
 *
 * <h2>安全设计</h2>
 * <p>采用透传认证请求头的方式，而不是直接传递用户信息：
 * <ul>
 *   <li>保存原始 Authorization、Cookie 等认证请求头</li>
 *   <li>远程服务通过验证这些请求头获取真实用户身份</li>
 *   <li>避免直接传递 userId、tenantId 被伪造的风险</li>
 * </ul>
 *
 * <h2>辅助信息</h2>
 * <p>仅传递非敏感的辅助信息：
 * <ul>
 *   <li>X-AFG-Request-Id - 请求追踪 ID</li>
 *   <li>X-AFG-Session-Id - AI 会话 ID</li>
 *   <li>X-AFG-Client-Ip - 客户端 IP（用于审计）</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "afg.ai.tool.discovery", name = "enabled", havingValue = "true")
public class RemoteToolContextFilterConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RemoteToolContextFilterConfiguration.class);

    /**
     * Servlet 远程工具上下文过滤器。
     */
    @Bean
    @ConditionalOnClass(name = "jakarta.servlet.Filter")
    public RemoteToolContextFilter remoteToolContextFilter() {
        log.info("Creating remote tool context filter");
        return new RemoteToolContextFilter();
    }

    /**
     * Servlet 过滤器实现。
     *
     * <p>保存原始认证请求头，供远程工具调用时透传。
     */
    public static class RemoteToolContextFilter implements Filter {

        @Override
        public void doFilter(
                @NonNull ServletRequest request,
                @NonNull ServletResponse response,
                @NonNull FilterChain chain) throws IOException, ServletException {

            if (!(request instanceof HttpServletRequest httpRequest)) {
                chain.doFilter(request, response);
                return;
            }

            // 提取原始请求头（用于透传认证）
            Map<String, String> originalHeaders = extractOriginalHeaders(httpRequest);

            // 提取辅助信息
            String requestId = getHeader(httpRequest, ToolContextHeaders.REQUEST_ID);
            String sessionId = getHeader(httpRequest, ToolContextHeaders.SESSION_ID);
            String clientIp = getHeader(httpRequest, ToolContextHeaders.CLIENT_IP);
            String toolCallId = getHeader(httpRequest, ToolContextHeaders.TOOL_CALL_ID);
            String sourceService = getHeader(httpRequest, ToolContextHeaders.SOURCE_SERVICE);

            // 构建上下文（仅包含辅助信息，用户身份由认证请求头验证）
            ToolContext context = ToolContext.builder()
                .requestId(requestId)
                .sessionId(sessionId)
                .clientIp(clientIp != null ? clientIp : httpRequest.getRemoteAddr())
                .originalHeaders(originalHeaders)
                .attribute("toolCallId", toolCallId)
                .attribute("sourceService", sourceService)
                .build();

            RemoteToolContextHolder.setContext(context);
            log.debug("Remote tool context set with auth headers: {}",
                ToolContextHeaders.getAuthType(originalHeaders));

            try {
                chain.doFilter(request, response);
            } finally {
                RemoteToolContextHolder.clear();
            }
        }

        /**
         * 提取原始 HTTP 请求头。
         */
        private Map<String, String> extractOriginalHeaders(HttpServletRequest request) {
            Map<String, String> headers = new HashMap<>();

            // 提取认证相关请求头
            extractHeaderIfPresent(request, ToolContextHeaders.AUTHORIZATION, headers);
            extractHeaderIfPresent(request, ToolContextHeaders.COOKIE, headers);
            extractHeaderIfPresent(request, ToolContextHeaders.X_AUTH_TOKEN, headers);
            extractHeaderIfPresent(request, ToolContextHeaders.X_API_KEY, headers);

            return headers;
        }

        /**
         * 提取请求头（如果存在）。
         */
        private void extractHeaderIfPresent(HttpServletRequest request, String headerName, Map<String, String> target) {
            String value = request.getHeader(headerName);
            if (value != null && !value.isBlank()) {
                target.put(headerName, value);
            }
        }

        /**
         * 获取请求头（不区分大小写）。
         */
        private String getHeader(HttpServletRequest request, String headerName) {
            String value = request.getHeader(headerName);
            if (value == null) {
                // 尝试小写形式
                value = request.getHeader(headerName.toLowerCase());
            }
            return value;
        }
    }
}
