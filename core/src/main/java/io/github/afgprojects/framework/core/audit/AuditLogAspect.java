package io.github.afgprojects.framework.core.audit;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.afgprojects.framework.core.web.context.AfgRequestContextHolder;

/**
 * 审计日志切面
 * <p>
 * 拦截 @{@link Audited} 注解的方法，自动记录操作审计日志
 * </p>
 *
 * <p>功能特性：</p>
 * <ul>
 *   <li>自动获取用户、租户、追踪等上下文信息</li>
 *   <li>支持敏感字段脱敏</li>
 *   <li>记录操作耗时和结果</li>
 *   <li>异常时记录失败信息</li>
 * </ul>
 */
@Aspect
@SuppressWarnings({
    "PMD.SignatureDeclareThrowsException",
    "PMD.AvoidThrowingRawExceptionTypes",
    "PMD.AvoidCatchingGenericException"
})
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);

    private final AuditLogStorage storage;
    private final SensitiveFieldProcessor sensitiveFieldProcessor;
    private final AuditLogSerializer serializer;
    private final TargetExpressionResolver targetResolver;

    /**
     * 构造函数
     *
     * @param storage    审计日志存储
     * @param properties 审计日志配置
     */
    public AuditLogAspect(@NonNull AuditLogStorage storage, @NonNull AuditLogProperties properties) {
        this.storage = storage;
        this.sensitiveFieldProcessor = new SensitiveFieldProcessor(properties);
        this.serializer = new AuditLogSerializer(sensitiveFieldProcessor);
        this.targetResolver = new TargetExpressionResolver();
    }

    /**
     * 审计切面
     *
     * @param joinPoint  切点
     * @param annotation 注解实例
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(annotation)")
    public Object auditAround(ProceedingJoinPoint joinPoint, Audited annotation) throws Throwable {
        // 获取方法信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        // 构建敏感字段集合
        Set<String> sensitiveFields = sensitiveFieldProcessor.buildSensitiveFields(annotation);

        // 获取操作名称和模块名称
        String operation = resolveOperation(annotation, methodName);
        String module = resolveModule(annotation, className);

        // 记录开始时间
        LocalDateTime startTime = LocalDateTime.now();
        long startMillis = System.currentTimeMillis();

        // 获取上下文信息
        String traceId = AfgRequestContextHolder.getTraceId();
        String requestId = AfgRequestContextHolder.getRequestId();
        Long userId = AfgRequestContextHolder.getUserId();
        String username = AfgRequestContextHolder.getUsername();
        Long tenantId = AfgRequestContextHolder.getTenantId();
        String clientIp = AfgRequestContextHolder.getClientIp();

        // 序列化方法参数（脱敏）
        String args = null;
        if (annotation.recordArgs()) {
            args = serializer.serializeAndMask(joinPoint.getArgs(), signature.getParameterNames(), sensitiveFields);
        }

        // 执行方法
        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            // 计算耗时
            long durationMs = System.currentTimeMillis() - startMillis;

            // 构建审计日志
            AuditLog.Builder builder = error == null ? AuditLog.successBuilder() : AuditLog.failureBuilder();

            AuditLog auditLog = builder
                    .id(UUID.randomUUID().toString())
                    .userId(userId)
                    .username(username)
                    .tenantId(tenantId)
                    .operation(operation)
                    .module(module)
                    .target(targetResolver.extractTarget(annotation, joinPoint))
                    .args(args)
                    .newValue(serializer.serializeResult(result, annotation, sensitiveFields))
                    .errorMessage(error != null ? error.getMessage() : null)
                    .timestamp(startTime)
                    .durationMs(durationMs)
                    .traceId(traceId)
                    .requestId(requestId)
                    .clientIp(clientIp)
                    .className(className)
                    .methodName(methodName)
                    .build();

            // 保存审计日志
            saveAuditLog(auditLog);
        }

        return result;
    }

    /**
     * 解析操作名称
     */
    private String resolveOperation(Audited annotation, String methodName) {
        String operation = annotation.operation();
        return operation.isEmpty() ? methodName : operation;
    }

    /**
     * 解析模块名称
     */
    private String resolveModule(Audited annotation, String className) {
        String module = annotation.module();
        return module.isEmpty() ? className : module;
    }

    /**
     * 保存审计日志
     */
    private void saveAuditLog(AuditLog auditLog) {
        try {
            storage.save(auditLog);
            log.debug(
                    "Audit log recorded: operation={}, module={}, result={}, durationMs={}",
                    auditLog.operation(),
                    auditLog.module(),
                    auditLog.result(),
                    auditLog.durationMs());
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage(), e);
        }
    }
}
