package io.github.afgprojects.framework.ai.core.tool;

import io.github.afgprojects.framework.core.invocation.BeanInvocationEngine;
import io.github.afgprojects.framework.core.invocation.InvocationPlan;
import io.github.afgprojects.framework.core.invocation.MethodKey;
import io.github.afgprojects.framework.core.invocation.OperationMetadata;
import io.github.afgprojects.framework.core.invocation.ParameterMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ServiceToolAdapter} tests
 */
@DisplayName("ServiceToolAdapter")
class ServiceToolAdapterTest {

    // ── 辅助方法 ───────────────────────────────────────────────────────────────

    private OperationMetadata createOperation(String name, String description, String inputSchema,
                                               String permission, List<String> requiredRoles,
                                               boolean audit, boolean dataScope, boolean deprecated) {
        return new OperationMetadata() {
            @Override public String name() { return name; }
            @Override public String description() { return description; }
            @Override public MethodKey method() { return new MethodKey(name, List.of("java.lang.String")); }
            @Override public List<ParameterMetadata> parameters() { return List.of(); }
            @Override public String returnType() { return "java.lang.Object"; }
            @Override public String returnDescription() { return "result"; }
            @Override public String permission() { return permission; }
            @Override public List<String> requiredRoles() { return requiredRoles; }
            @Override public boolean audit() { return audit; }
            @Override public boolean tenantScope() { return false; }
            @Override public boolean dataScope() { return dataScope; }
            @Override public boolean async() { return false; }
            @Override public boolean deprecated() { return deprecated; }
            @Override public String inputSchema() { return inputSchema; }
            @Override public boolean paged() { return false; }
        };
    }

    private BeanInvocationEngine createEngine() {
        return new BeanInvocationEngine() {
            @Override
            public Object invoke(String serviceName, String operationName, Map<String, Object> arguments) {
                return "Hello " + arguments.get("name");
            }

            @Override
            public Object invoke(String serviceName, String operationName, Object... arguments) {
                return null;
            }

            @Override
            public <T> T invoke(String serviceName, String operationName, Map<String, Object> arguments, Class<T> returnType) {
                return null;
            }

            @Override
            public <R> R invoke(String serviceName, String operationName, Object[] arguments, Class<R> resultType) {
                return null;
            }

            @Override
            public CompletableFuture<Object> invokeAsync(String serviceName, String operationName, Map<String, Object> arguments) {
                return null;
            }

            @Override
            public <T> CompletableFuture<T> invokeAsync(String serviceName, String operationName, Map<String, Object> arguments, Class<T> returnType) {
                return null;
            }

            @Override
            public CompletableFuture<Object> invokeAsync(String serviceName, String operationName, Object... arguments) {
                return null;
            }

            @Override
            public <R> CompletableFuture<R> invokeAsync(String serviceName, String operationName, Object[] arguments, Class<R> resultType) {
                return null;
            }

            @Override
            public InvocationPlan plan(String serviceName, String operationName, Map<String, Object> arguments) {
                return null;
            }

            @Override
            public InvocationPlan plan(String serviceName, String operationName, Object... arguments) {
                return null;
            }
        };
    }

    // ── 基本属性 ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("基本属性")
    class BasicProperties {

        @Test
        @DisplayName("name() 应组合 serviceName 和 operationName")
        void shouldCombineServiceAndOperationName() {
            OperationMetadata op = createOperation("greet", "Greet user", "{}",
                    "user:read", List.of(), true, false, false);
            ServiceToolAdapter adapter = new ServiceToolAdapter("userService", op, createEngine());

            assertThat(adapter.name()).isEqualTo("userService.greet");
        }

        @Test
        @DisplayName("description() 应返回 operation 的描述")
        void shouldReturnOperationDescription() {
            OperationMetadata op = createOperation("greet", "Greet user", "{}",
                    "user:read", List.of(), true, false, false);
            ServiceToolAdapter adapter = new ServiceToolAdapter("userService", op, createEngine());

            assertThat(adapter.description()).isEqualTo("Greet user");
        }

        @Test
        @DisplayName("inputSchema() 应返回 operation 的 inputSchema")
        void shouldReturnOperationInputSchema() {
            String schema = "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}";
            OperationMetadata op = createOperation("greet", "Greet user", schema,
                    "user:read", List.of(), true, false, false);
            ServiceToolAdapter adapter = new ServiceToolAdapter("userService", op, createEngine());

            assertThat(adapter.inputSchema()).isEqualTo(schema);
        }
    }

    // ── 安全属性 ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("安全属性")
    class SecurityProperties {

        @Test
        @DisplayName("requiredPermission() 应返回 operation 的权限")
        void shouldReturnOperationPermission() {
            OperationMetadata op = createOperation("greet", "Greet user", "{}",
                    "user:read", List.of(), true, false, false);
            ServiceToolAdapter adapter = new ServiceToolAdapter("userService", op, createEngine());

            assertThat(adapter.requiredPermission()).isEqualTo("user:read");
        }

        @Test
        @DisplayName("requiredPermission() 应返回 null 当 operation 无权限时")
        void shouldReturnNullPermissionWhenOperationHasNone() {
            OperationMetadata op = createOperation("greet", "Greet user", "{}",
                    null, List.of(), true, false, false);
            ServiceToolAdapter adapter = new ServiceToolAdapter("userService", op, createEngine());

            assertThat(adapter.requiredPermission()).isNull();
        }

        @Test
        @DisplayName("requiredRoles() 应返回 operation 的角色")
        void shouldReturnOperationRoles() {
            OperationMetadata op = createOperation("greet", "Greet user", "{}",
                    "user:read", List.of("ADMIN", "USER"), true, false, false);
            ServiceToolAdapter adapter = new ServiceToolAdapter("userService", op, createEngine());

            assertThat(adapter.requiredRoles()).containsExactlyInAnyOrder("ADMIN", "USER");
        }

        @Test
        @DisplayName("requiredRoles() 应返回空集合当 operation 无角色时")
        void shouldReturnEmptyRolesWhenOperationHasNone() {
            OperationMetadata op = createOperation("greet", "Greet user", "{}",
                    "user:read", List.of(), true, false, false);
            ServiceToolAdapter adapter = new ServiceToolAdapter("userService", op, createEngine());

            assertThat(adapter.requiredRoles()).isEmpty();
        }

        @Test
        @DisplayName("isAuditable() 应返回 operation 的审计标记")
        void shouldReturnOperationAudit() {
            OperationMetadata op = createOperation("greet", "Greet user", "{}",
                    "user:read", List.of(), true, false, false);
            ServiceToolAdapter adapter = new ServiceToolAdapter("userService", op, createEngine());

            assertThat(adapter.isAuditable()).isTrue();
        }

        @Test
        @DisplayName("isAuditable() 应返回 false 当 operation 不需要审计时")
        void shouldReturnFalseAuditWhenOperationNotAuditable() {
            OperationMetadata op = createOperation("greet", "Greet user", "{}",
                    "user:read", List.of(), false, false, false);
            ServiceToolAdapter adapter = new ServiceToolAdapter("userService", op, createEngine());

            assertThat(adapter.isAuditable()).isFalse();
        }

        @Test
        @DisplayName("isSensitive() 应返回 operation 的 dataScope 标记")
        void shouldReturnOperationDataScope() {
            OperationMetadata op = createOperation("greet", "Greet user", "{}",
                    "user:read", List.of(), true, true, false);
            ServiceToolAdapter adapter = new ServiceToolAdapter("userService", op, createEngine());

            assertThat(adapter.isSensitive()).isTrue();
        }
    }

    // ── 执行 ───────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("执行")
    class Execution {

        @Test
        @DisplayName("execute() 应调用 engine.invoke() 并返回结果")
        void shouldInvokeEngineAndReturnResult() {
            OperationMetadata op = createOperation("greet", "Greet user", "{}",
                    "user:read", List.of(), true, false, false);
            ServiceToolAdapter adapter = new ServiceToolAdapter("userService", op, createEngine());

            Object result = adapter.execute(Map.of("name", "World"));

            assertThat(result).isEqualTo("Hello World");
        }

        @Test
        @DisplayName("execute(input, context) 应调用 engine.invoke() 并返回结果")
        void shouldInvokeEngineWithContextAndReturnResult() {
            OperationMetadata op = createOperation("greet", "Greet user", "{}",
                    "user:read", List.of(), true, false, false);
            ServiceToolAdapter adapter = new ServiceToolAdapter("userService", op, createEngine());

            Object result = adapter.execute(Map.of("name", "World"), ToolContext.empty());

            assertThat(result).isEqualTo("Hello World");
        }
    }

    // ── 辅助访问器 ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("辅助访问器")
    class Accessors {

        @Test
        @DisplayName("serviceName() 应返回服务名")
        void shouldReturnServiceName() {
            OperationMetadata op = createOperation("greet", "Greet user", "{}",
                    "user:read", List.of(), true, false, false);
            ServiceToolAdapter adapter = new ServiceToolAdapter("userService", op, createEngine());

            assertThat(adapter.serviceName()).isEqualTo("userService");
        }

        @Test
        @DisplayName("operationMetadata() 应返回原始 OperationMetadata")
        void shouldReturnOperationMetadata() {
            OperationMetadata op = createOperation("greet", "Greet user", "{}",
                    "user:read", List.of(), true, false, false);
            ServiceToolAdapter adapter = new ServiceToolAdapter("userService", op, createEngine());

            assertThat(adapter.operationMetadata()).isSameAs(op);
        }
    }
}
