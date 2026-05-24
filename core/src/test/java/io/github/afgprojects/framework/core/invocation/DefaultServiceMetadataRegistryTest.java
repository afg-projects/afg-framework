package io.github.afgprojects.framework.core.invocation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultServiceMetadataRegistryTest {

    private DefaultServiceMetadataRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DefaultServiceMetadataRegistry();
    }

    // --- Helper factories ---

    private ServiceMetadata<?> createServiceMetadata(String name, String category, List<String> tags, List<OperationMetadata> operations) {
        return new ServiceMetadata<Object>() {
            @Override public String serviceName() { return name; }
            @Override public String description() { return "Service " + name; }
            @Override public String category() { return category; }
            @Override public List<String> tags() { return tags; }
            @Override public Class<Object> serviceType() { return Object.class; }
            @Override public List<OperationMetadata> operations() { return operations; }
        };
    }

    private OperationMetadata createOperationMetadata(String name) {
        return new OperationMetadata() {
            @Override public String name() { return name; }
            @Override public String description() { return "Operation " + name; }
            @Override public MethodKey method() { return new MethodKey(name, List.of()); }
            @Override public List<ParameterMetadata> parameters() { return List.of(); }
            @Override public String returnType() { return "java.lang.Object"; }
            @Override public String returnDescription() { return "result"; }
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

    // --- register and get ---

    @Test
    void register_andGet_shouldReturnRegisteredMetadata() {
        ServiceMetadata<?> metadata = createServiceMetadata("userService", "system", List.of("user"), List.of());
        registry.register(metadata);

        var result = registry.get("userService");
        assertTrue(result.isPresent());
        assertSame(metadata, result.get());
    }

    @Test
    void get_shouldReturnEmptyWhenNotFound() {
        var result = registry.get("nonExistent");
        assertTrue(result.isEmpty());
    }

    @Test
    void register_shouldOverwriteExistingService() {
        ServiceMetadata<?> first = createServiceMetadata("userService", "system", List.of(), List.of());
        ServiceMetadata<?> second = createServiceMetadata("userService", "core", List.of(), List.of());
        registry.register(first);
        registry.register(second);

        var result = registry.get("userService");
        assertTrue(result.isPresent());
        assertSame(second, result.get());
        assertEquals("core", result.get().category());
    }

    // --- getOperation ---

    @Test
    void getOperation_shouldReturnMatchingOperation() {
        OperationMetadata op1 = createOperationMetadata("create");
        OperationMetadata op2 = createOperationMetadata("delete");
        ServiceMetadata<?> metadata = createServiceMetadata("userService", "system", List.of(), List.of(op1, op2));
        registry.register(metadata);

        var result = registry.getOperation("userService", "create");
        assertTrue(result.isPresent());
        assertSame(op1, result.get());
    }

    @Test
    void getOperation_shouldReturnEmptyWhenServiceNotFound() {
        var result = registry.getOperation("nonExistent", "create");
        assertTrue(result.isEmpty());
    }

    @Test
    void getOperation_shouldReturnEmptyWhenOperationNotFound() {
        OperationMetadata op1 = createOperationMetadata("create");
        ServiceMetadata<?> metadata = createServiceMetadata("userService", "system", List.of(), List.of(op1));
        registry.register(metadata);

        var result = registry.getOperation("userService", "nonExistent");
        assertTrue(result.isEmpty());
    }

    // --- getAll ---

    @Test
    void getAll_shouldReturnAllRegisteredServices() {
        ServiceMetadata<?> user = createServiceMetadata("userService", "system", List.of(), List.of());
        ServiceMetadata<?> role = createServiceMetadata("roleService", "system", List.of(), List.of());
        registry.register(user);
        registry.register(role);

        List<ServiceMetadata<?>> all = registry.getAll();
        assertEquals(2, all.size());
    }

    @Test
    void getAll_shouldReturnEmptyListWhenNothingRegistered() {
        List<ServiceMetadata<?>> all = registry.getAll();
        assertTrue(all.isEmpty());
    }

    @Test
    void getAll_shouldReturnImmutableCopy() {
        ServiceMetadata<?> user = createServiceMetadata("userService", "system", List.of(), List.of());
        registry.register(user);

        List<ServiceMetadata<?>> all = registry.getAll();
        assertThrows(UnsupportedOperationException.class, () -> all.add(user));
    }

    // --- getByCategory ---

    @Test
    void getByCategory_shouldFilterCorrectly() {
        ServiceMetadata<?> user = createServiceMetadata("userService", "system", List.of(), List.of());
        ServiceMetadata<?> role = createServiceMetadata("roleService", "system", List.of(), List.of());
        ServiceMetadata<?> file = createServiceMetadata("fileService", "storage", List.of(), List.of());
        registry.register(user);
        registry.register(role);
        registry.register(file);

        List<ServiceMetadata<?>> system = registry.getByCategory("system");
        assertEquals(2, system.size());
        assertTrue(system.stream().allMatch(m -> "system".equals(m.category())));
    }

    @Test
    void getByCategory_shouldReturnEmptyWhenNoMatch() {
        ServiceMetadata<?> user = createServiceMetadata("userService", "system", List.of(), List.of());
        registry.register(user);

        List<ServiceMetadata<?>> result = registry.getByCategory("nonExistent");
        assertTrue(result.isEmpty());
    }

    @Test
    void getByCategory_shouldReturnEmptyWhenCategoryIsNull() {
        ServiceMetadata<?> user = createServiceMetadata("userService", null, List.of(), List.of());
        registry.register(user);

        List<ServiceMetadata<?>> result = registry.getByCategory("system");
        assertTrue(result.isEmpty());
    }

    // --- getByTag ---

    @Test
    void getByTag_shouldFilterCorrectly() {
        ServiceMetadata<?> user = createServiceMetadata("userService", "system", List.of("user", "auth"), List.of());
        ServiceMetadata<?> role = createServiceMetadata("roleService", "system", List.of("role", "auth"), List.of());
        ServiceMetadata<?> file = createServiceMetadata("fileService", "storage", List.of("file"), List.of());
        registry.register(user);
        registry.register(role);
        registry.register(file);

        List<ServiceMetadata<?>> auth = registry.getByTag("auth");
        assertEquals(2, auth.size());
        assertTrue(auth.stream().allMatch(m -> m.tags().contains("auth")));
    }

    @Test
    void getByTag_shouldReturnEmptyWhenNoMatch() {
        ServiceMetadata<?> user = createServiceMetadata("userService", "system", List.of("user"), List.of());
        registry.register(user);

        List<ServiceMetadata<?>> result = registry.getByTag("nonExistent");
        assertTrue(result.isEmpty());
    }
}
