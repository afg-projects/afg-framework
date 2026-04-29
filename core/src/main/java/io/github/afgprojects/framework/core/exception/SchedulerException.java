package io.github.afgprojects.framework.core.exception;

import org.jspecify.annotations.Nullable;

/**
 * 任务调度异常
 */
public class SchedulerException extends AfgException {

    /** 调度错误 */
    public static final int SCHEDULER_ERROR = 90400;
    /** 任务不存在 */
    public static final int JOB_NOT_FOUND = 90401;
    /** 任务执行失败 */
    public static final int JOB_EXECUTION_ERROR = 90402;
    /** 任务已存在 */
    public static final int JOB_ALREADY_EXISTS = 90403;

    public SchedulerException(String message) {
        super(SCHEDULER_ERROR, message);
    }

    public SchedulerException(String message, @Nullable Throwable cause) {
        super(SCHEDULER_ERROR, message, cause);
    }

    public SchedulerException(int code, String message) {
        super(code, message);
    }

    public SchedulerException(int code, String message, @Nullable Throwable cause) {
        super(code, message, cause);
    }
}
