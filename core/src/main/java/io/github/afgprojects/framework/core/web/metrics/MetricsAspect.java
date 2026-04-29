package io.github.afgprojects.framework.core.web.metrics;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * 指标度量切面
 * <p>
 * 拦截 @{@link TimedMetric} 和 @{@link CountedMetric} 注解，
 * 自动记录方法执行时间和调用次数
 * </p>
 */
@Aspect
@SuppressWarnings({
    "PMD.SignatureDeclareThrowsException",
    "PMD.AvoidThrowingRawExceptionTypes",
    "PMD.AvoidCatchingGenericException"
})
public class MetricsAspect {

    private final MeterRegistry meterRegistry;
    private final MetricsProperties properties;

    /**
     * 构造函数
     *
     * @param meterRegistry Micrometer 注册表
     * @param properties    指标配置属性
     */
    public MetricsAspect(MeterRegistry meterRegistry, MetricsProperties properties) {
        this.meterRegistry = meterRegistry;
        this.properties = properties;
    }

    /**
     * 计时切面
     *
     * @param joinPoint  切点
     * @param annotation 注解实例
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(annotation)")
    public Object timeAround(ProceedingJoinPoint joinPoint, TimedMetric annotation) throws Throwable {
        String metricName = resolveMetricName(joinPoint, annotation.name());
        String description = annotation.description();
        double[] percentiles = annotation.percentiles();

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            Object result = joinPoint.proceed();
            recordTimer(sample, metricName, description, percentiles, null);
            return result;
        } catch (Exception e) {
            recordTimer(
                    sample, metricName, description, percentiles, e.getClass().getSimpleName());
            throw e;
        }
    }

    /**
     * 计数切面
     *
     * @param joinPoint  切点
     * @param annotation 注解实例
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(annotation)")
    public Object countAround(ProceedingJoinPoint joinPoint, CountedMetric annotation) throws Throwable {
        String metricName = resolveMetricName(joinPoint, annotation.name());
        String description = annotation.description();

        try {
            Object result = joinPoint.proceed();
            recordCounter(metricName, description, null);
            return result;
        } catch (Exception e) {
            recordCounter(metricName, description, e.getClass().getSimpleName());
            throw e;
        }
    }

    /**
     * 解析指标名称
     * <p>
     * 如果注解未指定名称，则使用 "className.methodName" 格式
     * </p>
     */
    private String resolveMetricName(ProceedingJoinPoint joinPoint, String annotatedName) {
        if (annotatedName != null && !annotatedName.isEmpty()) {
            return annotatedName;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        return className + "." + methodName;
    }

    /**
     * 记录 Timer 指标
     */
    private void recordTimer(
            Timer.Sample sample, String name, String description, double[] percentiles, String exception) {
        Timer.Builder builder = Timer.builder(name).description(description).publishPercentiles(percentiles);

        // 添加全局标签
        properties.getTags().forEach(builder::tag);

        // 添加异常标签
        if (exception != null) {
            builder.tag("exception", exception);
        } else {
            builder.tag("exception", "none");
        }

        sample.stop(builder.register(meterRegistry));
    }

    /**
     * 记录 Counter 指标
     */
    private void recordCounter(String name, String description, String exception) {
        Counter.Builder builder = Counter.builder(name).description(description);

        // 添加全局标签
        properties.getTags().forEach(builder::tag);

        // 添加异常标签
        if (exception != null) {
            builder.tag("exception", exception);
            builder.tag("result", "failure");
        } else {
            builder.tag("exception", "none");
            builder.tag("result", "success");
        }

        builder.register(meterRegistry).increment();
    }
}
