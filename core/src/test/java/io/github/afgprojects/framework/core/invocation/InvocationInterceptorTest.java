package io.github.afgprojects.framework.core.invocation;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.afgprojects.framework.core.invocation.interceptor.AuditInvocationInterceptor;
import io.github.afgprojects.framework.core.invocation.interceptor.ValidationInvocationInterceptor;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class InvocationInterceptorTest {

    // --- Helper stubs ---

    record TestDto(@NotNull String name) {}

    static class StubServiceMetadata implements ServiceMetadata<Object> {
        private final String name;
        StubServiceMetadata(String name) { this.name = name; }
        @Override public String serviceName() { return name; }
        @Override public String description() { return ""; }
        @Override public String category() { return ""; }
        @Override public List<String> tags() { return List.of(); }
        @Override public Class<Object> serviceType() { return Object.class; }
        @Override public List<OperationMetadata> operations() { return List.of(); }
    }

    static class StubOperationMetadata implements OperationMetadata {
        private final String name;
        private final boolean audit;
        StubOperationMetadata(String name) { this(name, false); }
        StubOperationMetadata(String name, boolean audit) { this.name = name; this.audit = audit; }
        @Override public String name() { return name; }
        @Override public String description() { return ""; }
        @Override public MethodKey method() { return null; }
        @Override public List<ParameterMetadata> parameters() { return List.of(); }
        @Override public String returnType() { return ""; }
        @Override public String returnDescription() { return ""; }
        @Override public String permission() { return ""; }
        @Override public List<String> requiredRoles() { return List.of(); }
        @Override public boolean audit() { return audit; }
        @Override public boolean tenantScope() { return false; }
        @Override public boolean dataScope() { return false; }
        @Override public boolean async() { return false; }
        @Override public boolean deprecated() { return false; }
        @Override public String inputSchema() { return ""; }
        @Override public boolean paged() { return false; }
    }

    private InvocationContext makeContext(String serviceName, String opName, boolean audit,
                                          Object[] args, Map<String, Object> rawArgs) {
        StubServiceMetadata sm = new StubServiceMetadata(serviceName);
        StubOperationMetadata om = new StubOperationMetadata(opName, audit);
        Object bean = new Object();
        Object[] resolvedArgs = args != null ? args : new Object[0];
        Map<String, Object> arguments = rawArgs != null ? rawArgs : Map.of();
        try {
            Method method = Object.class.getMethod("toString");
            return new DefaultInvocationContext(
                serviceName, opName, sm, om,
                arguments, resolvedArgs, bean, method
            );
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    // --- Chain execution tests ---

    @Nested
    class ChainExecution {

        @Test
        void interceptorsExecuteInOrder() {
            StringBuilder log = new StringBuilder();
            List<InvocationInterceptor> interceptors = List.of(
                new OrderTrackingInterceptor(100, "A", log),
                new OrderTrackingInterceptor(200, "B", log),
                new OrderTrackingInterceptor(300, "C", log)
            );

            InvocationContext ctx = makeContext("svc", "op", false, null, null);
            for (InvocationInterceptor i : interceptors) {
                i.before(ctx);
            }
            Object result = "ok";
            for (InvocationInterceptor i : interceptors) {
                result = i.after(ctx, result);
            }

            assertEquals("before-A;before-B;before-C;after-A;after-B;after-C;", log.toString());
        }

        @Test
        void beforeReturningFalseInterruptsChain() {
            StringBuilder log = new StringBuilder();
            List<InvocationInterceptor> interceptors = List.of(
                new OrderTrackingInterceptor(100, "A", log),
                new RejectingInterceptor(200, "B", log),
                new OrderTrackingInterceptor(300, "C", log)
            );

            InvocationContext ctx = makeContext("svc", "op", false, null, null);
            boolean proceed = true;
            for (InvocationInterceptor i : interceptors) {
                if (!proceed) break;
                proceed = i.before(ctx);
            }

            assertEquals("before-A;reject-B;", log.toString());
            assertFalse(proceed);
        }

        @Test
        void afterCanModifyReturnValue() {
            InvocationInterceptor doubler = new InvocationInterceptor() {
                @Override public int order() { return 100; }
                @Override public boolean before(InvocationContext context) { return true; }
                @Override public Object after(InvocationContext context, Object result) {
                    return (result instanceof Integer n) ? n * 2 : result;
                }
                @Override public void onError(InvocationContext context, Exception exception) {}
            };

            InvocationContext ctx = makeContext("svc", "op", false, null, null);
            Object result = 21;
            result = doubler.after(ctx, result);
            assertEquals(42, result);
        }

        @Test
        void onErrorIsCalledOnException() {
            StringBuilder log = new StringBuilder();
            InvocationInterceptor tracker = new InvocationInterceptor() {
                @Override public int order() { return 100; }
                @Override public boolean before(InvocationContext context) { return true; }
                @Override public Object after(InvocationContext context, Object result) { return result; }
                @Override public void onError(InvocationContext context, Exception exception) {
                    log.append("error:" + exception.getMessage() + ";");
                }
            };

            InvocationContext ctx = makeContext("svc", "op", false, null, null);
            Exception ex = new RuntimeException("boom");
            tracker.onError(ctx, ex);

            assertEquals("error:boom;", log.toString());
        }
    }

    // --- AuditInvocationInterceptor tests ---

    @Nested
    class AuditInterceptorTests {

        @Test
        void logsBeforeAndAfter_whenAuditEnabled() {
            AuditInvocationInterceptor interceptor = new AuditInvocationInterceptor();
            Logger logger = (Logger) LoggerFactory.getLogger(AuditInvocationInterceptor.class);
            ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
            listAppender.start();
            logger.addAppender(listAppender);

            try {
                InvocationContext ctx = makeContext("UserService", "create", true, null, null);
                interceptor.before(ctx);
                interceptor.after(ctx, null);

                boolean foundBefore = listAppender.list.stream()
                    .anyMatch(e -> e.getFormattedMessage().contains("Invoking UserService.create"));
                boolean foundAfter = listAppender.list.stream()
                    .anyMatch(e -> e.getFormattedMessage().contains("Invoked UserService.create successfully"));
                assertTrue(foundBefore, "Should log before invocation");
                assertTrue(foundAfter, "Should log after invocation");
            } finally {
                logger.detachAppender(listAppender);
            }
        }

        @Test
        void doesNotLog_whenAuditDisabled() {
            AuditInvocationInterceptor interceptor = new AuditInvocationInterceptor();
            Logger logger = (Logger) LoggerFactory.getLogger(AuditInvocationInterceptor.class);
            ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
            listAppender.start();
            logger.addAppender(listAppender);

            try {
                InvocationContext ctx = makeContext("UserService", "create", false, null, null);
                interceptor.before(ctx);
                interceptor.after(ctx, null);

                assertTrue(listAppender.list.isEmpty(), "Should not log when audit is disabled");
            } finally {
                logger.detachAppender(listAppender);
            }
        }

        @Test
        void logsError_onError() {
            AuditInvocationInterceptor interceptor = new AuditInvocationInterceptor();
            Logger logger = (Logger) LoggerFactory.getLogger(AuditInvocationInterceptor.class);
            ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
            listAppender.start();
            logger.addAppender(listAppender);

            try {
                InvocationContext ctx = makeContext("UserService", "delete", true, null, null);
                interceptor.onError(ctx, new RuntimeException("db error"));

                boolean foundError = listAppender.list.stream()
                    .anyMatch(e -> e.getFormattedMessage().contains("Invoked UserService.delete failed"));
                assertTrue(foundError, "Should log error");
            } finally {
                logger.detachAppender(listAppender);
            }
        }

        @Test
        void doesNotLogError_whenAuditDisabled() {
            AuditInvocationInterceptor interceptor = new AuditInvocationInterceptor();
            Logger logger = (Logger) LoggerFactory.getLogger(AuditInvocationInterceptor.class);
            ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
            listAppender.start();
            logger.addAppender(listAppender);

            try {
                InvocationContext ctx = makeContext("UserService", "delete", false, null, null);
                interceptor.onError(ctx, new RuntimeException("db error"));

                assertTrue(listAppender.list.isEmpty(), "Should not log error when audit is disabled");
            } finally {
                logger.detachAppender(listAppender);
            }
        }
    }

    // --- ValidationInvocationInterceptor tests ---

    @Nested
    class ValidationInterceptorTests {

        private Validator validator;

        @BeforeEach
        void setUp() {
            ValidatorFactory factory = jakarta.validation.Validation.buildDefaultValidatorFactory();
            validator = factory.getValidator();
        }

        @Test
        void passesValidation_withValidDto() {
            ValidationInvocationInterceptor interceptor = new ValidationInvocationInterceptor(validator);
            InvocationContext ctx = makeContext("svc", "op", false,
                new Object[]{ new TestDto("hello") }, null);

            assertTrue(interceptor.before(ctx));
        }

        @Test
        void throwsException_withInvalidDto() {
            ValidationInvocationInterceptor interceptor = new ValidationInvocationInterceptor(validator);
            InvocationContext ctx = makeContext("svc", "op", false,
                new Object[]{ new TestDto(null) }, null);

            ServiceInvocationException ex = assertThrows(ServiceInvocationException.class,
                () -> interceptor.before(ctx));
            assertTrue(ex.getMessage().startsWith("Validation failed:"));
            assertTrue(ex.getMessage().contains("name"));
        }

        @Test
        void skipsNullArguments() {
            ValidationInvocationInterceptor interceptor = new ValidationInvocationInterceptor(validator);
            InvocationContext ctx = makeContext("svc", "op", false,
                new Object[]{ null }, null);

            assertTrue(interceptor.before(ctx));
        }

        @Test
        void noOpWithoutValidator() {
            ValidationInvocationInterceptor interceptor = new ValidationInvocationInterceptor();
            InvocationContext ctx = makeContext("svc", "op", false,
                new Object[]{ new TestDto(null) }, null);

            assertTrue(interceptor.before(ctx), "Should pass when no validator is set");
        }

        @Test
        void afterReturnsResultUnchanged() {
            ValidationInvocationInterceptor interceptor = new ValidationInvocationInterceptor(validator);
            InvocationContext ctx = makeContext("svc", "op", false, null, null);
            assertEquals("result", interceptor.after(ctx, "result"));
        }

        @Test
        void onErrorDoesNothing() {
            ValidationInvocationInterceptor interceptor = new ValidationInvocationInterceptor(validator);
            InvocationContext ctx = makeContext("svc", "op", false, null, null);
            assertDoesNotThrow(() -> interceptor.onError(ctx, new RuntimeException()));
        }
    }

    // --- DefaultInvocationContext tests ---

    @Nested
    class DefaultInvocationContextTests {

        @Test
        void recordAccessors() throws Exception {
            ServiceMetadata<?> sm = new StubServiceMetadata("svc");
            OperationMetadata om = new StubOperationMetadata("op");
            Object bean = new Object();
            Object[] resolvedArgs = new Object[]{ "a" };
            Map<String, Object> arguments = Map.of("key", "val");
            Method method = Object.class.getMethod("toString");

            DefaultInvocationContext ctx = new DefaultInvocationContext(
                "svc", "op", sm, om, arguments, resolvedArgs, bean, method);
            assertEquals("svc", ctx.serviceName());
            assertEquals("op", ctx.operationName());
            assertSame(sm, ctx.serviceMetadata());
            assertSame(om, ctx.operationMetadata());
            assertSame(arguments, ctx.arguments());
            assertSame(resolvedArgs, ctx.resolvedArguments());
            assertSame(bean, ctx.bean());
            assertSame(method, ctx.method());
        }
    }

    // --- DefaultInvocationPlan tests ---

    @Nested
    class DefaultInvocationPlanTests {

        @Test
        void recordAccessors() throws Exception {
            ServiceMetadata<?> sm = new StubServiceMetadata("svc");
            OperationMetadata om = new StubOperationMetadata("op");
            Object bean = new Object();
            Object[] resolvedArgs = new Object[]{ "a" };
            Map<String, Object> arguments = Map.of("key", "val");
            Method method = Object.class.getMethod("toString");
            List<InvocationInterceptor> interceptors = List.of();

            DefaultInvocationContext ctx = new DefaultInvocationContext(
                "svc", "op", sm, om, arguments, resolvedArgs, bean, method);
            DefaultInvocationPlan plan = new DefaultInvocationPlan(
                ctx, sm, om, bean, resolvedArgs, method, interceptors);
            assertSame(sm, plan.serviceMetadata());
            assertSame(om, plan.operationMetadata());
            assertSame(bean, plan.targetBean());
            assertSame(resolvedArgs, plan.resolvedArguments());
            assertSame(interceptors, plan.applicableInterceptors());
        }
    }

    // --- Test helper interceptors ---

    static class OrderTrackingInterceptor implements InvocationInterceptor {
        private final int ord;
        private final String label;
        private final StringBuilder log;

        OrderTrackingInterceptor(int ord, String label, StringBuilder log) {
            this.ord = ord;
            this.label = label;
            this.log = log;
        }

        @Override public int order() { return ord; }
        @Override public boolean before(InvocationContext context) {
            log.append("before-" + label + ";");
            return true;
        }
        @Override public Object after(InvocationContext context, Object result) {
            log.append("after-" + label + ";");
            return result;
        }
        @Override public void onError(InvocationContext context, Exception exception) {
            log.append("error-" + label + ";");
        }
    }

    static class RejectingInterceptor implements InvocationInterceptor {
        private final int ord;
        private final String label;
        private final StringBuilder log;

        RejectingInterceptor(int ord, String label, StringBuilder log) {
            this.ord = ord;
            this.label = label;
            this.log = log;
        }

        @Override public int order() { return ord; }
        @Override public boolean before(InvocationContext context) {
            log.append("reject-" + label + ";");
            return false;
        }
        @Override public Object after(InvocationContext context, Object result) {
            log.append("after-" + label + ";");
            return result;
        }
        @Override public void onError(InvocationContext context, Exception exception) {
            log.append("error-" + label + ";");
        }
    }
}
