package io.github.afgprojects.framework.ai.agent.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 安全工具配置属性。
 *
 * <p>配置示例：
 * <pre>{@code
 * afg:
 *   ai:
 *     tool:
 *       security:
 *         enabled: true
 *         max-iterations: 10
 *         timeout-ms: 30000
 *         permission-checker:
 *           enabled: true
 *         audit:
 *           enabled: true
 *           table-name: ai_tool_audit
 *         content-safety:
 *           enabled: true
 * }</pre>
 *
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "afg.ai.tool.security")
public class SecureToolProperties {

    /**
     * 是否启用安全工具功能。
     */
    private boolean enabled = true;

    /**
     * 工具执行最大迭代次数。
     */
    private int maxIterations = 10;

    /**
     * 工具执行超时时间（毫秒）。
     */
    private long timeoutMs = 30000;

    /**
     * 权限检查器配置。
     */
    private PermissionCheckerConfig permissionChecker = new PermissionCheckerConfig();

    /**
     * 审计日志配置。
     */
    private AuditConfig audit = new AuditConfig();

    /**
     * 内容安全配置。
     */
    private ContentSafetyConfig contentSafety = new ContentSafetyConfig();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public PermissionCheckerConfig getPermissionChecker() {
        return permissionChecker;
    }

    public void setPermissionChecker(PermissionCheckerConfig permissionChecker) {
        this.permissionChecker = permissionChecker;
    }

    public AuditConfig getAudit() {
        return audit;
    }

    public void setAudit(AuditConfig audit) {
        this.audit = audit;
    }

    public ContentSafetyConfig getContentSafety() {
        return contentSafety;
    }

    public void setContentSafety(ContentSafetyConfig contentSafety) {
        this.contentSafety = contentSafety;
    }

    /**
     * 权限检查器配置。
     */
    public static class PermissionCheckerConfig {

        /**
         * 是否启用权限检查。
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * 审计日志配置。
     */
    public static class AuditConfig {

        /**
         * 是否启用审计日志。
         */
        private boolean enabled = true;

        /**
         * 审计日志表名。
         */
        private String tableName = "ai_tool_audit";

        /**
         * 审计日志保留天数。
         */
        private int retentionDays = 90;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public int getRetentionDays() {
            return retentionDays;
        }

        public void setRetentionDays(int retentionDays) {
            this.retentionDays = retentionDays;
        }
    }

    /**
     * 内容安全配置。
     */
    public static class ContentSafetyConfig {

        /**
         * 是否启用内容安全检查。
         */
        private boolean enabled = true;

        /**
         * 严格模式（遇到风险直接拒绝）。
         */
        private boolean strictMode = false;

        /**
         * 检查类别。
         */
        private java.util.List<String> categories = java.util.List.of(
            "SENSITIVE_WORD",
            "HARMFUL_CONTENT",
            "PII"
        );

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isStrictMode() {
            return strictMode;
        }

        public void setStrictMode(boolean strictMode) {
            this.strictMode = strictMode;
        }

        public java.util.List<String> getCategories() {
            return categories;
        }

        public void setCategories(java.util.List<String> categories) {
            this.categories = categories;
        }
    }
}