package io.github.afgprojects.framework.core.invocation.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.afgprojects.framework.core.invocation.ArgumentConversionException;
import io.github.afgprojects.framework.core.invocation.ParameterMetadata;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ArgumentResolverChainTest {

    private List<ArgumentResolver> resolvers;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        resolvers = new ArrayList<>();
        resolvers.add(new IdentityResolver());
        resolvers.add(new JacksonConvertResolver());
        resolvers.add(new StringConverterResolver());
        resolvers.add(new CollectionResolver());
        resolvers.add(new NullDefaultResolver());
        resolvers.sort(java.util.Comparator.comparingInt(ArgumentResolver::priority));
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Chain resolution: iterate resolvers by priority, first that supports wins.
     * For null sources, NullDefaultResolver is checked first since it handles
     * null-with-default-value as a special case before type conversion.
     */
    private Object resolve(Object source, Class<?> targetType, ParameterMetadata meta) {
        ResolveContext context = new DefaultResolveContext(meta, objectMapper, Map.of());
        // When source is null, check NullDefaultResolver first (it handles default values)
        if (source == null) {
            for (ArgumentResolver resolver : resolvers) {
                if (resolver instanceof NullDefaultResolver && resolver.supports(targetType, targetType)) {
                    Object result = resolver.resolve(source, targetType, context);
                    if (result != null) return result;
                }
            }
            // If NullDefaultResolver returned null (no default), fall through to normal chain
        }
        Class<?> sourceType = source != null ? source.getClass() : Void.class;
        for (ArgumentResolver resolver : resolvers) {
            if (resolver.supports(sourceType, targetType)) {
                return resolver.resolve(source, targetType, context);
            }
        }
        throw new ArgumentConversionException(meta.name(), sourceType, targetType, null);
    }

    /** Direct resolver invocation (bypasses chain logic) for unit-testing individual resolvers. */
    private Object resolveDirect(ArgumentResolver resolver, Object source, Class<?> targetType, ParameterMetadata meta) {
        ResolveContext context = new DefaultResolveContext(meta, objectMapper, Map.of());
        Class<?> sourceType = source != null ? source.getClass() : Void.class;
        return resolver.resolve(source, targetType, context);
    }

    private ParameterMetadata paramMeta(String name, String defaultValue) {
        return new ParameterMetadata() {
            @Override public String name() { return name; }
            @Override public String type() { return "Object"; }
            @Override public boolean required() { return false; }
            @Override public String defaultValue() { return defaultValue; }
            @Override public int index() { return 0; }
            @Override public String description() { return ""; }
            @Override public List<String> enumValues() { return List.of(); }
            @Override public boolean injected() { return false; }
        };
    }

    @Data
    public static class TestDto {
        private String name;
        private int age;
    }

    @Nested
    @DisplayName("IdentityResolver")
    class IdentityResolverTests {

        @Test
        @DisplayName("same type String passes through")
        void sameTypeString() {
            ParameterMetadata meta = paramMeta("value", "");
            Object result = resolve("hello", String.class, meta);
            assertEquals("hello", result);
        }

        @Test
        @DisplayName("same type Integer passes through")
        void sameTypeInteger() {
            ParameterMetadata meta = paramMeta("value", "");
            Object result = resolve(42, Integer.class, meta);
            assertEquals(42, result);
        }

        @Test
        @DisplayName("subtype passes through for assignable target")
        void subtypePassesThrough() {
            ParameterMetadata meta = paramMeta("value", "");
            ArrayList<String> list = new ArrayList<>(List.of("a", "b"));
            Object result = resolve(list, List.class, meta);
            assertSame(list, result);
        }
    }

    @Nested
    @DisplayName("StringConverterResolver")
    class StringConverterResolverTests {

        @Test
        @DisplayName("String to Integer")
        void stringToInteger() {
            ParameterMetadata meta = paramMeta("num", "");
            Object result = resolve("42", Integer.class, meta);
            assertEquals(42, result);
        }

        @Test
        @DisplayName("String to Long")
        void stringToLong() {
            ParameterMetadata meta = paramMeta("num", "");
            Object result = resolve("123456789", Long.class, meta);
            assertEquals(123456789L, result);
        }

        @Test
        @DisplayName("String to Boolean true")
        void stringToBooleanTrue() {
            ParameterMetadata meta = paramMeta("flag", "");
            Object result = resolve("true", Boolean.class, meta);
            assertEquals(true, result);
        }

        @Test
        @DisplayName("String to Boolean false")
        void stringToBooleanFalse() {
            ParameterMetadata meta = paramMeta("flag", "");
            Object result = resolve("false", Boolean.class, meta);
            assertEquals(false, result);
        }

        @Test
        @DisplayName("String to LocalDate")
        void stringToLocalDate() {
            ParameterMetadata meta = paramMeta("date", "");
            Object result = resolve("2024-01-15", LocalDate.class, meta);
            assertEquals(LocalDate.of(2024, 1, 15), result);
        }

        @Test
        @DisplayName("String to BigDecimal")
        void stringToBigDecimal() {
            ParameterMetadata meta = paramMeta("amount", "");
            Object result = resolve("99.99", BigDecimal.class, meta);
            assertEquals(new BigDecimal("99.99"), result);
        }

        @Test
        @DisplayName("Integer to String")
        void integerToString() {
            ParameterMetadata meta = paramMeta("value", "");
            Object result = resolve(42, String.class, meta);
            assertEquals("42", result);
        }

        @Test
        @DisplayName("String to Double")
        void stringToDouble() {
            ParameterMetadata meta = paramMeta("num", "");
            Object result = resolve("3.14", Double.class, meta);
            assertEquals(3.14, result);
        }

        @Test
        @DisplayName("String to UUID")
        void stringToUuid() {
            ParameterMetadata meta = paramMeta("id", "");
            UUID uuid = UUID.randomUUID();
            Object result = resolve(uuid.toString(), UUID.class, meta);
            assertEquals(uuid, result);
        }

        @Test
        @DisplayName("String to LocalDateTime")
        void stringToLocalDateTime() {
            ParameterMetadata meta = paramMeta("ts", "");
            Object result = resolve("2024-01-15T10:30:00", LocalDateTime.class, meta);
            assertEquals(LocalDateTime.of(2024, 1, 15, 10, 30, 0), result);
        }

        @Test
        @DisplayName("invalid String to Integer throws ArgumentConversionException")
        void invalidStringToInteger() {
            ParameterMetadata meta = paramMeta("num", "");
            assertThrows(ArgumentConversionException.class, () -> resolve("not-a-number", Integer.class, meta));
        }
    }

    @Nested
    @DisplayName("JacksonConvertResolver")
    class JacksonConvertResolverTests {

        @Test
        @DisplayName("Map to POJO via Jackson")
        void mapToPojo() {
            ParameterMetadata meta = paramMeta("dto", "");
            Map<String, Object> map = Map.of("name", "Alice", "age", 30);
            Object result = resolve(map, TestDto.class, meta);
            assertInstanceOf(TestDto.class, result);
            TestDto dto = (TestDto) result;
            assertEquals("Alice", dto.getName());
            assertEquals(30, dto.getAge());
        }

        @Test
        @DisplayName("incompatible conversion throws ArgumentConversionException")
        void incompatibleConversion() {
            ParameterMetadata meta = paramMeta("value", "");
            assertThrows(ArgumentConversionException.class, () -> resolve("not-a-dto", TestDto.class, meta));
        }
    }

    @Nested
    @DisplayName("CollectionResolver")
    class CollectionResolverTests {

        @Test
        @DisplayName("List to Set")
        void listToSet() {
            ParameterMetadata meta = paramMeta("items", "");
            List<String> list = List.of("a", "b", "c");
            Object result = resolve(list, Set.class, meta);
            assertInstanceOf(Set.class, result);
            assertEquals(Set.of("a", "b", "c"), result);
        }

        @Test
        @DisplayName("Set to List")
        void setToList() {
            ParameterMetadata meta = paramMeta("items", "");
            Set<String> set = new LinkedHashSet<>(List.of("x", "y", "z"));
            Object result = resolve(set, List.class, meta);
            assertInstanceOf(List.class, result);
            assertEquals(List.of("x", "y", "z"), result);
        }

        @Test
        @DisplayName("List to LinkedHashSet preserves order")
        void listToLinkedHashSetPreservesOrder() {
            ParameterMetadata meta = paramMeta("items", "");
            List<String> list = List.of("first", "second", "third");
            Object result = resolve(list, LinkedHashSet.class, meta);
            assertInstanceOf(LinkedHashSet.class, result);
            LinkedHashSet<?> resultSet = (LinkedHashSet<?>) result;
            var iterator = resultSet.iterator();
            assertEquals("first", iterator.next());
            assertEquals("second", iterator.next());
            assertEquals("third", iterator.next());
        }

        @Test
        @DisplayName("Set to ArrayList")
        void setToArrayList() {
            ParameterMetadata meta = paramMeta("items", "");
            Set<String> set = new LinkedHashSet<>(List.of("p", "q"));
            Object result = resolve(set, ArrayList.class, meta);
            assertInstanceOf(ArrayList.class, result);
            assertEquals(List.of("p", "q"), result);
        }
    }

    @Nested
    @DisplayName("NullDefaultResolver")
    class NullDefaultResolverTests {

        @Test
        @DisplayName("null with defaultValue converts to Integer")
        void nullWithDefaultValue() {
            ParameterMetadata meta = paramMeta("count", "10");
            NullDefaultResolver resolver = new NullDefaultResolver();
            Object result = resolveDirect(resolver, null, Integer.class, meta);
            assertEquals(10, result);
        }

        @Test
        @DisplayName("null with defaultValue converts to Boolean")
        void nullWithDefaultBoolean() {
            ParameterMetadata meta = paramMeta("enabled", "true");
            NullDefaultResolver resolver = new NullDefaultResolver();
            Object result = resolveDirect(resolver, null, Boolean.class, meta);
            assertEquals(true, result);
        }

        @Test
        @DisplayName("null with empty defaultValue returns null")
        void nullWithEmptyDefaultValue() {
            ParameterMetadata meta = paramMeta("value", "");
            NullDefaultResolver resolver = new NullDefaultResolver();
            Object result = resolveDirect(resolver, null, String.class, meta);
            assertNull(result);
        }

        @Test
        @DisplayName("non-null value passes through")
        void nonNullPassesThrough() {
            ParameterMetadata meta = paramMeta("value", "default");
            NullDefaultResolver resolver = new NullDefaultResolver();
            Object result = resolveDirect(resolver, "actual", String.class, meta);
            assertEquals("actual", result);
        }

        @Test
        @DisplayName("null with defaultValue converts to Long")
        void nullWithDefaultLong() {
            ParameterMetadata meta = paramMeta("timeout", "5000");
            NullDefaultResolver resolver = new NullDefaultResolver();
            Object result = resolveDirect(resolver, null, Long.class, meta);
            assertEquals(5000L, result);
        }

        @Test
        @DisplayName("null with defaultValue converts to Double")
        void nullWithDefaultDouble() {
            ParameterMetadata meta = paramMeta("ratio", "0.5");
            NullDefaultResolver resolver = new NullDefaultResolver();
            Object result = resolveDirect(resolver, null, Double.class, meta);
            assertEquals(0.5, result);
        }
    }

    @Nested
    @DisplayName("Chain resolution")
    class ChainResolutionTests {

        @Test
        @DisplayName("resolvers are applied by priority order")
        void priorityOrder() {
            assertEquals(1, resolvers.get(0).priority());
            assertEquals(2, resolvers.get(1).priority());
            assertEquals(3, resolvers.get(2).priority());
            assertEquals(4, resolvers.get(3).priority());
            assertEquals(5, resolvers.get(4).priority());
        }

        @Test
        @DisplayName("IdentityResolver wins for same type over JacksonConvertResolver")
        void identityWinsOverJackson() {
            ParameterMetadata meta = paramMeta("value", "");
            // String -> String: IdentityResolver (priority 1) should win over JacksonConvertResolver (priority 2)
            Object result = resolve("hello", String.class, meta);
            assertEquals("hello", result);
            assertInstanceOf(String.class, result);
        }

        @Test
        @DisplayName("String→Integer resolved by JacksonConvertResolver (priority 2) before StringConverterResolver (priority 3)")
        void stringToIntegerViaJackson() {
            ParameterMetadata meta = paramMeta("num", "");
            // String → Integer: IdentityResolver doesn't support (not assignable),
            // JacksonConvertResolver (priority 2) supports and handles it via Jackson.
            Object result = resolve("42", Integer.class, meta);
            assertEquals(42, result);
        }

        @Test
        @DisplayName("CollectionResolver handles List→Set before NullDefaultResolver")
        void collectionBeforeNullDefault() {
            ParameterMetadata meta = paramMeta("items", "");
            List<String> list = List.of("a", "b");
            Object result = resolve(list, Set.class, meta);
            assertInstanceOf(Set.class, result);
        }

        @Test
        @DisplayName("full chain: Map→POJO via Jackson")
        void fullChainMapToPojo() {
            ParameterMetadata meta = paramMeta("dto", "");
            Map<String, Object> map = Map.of("name", "Bob", "age", 25);
            Object result = resolve(map, TestDto.class, meta);
            assertInstanceOf(TestDto.class, result);
            TestDto dto = (TestDto) result;
            assertEquals("Bob", dto.getName());
            assertEquals(25, dto.getAge());
        }

        @Test
        @DisplayName("null with defaultValue resolved by NullDefaultResolver in chain")
        void nullWithDefaultInChain() {
            ParameterMetadata meta = paramMeta("count", "10");
            Object result = resolve(null, Integer.class, meta);
            assertEquals(10, result);
        }

        @Test
        @DisplayName("null without defaultValue returns null in chain")
        void nullWithoutDefaultInChain() {
            ParameterMetadata meta = paramMeta("value", "");
            Object result = resolve(null, String.class, meta);
            assertNull(result);
        }
    }
}
