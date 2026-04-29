package io.github.afgprojects.framework.core.trace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;

import io.github.afgprojects.framework.core.web.trace.TraceContext;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

/**
 * TracedAspect 测试类
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TracedAspect 测试")
class TracedAspectTest {

    @Mock
    private Tracer tracer;

    @Mock
    private Span span;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    private TracingProperties properties;
    private TracedAspect aspect;

    @BeforeEach
    void setUp() {
        TraceContext.setTracer(null);

        properties = new TracingProperties();
        properties.setEnabled(true);
        properties.getAnnotations().setEnabled(true);
        properties.getSampling().setStrategy(SamplingStrategy.ALWAYS);
    }

    @Test
    @DisplayName("禁用时不创建 Span")
    void testDisabled() throws Throwable {
        properties.setEnabled(false);
        aspect = new TracedAspect(null, properties);

        Traced annotation = createAnnotation("testOp", SpanKind.INTERNAL, false, false, ExceptionLogLevel.MESSAGE);

        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.traceAround(joinPoint, annotation);

        assertThat(result).isEqualTo("result");
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("无 Tracer 时正常执行")
    void testNoTracer() throws Throwable {
        aspect = new TracedAspect(null, properties);

        Traced annotation = createAnnotation("testOp", SpanKind.INTERNAL, false, false, ExceptionLogLevel.MESSAGE);

        when(joinPoint.proceed()).thenReturn("result");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringType()).thenReturn((Class) Object.class);
        when(signature.getName()).thenReturn("testMethod");

        Object result = aspect.traceAround(joinPoint, annotation);

        assertThat(result).isEqualTo("result");
    }

    @Test
    @DisplayName("有 Tracer 时创建 Span")
    void testWithTracer() throws Throwable {
        aspect = new TracedAspect(tracer, properties);

        Traced annotation = createAnnotation("testOperation", SpanKind.SERVER, false, false, ExceptionLogLevel.MESSAGE);

        setupSpanMocks();

        when(joinPoint.proceed()).thenReturn("result");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringType()).thenReturn((Class) Object.class);
        when(signature.getName()).thenReturn("testMethod");

        Object result = aspect.traceAround(joinPoint, annotation);

        assertThat(result).isEqualTo("result");
        verify(span).start();
        verify(span).end();
        verify(span).tag("success", "true");
    }

    @Test
    @DisplayName("自动生成操作名称")
    void testAutoOperationName() throws Throwable {
        aspect = new TracedAspect(tracer, properties);

        Traced annotation = createAnnotation("", SpanKind.INTERNAL, false, false, ExceptionLogLevel.MESSAGE);

        setupSpanMocks();

        when(joinPoint.proceed()).thenReturn("result");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringType()).thenReturn(String.class);
        when(signature.getName()).thenReturn("myMethod");

        Object result = aspect.traceAround(joinPoint, annotation);

        assertThat(result).isEqualTo("result");
        verify(tracer).nextSpan();
    }

    @Test
    @DisplayName("记录参数")
    void testLogParameters() throws Throwable {
        aspect = new TracedAspect(tracer, properties);

        Traced annotation = createAnnotation("testOp", SpanKind.INTERNAL, true, false, ExceptionLogLevel.MESSAGE);

        setupSpanMocks();

        when(joinPoint.proceed()).thenReturn("result");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"arg1", "arg2"});
        when(signature.getParameterNames()).thenReturn(new String[]{"param1", "param2"});
        when(signature.getDeclaringType()).thenReturn((Class) Object.class);
        when(signature.getName()).thenReturn("testMethod");

        Object result = aspect.traceAround(joinPoint, annotation);

        assertThat(result).isEqualTo("result");
        verify(span).tag("param.param1", "arg1");
        verify(span).tag("param.param2", "arg2");
    }

    @Test
    @DisplayName("记录返回值")
    void testLogResult() throws Throwable {
        aspect = new TracedAspect(tracer, properties);

        Traced annotation = createAnnotation("testOp", SpanKind.INTERNAL, false, true, ExceptionLogLevel.MESSAGE);

        setupSpanMocks();

        when(joinPoint.proceed()).thenReturn("returnValue");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringType()).thenReturn((Class) Object.class);
        when(signature.getName()).thenReturn("testMethod");

        Object result = aspect.traceAround(joinPoint, annotation);

        assertThat(result).isEqualTo("returnValue");
        verify(span).tag("result", "returnValue");
    }

    @Test
    @DisplayName("记录异常信息")
    void testLogException() throws Throwable {
        aspect = new TracedAspect(tracer, properties);

        Traced annotation = createAnnotation("testOp", SpanKind.INTERNAL, false, false, ExceptionLogLevel.MESSAGE);

        setupSpanMocks();

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringType()).thenReturn((Class) Object.class);
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.proceed()).thenThrow(new RuntimeException("test error"));

        assertThatCode(() -> aspect.traceAround(joinPoint, annotation))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("test error");

        verify(span).tag("success", "false");
        verify(span).tag("exception", "RuntimeException");
    }

    @Test
    @DisplayName("记录异常堆栈")
    void testLogExceptionStackTrace() throws Throwable {
        aspect = new TracedAspect(tracer, properties);

        Traced annotation = createAnnotation("testOp", SpanKind.INTERNAL, false, false, ExceptionLogLevel.STACK_TRACE);

        setupSpanMocks();

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringType()).thenReturn((Class) Object.class);
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.proceed()).thenThrow(new RuntimeException("test error"));

        assertThatCode(() -> aspect.traceAround(joinPoint, annotation))
                .isInstanceOf(RuntimeException.class);

        // 验证异常信息被记录
        verify(span).tag("success", "false");
        verify(span).tag("exception", "RuntimeException");
        verify(span).tag("exception.message", "test error");
    }

    @Test
    @DisplayName("设置 Span 类型标签")
    void testSpanKindTags() throws Throwable {
        aspect = new TracedAspect(tracer, properties);

        Traced annotation = createAnnotation("testOp", SpanKind.CLIENT, false, false, ExceptionLogLevel.MESSAGE);

        setupSpanMocks();

        when(joinPoint.proceed()).thenReturn("result");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringType()).thenReturn((Class) Object.class);
        when(signature.getName()).thenReturn("testMethod");

        Object result = aspect.traceAround(joinPoint, annotation);

        assertThat(result).isEqualTo("result");
        verify(span).tag("span.kind", "client");
    }

    private void setupSpanMocks() {
        when(tracer.currentSpan()).thenReturn(null);
        when(tracer.nextSpan()).thenReturn(span);
        when(span.name(anyString())).thenReturn(span);
        when(span.context()).thenReturn(mock(io.micrometer.tracing.TraceContext.class));
        when(span.context().traceId()).thenReturn("trace123");
    }

    private Traced createAnnotation(
            String operationName, SpanKind kind, boolean logParameters, boolean logResult, ExceptionLogLevel logLevel) {
        return new Traced() {
            @Override
            public String operationName() {
                return operationName;
            }

            @Override
            public SpanKind kind() {
                return kind;
            }

            @Override
            public boolean logParameters() {
                return logParameters;
            }

            @Override
            public boolean logResult() {
                return logResult;
            }

            @Override
            public ExceptionLogLevel exceptionLogLevel() {
                return logLevel;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return Traced.class;
            }
        };
    }
}