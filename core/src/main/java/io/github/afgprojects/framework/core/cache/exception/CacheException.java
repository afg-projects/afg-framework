package io.github.afgprojects.framework.core.cache.exception;

import io.github.afgprojects.framework.core.exception.AfgException;

/**
 * 缓存异常
 * <p>
 * 缓存操作相关的异常
 * 错误码范围：17000-17999
 * </p>
 */
public class CacheException extends AfgException {

    /**
     * 缓存错误码
     */
    private static final int CACHE_ERROR_CODE = 17000;

    /**
     * 构造缓存异常
     *
     * @param message 异常消息
     */
    public CacheException(String message) {
        super(CACHE_ERROR_CODE, message);
    }

    /**
     * 构造缓存异常
     *
     * @param message 异常消息
     * @param cause   原因异常
     */
    public CacheException(String message, Throwable cause) {
        super(CACHE_ERROR_CODE, message, cause);
    }

    /**
     * 构造缓存异常
     *
     * @param code    错误码
     * @param message 异常消息
     */
    public CacheException(int code, String message) {
        super(code, message);
    }

    /**
     * 构造缓存异常
     *
     * @param code    错误码
     * @param message 异常消息
     * @param cause   原因异常
     */
    public CacheException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
}