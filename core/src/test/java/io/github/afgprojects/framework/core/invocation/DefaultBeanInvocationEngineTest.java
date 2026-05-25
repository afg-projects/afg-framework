package io.github.afgprojects.framework.core.invocation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.afgprojects.framework.core.invocation.processor.IdentityProcessor;
import io.github.afgprojects.framework.core.invocation.processor.ResultProcessor;
import io.github.afgprojects.framework.core.invocation.resolver.*;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
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
        objectMapper.registerModule(new JavaTimeModule());
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

    enum Status { ACTIVE, INACTIVE, PENDING }

    @Data
    static class Address {
        private String city;
        private String street;
    }

    @Data
    static class CreateUserRequest {
        private String username;
        private String email;
        private int age;
        private Address address;
    }

    @Data
    static class UserDTO {
        private Long id;
        private String username;
        private String email;
        private Address address;
    }

    static class TestService {
        public String getName() { return "TestService"; }
        public String greet(String name) { return "Hello " + name; }
        public String repeat(String text, int count) { return text.repeat(count); }

        public UserDTO createUser(CreateUserRequest req) {
            UserDTO dto = new UserDTO();
            dto.setId(1L);
            dto.setUsername(req.getUsername());
            dto.setEmail(req.getEmail());
            dto.setAddress(req.getAddress());
            return dto;
        }

        public List<String> batchGreet(List<String> names) {
            return names.stream().map(n -> "Hello " + n).toList();
        }

        public String checkStatus(Status status) {
            return "Status: " + status.name();
        }

        public Map<String, Object> queryStats(LocalDate date, BigDecimal amount) {
            return Map.of("date", date.toString(), "amount", amount, "count", 42);
        }
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

    // --- Complex type invocation tests ---

    private DefaultBeanInvocationEngine complexEngine;

    @BeforeEach
    void setUpComplex() {
        registry.register(createComplexServiceMetadata());
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        complexEngine = new DefaultBeanInvocationEngine(
                registry,
                serviceName -> testService,
                List.of(),
                List.of(new IdentityResolver(), new JacksonConvertResolver(), new StringConverterResolver(), new CollectionResolver(), new NullDefaultResolver()),
                List.of(new IdentityProcessor()),
                om
        );
    }

    @SuppressWarnings("unchecked")
    private ServiceMetadata<TestService> createComplexServiceMetadata() {
        return new ServiceMetadata<>() {
            @Override public String serviceName() { return "complexService"; }
            @Override public String description() { return "Complex Service"; }
            @Override public String category() { return "test"; }
            @Override public List<String> tags() { return List.of("complex"); }
            @Override public Class<TestService> serviceType() { return TestService.class; }
            @Override public List<OperationMetadata> operations() { return List.of(
                    createCreateUserOperation(),
                    createBatchGreetOperation(),
                    createCheckStatusOperation(),
                    createQueryStatsOperation()
            ); }
        };
    }

    private OperationMetadata createCreateUserOperation() {
        ParameterMetadata reqParam = new ParameterMetadata() {
            @Override public String name() { return "req"; }
            @Override public String type() { return "io.github.afgprojects.framework.core.invocation.DefaultBeanInvocationEngineTest$CreateUserRequest"; }
            @Override public boolean required() { return true; }
            @Override public String defaultValue() { return ""; }
            @Override public int index() { return 0; }
            @Override public String description() { return "Create user request"; }
            @Override public List<String> enumValues() { return List.of(); }
            @Override public boolean injected() { return false; }
        };
        return new OperationMetadata() {
            @Override public String name() { return "createUser"; }
            @Override public String description() { return "Create a user"; }
            @Override public MethodKey method() { return new MethodKey("createUser", List.of(
                    "io.github.afgprojects.framework.core.invocation.DefaultBeanInvocationEngineTest$CreateUserRequest")); }
            @Override public List<ParameterMetadata> parameters() { return List.of(reqParam); }
            @Override public String returnType() { return "io.github.afgprojects.framework.core.invocation.DefaultBeanInvocationEngineTest$UserDTO"; }
            @Override public String returnDescription() { return "Created user"; }
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

    private OperationMetadata createBatchGreetOperation() {
        ParameterMetadata namesParam = new ParameterMetadata() {
            @Override public String name() { return "names"; }
            @Override public String type() { return "java.util.List"; }
            @Override public boolean required() { return true; }
            @Override public String defaultValue() { return ""; }
            @Override public int index() { return 0; }
            @Override public String description() { return "Names to greet"; }
            @Override public List<String> enumValues() { return List.of(); }
            @Override public boolean injected() { return false; }
        };
        return new OperationMetadata() {
            @Override public String name() { return "batchGreet"; }
            @Override public String description() { return "Greet multiple people"; }
            @Override public MethodKey method() { return new MethodKey("batchGreet", List.of("java.util.List")); }
            @Override public List<ParameterMetadata> parameters() { return List.of(namesParam); }
            @Override public String returnType() { return "java.util.List"; }
            @Override public String returnDescription() { return "Greetings"; }
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

    private OperationMetadata createCheckStatusOperation() {
        ParameterMetadata statusParam = new ParameterMetadata() {
            @Override public String name() { return "status"; }
            @Override public String type() { return "io.github.afgprojects.framework.core.invocation.DefaultBeanInvocationEngineTest$Status"; }
            @Override public boolean required() { return true; }
            @Override public String defaultValue() { return ""; }
            @Override public int index() { return 0; }
            @Override public String description() { return "Status to check"; }
            @Override public List<String> enumValues() { return List.of("ACTIVE", "INACTIVE", "PENDING"); }
            @Override public boolean injected() { return false; }
        };
        return new OperationMetadata() {
            @Override public String name() { return "checkStatus"; }
            @Override public String description() { return "Check status"; }
            @Override public MethodKey method() { return new MethodKey("checkStatus", List.of(
                    "io.github.afgprojects.framework.core.invocation.DefaultBeanInvocationEngineTest$Status")); }
            @Override public List<ParameterMetadata> parameters() { return List.of(statusParam); }
            @Override public String returnType() { return "java.lang.String"; }
            @Override public String returnDescription() { return "Status result"; }
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

    private OperationMetadata createQueryStatsOperation() {
        ParameterMetadata dateParam = new ParameterMetadata() {
            @Override public String name() { return "date"; }
            @Override public String type() { return "java.time.LocalDate"; }
            @Override public boolean required() { return true; }
            @Override public String defaultValue() { return ""; }
            @Override public int index() { return 0; }
            @Override public String description() { return "Query date"; }
            @Override public List<String> enumValues() { return List.of(); }
            @Override public boolean injected() { return false; }
        };
        ParameterMetadata amountParam = new ParameterMetadata() {
            @Override public String name() { return "amount"; }
            @Override public String type() { return "java.math.BigDecimal"; }
            @Override public boolean required() { return true; }
            @Override public String defaultValue() { return ""; }
            @Override public int index() { return 1; }
            @Override public String description() { return "Amount"; }
            @Override public List<String> enumValues() { return List.of(); }
            @Override public boolean injected() { return false; }
        };
        return new OperationMetadata() {
            @Override public String name() { return "queryStats"; }
            @Override public String description() { return "Query statistics"; }
            @Override public MethodKey method() { return new MethodKey("queryStats", List.of("java.time.LocalDate", "java.math.BigDecimal")); }
            @Override public List<ParameterMetadata> parameters() { return List.of(dateParam, amountParam); }
            @Override public String returnType() { return "java.util.Map"; }
            @Override public String returnDescription() { return "Stats"; }
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

    @Nested
    @DisplayName("Complex input/output types")
    class ComplexTypeTests {

        @Test
        @DisplayName("nested POJO parameter: Map→CreateUserRequest with nested Address")
        void nestedPojoParameter() {
            Map<String, Object> addressMap = Map.of("city", "Beijing", "street", "Chaoyang Road");
            Map<String, Object> reqMap = Map.of(
                    "username", "alice",
                    "email", "alice@example.com",
                    "age", 28,
                    "address", addressMap
            );
            Object result = complexEngine.invoke("complexService", "createUser", Map.of("req", reqMap));

            assertInstanceOf(UserDTO.class, result);
            UserDTO dto = (UserDTO) result;
            assertEquals("alice", dto.getUsername());
            assertEquals("alice@example.com", dto.getEmail());
            assertNotNull(dto.getAddress());
            assertEquals("Beijing", dto.getAddress().getCity());
            assertEquals("Chaoyang Road", dto.getAddress().getStreet());
        }

        @Test
        @DisplayName("nested POJO return type: UserDTO converted via invoke(class)")
        void nestedPojoReturnType() {
            Map<String, Object> addressMap = Map.of("city", "Shanghai", "street", "Nanjing Road");
            Map<String, Object> reqMap = Map.of(
                    "username", "bob",
                    "email", "bob@example.com",
                    "age", 35,
                    "address", addressMap
            );
            UserDTO dto = complexEngine.invoke("complexService", "createUser", Map.of("req", reqMap), UserDTO.class);

            assertEquals("bob", dto.getUsername());
            assertNotNull(dto.getAddress());
            assertEquals("Shanghai", dto.getAddress().getCity());
        }

        @Test
        @DisplayName("List parameter: List<String> passes through directly")
        void listParameterDirect() {
            List<String> names = List.of("Alice", "Bob", "Charlie");
            Object result = complexEngine.invoke("complexService", "batchGreet", Map.of("names", names));

            assertInstanceOf(List.class, result);
            List<?> list = (List<?>) result;
            assertEquals(3, list.size());
            assertEquals("Hello Alice", list.get(0));
            assertEquals("Hello Bob", list.get(1));
            assertEquals("Hello Charlie", list.get(2));
        }

        @Test
        @DisplayName("enum parameter: String→enum via Jackson")
        void enumParameterFromString() {
            Object result = complexEngine.invoke("complexService", "checkStatus", Map.of("status", "ACTIVE"));
            assertEquals("Status: ACTIVE", result);
        }

        @Test
        @DisplayName("enum parameter: enum value passes through directly")
        void enumParameterDirect() {
            Object result = complexEngine.invoke("complexService", "checkStatus", Map.of("status", Status.PENDING));
            assertEquals("Status: PENDING", result);
        }

        @Test
        @DisplayName("mixed complex parameters: LocalDate + BigDecimal via String conversion")
        void mixedDateAndBigDecimal() {
            Object result = complexEngine.invoke("complexService", "queryStats",
                    Map.of("date", "2024-06-15", "amount", "999.99"));

            assertInstanceOf(Map.class, result);
            Map<?, ?> map = (Map<?, ?>) result;
            assertEquals("2024-06-15", map.get("date"));
            assertEquals(new BigDecimal("999.99"), map.get("amount"));
            assertEquals(42, map.get("count"));
        }

        @Test
        @DisplayName("mixed complex parameters: typed LocalDate + BigDecimal pass through")
        void mixedDateAndBigDecimalDirect() {
            Object result = complexEngine.invoke("complexService", "queryStats",
                    Map.of("date", LocalDate.of(2024, 6, 15), "amount", new BigDecimal("500.00")));

            assertInstanceOf(Map.class, result);
            Map<?, ?> map = (Map<?, ?>) result;
            assertEquals("2024-06-15", map.get("date"));
            assertEquals(new BigDecimal("500.00"), map.get("amount"));
        }

        @Test
        @DisplayName("Map return value converted via invoke(class)")
        void mapReturnTypeConversion() {
            Map<String, Object> result = complexEngine.invoke("complexService", "queryStats",
                    Map.of("date", LocalDate.of(2024, 6, 15), "amount", new BigDecimal("500.00")),
                    Map.class);
            assertNotNull(result);
            assertEquals("2024-06-15", result.get("date"));
        }

        @Test
        @DisplayName("async invocation with nested POJO parameter and return")
        void asyncNestedPojo() throws Exception {
            Map<String, Object> addressMap = Map.of("city", "Guangzhou", "street", "Tianhe Road");
            Map<String, Object> reqMap = Map.of(
                    "username", "carol",
                    "email", "carol@example.com",
                    "age", 22,
                    "address", addressMap
            );
            CompletableFuture<Object> future = complexEngine.invokeAsync("complexService", "createUser", Map.of("req", reqMap));
            Object result = future.get();

            assertInstanceOf(UserDTO.class, result);
            UserDTO dto = (UserDTO) result;
            assertEquals("carol", dto.getUsername());
            assertEquals("Guangzhou", dto.getAddress().getCity());
        }

        @Test
        @DisplayName("async invocation with typed return: UserDTO")
        void asyncNestedPojoTypedReturn() throws Exception {
            Map<String, Object> addressMap = Map.of("city", "Shenzhen", "street", "Nanshan Road");
            Map<String, Object> reqMap = Map.of(
                    "username", "dave",
                    "email", "dave@example.com",
                    "age", 40,
                    "address", addressMap
            );
            CompletableFuture<UserDTO> future = complexEngine.invokeAsync("complexService", "createUser",
                    Map.of("req", reqMap), UserDTO.class);
            UserDTO dto = future.get();

            assertEquals("dave", dto.getUsername());
            assertEquals("Shenzhen", dto.getAddress().getCity());
        }

        @Test
        @DisplayName("plan resolves nested POJO argument from Map")
        void planNestedPojoArgument() {
            Map<String, Object> addressMap = Map.of("city", "Wuhan", "street", "Optics Valley");
            Map<String, Object> reqMap = Map.of("username", "eve", "email", "eve@example.com", "age", 18, "address", addressMap);

            InvocationPlan plan = complexEngine.plan("complexService", "createUser", Map.of("req", reqMap));
            Object[] args = plan.resolvedArguments();
            assertEquals(1, args.length);
            assertInstanceOf(CreateUserRequest.class, args[0]);
            CreateUserRequest req = (CreateUserRequest) args[0];
            assertEquals("eve", req.getUsername());
            assertNotNull(req.getAddress());
            assertEquals("Wuhan", req.getAddress().getCity());
        }
    }
}
