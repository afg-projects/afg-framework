package io.github.afgprojects.framework.core.web.security.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

import io.github.afgprojects.framework.core.model.exception.BusinessException;
import io.github.afgprojects.framework.core.model.exception.CommonErrorCode;
import lombok.extern.slf4j.Slf4j;

/**
 * 安全过滤器抽象基类
 * 提供参数检查的通用逻辑
 */
@Slf4j
public abstract class AbstractSecurityFilter extends OncePerRequestFilter {

    /**
     * 获取安全检查器
     *
     * @return 安全检查器实例
     */
    protected abstract SecurityChecker getChecker();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        SecurityChecker checker = getChecker();

        if (checker.needsCheck(request)) {
            checkParameters(request, checker);
        }

        filterChain.doFilter(request, response);
    }

    private void checkParameters(HttpServletRequest request, SecurityChecker checker) {
        java.util.Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String[] values = request.getParameterValues(paramName);
            if (values != null) {
                for (String value : values) {
                    if (checker.containsThreat(value)) {
                        log.warn("Security threat detected by {}: param={}", checker.getName(), paramName);
                        throw new BusinessException(CommonErrorCode.PARAM_ERROR, "请求参数包含不安全内容: " + paramName);
                    }
                }
            }
        }
    }
}
