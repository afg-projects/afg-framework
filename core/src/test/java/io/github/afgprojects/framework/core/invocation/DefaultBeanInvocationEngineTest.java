package io.github.afgprojects.framework.core.invocation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.core.invocation.processor.IdentityProcessor;
import io.github.afgprojects.framework.core.invocation.processor.ResultProcessor;
import io.github.afgprojects.framework.core.invocation.resolver.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class DefaultBeanInvocationEngineTest {

    private DefaultBeanInvocationEngine engine;
    private TestService testService;
    private DefaultServiceMetadataRegistry registry;

    @BeforeEach
    void setUp() {
        testService = new TestService();
        registry = new DefaultServiceMetadataRegistry();
        registry.register(createTestServiceMetadata());

        ObjectMapper objectMapper = new ObjectMapper();
        List<ArgumentResolver> resolvers = List.of(
                new IdentityResolver(),
                new JacksonConvertResolver(),
                new StringConverterResolver(),
                new CollectionResolver(),
                new NullDefaultResolver()
        );
        List<ResultProcessor> processors = List.of(new IdentityProcessor());
        List<InvocationInterceptor> interceptors = List.of();

        engine = new DefaultBeanInvocationEngine(
                registry,
                serviceName -> testService,
                interceptors,
                resolvers,
                processors,
                objectMapper
        );
    }

    // --- Test service ---

    static class TestService {
        public String getName() { return "TestService"; }
        public String greet(String name) { return "Hello " + name; }
        public String repeat(String text, int count) { return text.repeat(count); }
    }

    // --- Metadata factories ---

    @SuppressWarnings("unchecked")
    private ServiceMetadata<TestService> createTestServiceMetadata() {
        return new ServiceMetadata<>() {
            @Override public String serviceName() { return "testService"; }
            @Override public String description() { return "Test Service"; }
            @Override public String category() { return "test"; }
            @Override public List<String> tags() { return List.of("test"); }
            @Override public Class<TestService> serviceType() { return TestService.class; }
            @Override public List<OperationMetadata> operations() { return List.of(
                    createGetNameOperation(),
                    createGreetOperation(),
                    createRepeatOperation()
            ); }
        };
    }

    private OperationMetadata createGetNameOperation() {
        return new OperationMetadata() {
            @Override public String name() { return "getName"; }
            @Override public String description() { return "Get name"; }
            @Override public MethodKey method() { return new MethodKey("getName", List.of()); }
            @Override public List<ParameterMetadata> parameters() { return List.of(); }
            @Override public String returnType() { return "java.lang.String"; }
            @Override public String returnDescription() { return "name"; }
            @Override public String permission() { return ""; }
            @Override public List<String> requiredRoles() { return List.of(); }
            @Override public boolean audit() { return false; }
            @Override public boolean tenantScope() { return false; }
            @Override public boolean dataScope() { return false; }
            @Override public boolean async() { return false; }
            @Override public boolean deprecated() { return false; }
            @Override public String inputSchema() { return ""; }
            @Override public boolean paged() { return false; }
        };
    }

    private OperationMetadata createGreetOperation() {
        ParameterMetadata nameParam = new ParameterMetadata() {
            @Override public String name() { return "name"; }
            @Override public String type() { return "java.lang.String"; }
            @Override public boolean required() { return true; }
            @Override public String defaultValue() { return ""; }
            @Override public int index() { return 0; }
            @Override public String description() { return "Name to greet"; }
            @Override public List<String> enumValues() { return List.of(); }
            @Override public boolean injected() { return false; }
        };
        return new OperationMetadata() {
            @Override public String name() { return "greet"; }
            @Override public String description() { return "Greet someone"; }
            @Override public MethodKey method() { return new MethodKey("greet", List.of("java.lang.String")); }
            @Override public List<ParameterMetadata> parameters() { return List.of(nameParam); }
            @Override public String returnType() { return "java.lang.String"; }
            @Override public String returnDescription() { return "greeting"; }
            @Override public String permission() { return ""; }
            @Override public List<String> requiredRoles() { return List.of(); }
            @Override public boolean audit() { return false; }
            @Override public boolean tenantScope() { return false; }
            @Override public boolean dataScope() { return false; }
            @Override public boolean async() { return false; }
            @Override public boolean deprecated() { return false; }
            @Override public String inputSchema() { return ""; }
            @Override public boolean paged() { return false; }
        };
    }

    private OperationMetadata createRepeatOperation() {
        ParameterMetadata textParam = new ParameterMetadata() {
            @Override public String name() { return "text"; }
            @Override public String type() { return "java.lang.String"; }
            @Override public boolean required() { return true; }
            @Override public String defaultValue() { return ""; }
            @Override public int index() { return 0; }
            @Override public String description() { return "Text to repeat"; }
            @Override public List<String> enumValues() { return List.of(); }
            @Override public boolean injected() { return false; }
        };
        ParameterMetadata countParam = new ParameterMetadata() {
            @Override public String name() { return "count"; }
            @Override public String type() { return "int"; }
            @Override public boolean required() { return true; }
            @Override public String defaultValue() { return ""; }
            @Override public int index() { return 1; }
            @Override public String description() { return "Repeat count"; }
            @Override public List<String> enumValues() { return List.of(); }
            @Override public boolean injected() { return false; }
        };
        return new OperationMetadata() {
            @Override public String name() { return "repeat"; }
            @Override public String description() { return "Repeat text"; }
            @Override public MethodKey method() { return new MethodKey("repeat", List.of("java.lang.String", "int")); }
            @Override public List<ParameterMetadata> parameters() { return List.of(textParam, countParam); }
            @Override public String returnType() { return "java.lang.String"; }
            @Override public String returnDescription() { return "repeated text"; }
            @Override public String permission() { return ""; }
            @Override public List<String> requiredRoles() { return List.of(); }
            @Override public boolean audit() { return false; }
            @Override public boolean tenantScope() { return false; }
            @Override public boolean dataScope() { return false; }
            @Override public boolean async() { return false; }
            @Override public boolean deprecated() { return false; }
            @Override public String inputSchema() { return ""; }
            @Override public boolean paged() { return false; }
        };
    }

    // --- invoke tests ---

    @Test
    void invoke_withArguments_shouldReturnResult() {
        Object result = engine.invoke("testService", "greet", Map.of("name", "World"));
        assertEquals("Hello World", result);
    }

    @Test
    void invoke_noArgMethod_shouldReturnResult() {
        Object result = engine.invoke("testService", "getName", Map.of());
        assertEquals("TestService", result);
    }

    @Test
    void invoke_withStringToIntConversion_shouldConvertAndReturnResult() {
        // StringConverterResolver handles "3" -> 3 (int)
        Object result = engine.invoke("testService", "repeat", Map.of("text", "ab", "count", "3"));
        assertEquals("ababab", result);
    }

    @Test
    void invoke_withIntArgument_shouldReturnResult() {
        Object result = engine.invoke("testService", "repeat", Map.of("text", "ab", "count", 3));
        assertEquals("ababab", result);
    }

    @Test
    void invoke_serviceNotFound_shouldThrowServiceNotFoundException() {
        assertThrows(ServiceNotFoundException.class,
                () -> engine.invoke("nonExistent", "greet", Map.of()));
    }

    @Test
    void invoke_operationNotFound_shouldThrowServiceNotFoundException() {
        assertThrows(ServiceNotFoundException.class,
                () -> engine.invoke("testService", "nonExistent", Map.of()));
    }

    @Test
    void invoke_missingRequiredArgument_shouldThrowMissingArgumentException() {
        assertThrows(MissingArgumentException.class,
                () -> engine.invoke("testService", "greet", Map.of()));
    }

    @Test
    void invoke_withReturnType_shouldReturnTypedResult() {
        String result = engine.invoke("testService", "greet", Map.of("name", "World"), String.class);
        assertEquals("Hello World", result);
    }

    @Test
    void invoke_withReturnTypeNullResult_shouldReturnNull() {
        // Register a service that returns null
        registry.register(createNullReturnServiceMetadata());
        DefaultBeanInvocationEngine nullEngine = new DefaultBeanInvocationEngine(
                registry,
                serviceName -> new NullReturnService(),
                List.of(),
                List.of(new IdentityResolver(), new JacksonConvertResolver(), new StringConverterResolver(), new CollectionResolver(), new NullDefaultResolver()),
                List.of(new IdentityProcessor()),
                new ObjectMapper()
        );
        String result = nullEngine.invoke("nullService", "getNull", Map.of(), String.class);
        assertNull(result);
    }

    // --- invokeAsync tests ---

    @Test
    void invokeAsync_shouldReturnCompletableFutureWithResult() throws Exception {
        CompletableFuture<Object> future = engine.invokeAsync("testService", "greet", Map.of("name", "World"));
        Object result = future.get();
        assertEquals("Hello World", result);
    }

    @Test
    void invokeAsync_withReturnType_shouldReturnTypedCompletableFuture() throws Exception {
        CompletableFuture<String> future = engine.invokeAsync("testService", "greet", Map.of("name", "World"), String.class);
        String result = future.get();
        assertEquals("Hello World", result);
    }

    @Test
    void invokeAsync_serviceNotFound_shouldThrowServiceNotFoundException() {
        assertThrows(ServiceNotFoundException.class,
                () -> engine.invokeAsync("nonExistent", "greet", Map.of()));
    }

    // --- plan tests ---

    @Test
    void plan_shouldReturnInvocationPlanWithCorrectMetadata() {
        InvocationPlan plan = engine.plan("testService", "greet", Map.of("name", "World"));

        assertNotNull(plan);
        assertEquals("testService", plan.serviceMetadata().serviceName());
        assertEquals("greet", plan.operationMetadata().name());
        assertSame(testService, plan.targetBean());
    }

    @Test
    void plan_shouldResolveArguments() {
        InvocationPlan plan = engine.plan("testService", "greet", Map.of("name", "World"));

        Object[] args = plan.resolvedArguments();
        assertEquals(1, args.length);
        assertEquals("World", args[0]);
    }

    @Test
    void plan_shouldResolveMultipleArgumentsWithTypeConversion() {
        InvocationPlan plan = engine.plan("testService", "repeat", Map.of("text", "ab", "count", "3"));

        Object[] args = plan.resolvedArguments();
        assertEquals(2, args.length);
        assertEquals("ab", args[0]);
        assertEquals(3, args[1]);
    }

    @Test
    void plan_serviceNotFound_shouldThrowServiceNotFoundException() {
        assertThrows(ServiceNotFoundException.class,
                () -> engine.plan("nonExistent", "greet", Map.of()));
    }

    @Test
    void plan_operationNotFound_shouldThrowServiceNotFoundException() {
        assertThrows(ServiceNotFoundException.class,
                () -> engine.plan("testService", "nonExistent", Map.of()));
    }

    // --- interceptor integration ---

    @Test
    void invoke_withRejectingInterceptor_shouldThrowInvocationRejectedException() {
        InvocationInterceptor rejectingInterceptor = new InvocationInterceptor() {
            @Override public int order() { return 1; }
            @Override public boolean before(InvocationContext context) { return false; }
            @Override public Object after(InvocationContext context, Object result) { return result; }
            @Override public void onError(InvocationContext context, Exception exception) {}
        };

        DefaultBeanInvocationEngine engineWithInterceptor = new DefaultBeanInvocationEngine(
                registry,
                serviceName -> testService,
                List.of(rejectingInterceptor),
                List.of(new IdentityResolver(), new JacksonConvertResolver(), new StringConverterResolver(), new CollectionResolver(), new NullDefaultResolver()),
                List.of(new IdentityProcessor()),
                new ObjectMapper()
        );

        assertThrows(InvocationRejectedException.class,
                () -> engineWithInterceptor.invoke("testService", "getName", Map.of()));
    }

    @Test
    void invoke_withModifyingAfterInterceptor_shouldReturnModifiedResult() {
        InvocationInterceptor modifyingInterceptor = new InvocationInterceptor() {
            @Override public int order() { return 1; }
            @Override public boolean before(InvocationContext context) { return true; }
            @Override public Object after(InvocationContext context, Object result) {
                if (result instanceof String s) return s + " [modified]";
                return result;
            }
            @Override public void onError(InvocationContext context, Exception exception) {}
        };

        DefaultBeanInvocationEngine engineWithInterceptor = new DefaultBeanInvocationEngine(
                registry,
                serviceName -> testService,
                List.of(modifyingInterceptor),
                List.of(new IdentityResolver(), new JacksonConvertResolver(), new StringConverterResolver(), new CollectionResolver(), new NullDefaultResolver()),
                List.of(new IdentityProcessor()),
                new ObjectMapper()
        );

        Object result = engineWithInterceptor.invoke("testService", "getName", Map.of());
        assertEquals("TestService [modified]", result);
    }

    // --- Helper for null return test ---

    static class NullReturnService {
        public String getNull() { return null; }
    }

    @SuppressWarnings("unchecked")
    private ServiceMetadata<NullReturnService> createNullReturnServiceMetadata() {
        OperationMetadata getNullOp = new OperationMetadata() {
            @Override public String name() { return "getNull"; }
            @Override public String description() { return "Returns null"; }
            @Override public MethodKey method() { return new MethodKey("getNull", List.of()); }
            @Override public List<ParameterMetadata> parameters() { return List.of(); }
            @Override public String returnType() { return "java.lang.String"; }
            @Override public String returnDescription() { return "null"; }
            @Override public String permission() { return ""; }
            @Override public List<String> requiredRoles() { return List.of(); }
            @Override public boolean audit() { return false; }
            @Override public boolean tenantScope() { return false; }
            @Override public boolean dataScope() { return false; }
            @Override public boolean async() { return false; }
            @Override public boolean deprecated() { return false; }
            @Override public String inputSchema() { return ""; }
            @Override public boolean paged() { return false; }
        };
        return new ServiceMetadata<>() {
            @Override public String serviceName() { return "nullService"; }
            @Override public String description() { return "Null Service"; }
            @Override public String category() { return "test"; }
            @Override public List<String> tags() { return List.of("test"); }
            @Override public Class<NullReturnService> serviceType() { return NullReturnService.class; }
            @Override public List<OperationMetadata> operations() { return List.of(getNullOp); }
        };
    }
}
