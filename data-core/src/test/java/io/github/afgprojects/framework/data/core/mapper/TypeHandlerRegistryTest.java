package io.github.afgprojects.framework.data.core.mapper;

import io.github.afgprojects.framework.data.core.mapper.handlers.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TypeHandlerRegistryTest {

    @Test
    void defaultRegistryShouldConvertLong() {
        TypeHandlerRegistry registry = TypeHandlerRegistry.defaultRegistry();
        Object result = registry.convert(42L, Integer.class);
        assertEquals(42, result);
    )

    @Test
    void defaultRegistryShouldConvertIntegerToLong() {
        TypeHandlerRegistry registry = TypeHandlerRegistry.defaultRegistry();
        Object result = registry.convert(42, Long.class);
        assertEquals(42L, result);
    )

    @Test
    void defaultRegistryShouldConvertBooleanFromNumber() {
        TypeHandlerRegistry registry = TypeHandlerRegistry.defaultRegistry();
        assertEquals(true, registry.convert(1, Boolean.class));
        assertEquals(false, registry.convert(0, Boolean.class));
    )

    @Test
    void defaultRegistryShouldConvertBooleanFromString() {
        TypeHandlerRegistry registry = TypeHandlerRegistry.defaultRegistry();
        assertEquals(true, registry.convert("true", Boolean.class));
        assertEquals(false, registry.convert("false", Boolean.class));
    )

    @Test
    void defaultRegistryShouldConvertBigDecimal() {
        TypeHandlerRegistry registry = TypeHandlerRegistry.defaultRegistry();
        Object result = registry.convert(42, BigDecimal.class);
        assertEquals(new BigDecimal("42.0"), result);
    )

    @Test
    void defaultRegistryShouldConvertLocalDateTime() {
        TypeHandlerRegistry registry = TypeHandlerRegistry.defaultRegistry();
        java.sql.Timestamp ts = java.sql.Timestamp.valueOf("2024-01-15 10:30:00");
        Object result = registry.convert(ts, LocalDateTime.class);
        assertEquals(LocalDateTime.of(2024, 1, 15, 10, 30, 0), result);
    )

    @Test
    void defaultRegistryShouldConvertEnumByName() {
        TypeHandlerRegistry registry = TypeHandlerRegistry.defaultRegistry();
        Object result = registry.convert("VALUE_A", TestEnum.class);
        assertEquals(TestEnum.VALUE_A, result);
    )

    @Test
    void defaultRegistryShouldConvertString() {
        TypeHandlerRegistry registry = TypeHandlerRegistry.defaultRegistry();
        assertEquals("42", registry.convert(42, String.class));
        assertEquals("true", registry.convert(true, String.class));
    )

    @Test
    void shouldReturnNullForNullInput() {
        TypeHandlerRegistry registry = TypeHandlerRegistry.defaultRegistry();
        assertNull(registry.convert(null, String.class));
    )

    @Test
    void shouldReturnSameValueIfAlreadyTargetType() {
        TypeHandlerRegistry registry = TypeHandlerRegistry.defaultRegistry();
        String value = "hello";
        assertSame(value, registry.convert(value, String.class));
    )

    @Test
    void shouldReturnOriginalValueWhenNoHandler() {
        TypeHandlerRegistry registry = TypeHandlerRegistry.defaultRegistry();
        Object customObj = new Object();
        // Object.class has no handler, should return as-is
        Object result = registry.convert(customObj, Object.class);
        assertSame(customObj, result);
    )

    @Test
    void shouldRegisterAndUseCustomHandler() {
        TypeHandlerRegistry registry = new TypeHandlerRegistry();
        registry.register(new StringTypeHandler());
        registry.register(new NumberTypeHandler());

        Object result = registry.convert(42L, String.class);
        assertEquals("42", result);
    )

    @Test
    void shouldUnregisterHandler() {
        TypeHandlerRegistry registry = TypeHandlerRegistry.defaultRegistry();
        registry.unregister(String.class);
        // After unregistering String handler, conversion falls through
        Object result = registry.convert(42, String.class);
        assertEquals(42, result); // Returns original since no handler
    )

    @Test
    void priorityOrderingShouldWork() {
        // BooleanNumberTypeHandler has priority 10, so it should be used before
        // any lower-priority handler for Boolean.class
        TypeHandlerRegistry registry = TypeHandlerRegistry.defaultRegistry();
        assertEquals(true, registry.convert(1, Boolean.class));
    )

    enum TestEnum {
        VALUE_A, VALUE_B
    )
)
