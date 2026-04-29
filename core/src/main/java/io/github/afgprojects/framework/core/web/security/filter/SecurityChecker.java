package io.github.afgprojects.framework.core.web.security.filter;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 安全检查器接口
 * 用于检测请求参数中的安全问题
 */
public interface SecurityChecker {

    /**
     * 获取检查器名称
     *
     * @return 检查器名称
     */
    String getName();

    /**
     * 检查输入是否包含安全问题
     *
     * @param input 输入字符串
     * @return 如果包含安全问题返回 true
     */
    boolean containsThreat(String input);

    /**
     * 判断请求是否需要检查
     * 默认检查所有请求
     *
     * @param request HTTP 请求
     * @return 如果需要检查返回 true
     */
    default boolean needsCheck(HttpServletRequest request) {
        return true;
    }
}
