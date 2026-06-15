package io.github.afgprojects.framework.data.jdbc.safety;

import io.github.afgprojects.framework.data.core.safety.FullTableOperationPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 数据安全配置属性
 *
 * <p>配置前缀：{@code afg.data.safety}
 *
 * <p>示例：
 * <pre>
 * afg:
 *   data:
 *     safety:
 *       full-table-operation-policy: BLOCK
 *       full-table-operation-limit: 1000
 * </pre>
 */
@ConfigurationProperties(prefix = "afg.data.safety")
public class DataSafetyProperties {

    /**
     * 是否启用数据安全功能，默认 true
     *
     * <p>当设置为 false 时，使用 {@link io.github.afgprojects.framework.data.core.safety.NoOpFullTableOperationChecker}，
     * 不做任何全表操作检查。
     */
    private boolean enabled = true;

    /**
     * 全表操作保护策略，默认 BLOCK（阻止全表操作）
     */
    private FullTableOperationPolicy fullTableOperationPolicy = FullTableOperationPolicy.BLOCK;

    /**
     * 全表操作 LIMIT 模式下的限制行数，默认 1000
     *
     * <p>仅在 {@link #fullTableOperationPolicy} 为 {@link FullTableOperationPolicy#LIMIT} 时生效
     */
    private long fullTableOperationLimit = 1000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public FullTableOperationPolicy getFullTableOperationPolicy() {
        return fullTableOperationPolicy;
    }

    public void setFullTableOperationPolicy(FullTableOperationPolicy fullTableOperationPolicy) {
        this.fullTableOperationPolicy = fullTableOperationPolicy;
    }

    public long getFullTableOperationLimit() {
        return fullTableOperationLimit;
    }

    public void setFullTableOperationLimit(long fullTableOperationLimit) {
        this.fullTableOperationLimit = fullTableOperationLimit;
    }
}
