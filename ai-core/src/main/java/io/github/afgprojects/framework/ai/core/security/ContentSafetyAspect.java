package io.github.afgprojects.framework.ai.core.security;

import io.github.afgprojects.framework.ai.core.api.exception.AiException;
import io.github.afgprojects.framework.ai.core.api.security.ContentSafetyChecker;
import io.github.afgprojects.framework.ai.core.security.annotation.ContentSafety;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @ContentSafety 注解的 AOP 切面，拦截标注了 @ContentSafety 的方法，
 * 对输入和输出内容进行安全检查。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class ContentSafetyAspect {

    private final ContentSafetyChecker contentSafetyChecker;

    @Around("@annotation(contentSafety)")
    public Object aroundContentSafety(ProceedingJoinPoint joinPoint, ContentSafety contentSafety) throws Throwable {
        log.debug("ContentSafety aspect: checkInput={}, checkOutput={}",
                contentSafety.checkInput(), contentSafety.checkOutput());

        // 检查输入
        if (contentSafety.checkInput()) {
            Object[] args = joinPoint.getArgs();
            for (Object arg : args) {
                if (arg instanceof String text) {
                    var result = contentSafetyChecker.checkInput(text, SimpleSafetyCheckContext.INSTANCE);
                    if (!result.isSafe() && contentSafety.block()) {
                        throw new AiException("Unsafe content detected in input: " + result.getReason(),
                                AiException.ErrorCodes.CONFIG_INVALID);
                    }
                }
            }
        }

        // 执行方法
        Object result = joinPoint.proceed();

        // 检查输出
        if (contentSafety.checkOutput() && result instanceof String text) {
            var checkResult = contentSafetyChecker.checkOutput(text, SimpleSafetyCheckContext.INSTANCE);
            if (!checkResult.isSafe() && contentSafety.block()) {
                throw new AiException("Unsafe content detected in output: " + checkResult.getReason(),
                        AiException.ErrorCodes.CONFIG_INVALID);
            }
        }

        return result;
    }

    /**
     * 简单的安全检查上下文，使用默认值
     */
    private static class SimpleSafetyCheckContext implements ContentSafetyChecker.SafetyCheckContext {

        static final SimpleSafetyCheckContext INSTANCE = new SimpleSafetyCheckContext();

        @Override
        public @Nullable String getUserId() {
            return null;
        }

        @Override
        public @Nullable String getTenantId() {
            return null;
        }

        @Override
        public @Nullable String getModelName() {
            return null;
        }

        @Override
        public String getOperationType() {
            return "aop-check";
        }

        @Override
        public boolean isStrictMode() {
            return false;
        }

        @Override
        public List<String> getCheckCategories() {
            return List.of();
        }
    }
}
