package io.github.afgprojects.framework.security.casbin;

import org.jspecify.annotations.Nullable;

/**
 * Casbin 配置属性。
 *
 * @since 1.0.0
 */
public class CasbinProperties {

    /**
     * 是否启用 Casbin
     */
    private boolean enabled = false;

    /**
     * 模型文件路径
     */
    @Nullable
    private String modelPath;

    /**
     * 策略文件路径
     */
    @Nullable
    private String policyPath;

    /**
     * 是否使用数据库存储策略
     */
    private boolean useDatabaseStorage = false;

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Nullable
    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(@Nullable String modelPath) {
        this.modelPath = modelPath;
    }

    @Nullable
    public String getPolicyPath() {
        return policyPath;
    }

    public void setPolicyPath(@Nullable String policyPath) {
        this.policyPath = policyPath;
    }

    public boolean isUseDatabaseStorage() {
        return useDatabaseStorage;
    }

    public void setUseDatabaseStorage(boolean useDatabaseStorage) {
        this.useDatabaseStorage = useDatabaseStorage;
    }
}