package io.github.afgprojects.framework.core.feature;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.core.model.exception.BusinessException;
import io.github.afgprojects.framework.core.model.exception.CommonErrorCode;

/**
 * 功能禁用异常
 * <p>
 * 当功能开关关闭且未配置回退方法时抛出
 * </p>
 */
public class FeatureDisabledException extends BusinessException {

    private static final long serialVersionUID = 1L;

    /**
     * 功能名称
     */
    private final String featureName;

    /**
     * 构造函数
     *
     * @param featureName 功能名称
     */
    public FeatureDisabledException(@NonNull String featureName) {
        super(CommonErrorCode.FEATURE_DISABLED, "功能已禁用: " + featureName);
        this.featureName = featureName;
    }

    /**
     * 构造函数
     *
     * @param featureName 功能名称
     * @param message     自定义消息
     */
    public FeatureDisabledException(@NonNull String featureName, @NonNull String message) {
        super(CommonErrorCode.FEATURE_DISABLED, message);
        this.featureName = featureName;
    }

    /**
     * 构造函数
     *
     * @param featureName 功能名称
     * @param message     自定义消息
     * @param cause       原始异常
     */
    public FeatureDisabledException(@NonNull String featureName, @NonNull String message, @Nullable Throwable cause) {
        super(CommonErrorCode.FEATURE_DISABLED, message, cause);
        this.featureName = featureName;
    }

    /**
     * 获取功能名称
     *
     * @return 功能名称
     */
    @NonNull
    public String getFeatureName() {
        return featureName;
    }
}