package io.github.afgprojects.framework.core.duplicatesubmit.exception;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;

/**
 * 重复提交异常
 * <p>
 * 当在去重间隔内检测到重复请求时抛出。
 * </p>
 *
 * @since 1.0.0
 */
public class DuplicateSubmitException extends BusinessException {

    private static final long serialVersionUID = 1L;

    /**
     * 重复提交键
     */
    private final String submitKey;

    /**
     * 构造重复提交异常
     *
     * @param submitKey 去重键
     */
    public DuplicateSubmitException(String submitKey) {
        super(CommonErrorCode.DUPLICATE_SUBMIT);
        this.submitKey = submitKey;
    }

    /**
     * 构造重复提交异常
     *
     * @param submitKey 去重键
     * @param message   自定义错误消息
     */
    public DuplicateSubmitException(String submitKey, String message) {
        super(CommonErrorCode.DUPLICATE_SUBMIT, message);
        this.submitKey = submitKey;
    }

    /**
     * 构造重复提交异常
     *
     * @param submitKey 去重键
     * @param message   自定义错误消息
     * @param cause     原始异常
     */
    public DuplicateSubmitException(String submitKey, String message, Throwable cause) {
        super(CommonErrorCode.DUPLICATE_SUBMIT, message, cause);
        this.submitKey = submitKey;
    }

    /**
     * 获取去重键
     *
     * @return 去重键
     */
    public String getSubmitKey() {
        return submitKey;
    }
}
