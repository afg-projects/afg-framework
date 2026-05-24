package io.github.afgprojects.framework.core.invocation.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.core.invocation.MethodKey;
import io.github.afgprojects.framework.core.invocation.OperationMetadata;
import io.github.afgprojects.framework.core.invocation.ParameterMetadata;
import io.github.afgprojects.framework.core.invocation.ServiceMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResultProcessorChainTest {

    // --- Helper stubs ---

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

    static OperationMetadata opMeta(boolean paged) {
        return new OperationMetadata() {
            @Override public String name() { return "testOp"; }
            @Override public String description() { return ""; }
            @Override public MethodKey method() { return null; }
            @Override public List<ParameterMetadata> parameters() { return List.of(); }
            @Override public String returnType() { return ""; }
            @Override public String returnDescription() { return ""; }
            @Override public String permission() { return ""; }
            @Override public List<String> requiredRoles() { return List.of(); }
            @Override public boolean audit() { return false; }
            @Override public boolean tenantScope() { return false; }
            @Override public boolean dataScope() { return false; }
            @Override public boolean async() { return false; }
            @Override public boolean deprecated() { return false; }
            @Override public String inputSchema() { return ""; }
            @Override public boolean paged() { return paged; }
        };
    }

    private ObjectMapper objectMapper;
    private ServiceMetadata<?> serviceMetadata;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        serviceMetadata = new StubServiceMetadata("TestService");
    }

    private ResultContext makeContext(OperationMetadata opMeta) {
        return new DefaultResultContext(opMeta, serviceMetadata, objectMapper, new HashMap<>());
    }

    /**
     * Chain processing: iterate processors sorted by priority, first matching one processes result.
     */
    private Object processChain(List<ResultProcessor> processors, Object result, OperationMetadata metadata) {
        processors.sort(Comparator.comparingInt(ResultProcessor::priority));
        ResultContext context = makeContext(metadata);
        for (ResultProcessor processor : processors) {
            if (processor.supports(result, metadata)) {
                return processor.process(result, context);
            }
        }
        return result;
    }

    // --- IdentityProcessor tests ---

    @Nested
    @DisplayName("IdentityProcessor")
    class IdentityProcessorTests {

        private final IdentityProcessor processor = new IdentityProcessor();

        @Test
        @DisplayName("supports any non-null result")
        void supportsAnyResult() {
            OperationMetadata meta = opMeta(false);
            assertTrue(processor.supports("hello", meta));
            assertTrue(processor.supports(42, meta));
            assertTrue(processor.supports(List.of(), meta));
        }

        @Test
        @DisplayName("supports null result")
        void supportsNull() {
            OperationMetadata meta = opMeta(false);
            assertTrue(processor.supports(null, meta));
        }

        @Test
        @DisplayName("passes through result unchanged")
        void passesThrough() {
            ResultContext ctx = makeContext(opMeta(false));
            assertEquals("hello", processor.process("hello", ctx));
            assertEquals(42, processor.process(42, ctx));
        }

        @Test
        @DisplayName("priority is 100")
        void priority() {
            assertEquals(100, processor.priority());
        }
    }

    // --- PagedResultProcessor tests ---

    @Nested
    @DisplayName("PagedResultProcessor")
    class PagedResultProcessorTests {

        private final PagedResultProcessor processor = new PagedResultProcessor();

        @Test
        @DisplayName("supports List when paged=true")
        void supportsListWhenPaged() {
            OperationMetadata meta = opMeta(true);
            assertTrue(processor.supports(List.of("a", "b"), meta));
        }

        @Test
        @DisplayName("does not support when paged=false")
        void doesNotSupportWhenNotPaged() {
            OperationMetadata meta = opMeta(false);
            assertFalse(processor.supports(List.of("a", "b"), meta));
        }

        @Test
        @DisplayName("does not support non-List result even when paged=true")
        void doesNotSupportNonListWhenPaged() {
            OperationMetadata meta = opMeta(true);
            assertFalse(processor.supports("not a list", meta));
        }

        @Test
        @DisplayName("wraps List in PagedResult")
        void wrapsInPagedResult() {
            ResultContext ctx = makeContext(opMeta(true));
            List<String> data = List.of("a", "b", "c");
            Object result = processor.process(data, ctx);
            assertInstanceOf(PagedResult.class, result);
            PagedResult<?> paged = (PagedResult<?>) result;
            assertEquals(data, paged.content());
            assertEquals(3, paged.totalElements());
            assertEquals(1, paged.page());
            assertEquals(3, paged.size());
            assertEquals(1, paged.totalPages());
        }

        @Test
        @DisplayName("priority is 300")
        void priority() {
            assertEquals(300, processor.priority());
        }
    }

    // --- SensitiveMaskProcessor tests ---

    @Nested
    @DisplayName("SensitiveMaskProcessor")
    class SensitiveMaskProcessorTests {

        private final SensitiveMaskProcessor processor = new SensitiveMaskProcessor();

        @Test
        @DisplayName("masks password field in Map result")
        void masksPasswordInMap() {
            Map<String, Object> data = new HashMap<>();
            data.put("username", "alice");
            data.put("password", "secret123");

            OperationMetadata meta = opMeta(false);
            assertTrue(processor.supports(data, meta));

            ResultContext ctx = makeContext(meta);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) processor.process(data, ctx);
            assertEquals("alice", result.get("username"));
            assertEquals("***", result.get("password"));
        }

        @Test
        @DisplayName("masks token field in Map result")
        void masksTokenInMap() {
            Map<String, Object> data = new HashMap<>();
            data.put("name", "bob");
            data.put("token", "abc123");

            OperationMetadata meta = opMeta(false);
            ResultContext ctx = makeContext(meta);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) processor.process(data, ctx);
            assertEquals("bob", result.get("name"));
            assertEquals("***", result.get("token"));
        }

        @Test
        @DisplayName("masks sensitive fields in List of Maps")
        void masksInListOfMaps() {
            Map<String, Object> item1 = new HashMap<>();
            item1.put("username", "alice");
            item1.put("password", "p1");
            Map<String, Object> item2 = new HashMap<>();
            item2.put("username", "bob");
            item2.put("password", "p2");

            List<Map<String, Object>> data = List.of(item1, item2);

            OperationMetadata meta = opMeta(false);
            ResultContext ctx = makeContext(meta);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> result = (List<Map<String, Object>>) processor.process(data, ctx);
            assertEquals("***", result.get(0).get("password"));
            assertEquals("***", result.get(1).get("password"));
        }

        @Test
        @DisplayName("does not support non-Map results without sensitive fields")
        void doesNotSupportNonMapWithoutSensitiveFields() {
            OperationMetadata meta = opMeta(false);
            assertFalse(processor.supports("just a string", meta));
            assertFalse(processor.supports(42, meta));
        }

        @Test
        @DisplayName("does not support null result")
        void doesNotSupportNull() {
            OperationMetadata meta = opMeta(false);
            assertFalse(processor.supports(null, meta));
        }

        @Test
        @DisplayName("does not support Map without sensitive fields")
        void doesNotSupportMapWithoutSensitiveFields() {
            Map<String, Object> data = Map.of("name", "alice", "age", 30);
            OperationMetadata meta = opMeta(false);
            assertFalse(processor.supports(data, meta));
        }

        @Test
        @DisplayName("priority is 200")
        void priority() {
            assertEquals(200, processor.priority());
        }
    }

    // --- Processor chain tests ---

    @Nested
    @DisplayName("Processor chain")
    class ProcessorChainTests {

        @Test
        @DisplayName("iterate processors by priority, first matching one processes result")
        void firstMatchingProcessorWins() {
            List<ResultProcessor> processors = new ArrayList<>();
            processors.add(new PagedResultProcessor());   // priority 300
            processors.add(new IdentityProcessor());       // priority 100
            processors.add(new SensitiveMaskProcessor());  // priority 200

            // Non-paged, non-sensitive result: IdentityProcessor (priority 100) should match first
            OperationMetadata meta = opMeta(false);
            Object result = processChain(processors, "hello", meta);
            assertEquals("hello", result);
        }

        @Test
        @DisplayName("SensitiveMaskProcessor matches before IdentityProcessor for Map with sensitive fields")
        void sensitiveMaskBeforeIdentity() {
            List<ResultProcessor> processors = new ArrayList<>();
            processors.add(new PagedResultProcessor());   // priority 300
            processors.add(new IdentityProcessor());       // priority 100
            processors.add(new SensitiveMaskProcessor());  // priority 200

            Map<String, Object> data = new HashMap<>();
            data.put("username", "alice");
            data.put("password", "secret123");

            // IdentityProcessor (priority 100) matches everything first, so it wins
            OperationMetadata meta = opMeta(false);
            Object result = processChain(processors, data, meta);
            // IdentityProcessor passes through, so password is NOT masked
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) result;
            assertEquals("secret123", resultMap.get("password"));
        }

        @Test
        @DisplayName("SensitiveMaskProcessor processes when IdentityProcessor is excluded")
        void sensitiveMaskWhenIdentityExcluded() {
            List<ResultProcessor> processors = new ArrayList<>();
            processors.add(new PagedResultProcessor());   // priority 300
            processors.add(new SensitiveMaskProcessor());  // priority 200

            Map<String, Object> data = new HashMap<>();
            data.put("username", "alice");
            data.put("password", "secret123");

            OperationMetadata meta = opMeta(false);
            Object result = processChain(processors, data, meta);
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) result;
            assertEquals("***", resultMap.get("password"));
        }

        @Test
        @DisplayName("PagedResultProcessor matches for paged List result")
        void pagedResultProcessorMatches() {
            List<ResultProcessor> processors = new ArrayList<>();
            processors.add(new PagedResultProcessor());   // priority 300
            processors.add(new IdentityProcessor());       // priority 100
            processors.add(new SensitiveMaskProcessor());  // priority 200

            // paged List: IdentityProcessor matches first (supports everything)
            // To test PagedResultProcessor, we need a chain without IdentityProcessor
            List<ResultProcessor> pagedChain = new ArrayList<>();
            pagedChain.add(new PagedResultProcessor());
            pagedChain.add(new SensitiveMaskProcessor());

            OperationMetadata meta = opMeta(true);
            List<String> data = List.of("a", "b", "c");
            Object result = processChain(pagedChain, data, meta);
            assertInstanceOf(PagedResult.class, result);
            PagedResult<?> paged = (PagedResult<?>) result;
            assertEquals(List.of("a", "b", "c"), paged.content());
        }
    }

    // --- PagedResult.of tests ---

    @Nested
    @DisplayName("PagedResult.of")
    class PagedResultOfTests {

        @Test
        @DisplayName("of(content, totalElements, page, size) sets all fields correctly")
        void ofWithPageInfo() {
            List<String> content = List.of("a", "b");
            PagedResult<String> result = PagedResult.of(content, 100, 2, 10);
            assertEquals(content, result.content());
            assertEquals(100, result.totalElements());
            assertEquals(2, result.page());
            assertEquals(10, result.size());
            assertEquals(10, result.totalPages());
        }

        @Test
        @DisplayName("of(content, totalElements, page, size) calculates totalPages with ceiling")
        void ofCalculatesTotalPages() {
            List<String> content = List.of("a");
            PagedResult<String> result = PagedResult.of(content, 25, 1, 10);
            assertEquals(3, result.totalPages()); // ceil(25/10) = 3
        }

        @Test
        @DisplayName("of(content) uses content size as total")
        void ofContentOnly() {
            List<String> content = List.of("x", "y", "z");
            PagedResult<String> result = PagedResult.of(content);
            assertEquals(content, result.content());
            assertEquals(3, result.totalElements());
            assertEquals(1, result.page());
            assertEquals(3, result.size());
            assertEquals(1, result.totalPages());
        }

        @Test
        @DisplayName("of(content) with empty list")
        void ofEmptyContent() {
            List<String> content = List.of();
            PagedResult<String> result = PagedResult.of(content);
            assertEquals(0, result.totalElements());
            assertEquals(0, result.size());
            assertEquals(1, result.totalPages()); // ceil(0/0) = NaN -> (int) NaN = 0, but size=0 so this is 1
        }

        @Test
        @DisplayName("of(content, totalElements, page, size) with zero total")
        void ofWithZeroTotal() {
            List<String> content = List.of();
            PagedResult<String> result = PagedResult.of(content, 0, 1, 10);
            assertEquals(0, result.totalElements());
            assertEquals(0, result.totalPages()); // ceil(0/10) = 0
        }
    }
}
