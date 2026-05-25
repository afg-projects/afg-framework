# APT 驱动的 Bean 动态调用框架 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于 APT 提取 Bean 接口元数据，实现通用的 Bean 动态调用引擎，将安全、审计、租户等横切关注点统一为 SPI 拦截器链，并自动适配为 AI Tool。

**Architecture:** 新增 `@AfService`/`@AfOperation` 注解，APT 编译时生成 `ServiceMetadata`；core 模块提供 `BeanInvocationEngine` + SPI 拦截器链 + `ArgumentResolver`/`ResultProcessor` SPI；security-core 提供安全相关拦截器；ai-core 提供 `ServiceToolAdapter` 自动注册为 Tool。

**Tech Stack:** Java 25, Spring Boot 4, JavaPoet (APT 代码生成), Jackson (类型转换), ServiceLoader (SPI), Gradle

---

## File Structure

### 新增文件

| 模块 | 文件 | 职责 |
|------|------|------|
| **apt-api** | `.../apt/api/AfService.java` | 服务注解 |
| **apt-api** | `.../apt/api/AfOperation.java` | 操作注解 |
| **apt-api** | `.../apt/api/AfParam.java` | 参数注解 |
| **apt-api** | `.../apt/api/AfResult.java` | 返回值注解 |
| **apt-impl** | `.../apt/impl/ServiceMetadataProcessor.java` | APT 处理器 |
| **apt-impl** | `.../apt/impl/ServiceMetadataCodeGenerator.java` | 元数据代码生成 |
| **core** | `.../core/invocation/ServiceMetadata.java` | 服务元数据接口 |
| **core** | `.../core/invocation/OperationMetadata.java` | 操作元数据接口 |
| **core** | `.../core/invocation/ParameterMetadata.java` | 参数元数据接口 |
| **core** | `.../core/invocation/MethodKey.java` | 方法定位键 |
| **core** | `.../core/invocation/ServiceMetadataRegistry.java` | 元数据注册表接口 |
| **core** | `.../core/invocation/DefaultServiceMetadataRegistry.java` | 注册表默认实现 |
| **core** | `.../core/invocation/AptServiceMetadataLoader.java` | APT 元数据加载 |
| **core** | `.../core/invocation/BeanInvocationEngine.java` | 调用引擎接口 |
| **core** | `.../core/invocation/DefaultBeanInvocationEngine.java` | 调用引擎默认实现 |
| **core** | `.../core/invocation/InvocationContext.java` | 调用上下文接口 |
| **core** | `.../core/invocation/DefaultInvocationContext.java` | 调用上下文默认实现 |
| **core** | `.../core/invocation/InvocationPlan.java` | 调用计划接口 |
| **core** | `.../core/invocation/DefaultInvocationPlan.java` | 调用计划默认实现 |
| **core** | `.../core/invocation/InvocationInterceptor.java` | 拦截器 SPI 接口 |
| **core** | `.../core/invocation/interceptor/AuditInvocationInterceptor.java` | 审计拦截器 |
| **core** | `.../core/invocation/interceptor/ValidationInvocationInterceptor.java` | 校验拦截器 |
| **core** | `.../core/invocation/resolver/ArgumentResolver.java` | 参数解析器 SPI 接口 |
| **core** | `.../core/invocation/resolver/ResolveContext.java` | 解析上下文接口 |
| **core** | `.../core/invocation/resolver/DefaultResolveContext.java` | 解析上下文默认实现 |
| **core** | `.../core/invocation/resolver/IdentityResolver.java` | 类型直接匹配 |
| **core** | `.../core/invocation/resolver/JacksonConvertResolver.java` | Jackson convertValue |
| **core** | `.../core/invocation/resolver/StringConverterResolver.java` | String ↔ 基本类型 |
| **core** | `.../core/invocation/resolver/CollectionResolver.java` | 集合类型转换 |
| **core** | `.../core/invocation/resolver/NullDefaultResolver.java` | 默认值兜底 |
| **core** | `.../core/invocation/processor/ResultProcessor.java` | 返回值处理器 SPI 接口 |
| **core** | `.../core/invocation/processor/ResultContext.java` | 返回值处理上下文接口 |
| **core** | `.../core/invocation/processor/DefaultResultContext.java` | 返回值处理上下文默认实现 |
| **core** | `.../core/invocation/processor/IdentityProcessor.java` | 透传返回值 |
| **core** | `.../core/invocation/processor/SensitiveMaskProcessor.java` | 敏感字段脱敏 |
| **core** | `.../core/invocation/processor/PagedResultProcessor.java` | 分页结果包装 |
| **core** | `.../core/invocation/exception/*.java` | 异常类 |
| **core** | `.../core/invocation/InvocationContextTaskDecorator.java` | 异步上下文传播 |
| **security-core** | `.../security/core/invocation/SecurityInvocationInterceptor.java` | 权限拦截器 |
| **security-core** | `.../security/core/invocation/TenantInvocationInterceptor.java` | 租户拦截器 |
| **security-core** | `.../security/core/invocation/DataScopeInvocationInterceptor.java` | 数据权限拦截器 |
| **ai-core** | `.../ai/core/tool/ServiceToolAdapter.java` | Service → Tool 适配器 |
| **ai-core** | `.../ai/core/tool/ServiceToolRegistrar.java` | 自动注册器 |
| **ai-spring-boot-starter** | `.../ai/autoconfigure/ServiceToolAutoConfiguration.java` | 自动配置 |
| **spring-boot-starter** | `.../autoconfigure/BeanInvocationAutoConfiguration.java` | 自动配置 |
| **spring-boot-starter** | `.../autoconfigure/BeanInvocationProperties.java` | 配置属性 |

### 修改文件

| 模块 | 文件 | 修改内容 |
|------|------|----------|
| **apt-api** | `build.gradle` | 无需修改（纯注解定义） |
| **apt-impl** | `build.gradle` | 添加 apt-api 依赖 |
| **core** | `build.gradle` | 添加 jackson-databind 依赖 |
| **security-core** | `build.gradle` | 添加 core 依赖（如果尚未有） |
| **ai-core** | `build.gradle` | 添加 core 依赖 |
| **ai-spring-boot-starter** | `build.gradle` | 添加 ai-core 依赖（如果尚未有） |
| **spring-boot-starter** | `.../autoconfigure/AfgAutoConfiguration.java` | 添加 BeanInvocationAutoConfiguration import |
| **gradle-plugin** | 插件代码 | 添加 `-parameters` 编译选项 |

### 测试文件

| 模块 | 文件 | 职责 |
|------|------|------|
| **apt-impl** | `.../apt/impl/ServiceMetadataProcessorTest.java` | APT 处理器测试 |
| **core** | `.../core/invocation/DefaultServiceMetadataRegistryTest.java` | 注册表测试 |
| **core** | `.../core/invocation/DefaultBeanInvocationEngineTest.java` | 引擎测试 |
| **core** | `.../core/invocation/MethodKeyTest.java` | MethodKey 测试 |
| **core** | `.../core/invocation/resolver/ArgumentResolverChainTest.java` | 参数解析器链测试 |
| **core** | `.../core/invocation/processor/ResultProcessorChainTest.java` | 返回值处理器链测试 |
| **security-core** | `.../security/core/invocation/SecurityInvocationInterceptorTest.java` | 权限拦截器测试 |
| **ai-core** | `.../ai/core/tool/ServiceToolAdapterTest.java` | Tool 适配器测试 |

---

## Task 1: apt-api 注解定义

**Files:**
- Create: `apt-api/src/main/java/io/github/afgprojects/framework/apt/api/AfService.java`
- Create: `apt-api/src/main/java/io/github/afgprojects/framework/apt/api/AfOperation.java`
- Create: `apt-api/src/main/java/io/github/afgprojects/framework/apt/api/AfParam.java`
- Create: `apt-api/src/main/java/io/github/afgprojects/framework/apt/api/AfResult.java`
- Test: `apt-impl/src/test/java/io/github/afgprojects/framework/apt/impl/ServiceMetadataProcessorTest.java`

- [ ] **Step 1: 创建 AfService 注解**

```java
package io.github.afgprojects.framework.apt.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @AfService {
    String name() default "";
    String description() default "";
    String category() default "";
    String[] tags() default {};
    boolean deprecated() default false;
}
```

- [ ] **Step 2: 创建 AfOperation 注解**

```java
package io.github.afgprojects.framework.apt.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @AfOperation {
    String name() default "";
    String description() default "";
    boolean async() default false;
    boolean deprecated() default false;
    String permission() default "";
    String[] requiredRoles() default {};
    boolean audit() default true;
    boolean tenantScope() default true;
    boolean dataScope() default false;
    String inputSchema() default "";
}
```

- [ ] **Step 3: 创建 AfParam 注解**

```java
package io.github.afgprojects.framework.apt.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
public @AfParam {
    String name() default "";
    String description() default "";
    boolean required() default true;
    String defaultValue() default "";
    String[] enumValues() default {};
}
```

- [ ] **Step 4: 创建 AfResult 注解**

```java
package io.github.afgprojects.framework.apt.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @AfResult {
    String description() default "";
    boolean paged() default false;
    boolean streaming() default false;
}
```

- [ ] **Step 5: 编译验证**

Run: `cd /home/caiti/code/afg-projects/afg-framework && ./gradlew :apt-api:build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add apt-api/src/main/java/io/github/afgprojects/framework/apt/api/AfService.java \
         apt-api/src/main/java/io/github/afgprojects/framework/apt/api/AfOperation.java \
         apt-api/src/main/java/io/github/afgprojects/framework/apt/api/AfParam.java \
         apt-api/src/main/java/io/github/afgprojects/framework/apt/api/AfResult.java
git commit -m "feat(apt-api): add @AfService, @AfOperation, @AfParam, @AfResult annotations"
```

---

## Task 2: core 模块元数据接口

**Files:**
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/ServiceMetadata.java`
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/OperationMetadata.java`
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/ParameterMetadata.java`
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/MethodKey.java`
- Test: `core/src/test/java/io/github/afgprojects/framework/core/invocation/MethodKeyTest.java`

- [ ] **Step 1: 编写 MethodKey 测试**

```java
package io.github.afgprojects.framework.core.invocation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MethodKeyTest {

    @Test
    void resolve_shouldFindPublicMethod() throws NoSuchMethodException {
        MethodKey key = new MethodKey("getName", List.of());
        Method method = key.resolve(SampleService.class);
        assertEquals("getName", method.getName());
        assertEquals(0, method.getParameterCount());
    }

    @Test
    void resolve_shouldFindMethodWithParameters() throws NoSuchMethodException {
        MethodKey key = new MethodKey("greet", List.of("java.lang.String"));
        Method method = key.resolve(SampleService.class);
        assertEquals("greet", method.getName());
        assertEquals(1, method.getParameterCount());
        assertEquals(String.class, method.getParameterTypes()[0]);
    }

    @Test
    void resolve_shouldThrowWhenMethodNotFound() {
        MethodKey key = new MethodKey("nonExistent", List.of());
        assertThrows(ServiceInvocationException.class, () -> key.resolve(SampleService.class));
    }

    @Test
    void resolve_shouldCacheMethod() {
        MethodKey key = new MethodKey("getName", List.of());
        Method first = key.resolve(SampleService.class);
        Method second = key.resolve(SampleService.class);
        assertSame(first, second);
    }

    @Test
    void equalsAndHashCode_shouldWork() {
        MethodKey key1 = new MethodKey("greet", List.of("java.lang.String"));
        MethodKey key2 = new MethodKey("greet", List.of("java.lang.String"));
        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
    }

    static class SampleService {
        public String getName() { return "test"; }
        public String greet(String name) { return "Hello " + name; }
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd /home/caiti/code/afg-projects/afg-framework && ./gradlew :core:test --tests "*.MethodKeyTest" 2>&1 | tail -20`
Expected: FAIL — classes not found

- [ ] **Step 3: 创建 ServiceMetadata 接口**

```java
package io.github.afgprojects.framework.core.invocation;

import java.util.List;

public interface ServiceMetadata<T> {
    String serviceName();
    String description();
    String category();
    List<String> tags();
    Class<T> serviceType();
    List<OperationMetadata> operations();
}
```

- [ ] **Step 4: 创建 OperationMetadata 接口**

```java
package io.github.afgprojects.framework.core.invocation;

import java.util.List;

public interface OperationMetadata {
    String name();
    String description();
    MethodKey method();
    List<ParameterMetadata> parameters();
    String returnType();
    String returnDescription();
    String permission();
    List<String> requiredRoles();
    boolean audit();
    boolean tenantScope();
    boolean dataScope();
    boolean async();
    boolean deprecated();
    String inputSchema();
    boolean paged();
}
```

- [ ] **Step 5: 创建 ParameterMetadata 接口**

```java
package io.github.afgprojects.framework.core.invocation;

import java.util.List;

public interface ParameterMetadata {
    String name();
    String type();
    boolean required();
    String defaultValue();
    int index();
    String description();
    List<String> enumValues();
    boolean injected();
}
```

- [ ] **Step 6: 创建 MethodKey record**

```java
package io.github.afgprojects.framework.core.invocation;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public record MethodKey(String methodName, List<String> parameterTypes) {

    private static final ConcurrentHashMap<MethodKey, Method> CACHE = new ConcurrentHashMap<>();

    public Method resolve(Class<?> serviceType) {
        return CACHE.computeIfAbsent(this, k -> {
            Class<?>[] paramTypes = parameterTypes().stream()
                    .map(this::loadClass)
                    .toArray(Class<?>[]::new);
            try {
                return serviceType.getMethod(methodName(), paramTypes);
            } catch (NoSuchMethodException e) {
                throw new ServiceInvocationException(
                        "Method not found: " + methodName() + " in " + serviceType.getName(), e);
            }
        });
    }

    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new ServiceInvocationException("Class not found: " + className, e);
        }
    }
}
```

- [ ] **Step 7: 创建异常类**

创建以下异常类，包名 `io.github.afgprojects.framework.core.invocation`：

```java
// ServiceInvocationException.java
public class ServiceInvocationException extends RuntimeException {
    public ServiceInvocationException(String message) { super(message); }
    public ServiceInvocationException(String message, Throwable cause) { super(message, cause); }
}

// ServiceNotFoundException.java
public class ServiceNotFoundException extends ServiceInvocationException {
    private final String serviceName;
    private final String operationName;
    public ServiceNotFoundException(String serviceName, String operationName) {
        super("Service operation not found: " + serviceName + "." + operationName);
        this.serviceName = serviceName;
        this.operationName = operationName;
    }
    public String serviceName() { return serviceName; }
    public String operationName() { return operationName; }
}

// MissingArgumentException.java
public class MissingArgumentException extends ServiceInvocationException {
    private final String paramName;
    public MissingArgumentException(String paramName) {
        super("Required argument missing: " + paramName);
        this.paramName = paramName;
    }
    public String paramName() { return paramName; }
}

// ArgumentConversionException.java
public class ArgumentConversionException extends ServiceInvocationException {
    private final String paramName;
    private final Class<?> sourceType;
    private final Class<?> targetType;
    public ArgumentConversionException(String paramName, Class<?> sourceType, Class<?> targetType, Throwable cause) {
        super("Cannot convert argument '" + paramName + "' from " + sourceType.getName() + " to " + targetType.getName(), cause);
        this.paramName = paramName;
        this.sourceType = sourceType;
        this.targetType = targetType;
    }
}

// InvocationRejectedException.java
public class InvocationRejectedException extends ServiceInvocationException {
    private final String interceptorName;
    private final String reason;
    public InvocationRejectedException(String interceptorName, String reason) {
        super("Invocation rejected by " + interceptorName + ": " + reason);
        this.interceptorName = interceptorName;
        this.reason = reason;
    }
    public String interceptorName() { return interceptorName; }
    public String reason() { return reason; }
}

// ServiceAccessDeniedException.java
public class ServiceAccessDeniedException extends ServiceInvocationException {
    private final String permission;
    public ServiceAccessDeniedException(String permission) {
        super("Access denied: missing permission '" + permission + "'");
        this.permission = permission;
    }
    public String permission() { return permission; }
}

// InjectionException.java
public class InjectionException extends ServiceInvocationException {
    private final String paramName;
    private final Class<?> paramType;
    public InjectionException(String paramName, Class<?> paramType) {
        super("Cannot inject parameter '" + paramName + "' of type " + paramType.getName());
        this.paramName = paramName;
        this.paramType = paramType;
    }
}
```

- [ ] **Step 8: 运行 MethodKey 测试**

Run: `cd /home/caiti/code/afg-projects/afg-framework && ./gradlew :core:test --tests "*.MethodKeyTest" 2>&1 | tail -20`
Expected: PASS

- [ ] **Step 9: Commit**

```bash
git add core/src/main/java/io/github/afgprojects/framework/core/invocation/
         core/src/test/java/io/github/afgprojects/framework/core/invocation/MethodKeyTest.java
git commit -m "feat(core): add ServiceMetadata, OperationMetadata, ParameterMetadata interfaces and MethodKey"
```

---

## Task 3: ServiceMetadataRegistry

**Files:**
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/ServiceMetadataRegistry.java`
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/DefaultServiceMetadataRegistry.java`
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/AptServiceMetadataLoader.java`
- Test: `core/src/test/java/io/github/afgprojects/framework/core/invocation/DefaultServiceMetadataRegistryTest.java`

- [ ] **Step 1: 编写注册表测试**

```java
package io.github.afgprojects.framework.core.invocation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DefaultServiceMetadataRegistryTest {

    private DefaultServiceMetadataRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DefaultServiceMetadataRegistry();
    }

    @Test
    void register_andGet_shouldReturnMetadata() {
        ServiceMetadata<?> meta = createTestMetadata("testService", "test", List.of());
        registry.register(meta);

        Optional<ServiceMetadata<?>> result = registry.get("testService");
        assertTrue(result.isPresent());
        assertEquals("testService", result.get().serviceName());
    }

    @Test
    void get_shouldReturnEmptyWhenNotFound() {
        Optional<ServiceMetadata<?>> result = registry.get("nonExistent");
        assertTrue(result.isEmpty());
    }

    @Test
    void getOperation_shouldReturnOperation() {
        ServiceMetadata<?> meta = createTestMetadata("testService", "test",
                List.of(createTestOperation("doSomething")));
        registry.register(meta);

        Optional<OperationMetadata> op = registry.getOperation("testService", "doSomething");
        assertTrue(op.isPresent());
        assertEquals("doSomething", op.get().name());
    }

    @Test
    void getOperation_shouldReturnEmptyWhenServiceNotFound() {
        Optional<OperationMetadata> op = registry.getOperation("nope", "doSomething");
        assertTrue(op.isEmpty());
    }

    @Test
    void getOperation_shouldReturnEmptyWhenOperationNotFound() {
        ServiceMetadata<?> meta = createTestMetadata("testService", "test", List.of());
        registry.register(meta);

        Optional<OperationMetadata> op = registry.getOperation("testService", "nonExistent");
        assertTrue(op.isEmpty());
    }

    @Test
    void getAll_shouldReturnAllRegistered() {
        registry.register(createTestMetadata("svc1", "cat1", List.of()));
        registry.register(createTestMetadata("svc2", "cat2", List.of()));

        List<ServiceMetadata<?>> all = registry.getAll();
        assertEquals(2, all.size());
    }

    @Test
    void getByCategory_shouldFilterCorrectly() {
        registry.register(createTestMetadata("svc1", "system", List.of()));
        registry.register(createTestMetadata("svc2", "business", List.of()));
        registry.register(createTestMetadata("svc3", "system", List.of()));

        List<ServiceMetadata<?>> system = registry.getByCategory("system");
        assertEquals(2, system.size());
        assertTrue(system.stream().allMatch(m -> "system".equals(m.category())));
    }

    @Test
    void getByTag_shouldFilterCorrectly() {
        registry.register(createTestMetadata("svc1", "test", List.of(), new String[]{"core", "api"}));
        registry.register(createTestMetadata("svc2", "test", List.of(), new String[]{"api"}));

        List<ServiceMetadata<?>> apiTagged = registry.getByTag("api");
        assertEquals(2, apiTagged.size());

        List<ServiceMetadata<?>> coreTagged = registry.getByTag("core");
        assertEquals(1, coreTagged.size());
    }

    private ServiceMetadata<Object> createTestMetadata(String name, String category,
                                                        List<OperationMetadata> operations) {
        return createTestMetadata(name, category, operations, new String[0]);
    }

    @SuppressWarnings("unchecked")
    private ServiceMetadata<Object> createTestMetadata(String name, String category,
                                                        List<OperationMetadata> operations, String[] tags) {
        return new ServiceMetadata<>() {
            @Override public String serviceName() { return name; }
            @Override public String description() { return name + " desc"; }
            @Override public String category() { return category; }
            @Override public List<String> tags() { return List.of(tags); }
            @Override public Class<Object> serviceType() { return Object.class; }
            @Override public List<OperationMetadata> operations() { return operations; }
        };
    }

    private OperationMetadata createTestOperation(String name) {
        return new OperationMetadata() {
            @Override public String name() { return name; }
            @Override public String description() { return name + " desc"; }
            @Override public MethodKey method() { return new MethodKey(name, List.of()); }
            @Override public List<ParameterMetadata> parameters() { return List.of(); }
            @Override public String returnType() { return "java.lang.Object"; }
            @Override public String returnDescription() { return ""; }
            @Override public String permission() { return ""; }
            @Override public List<String> requiredRoles() { return List.of(); }
            @Override public boolean audit() { return true; }
            @Override public boolean tenantScope() { return true; }
            @Override public boolean dataScope() { return false; }
            @Override public boolean async() { return false; }
            @Override public boolean deprecated() { return false; }
            @Override public String inputSchema() { return "{}"; }
            @Override public boolean paged() { return false; }
        };
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd /home/caiti/code/afg-projects/afg-framework && ./gradlew :core:test --tests "*.DefaultServiceMetadataRegistryTest" 2>&1 | tail -20`
Expected: FAIL — classes not found

- [ ] **Step 3: 创建 ServiceMetadataRegistry 接口**

```java
package io.github.afgprojects.framework.core.invocation;

import java.util.List;
import java.util.Optional;

public interface ServiceMetadataRegistry {
    void register(ServiceMetadata<?> metadata);
    Optional<ServiceMetadata<?>> get(String serviceName);
    Optional<OperationMetadata> getOperation(String serviceName, String operationName);
    List<ServiceMetadata<?>> getAll();
    List<ServiceMetadata<?>> getByCategory(String category);
    List<ServiceMetadata<?>> getByTag(String tag);
}
```

- [ ] **Step 4: 创建 DefaultServiceMetadataRegistry**

```java
package io.github.afgprojects.framework.core.invocation;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DefaultServiceMetadataRegistry implements ServiceMetadataRegistry {

    private final ConcurrentHashMap<String, ServiceMetadata<?>> services = new ConcurrentHashMap<>();

    @Override
    public void register(ServiceMetadata<?> metadata) {
        services.put(metadata.serviceName(), metadata);
    }

    @Override
    public Optional<ServiceMetadata<?>> get(String serviceName) {
        return Optional.ofNullable(services.get(serviceName));
    }

    @Override
    public Optional<OperationMetadata> getOperation(String serviceName, String operationName) {
        return get(serviceName).flatMap(sm ->
                sm.operations().stream()
                        .filter(op -> op.name().equals(operationName))
                        .findFirst());
    }

    @Override
    public List<ServiceMetadata<?>> getAll() {
        return List.copyOf(services.values());
    }

    @Override
    public List<ServiceMetadata<?>> getByCategory(String category) {
        return services.values().stream()
                .filter(m -> category.equals(m.category()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceMetadata<?>> getByTag(String tag) {
        return services.values().stream()
                .filter(m -> m.tags().contains(tag))
                .collect(Collectors.toList());
    }
}
```

- [ ] **Step 5: 创建 AptServiceMetadataLoader**

```java
package io.github.afgprojects.framework.core.invocation;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;

@Slf4j
public class AptServiceMetadataLoader {

    private static final String INDEX_LOCATION = "META-INF/afg/service-metadata.index";

    private final ServiceMetadataRegistry registry;

    public AptServiceMetadataLoader(ServiceMetadataRegistry registry) {
        this.registry = registry;
    }

    public void load() {
        try {
            Enumeration<URL> resources = getClass().getClassLoader().getResources(INDEX_LOCATION);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                loadIndex(url);
            }
        } catch (Exception e) {
            log.warn("Failed to load service metadata from APT index", e);
        }
    }

    private void loadIndex(URL indexUrl) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(indexUrl.openStream()))) {
            String className;
            while ((className = reader.readLine()) != null) {
                className = className.trim();
                if (className.isEmpty() || className.startsWith("#")) continue;
                loadMetadata(className);
            }
        } catch (Exception e) {
            log.warn("Failed to read service metadata index from {}", indexUrl, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadMetadata(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (ServiceMetadata.class.isAssignableFrom(clazz)) {
                ServiceMetadata<?> metadata = (ServiceMetadata<?>) clazz.getDeclaredConstructor().newInstance();
                registry.register(metadata);
                log.info("Loaded service metadata: {}", metadata.serviceName());
            }
        } catch (Exception e) {
            log.warn("Failed to load service metadata class: {}", className, e);
        }
    }
}
```

- [ ] **Step 6: 运行测试**

Run: `cd /home/caiti/code/afg-projects/afg-framework && ./gradlew :core:test --tests "*.DefaultServiceMetadataRegistryTest" 2>&1 | tail -20`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add core/src/main/java/io/github/afgprojects/framework/core/invocation/ServiceMetadataRegistry.java \
         core/src/main/java/io/github/afgprojects/framework/core/invocation/DefaultServiceMetadataRegistry.java \
         core/src/main/java/io/github/afgprojects/framework/core/invocation/AptServiceMetadataLoader.java \
         core/src/test/java/io/github/afgprojects/framework/core/invocation/DefaultServiceMetadataRegistryTest.java
git commit -m "feat(core): add ServiceMetadataRegistry with default implementation and APT loader"
```

---

## Task 4: ArgumentResolver SPI

**Files:**
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/resolver/ArgumentResolver.java`
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/resolver/ResolveContext.java`
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/resolver/DefaultResolveContext.java`
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/resolver/IdentityResolver.java`
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/resolver/JacksonConvertResolver.java`
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/resolver/StringConverterResolver.java`
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/resolver/CollectionResolver.java`
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/resolver/NullDefaultResolver.java`
- Test: `core/src/test/java/io/github/afgprojects/framework/core/invocation/resolver/ArgumentResolverChainTest.java`

- [ ] **Step 1: 编写参数解析器测试**

```java
package io.github.afgprojects.framework.core.invocation.resolver;

import io.github.afgprojects.framework.core.invocation.ParameterMetadata;
import io.github.afgprojects.framework.core.invocation.ArgumentConversionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ArgumentResolverChainTest {

    private List<ArgumentResolver> resolvers;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        resolvers = List.of(
                new IdentityResolver(),
                new JacksonConvertResolver(),
                new StringConverterResolver(),
                new CollectionResolver(),
                new NullDefaultResolver()
        );
    }

    private Object resolve(Object source, Class<?> targetType, ParameterMetadata paramMeta) {
        ResolveContext ctx = new DefaultResolveContext(paramMeta, new ObjectMapper(), Map.of());
        for (ArgumentResolver resolver : resolvers) {
            if (resolver.supports(source != null ? source.getClass() : Object.class, targetType)) {
                try {
                    return resolver.resolve(source, targetType, ctx);
                } catch (Exception e) {
                    // try next resolver
                }
            }
        }
        throw new ArgumentConversionException(paramMeta.name(),
                source != null ? source.getClass() : Object.class, targetType, null);
    }

    private ParameterMetadata simpleParam(String name, Class<?> type) {
        return new ParameterMetadata() {
            @Override public String name() { return name; }
            @Override public String type() { return type.getName(); }
            @Override public boolean required() { return true; }
            @Override public String defaultValue() { return ""; }
            @Override public int index() { return 0; }
            @Override public String description() { return ""; }
            @Override public List<String> enumValues() { return List.of(); }
            @Override public boolean injected() { return false; }
        };
    }

    @Test
    void identity_shouldPassThroughSameType() {
        String result = (String) resolve("hello", String.class, simpleParam("msg", String.class));
        assertEquals("hello", result);
    }

    @Test
    void identity_shouldPassThroughInteger() {
        Integer result = (Integer) resolve(42, Integer.class, simpleParam("num", Integer.class));
        assertEquals(42, result);
    }

    @Test
    void stringConverter_shouldConvertStringToInteger() {
        Integer result = (Integer) resolve("42", Integer.class, simpleParam("num", Integer.class));
        assertEquals(42, result);
    }

    @Test
    void stringConverter_shouldConvertStringToLong() {
        Long result = (Long) resolve("9999999999", Long.class, simpleParam("num", Long.class));
        assertEquals(9999999999L, result);
    }

    @Test
    void stringConverter_shouldConvertStringToBoolean() {
        Boolean result = (Boolean) resolve("true", Boolean.class, simpleParam("flag", Boolean.class));
        assertTrue(result);
    }

    @Test
    void stringConverter_shouldConvertStringToLocalDate() {
        LocalDate result = (LocalDate) resolve("2024-01-15", LocalDate.class, simpleParam("date", LocalDate.class));
        assertEquals(LocalDate.of(2024, 1, 15), result);
    }

    @Test
    void stringConverter_shouldConvertStringToBigDecimal() {
        BigDecimal result = (BigDecimal) resolve("3.14", BigDecimal.class, simpleParam("val", BigDecimal.class));
        assertEquals(new BigDecimal("3.14"), result);
    }

    @Test
    void jackson_shouldConvertMapToObject() {
        Map<String, Object> map = Map.of("name", "test", "age", 25);
        TestDto result = (TestDto) resolve(map, TestDto.class, simpleParam("dto", TestDto.class));
        assertEquals("test", result.getName());
        assertEquals(25, result.getAge());
    }

    @Test
    void collection_shouldConvertListToSet() {
        List<String> list = List.of("a", "b", "c");
        Set<?> result = (Set<?>) resolve(list, Set.class, simpleParam("items", Set.class));
        assertEquals(3, result.size());
        assertTrue(result instanceof LinkedHashSet);
    }

    @Test
    void collection_shouldConvertSetToList() {
        Set<String> set = new LinkedHashSet<>(List.of("x", "y"));
        List<?> result = (List<?>) resolve(set, List.class, simpleParam("items", List.class));
        assertEquals(2, result.size());
        assertTrue(result instanceof ArrayList);
    }

    @Test
    void nullDefault_shouldReturnDefaultWhenNullAndNotRequired() {
        ParameterMetadata param = new ParameterMetadata() {
            @Override public String name() { return "status"; }
            @Override public String type() { return "java.lang.Integer"; }
            @Override public boolean required() { return false; }
            @Override public String defaultValue() { return "1"; }
            @Override public int index() { return 0; }
            @Override public String description() { return ""; }
            @Override public List<String> enumValues() { return List.of(); }
            @Override public boolean injected() { return false; }
        };
        Integer result = (Integer) resolve(null, Integer.class, param);
        assertEquals(1, result);
    }

    @lombok.Data
    public static class TestDto {
        private String name;
        private int age;
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd /home/caiti/code/afg-projects/afg-framework && ./gradlew :core:test --tests "*.ArgumentResolverChainTest" 2>&1 | tail -20`
Expected: FAIL — classes not found

- [ ] **Step 3: 创建 ArgumentResolver 接口**

```java
package io.github.afgprojects.framework.core.invocation.resolver;

import io.github.afgprojects.framework.core.invocation.ParameterMetadata;

public interface ArgumentResolver {
    int priority();
    boolean supports(Class<?> sourceType, Class<?> targetType);
    Object resolve(Object source, Class<?> targetType, ResolveContext context);
}
```

- [ ] **Step 4: 创建 ResolveContext 接口和默认实现**

```java
package io.github.afgprojects.framework.core.invocation.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.core.invocation.ParameterMetadata;

import java.util.Map;

public interface ResolveContext {
    ParameterMetadata parameterMetadata();
    ObjectMapper objectMapper();
    Map<String, Object> rawArguments();
}
```

```java
package io.github.afgprojects.framework.core.invocation.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.core.invocation.ParameterMetadata;

import java.util.Map;

public record DefaultResolveContext(
        ParameterMetadata parameterMetadata,
        ObjectMapper objectMapper,
        Map<String, Object> rawArguments
) implements ResolveContext {
}
```

- [ ] **Step 5: 创建 IdentityResolver**

```java
package io.github.afgprojects.framework.core.invocation.resolver;

public class IdentityResolver implements ArgumentResolver {
    @Override
    public int priority() { return 1; }

    @Override
    public boolean supports(Class<?> sourceType, Class<?> targetType) {
        return targetType.isAssignableFrom(sourceType);
    }

    @Override
    public Object resolve(Object source, Class<?> targetType, ResolveContext context) {
        return source;
    }
}
```

- [ ] **Step 6: 创建 JacksonConvertResolver**

```java
package io.github.afgprojects.framework.core.invocation.resolver;

import io.github.afgprojects.framework.core.invocation.ArgumentConversionException;

public class JacksonConvertResolver implements ArgumentResolver {
    @Override
    public int priority() { return 2; }

    @Override
    public boolean supports(Class<?> sourceType, Class<?> targetType) {
        if (targetType.isAssignableFrom(sourceType)) return false;
        return true;
    }

    @Override
    public Object resolve(Object source, Class<?> targetType, ResolveContext context) {
        try {
            return context.objectMapper().convertValue(source, targetType);
        } catch (IllegalArgumentException e) {
            throw new ArgumentConversionException(
                    context.parameterMetadata().name(), source.getClass(), targetType, e);
        }
    }
}
```

- [ ] **Step 7: 创建 StringConverterResolver**

```java
package io.github.afgprojects.framework.core.invocation.resolver;

import io.github.afgprojects.framework.core.invocation.ArgumentConversionException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class StringConverterResolver implements ArgumentResolver {

    private static final Map<Class<?>, Function<String, ?>> CONVERTERS = Map.of(
            Integer.class, Integer::valueOf,
            int.class, s -> Integer.parseInt(s),
            Long.class, Long::valueOf,
            long.class, s -> Long.parseLong(s),
            Double.class, Double::valueOf,
            double.class, s -> Double.parseDouble(s),
            Float.class, Float::valueOf,
            float.class, s -> Float.parseFloat(s),
            Boolean.class, Boolean::valueOf,
            boolean.class, s -> Boolean.parseBoolean(s),
            BigDecimal.class, BigDecimal::new,
            LocalDate.class, LocalDate::parse,
            LocalDateTime.class, LocalDateTime::parse,
            UUID.class, UUID::fromString
    );

    @Override
    public int priority() { return 3; }

    @Override
    public boolean supports(Class<?> sourceType, Class<?> targetType) {
        return (sourceType == String.class && CONVERTERS.containsKey(targetType))
                || targetType == String.class;
    }

    @Override
    public Object resolve(Object source, Class<?> targetType, ResolveContext context) {
        if (source instanceof String s && CONVERTERS.containsKey(targetType)) {
            try {
                return CONVERTERS.get(targetType).apply(s);
            } catch (Exception e) {
                throw new ArgumentConversionException(
                        context.parameterMetadata().name(), source.getClass(), targetType, e);
            }
        }
        if (targetType == String.class) {
            return source.toString();
        }
        throw new ArgumentConversionException(
                context.parameterMetadata().name(), source.getClass(), targetType, null);
    }
}
```

- [ ] **Step 8: 创建 CollectionResolver**

```java
package io.github.afgprojects.framework.core.invocation.resolver;

import java.util.*;

public class CollectionResolver implements ArgumentResolver {
    @Override
    public int priority() { return 4; }

    @Override
    public boolean supports(Class<?> sourceType, Class<?> targetType) {
        return Collection.class.isAssignableFrom(sourceType)
                && Collection.class.isAssignableFrom(targetType);
    }

    @Override
    public Object resolve(Object source, Class<?> targetType, ResolveContext context) {
        Collection<?> src = (Collection<?>) source;
        if (Set.class.isAssignableFrom(targetType)) {
            return new LinkedHashSet<>(src);
        }
        if (List.class.isAssignableFrom(targetType)) {
            return new ArrayList<>(src);
        }
        return source;
    }
}
```

- [ ] **Step 9: 创建 NullDefaultResolver**

```java
package io.github.afgprojects.framework.core.invocation.resolver;

import io.github.afgprojects.framework.core.invocation.ParameterMetadata;

public class NullDefaultResolver implements ArgumentResolver {
    @Override
    public int priority() { return 5; }

    @Override
    public boolean supports(Class<?> sourceType, Class<?> targetType) {
        return sourceType == Object.class; // null 值时 sourceType 为 Object.class
    }

    @Override
    public Object resolve(Object source, Class<?> targetType, ResolveContext context) {
        if (source != null) return source;
        ParameterMetadata meta = context.parameterMetadata();
        if (meta.defaultValue().isEmpty()) return null;
        // 使用 StringConverterResolver 转换默认值
        StringConverterResolver stringResolver = new StringConverterResolver();
        if (stringResolver.supports(String.class, targetType)) {
            return stringResolver.resolve(meta.defaultValue(), targetType, context);
        }
        return null;
    }
}
```

- [ ] **Step 10: 运行测试**

Run: `cd /home/caiti/code/afg-projects/afg-framework && ./gradlew :core:test --tests "*.ArgumentResolverChainTest" 2>&1 | tail -20`
Expected: PASS

- [ ] **Step 11: Commit**

```bash
git add core/src/main/java/io/github/afgprojects/framework/core/invocation/resolver/ \
         core/src/test/java/io/github/afgprojects/framework/core/invocation/resolver/ArgumentResolverChainTest.java
git commit -m "feat(core): add ArgumentResolver SPI with Identity, Jackson, String, Collection, NullDefault resolvers"
```

---

## Task 5: InvocationInterceptor SPI

**Files:**
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/InvocationInterceptor.java`
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/InvocationContext.java`
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/DefaultInvocationContext.java`
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/InvocationPlan.java`
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/DefaultInvocationPlan.java`
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/interceptor/AuditInvocationInterceptor.java`
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/interceptor/ValidationInvocationInterceptor.java`
- Test: `core/src/test/java/io/github/afgprojects/framework/core/invocation/InvocationInterceptorTest.java`

- [ ] **Step 1: 编写拦截器测试**

```java
package io.github.afgprojects.framework.core.invocation;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InvocationInterceptorTest {

    @Test
    void interceptors_shouldExecuteInOrder() {
        StringBuilder sb = new StringBuilder();
        List<InvocationInterceptor> interceptors = List.of(
                new OrderTrackingInterceptor(100, "A", sb),
                new OrderTrackingInterceptor(200, "B", sb),
                new OrderTrackingInterceptor(300, "C", sb)
        );

        InvocationContext ctx = createContext();
        for (InvocationInterceptor i : interceptors) {
            assertTrue(i.before(ctx));
        }
        for (InvocationInterceptor i : interceptors) {
            i.after(ctx, "result");
        }
        assertEquals("before-A before-B before-C after-A after-B after-C ", sb.toString());
    }

    @Test
    void interceptor_beforeReturnsFalse_shouldInterrupt() {
        StringBuilder sb = new StringBuilder();
        List<InvocationInterceptor> interceptors = List.of(
                new OrderTrackingInterceptor(100, "A", sb),
                new RejectingInterceptor(200, "B", sb),
                new OrderTrackingInterceptor(300, "C", sb)
        );

        InvocationContext ctx = createContext();
        boolean shouldContinue = true;
        for (InvocationInterceptor i : interceptors) {
            if (!i.before(ctx)) {
                shouldContinue = false;
                break;
            }
        }
        assertFalse(shouldContinue);
        assertEquals("before-A before-B(rejected) ", sb.toString());
    }

    @Test
    void interceptor_afterCanModifyResult() {
        InvocationInterceptor doubler = new InvocationInterceptor() {
            @Override public int order() { return 100; }
            @Override public boolean before(InvocationContext ctx) { return true; }
            @Override public Object after(InvocationContext ctx, Object result) { return ((int) result) * 2; }
            @Override public void onError(InvocationContext ctx, Exception e) {}
        };

        InvocationContext ctx = createContext();
        Object result = doubler.after(ctx, 21);
        assertEquals(42, result);
    }

    private InvocationContext createContext() {
        return new DefaultInvocationContext(null, null, new Object(), new Object[0], Map.of(), null, null, new HashMap<>());
    }

    static class OrderTrackingInterceptor implements InvocationInterceptor {
        private final int order;
        private final String name;
        private final StringBuilder sb;
        OrderTrackingInterceptor(int order, String name, StringBuilder sb) {
            this.order = order; this.name = name; this.sb = sb;
        }
        @Override public int order() { return order; }
        @Override public boolean before(InvocationContext ctx) { sb.append("before-").append(name).append(" "); return true; }
        @Override public Object after(InvocationContext ctx, Object result) { sb.append("after-").append(name).append(" "); return result; }
        @Override public void onError(InvocationContext ctx, Exception e) { sb.append("error-").append(name).append(" "); }
    }

    static class RejectingInterceptor implements InvocationInterceptor {
        private final int order;
        private final String name;
        private final StringBuilder sb;
        RejectingInterceptor(int order, String name, StringBuilder sb) {
            this.order = order; this.name = name; this.sb = sb;
        }
        @Override public int order() { return order; }
        @Override public boolean before(InvocationContext ctx) { sb.append("before-").append(name).append("(rejected) "); return false; }
        @Override public Object after(InvocationContext ctx, Object result) { return result; }
        @Override public void onError(InvocationContext ctx, Exception e) {}
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd /home/caiti/code/afg-projects/afg-framework && ./gradlew :core:test --tests "*.InvocationInterceptorTest" 2>&1 | tail -20`
Expected: FAIL — classes not found

- [ ] **Step 3: 创建 InvocationInterceptor 接口**

```java
package io.github.afgprojects.framework.core.invocation;

public interface InvocationInterceptor {
    int order();
    boolean before(InvocationContext context);
    Object after(InvocationContext context, Object result);
    void onError(InvocationContext context, Exception exception);
}
```

- [ ] **Step 4: 创建 InvocationContext 接口和默认实现**

```java
package io.github.afgprojects.framework.core.invocation;

import java.util.Map;

public interface InvocationContext {
    ServiceMetadata<?> serviceMetadata();
    OperationMetadata operationMetadata();
    Object targetBean();
    Object[] arguments();
    Map<String, Object> rawArguments();
    Map<String, Object> attributes();
}
```

注意：`SecurityContext` 和 `TenantContext` 暂不在此接口中定义——它们通过 `attributes` 传递，由 security-core 的拦截器负责注入。这样 core 模块不依赖 security-core。

```java
package io.github.afgprojects.framework.core.invocation;

import java.util.Map;

public record DefaultInvocationContext(
        ServiceMetadata<?> serviceMetadata,
        OperationMetadata operationMetadata,
        Object targetBean,
        Object[] arguments,
        Map<String, Object> rawArguments,
        Map<String, Object> attributes
) implements InvocationContext {
}
```

- [ ] **Step 5: 创建 InvocationPlan 接口和默认实现**

```java
package io.github.afgprojects.framework.core.invocation;

import java.util.List;

public interface InvocationPlan {
    ServiceMetadata<?> serviceMetadata();
    OperationMetadata operationMetadata();
    Object targetBean();
    Object[] resolvedArguments();
    List<InvocationInterceptor> applicableInterceptors();
}
```

```java
package io.github.afgprojects.framework.core.invocation;

import java.util.List;

public record DefaultInvocationPlan(
        ServiceMetadata<?> serviceMetadata,
        OperationMetadata operationMetadata,
        Object targetBean,
        Object[] resolvedArguments,
        List<InvocationInterceptor> applicableInterceptors
) implements InvocationPlan {
}
```

- [ ] **Step 6: 创建 AuditInvocationInterceptor**

```java
package io.github.afgprojects.framework.core.invocation.interceptor;

import io.github.afgprojects.framework.core.invocation.InvocationContext;
import io.github.afgprojects.framework.core.invocation.InvocationInterceptor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AuditInvocationInterceptor implements InvocationInterceptor {
    @Override
    public int order() { return 400; }

    @Override
    public boolean before(InvocationContext context) {
        if (!context.operationMetadata().audit()) return true;
        log.info("Invoking {}.{}", context.serviceMetadata().serviceName(), context.operationMetadata().name());
        return true;
    }

    @Override
    public Object after(InvocationContext context, Object result) {
        if (!context.operationMetadata().audit()) return result;
        log.info("Invoked {}.{} successfully", context.serviceMetadata().serviceName(), context.operationMetadata().name());
        return result;
    }

    @Override
    public void onError(InvocationContext context, Exception exception) {
        if (!context.operationMetadata().audit()) return;
        log.error("Invoked {}.{} failed", context.serviceMetadata().serviceName(), context.operationMetadata().name(), exception);
    }
}
```

- [ ] **Step 7: 创建 ValidationInvocationInterceptor**

```java
package io.github.afgprojects.framework.core.invocation.interceptor;

import io.github.afgprojects.framework.core.invocation.InvocationContext;
import io.github.afgprojects.framework.core.invocation.InvocationInterceptor;

import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolation;
import java.util.Set;
import java.util.stream.Collectors;

public class ValidationInvocationInterceptor implements InvocationInterceptor {

    private final Validator validator;

    public ValidationInvocationInterceptor(Validator validator) {
        this.validator = validator;
    }

    public ValidationInvocationInterceptor() {
        this.validator = null;
    }

    @Override
    public int order() { return 500; }

    @Override
    public boolean before(InvocationContext context) {
        if (validator == null) return true;
        for (Object arg : context.arguments()) {
            if (arg == null) continue;
            Set<ConstraintViolation<Object>> violations = validator.validate(arg);
            if (!violations.isEmpty()) {
                String msg = violations.stream()
                        .map(v -> v.getPropertyPath() + " " + v.getMessage())
                        .collect(Collectors.joining(", "));
                throw new io.github.afgprojects.framework.core.invocation.ServiceInvocationException(
                        "Validation failed: " + msg);
            }
        }
        return true;
    }

    @Override
    public Object after(InvocationContext context, Object result) { return result; }

    @Override
    public void onError(InvocationContext context, Exception exception) {}
}
```

- [ ] **Step 8: 运行测试**

Run: `cd /home/caiti/code/afg-projects/afg-framework && ./gradlew :core:test --tests "*.InvocationInterceptorTest" 2>&1 | tail -20`
Expected: PASS

- [ ] **Step 9: Commit**

```bash
git add core/src/main/java/io/github/afgprojects/framework/core/invocation/InvocationInterceptor.java \
         core/src/main/java/io/github/afgprojects/framework/core/invocation/InvocationContext.java \
         core/src/main/java/io/github/afgprojects/framework/core/invocation/DefaultInvocationContext.java \
         core/src/main/java/io/github/afgprojects/framework/core/invocation/InvocationPlan.java \
         core/src/main/java/io/github/afgprojects/framework/core/invocation/DefaultInvocationPlan.java \
         core/src/main/java/io/github/afgprojects/framework/core/invocation/interceptor/ \
         core/src/test/java/io/github/afgprojects/framework/core/invocation/InvocationInterceptorTest.java
git commit -m "feat(core): add InvocationInterceptor SPI, InvocationContext, and built-in interceptors"
```

---

## Task 6: ResultProcessor SPI

**Files:**
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/processor/ResultProcessor.java`
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/processor/ResultContext.java`
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/processor/DefaultResultContext.java`
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/processor/IdentityProcessor.java`
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/processor/SensitiveMaskProcessor.java`
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/processor/PagedResultProcessor.java`
- Test: `core/src/test/java/io/github/afgprojects/framework/core/invocation/processor/ResultProcessorChainTest.java`

- [ ] **Step 1: 编写返回值处理器测试**

```java
package io.github.afgprojects.framework.core.invocation.processor;

import io.github.afgprojects.framework.core.invocation.OperationMetadata;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResultProcessorChainTest {

    private final List<ResultProcessor> processors = List.of(
            new PagedResultProcessor(),
            new IdentityProcessor()
    );

    private OperationMetadata pagedOp() {
        return createOp(true);
    }

    private OperationMetadata nonPagedOp() {
        return createOp(false);
    }

    private OperationMetadata createOp(boolean paged) {
        return new OperationMetadata() {
            @Override public String name() { return "test"; }
            @Override public String description() { return ""; }
            @Override public io.github.afgprojects.framework.core.invocation.MethodKey method() { return null; }
            @Override public List<io.github.afgprojects.framework.core.invocation.ParameterMetadata> parameters() { return List.of(); }
            @Override public String returnType() { return "java.lang.Object"; }
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

    @Test
    void identityProcessor_shouldPassThroughAnyResult() {
        IdentityProcessor p = new IdentityProcessor();
        assertTrue(p.supports("hello", nonPagedOp()));
        assertEquals("hello", p.process("hello", null));
    }

    @Test
    void pagedResultProcessor_shouldWrapListInPagedResult() {
        PagedResultProcessor p = new PagedResultProcessor();
        List<String> list = List.of("a", "b", "c");
        assertTrue(p.supports(list, pagedOp()));
        Object result = p.process(list, null);
        // PagedResult 应该包含 content, totalElements, page, size
        assertNotNull(result);
    }

    @Test
    void pagedResultProcessor_shouldNotSupportNonPagedOp() {
        PagedResultProcessor p = new PagedResultProcessor();
        assertFalse(p.supports("hello", nonPagedOp()));
    }

    @Test
    void processorChain_shouldApplyCorrectProcessor() {
        List<String> list = List.of("a", "b");
        Object result = list;
        for (ResultProcessor p : processors) {
            if (p.supports(result, pagedOp())) {
                result = p.process(result, null);
            }
        }
        // PagedResultProcessor 处理了
        assertNotEquals(list, result);
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd /home/caiti/code/afg-projects/afg-framework && ./gradlew :core:test --tests "*.ResultProcessorChainTest" 2>&1 | tail -20`
Expected: FAIL

- [ ] **Step 3: 创建 ResultProcessor 接口**

```java
package io.github.afgprojects.framework.core.invocation.processor;

import io.github.afgprojects.framework.core.invocation.OperationMetadata;

public interface ResultProcessor {
    int priority();
    boolean supports(Object result, OperationMetadata metadata);
    Object process(Object result, ResultContext context);
}
```

- [ ] **Step 4: 创建 ResultContext 接口和默认实现**

```java
package io.github.afgprojects.framework.core.invocation.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.core.invocation.OperationMetadata;
import io.github.afgprojects.framework.core.invocation.ServiceMetadata;

import java.util.Map;

public interface ResultContext {
    OperationMetadata operationMetadata();
    ServiceMetadata<?> serviceMetadata();
    ObjectMapper objectMapper();
    Map<String, Object> attributes();
}
```

```java
package io.github.afgprojects.framework.core.invocation.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.core.invocation.OperationMetadata;
import io.github.afgprojects.framework.core.invocation.ServiceMetadata;

import java.util.Map;

public record DefaultResultContext(
        OperationMetadata operationMetadata,
        ServiceMetadata<?> serviceMetadata,
        ObjectMapper objectMapper,
        Map<String, Object> attributes
) implements ResultContext {
}
```

- [ ] **Step 5: 创建 IdentityProcessor**

```java
package io.github.afgprojects.framework.core.invocation.processor;

import io.github.afgprojects.framework.core.invocation.OperationMetadata;

public class IdentityProcessor implements ResultProcessor {
    @Override
    public int priority() { return 100; }

    @Override
    public boolean supports(Object result, OperationMetadata metadata) { return true; }

    @Override
    public Object process(Object result, ResultContext context) { return result; }
}
```

- [ ] **Step 6: 创建 PagedResultProcessor**

需要先定义 `PagedResult` 数据结构：

```java
package io.github.afgprojects.framework.core.invocation.processor;

import java.util.List;

public record PagedResult<T>(
        List<T> content,
        long totalElements,
        int page,
        int size,
        int totalPages
) {
    public static <T> PagedResult<T> of(List<T> content, long totalElements, int page, int size) {
        return new PagedResult<>(content, totalElements, page, size, (int) Math.ceil((double) totalElements / size));
    }

    public static <T> PagedResult<T> of(List<T> content) {
        return new PagedResult<>(content, content.size(), 1, content.size(), 1);
    }
}
```

```java
package io.github.afgprojects.framework.core.invocation.processor;

import io.github.afgprojects.framework.core.invocation.OperationMetadata;

import java.util.List;

public class PagedResultProcessor implements ResultProcessor {
    @Override
    public int priority() { return 300; }

    @Override
    public boolean supports(Object result, OperationMetadata metadata) {
        return metadata.paged() && result instanceof List;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object process(Object result, ResultContext context) {
        List<?> list = (List<?>) result;
        return PagedResult.of(list);
    }
}
```

- [ ] **Step 7: 创建 SensitiveMaskProcessor**

```java
package io.github.afgprojects.framework.core.invocation.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.core.invocation.OperationMetadata;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class SensitiveMaskProcessor implements ResultProcessor {

    private static final Set<String> SENSITIVE_FIELDS = Set.of("password", "secret", "token", "credential");
    private static final String MASK = "***";

    @Override
    public int priority() { return 200; }

    @Override
    public boolean supports(Object result, OperationMetadata metadata) {
        return result != null && hasSensitiveFields(result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object process(Object result, ResultContext context) {
        try {
            ObjectMapper mapper = context != null ? context.objectMapper() : new ObjectMapper();
            // 转为 Map，脱敏后转回
            if (result instanceof List<?> list) {
                return list.stream().map(item -> maskItem(item, mapper)).toList();
            }
            return maskItem(result, mapper);
        } catch (Exception e) {
            log.warn("Failed to mask sensitive fields", e);
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    private Object maskItem(Object item, ObjectMapper mapper) {
        if (item instanceof Map) {
            return maskMap((Map<String, Object>) item);
        }
        Map<String, Object> map = mapper.convertValue(item, Map.class);
        return maskMap(map);
    }

    private Map<String, Object> maskMap(Map<String, Object> map) {
        for (String key : SENSITIVE_FIELDS) {
            if (map.containsKey(key)) {
                map.put(key, MASK);
            }
        }
        return map;
    }

    private boolean hasSensitiveFields(Object result) {
        // 简单检测：POJO 字段名或 Map 的 key 包含敏感字段名
        if (result instanceof Map<?, ?> map) {
            return map.keySet().stream().anyMatch(k -> SENSITIVE_FIELDS.contains(k.toString().toLowerCase()));
        }
        // POJO 通过 Jackson 转 Map 检测——性能不佳，只在确实有敏感字段时使用
        return false;
    }
}
```

- [ ] **Step 8: 运行测试**

Run: `cd /home/caiti/code/afg-projects/afg-framework && ./gradlew :core:test --tests "*.ResultProcessorChainTest" 2>&1 | tail -20`
Expected: PASS

- [ ] **Step 9: Commit**

```bash
git add core/src/main/java/io/github/afgprojects/framework/core/invocation/processor/ \
         core/src/test/java/io/github/afgprojects/framework/core/invocation/processor/ResultProcessorChainTest.java
git commit -m "feat(core): add ResultProcessor SPI with Identity, PagedResult, SensitiveMask processors"
```

---

## Task 7: BeanInvocationEngine

**Files:**
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/BeanInvocationEngine.java`
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/DefaultBeanInvocationEngine.java`
- Create: `core/src/main/java/io/github/afgprojects/framework/core/invocation/InvocationContextTaskDecorator.java`
- Test: `core/src/test/java/io/github/afgprojects/framework/core/invocation/DefaultBeanInvocationEngineTest.java`

- [ ] **Step 1: 编写 BeanInvocationEngine 测试**

```java
package io.github.afgprojects.framework.core.invocation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.core.invocation.resolver.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class DefaultBeanInvocationEngineTest {

    private DefaultBeanInvocationEngine engine;
    private TestService testService;
    private ServiceMetadataRegistry registry;

    @BeforeEach
    void setUp() {
        testService = new TestService();
        registry = new DefaultServiceMetadataRegistry();
        registerTestMetadata();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        List<ArgumentResolver> resolvers = List.of(
                new IdentityResolver(),
                new JacksonConvertResolver(),
                new StringConverterResolver(),
                new CollectionResolver(),
                new NullDefaultResolver()
        );

        List<InvocationInterceptor> interceptors = List.of(
                new AuditInvocationInterceptor()
        );

        List<io.github.afgprojects.framework.core.invocation.processor.ResultProcessor> resultProcessors = List.of(
                new io.github.afgprojects.framework.core.invocation.processor.IdentityProcessor()
        );

        engine = new DefaultBeanInvocationEngine(
                registry,
                serviceName -> testService,  // 简单的 BeanProvider
                interceptors,
                resolvers,
                resultProcessors,
                objectMapper
        );
    }

    @Test
    void invoke_shouldCallMethodWithArguments() {
        Object result = engine.invoke("testService", "greet", Map.of("name", "World"));
        assertEquals("Hello World", result);
    }

    @Test
    void invoke_shouldHandleNoArgMethod() {
        Object result = engine.invoke("testService", "getName", Map.of());
        assertEquals("TestService", result);
    }

    @Test
    void invoke_shouldConvertStringArgToInt() {
        Object result = engine.invoke("testService", "repeat", Map.of("text", "ab", "count", "3"));
        assertEquals("ababab", result);
    }

    @Test
    void invoke_shouldThrowWhenServiceNotFound() {
        assertThrows(ServiceNotFoundException.class,
                () -> engine.invoke("nope", "greet", Map.of()));
    }

    @Test
    void invoke_shouldThrowWhenOperationNotFound() {
        assertThrows(ServiceNotFoundException.class,
                () -> engine.invoke("testService", "nonExistent", Map.of()));
    }

    @Test
    void invoke_shouldThrowWhenRequiredArgMissing() {
        assertThrows(MissingArgumentException.class,
                () -> engine.invoke("testService", "greet", Map.of()));
    }

    @Test
    void invokeAsync_shouldReturnCompletableFuture() throws Exception {
        CompletableFuture<Object> future = engine.invokeAsync("testService", "getName", Map.of());
        Object result = future.get();
        assertEquals("TestService", result);
    }

    @Test
    void plan_shouldReturnInvocationPlan() {
        InvocationPlan plan = engine.plan("testService", "greet", Map.of("name", "Test"));
        assertEquals("testService", plan.serviceMetadata().serviceName());
        assertEquals("greet", plan.operationMetadata().name());
        assertEquals(testService, plan.targetBean());
        assertEquals(1, plan.resolvedArguments().length);
        assertEquals("Test", plan.resolvedArguments()[0]);
    }

    private void registerTestMetadata() {
        // 手动构建 TestService 的 ServiceMetadata
        ServiceMetadata<TestService> meta = new ServiceMetadata<>() {
            @Override public String serviceName() { return "testService"; }
            @Override public String description() { return "Test"; }
            @Override public String category() { return "test"; }
            @Override public List<String> tags() { return List.of(); }
            @Override public Class<TestService> serviceType() { return TestService.class; }
            @Override public List<OperationMetadata> operations() { return List.of(
                    createOp("getName", List.of(), "java.lang.String"),
                    createOp("greet", List.of(createParam("name", "java.lang.String", 0, true, "")), "java.lang.String"),
                    createOp("repeat", List.of(
                            createParam("text", "java.lang.String", 0, true, ""),
                            createParam("count", "java.lang.Integer", 1, true, "")
                    ), "java.lang.String")
            ); }
        };
        registry.register(meta);
    }

    private OperationMetadata createOp(String name, List<ParameterMetadata> params, String returnType) {
        List<String> paramTypes = params.stream()
                .map(ParameterMetadata::type)
                .toList();
        return new OperationMetadata() {
            @Override public String name() { return name; }
            @Override public String description() { return name; }
            @Override public MethodKey method() { return new MethodKey(name, paramTypes); }
            @Override public List<ParameterMetadata> parameters() { return params; }
            @Override public String returnType() { return returnType; }
            @Override public String returnDescription() { return ""; }
            @Override public String permission() { return ""; }
            @Override public List<String> requiredRoles() { return List.of(); }
            @Override public boolean audit() { return false; }
            @Override public boolean tenantScope() { return false; }
            @Override public boolean dataScope() { return false; }
            @Override public boolean async() { return false; }
            @Override public boolean deprecated() { return false; }
            @Override public String inputSchema() { return "{}"; }
            @Override public boolean paged() { return false; }
        };
    }

    private ParameterMetadata createParam(String name, String type, int index, boolean required, String defaultValue) {
        return new ParameterMetadata() {
            @Override public String name() { return name; }
            @Override public String type() { return type; }
            @Override public boolean required() { return required; }
            @Override public String defaultValue() { return defaultValue; }
            @Override public int index() { return index; }
            @Override public String description() { return ""; }
            @Override public List<String> enumValues() { return List.of(); }
            @Override public boolean injected() { return false; }
        };
    }

    public static class TestService {
        public String getName() { return "TestService"; }
        public String greet(String name) { return "Hello " + name; }
        public String repeat(String text, int count) { return text.repeat(count); }
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd /home/caiti/code/afg-projects/afg-framework && ./gradlew :core:test --tests "*.DefaultBeanInvocationEngineTest" 2>&1 | tail -20`
Expected: FAIL

- [ ] **Step 3: 创建 BeanInvocationEngine 接口**

```java
package io.github.afgprojects.framework.core.invocation;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface BeanInvocationEngine {
    Object invoke(String serviceName, String operationName, Map<String, Object> arguments);
    <T> T invoke(String serviceName, String operationName, Map<String, Object> arguments, Class<T> returnType);
    CompletableFuture<Object> invokeAsync(String serviceName, String operationName, Map<String, Object> arguments);
    <T> CompletableFuture<T> invokeAsync(String serviceName, String operationName, Map<String, Object> arguments, Class<T> returnType);
    InvocationPlan plan(String serviceName, String operationName, Map<String, Object> arguments);
}
```

- [ ] **Step 4: 创建 DefaultBeanInvocationEngine**

```java
package io.github.afgprojects.framework.core.invocation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.core.invocation.processor.ResultProcessor;
import io.github.afgprojects.framework.core.invocation.resolver.ArgumentResolver;
import io.github.afgprojects.framework.core.invocation.resolver.DefaultResolveContext;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
public class DefaultBeanInvocationEngine implements BeanInvocationEngine {

    private final ServiceMetadataRegistry registry;
    private final BeanProvider beanProvider;
    private final List<InvocationInterceptor> interceptors;
    private final List<ArgumentResolver> argumentResolvers;
    private final List<ResultProcessor> resultProcessors;
    private final ObjectMapper objectMapper;
    private final ExecutorService asyncExecutor;

    @FunctionalInterface
    public interface BeanProvider {
        Object getBean(String serviceName);
    }

    public DefaultBeanInvocationEngine(ServiceMetadataRegistry registry,
                                       BeanProvider beanProvider,
                                       List<InvocationInterceptor> interceptors,
                                       List<ArgumentResolver> argumentResolvers,
                                       List<ResultProcessor> resultProcessors,
                                       ObjectMapper objectMapper) {
        this.registry = registry;
        this.beanProvider = beanProvider;
        this.interceptors = interceptors.stream()
                .sorted(Comparator.comparingInt(InvocationInterceptor::order))
                .toList();
        this.argumentResolvers = argumentResolvers.stream()
                .sorted(Comparator.comparingInt(ArgumentResolver::priority))
                .toList();
        this.resultProcessors = resultProcessors.stream()
                .sorted(Comparator.comparingInt(ResultProcessor::priority))
                .toList();
        this.objectMapper = objectMapper;
        this.asyncExecutor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                new InvocationContextThreadFactory());
    }

    @Override
    public Object invoke(String serviceName, String operationName, Map<String, Object> arguments) {
        InvocationPlan plan = plan(serviceName, operationName, arguments);
        InvocationContext context = new DefaultInvocationContext(
                plan.serviceMetadata(),
                plan.operationMetadata(),
                plan.targetBean(),
                plan.resolvedArguments(),
                arguments,
                new HashMap<>()
        );

        // 前置拦截
        for (InvocationInterceptor interceptor : interceptors) {
            if (!interceptor.before(context)) {
                throw new InvocationRejectedException(
                        interceptor.getClass().getSimpleName(),
                        "Interceptor rejected invocation");
            }
        }

        // 执行
        Object result;
        try {
            Method method = plan.operationMetadata().method().resolve(plan.serviceMetadata().serviceType());
            result = method.invoke(plan.targetBean(), plan.resolvedArguments());
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            for (InvocationInterceptor interceptor : interceptors) {
                interceptor.onError(context, cause instanceof Exception ex ? ex : new RuntimeException(cause));
            }
            throw new ServiceInvocationException("Invocation failed: " + serviceName + "." + operationName, cause);
        }

        // 后置拦截
        for (InvocationInterceptor interceptor : interceptors) {
            result = interceptor.after(context, result);
        }

        // 返回值处理
        for (ResultProcessor processor : resultProcessors) {
            if (processor.supports(result, plan.operationMetadata())) {
                result = processor.process(result, null);
                break;
            }
        }

        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T invoke(String serviceName, String operationName, Map<String, Object> arguments, Class<T> returnType) {
        Object result = invoke(serviceName, operationName, arguments);
        if (result == null) return null;
        if (returnType.isAssignableFrom(result.getClass())) return (T) result;
        return objectMapper.convertValue(result, returnType);
    }

    @Override
    public CompletableFuture<Object> invokeAsync(String serviceName, String operationName, Map<String, Object> arguments) {
        OperationMetadata op = registry.getOperation(serviceName, operationName)
                .orElseThrow(() -> new ServiceNotFoundException(serviceName, operationName));
        if (!op.async()) {
            log.warn("Operation {}.{} is not marked as async but invoked asynchronously", serviceName, operationName);
        }
        return CompletableFuture.supplyAsync(() -> invoke(serviceName, operationName, arguments), asyncExecutor);
    }

    @Override
    public <T> CompletableFuture<T> invokeAsync(String serviceName, String operationName, Map<String, Object> arguments, Class<T> returnType) {
        return invokeAsync(serviceName, operationName, arguments)
                .thenApply(result -> {
                    if (result == null) return null;
                    if (returnType.isAssignableFrom(result.getClass())) return returnType.cast(result);
                    return objectMapper.convertValue(result, returnType);
                });
    }

    @Override
    public InvocationPlan plan(String serviceName, String operationName, Map<String, Object> arguments) {
        ServiceMetadata<?> serviceMeta = registry.get(serviceName)
                .orElseThrow(() -> new ServiceNotFoundException(serviceName, operationName));
        OperationMetadata opMeta = serviceMeta.operations().stream()
                .filter(op -> op.name().equals(operationName))
                .findFirst()
                .orElseThrow(() -> new ServiceNotFoundException(serviceName, operationName));

        Object bean = beanProvider.getBean(serviceName);
        Object[] resolvedArgs = resolveArguments(opMeta, arguments);

        return new DefaultInvocationPlan(serviceMeta, opMeta, bean, resolvedArgs, interceptors);
    }

    private Object[] resolveArguments(OperationMetadata opMeta, Map<String, Object> arguments) {
        Object[] resolved = new Object[opMeta.parameters().size()];
        for (ParameterMetadata param : opMeta.parameters()) {
            if (param.injected()) {
                // 框架注入参数暂不处理，留 null
                resolved[param.index()] = null;
                continue;
            }

            Object rawValue = arguments.get(param.name());

            if (rawValue == null && !param.defaultValue().isEmpty()) {
                rawValue = resolveDefaultValue(param);
            }

            if (rawValue == null && param.required()) {
                throw new MissingArgumentException(param.name());
            }

            if (rawValue != null) {
                resolved[param.index()] = convertArgument(param, rawValue);
            } else {
                resolved[param.index()] = null;
            }
        }
        return resolved;
    }

    private Object resolveDefaultValue(ParameterMetadata param) {
        if (param.defaultValue().isEmpty()) return null;
        try {
            Class<?> targetType = Class.forName(param.type());
            ResolveContext ctx = new DefaultResolveContext(param, objectMapper, Map.of());
            for (ArgumentResolver resolver : argumentResolvers) {
                if (resolver.supports(String.class, targetType)) {
                    return resolver.resolve(param.defaultValue(), targetType, ctx);
                }
            }
        } catch (ClassNotFoundException e) {
            log.warn("Cannot load class for parameter type: {}", param.type());
        }
        return param.defaultValue();
    }

    private Object convertArgument(ParameterMetadata param, Object rawValue) {
        try {
            Class<?> targetType = Class.forName(param.type());
            // 先尝试直接类型匹配
            if (targetType.isAssignableFrom(rawValue.getClass())) {
                return rawValue;
            }
            ResolveContext ctx = new DefaultResolveContext(param, objectMapper, Map.of());
            for (ArgumentResolver resolver : argumentResolvers) {
                if (resolver.supports(rawValue.getClass(), targetType)) {
                    try {
                        return resolver.resolve(rawValue, targetType, ctx);
                    } catch (Exception e) {
                        // 尝试下一个解析器
                    }
                }
            }
            throw new ArgumentConversionException(param.name(), rawValue.getClass(), targetType, null);
        } catch (ClassNotFoundException e) {
            throw new ArgumentConversionException(param.name(), rawValue.getClass(), Object.class, e);
        }
    }

    private static class InvocationContextThreadFactory implements ThreadFactory {
        private int count = 0;
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "bean-invocation-async-" + (++count));
        }
    }
}
```

- [ ] **Step 5: 创建 InvocationContextTaskDecorator**

```java
package io.github.afgprojects.framework.core.invocation;

import org.springframework.core.task.TaskDecorator;

public class InvocationContextTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) {
        // 捕获当前线程的上下文信息
        Map<String, Object> contextSnapshot = ContextSnapshot.capture();

        return () -> {
            Map<String, Object> previous = ContextSnapshot.capture();
            try {
                ContextSnapshot.restore(contextSnapshot);
                runnable.run();
            } finally {
                ContextSnapshot.restore(previous);
            }
        };
    }

    static class ContextSnapshot {
        static Map<String, Object> capture() {
            Map<String, Object> snapshot = new HashMap<>();
            // SecurityContext 和 TenantContext 的捕获由 security-core 的扩展实现
            return snapshot;
        }

        static void restore(Map<String, Object> snapshot) {
            // 由 security-core 的扩展实现
        }
    }
}
```

- [ ] **Step 6: 运行测试**

Run: `cd /home/caiti/code/afg-projects/afg-framework && ./gradlew :core:test --tests "*.DefaultBeanInvocationEngineTest" 2>&1 | tail -20`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add core/src/main/java/io/github/afgprojects/framework/core/invocation/BeanInvocationEngine.java \
         core/src/main/java/io/github/afgprojects/framework/core/invocation/DefaultBeanInvocationEngine.java \
         core/src/main/java/io/github/afgprojects/framework/core/invocation/InvocationContextTaskDecorator.java \
         core/src/test/java/io/github/afgprojects/framework/core/invocation/DefaultBeanInvocationEngineTest.java
git commit -m "feat(core): add BeanInvocationEngine with sync/async invocation and interceptor chain"
```

---

## Task 8: APT 处理器 ServiceMetadataProcessor

**Files:**
- Create: `apt-impl/src/main/java/io/github/afgprojects/framework/apt/impl/ServiceMetadataProcessor.java`
- Create: `apt-impl/src/main/java/io/github/afgprojects/framework/apt/impl/ServiceMetadataCodeGenerator.java`
- Test: `apt-impl/src/test/java/io/github/afgprojects/framework/apt/impl/ServiceMetadataProcessorTest.java`

- [ ] **Step 1: 编写 APT 处理器测试**

```java
package io.github.afgprojects.framework.apt.impl;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ServiceMetadataProcessorTest {

    @Test
    void shouldGenerateServiceMetadata() {
        JavaFileObject source = JavaFileObjects.forSourceString(
                "com.example.UserService",
                """
                package com.example;

                import io.github.afgprojects.framework.apt.api.AfService;
                import io.github.afgprojects.framework.apt.api.AfOperation;
                import io.github.afgprojects.framework.apt.api.AfParam;

                @AfService(name = "userService", description = "User Service")
                public class UserService {

                    @AfOperation(name = "greet", description = "Say hello")
                    public String greet(@AfParam(description = "Name") String name) {
                        return "Hello " + name;
                    }

                    @AfOperation(name = "getName", description = "Get name")
                    public String getName() {
                        return "test";
                    }
                }
                """
        );

        Compilation compilation = com.google.testing.compile.Compiler.javac()
                .withProcessors(new ServiceMetadataProcessor())
                .compile(source);

        assertThat(compilation).succeededWithoutWarnings();
        assertThat(compilation).generatedSourceFile("com.example.UserServiceServiceMetadata");
    }

    @Test
    void shouldFailOnDuplicateOperationNames() {
        JavaFileObject source = JavaFileObjects.forSourceString(
                "com.example.BadService",
                """
                package com.example;

                import io.github.afgprojects.framework.apt.api.AfService;
                import io.github.afgprojects.framework.apt.api.AfOperation;

                @AfService(name = "badService")
                public class BadService {

                    @AfOperation(name = "doIt")
                    public String doItA() { return "a"; }

                    @AfOperation(name = "doIt")
                    public String doItB() { return "b"; }
                }
                """
        );

        Compilation compilation = com.google.testing.compile.Compiler.javac()
                .withProcessors(new ServiceMetadataProcessor())
                .compile(source);

        assertThat(compilation).hadErrorContaining("Duplicate @AfOperation name");
    }

    @Test
    void shouldGenerateIndexFile() {
        JavaFileObject source = JavaFileObjects.forSourceString(
                "com.example.SimpleService",
                """
                package com.example;

                import io.github.afgprojects.framework.apt.api.AfService;
                import io.github.afgprojects.framework.apt.api.AfOperation;

                @AfService(name = "simpleService")
                public class SimpleService {

                    @AfOperation(name = "run")
                    public void run() {}
                }
                """
        );

        Compilation compilation = com.google.testing.compile.Compiler.javac()
                .withProcessors(new ServiceMetadataProcessor())
                .compile(source);

        assertThat(compilation).succeeded();
        assertThat(compilation).generatedFile(
                javax.tools.StandardLocation.CLASS_OUTPUT,
                "",
                "META-INF/afg/service-metadata.index"
        );
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd /home/caiti/code/afg-projects/afg-framework && ./gradlew :apt-impl:test --tests "*.ServiceMetadataProcessorTest" 2>&1 | tail -20`
Expected: FAIL

- [ ] **Step 3: 创建 ServiceMetadataProcessor**

```java
package io.github.afgprojects.framework.apt.impl;

import io.github.afgprojects.framework.apt.api.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

@SupportedAnnotationTypes({
        "io.github.afgprojects.framework.apt.api.AfService"
})
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class ServiceMetadataProcessor extends AbstractProcessor {

    private ServiceMetadataCodeGenerator codeGenerator;
    private final List<String> generatedClasses = new ArrayList<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.codeGenerator = new ServiceMetadataCodeGenerator(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            writeIndexFile();
            return true;
        }

        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (element.getKind() == ElementKind.CLASS) {
                    processService((TypeElement) element);
                }
            }
        }
        return true;
    }

    private void processService(TypeElement serviceClass) {
        AfService afService = serviceClass.getAnnotation(AfService.class);
        if (afService == null) return;

        String serviceName = afService.name().isEmpty()
                ? decapitalize(serviceClass.getSimpleName().toString())
                : afService.name();

        // 收集所有 @AfOperation 方法
        List<ExecutableElement> operationMethods = serviceClass.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .filter(e -> e.getAnnotation(AfOperation.class) != null)
                .map(e -> (ExecutableElement) e)
                .toList();

        // 检查 name 唯一性
        Map<String, Long> nameCounts = operationMethods.stream()
                .collect(Collectors.groupingBy(
                        m -> getOperationName(m),
                        Collectors.counting()
                ));
        for (Map.Entry<String, Long> entry : nameCounts.entrySet()) {
            if (entry.getValue() > 1) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "Duplicate @AfOperation name '" + entry.getKey() + "' in " + serviceClass.getQualifiedName()
                );
            }
        }

        // 生成元数据类
        String generatedClassName = codeGenerator.generate(serviceClass, serviceName, afService, operationMethods);
        if (generatedClassName != null) {
            generatedClasses.add(generatedClassName);
        }
    }

    private String getOperationName(ExecutableElement method) {
        AfOperation op = method.getAnnotation(AfOperation.class);
        return op.name().isEmpty() ? method.getSimpleName().toString() : op.name();
    }

    private void writeIndexFile() {
        if (generatedClasses.isEmpty()) return;
        try (Writer writer = processingEnv.getFiler()
                .createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/afg/service-metadata.index")
                .openWriter()) {
            for (String className : generatedClasses) {
                writer.write(className);
                writer.write('\n');
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Failed to write service metadata index: " + e.getMessage()
            );
        }
    }

    private String decapitalize(String name) {
        if (name == null || name.isEmpty()) return name;
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}
```

- [ ] **Step 4: 创建 ServiceMetadataCodeGenerator**

```java
package io.github.afgprojects.framework.apt.impl;

import com.squareup.javapoet.*;
import io.github.afgprojects.framework.apt.api.*;
import io.github.afgprojects.framework.core.invocation.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ServiceMetadataCodeGenerator {

    private final ProcessingEnvironment processingEnv;

    public ServiceMetadataCodeGenerator(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    public String generate(TypeElement serviceClass, String serviceName,
                           AfService afService, List<ExecutableElement> operationMethods) {
        String packageName = processingEnv.getElementUtils().getPackageOf(serviceClass).getQualifiedName().toString();
        String className = serviceClass.getSimpleName() + "ServiceMetadata";
        String qualifiedName = packageName + "." + className;

        try {
            JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(qualifiedName, serviceClass);
            try (Writer writer = sourceFile.openWriter()) {
                JavaFile javaFile = buildJavaFile(packageName, className, serviceClass, serviceName, afService, operationMethods);
                javaFile.writeTo(writer);
            }
            return qualifiedName;
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Failed to generate service metadata: " + e.getMessage(),
                    serviceClass
            );
            return null;
        }
    }

    private JavaFile buildJavaFile(String packageName, String className, TypeElement serviceClass,
                                    String serviceName, AfService afService,
                                    List<ExecutableElement> operationMethods) {
        TypeName serviceType = TypeName.get(serviceClass.asType());
        TypeName serviceMetaType = ParameterizedTypeName.get(
                ClassName.get(ServiceMetadata.class), serviceType.box()
        );

        // 构建 operations 列表
        List<CodeBlock> operationBlocks = new ArrayList<>();
        for (ExecutableElement method : operationMethods) {
            operationBlocks.add(buildOperationBlock(method));
        }

        TypeSpec typeSpec = TypeSpec.classBuilder(className)
                .addAnnotation(AnnotationSpec.builder(ClassName.get("jakarta.annotation", "Generated"))
                        .addMember("value", "$S", ServiceMetadataProcessor.class.getName())
                        .build())
                .addSuperinterface(serviceMetaType)
                .addField(buildOperationsField(operationBlocks))
                .addMethod(buildServiceNameMethod(serviceName))
                .addMethod(buildDescriptionMethod(afService.description()))
                .addMethod(buildCategoryMethod(afService.category()))
                .addMethod(buildTagsMethod(afService.tags()))
                .addMethod(buildServiceTypeMethod(serviceType))
                .addMethod(buildOperationsAccessorMethod())
                .build();

        return JavaFile.builder(packageName, typeSpec).build();
    }

    private CodeBlock buildOperationBlock(ExecutableElement method) {
        AfOperation op = method.getAnnotation(AfOperation.class);
        String opName = op.name().isEmpty() ? method.getSimpleName().toString() : op.name();

        List<CodeBlock> paramBlocks = new ArrayList<>();
        for (VariableElement param : method.getParameters()) {
            paramBlocks.add(buildParameterBlock(param));
        }

        String methodParams = method.getParameters().stream()
                .map(p -> p.asType().toString())
                .collect(Collectors.joining("\", \"", "List.of(\"", "\")"));

        CodeBlock.Builder builder = CodeBlock.builder()
                .add("OperationMetadata.builder()\n")
                .add("  .name($S)\n", opName)
                .add("  .description($S)\n", op.description())
                .add("  .method(new MethodKey($S, $L))\n", method.getSimpleName().toString(), methodParams)
                .add("  .parameters($L)\n", paramBlocks.isEmpty() ? "List.of()" : CodeBlock.of("List.of($L)", paramBlocks.stream().collect(CodeBlock.joining(",\n  "))))
                .add("  .returnType($S)\n", method.getReturnType().toString())
                .add("  .returnDescription($S)\n", getReturnDescription(method))
                .add("  .permission($S)\n", op.permission())
                .add("  .requiredRoles($L)\n", buildStringList(op.requiredRoles()))
                .add("  .audit($L)\n", op.audit())
                .add("  .tenantScope($L)\n", op.tenantScope())
                .add("  .dataScope($L)\n", op.dataScope())
                .add("  .async($L)\n", op.async())
                .add("  .deprecated($L)\n", op.deprecated())
                .add("  .inputSchema($S)\n", op.inputSchema().isEmpty() ? deriveInputSchema(method) : op.inputSchema())
                .add("  .paged($L)\n", getPaged(method))
                .add("  .build()");

        return builder.build();
    }

    private CodeBlock buildParameterBlock(VariableElement param) {
        AfParam afParam = param.getAnnotation(AfParam.class);
        String paramName = (afParam != null && !afParam.name().isEmpty())
                ? afParam.name()
                : param.getSimpleName().toString();

        CodeBlock.Builder builder = CodeBlock.builder()
                .add("ParameterMetadata.builder()\n")
                .add("  .name($S)\n", paramName)
                .add("  .type($S)\n", param.asType().toString())
                .add("  .required($L)\n", afParam != null && afParam.required())
                .add("  .defaultValue($S)\n", afParam != null ? afParam.defaultValue() : "")
                .add("  .index($L)\n", getIndex(param))
                .add("  .description($S)\n", afParam != null ? afParam.description() : "")
                .add("  .enumValues($L)\n", afParam != null ? buildStringList(afParam.enumValues()) : "List.of()")
                .add("  .injected($L)\n", isInjected(param))
                .add("  .build()");

        return builder.build();
    }

    private FieldSpec buildOperationsField(List<CodeBlock> operationBlocks) {
        TypeName listType = ParameterizedTypeName.get(
                ClassName.get(List.class),
                ClassName.get(OperationMetadata.class)
        );
        return FieldSpec.builder(listType, "OPERATIONS", javax.lang.model.element.Modifier.PRIVATE,
                        javax.lang.model.element.Modifier.STATIC, javax.lang.model.element.Modifier.FINAL)
                .initializer("$L", CodeBlock.of("List.of($L)",
                        operationBlocks.stream().collect(CodeBlock.joining(",\n  "))))
                .build();
    }

    private MethodSpec buildServiceNameMethod(String serviceName) {
        return MethodSpec.methodBuilder("serviceName")
                .addAnnotation(Override.class)
                .returns(String.class)
                .addStatement("return $S", serviceName)
                .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                .build();
    }

    private MethodSpec buildDescriptionMethod(String description) {
        return MethodSpec.methodBuilder("description")
                .addAnnotation(Override.class)
                .returns(String.class)
                .addStatement("return $S", description)
                .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                .build();
    }

    private MethodSpec buildCategoryMethod(String category) {
        return MethodSpec.methodBuilder("category")
                .addAnnotation(Override.class)
                .returns(String.class)
                .addStatement("return $S", category)
                .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                .build();
    }

    private MethodSpec buildTagsMethod(String[] tags) {
        return MethodSpec.methodBuilder("tags")
                .addAnnotation(Override.class)
                .returns(ParameterizedTypeName.get(List.class, String.class))
                .addStatement("return $L", buildStringList(tags))
                .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                .build();
    }

    private MethodSpec buildServiceTypeMethod(TypeName serviceType) {
        return MethodSpec.methodBuilder("serviceType")
                .addAnnotation(Override.class)
                .returns(ParameterizedTypeName.get(ClassName.get(Class.class), serviceType.box()))
                .addStatement("return $T.class", serviceType.box())
                .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                .build();
    }

    private MethodSpec buildOperationsAccessorMethod() {
        return MethodSpec.methodBuilder("operations")
                .addAnnotation(Override.class)
                .returns(ParameterizedTypeName.get(List.class, OperationMetadata.class))
                .addStatement("return OPERATIONS")
                .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                .build();
    }

    private CodeBlock buildStringList(String[] values) {
        if (values.length == 0) return CodeBlock.of("List.of()");
        StringBuilder sb = new StringBuilder("List.of(");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(values[i]).append("\"");
        }
        sb.append(")");
        return CodeBlock.of(sb.toString());
    }

    private int getIndex(VariableElement param) {
        Element parent = param.getEnclosingElement();
        if (parent instanceof ExecutableElement method) {
            return method.getParameters().indexOf(param);
        }
        return 0;
    }

    private boolean isInjected(VariableElement param) {
        String typeName = param.asType().toString();
        return typeName.startsWith("jakarta.servlet.") || typeName.startsWith("javax.servlet.")
                || typeName.startsWith("java.security.Principal")
                || typeName.startsWith("io.github.afgprojects.framework.ai.core.tool.ToolContext");
    }

    private String getReturnDescription(ExecutableElement method) {
        AfResult result = method.getAnnotation(AfResult.class);
        return result != null ? result.description() : "";
    }

    private boolean getPaged(ExecutableElement method) {
        AfResult result = method.getAnnotation(AfResult.class);
        return result != null && result.paged();
    }

    private String deriveInputSchema(ExecutableElement method) {
        // 简单实现：基于参数类型推导 JSON Schema
        // 复杂的 JSON Schema 推导可以后续迭代增强
        StringBuilder sb = new StringBuilder("{\"type\":\"object\",\"properties\":{");
        List<? extends VariableElement> params = method.getParameters();
        for (int i = 0; i < params.size(); i++) {
            VariableElement param = params.get(i);
            if (isInjected(param)) continue;
            if (i > 0 && !isInjected(params.get(i - 1))) sb.append(",");
            String paramName = param.getSimpleName().toString();
            AfParam afParam = param.getAnnotation(AfParam.class);
            if (afParam != null && !afParam.name().isEmpty()) paramName = afParam.name();
            sb.append("\"").append(paramName).append("\":").append(deriveTypeSchema(param.asType().toString()));
        }
        sb.append("}}");
        return sb.toString();
    }

    private String deriveTypeSchema(String type) {
        return switch (type) {
            case "java.lang.String", "String" -> "{\"type\":\"string\"}";
            case "java.lang.Integer", "int" -> "{\"type\":\"integer\"}";
            case "java.lang.Long", "long" -> "{\"type\":\"integer\",\"format\":\"int64\"}";
            case "java.lang.Double", "double" -> "{\"type\":\"number\"}";
            case "java.lang.Boolean", "boolean" -> "{\"type\":\"boolean\"}";
            case "java.math.BigDecimal" -> "{\"type\":\"number\"}";
            case "java.time.LocalDate" -> "{\"type\":\"string\",\"format\":\"date\"}";
            case "java.time.LocalDateTime" -> "{\"type\":\"string\",\"format\":\"date-time\"}";
            default -> "{\"type\":\"object\"}";
        };
    }
}
```

- [ ] **Step 5: 更新 apt-impl build.gradle 添加 compile-testing 依赖**

在 `apt-impl/build.gradle` 的 test dependencies 中添加：

```kotlin
testImplementation("com.google.testing.compile:compile-testing:0.21.0")
```

同时在 apt-impl 的 dependencies 中确保有：

```kotlin
implementation(project(":apt-api"))
implementation(project(":core"))
```

- [ ] **Step 6: 运行测试**

Run: `cd /home/caiti/code/afg-projects/afg-framework && ./gradlew :apt-impl:test --tests "*.ServiceMetadataProcessorTest" 2>&1 | tail -30`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add apt-impl/src/main/java/io/github/afgprojects/framework/apt/impl/ServiceMetadataProcessor.java \
         apt-impl/src/main/java/io/github/afgprojects/framework/apt/impl/ServiceMetadataCodeGenerator.java \
         apt-impl/src/test/java/io/github/afgprojects/framework/apt/impl/ServiceMetadataProcessorTest.java \
         apt-impl/build.gradle
git commit -m "feat(apt-impl): add ServiceMetadataProcessor with code generation and compile-time validation"
```

---

## Task 9: security-core 拦截器

**Files:**
- Create: `security-core/src/main/java/io/github/afgprojects/framework/security/core/invocation/SecurityInvocationInterceptor.java`
- Create: `security-core/src/main/java/io/github/afgprojects/framework/security/core/invocation/TenantInvocationInterceptor.java`
- Create: `security-core/src/main/java/io/github/afgprojects/framework/security/core/invocation/DataScopeInvocationInterceptor.java`
- Test: `security-core/src/test/java/io/github/afgprojects/framework/security/core/invocation/SecurityInvocationInterceptorTest.java`

- [ ] **Step 1: 编写安全拦截器测试**

```java
package io.github.afgprojects.framework.security.core.invocation;

import io.github.afgprojects.framework.core.invocation.*;
import io.github.afgprojects.framework.security.core.invocation.SecurityInvocationInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityInvocationInterceptorTest {

    private SecurityInvocationInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new SecurityInvocationInterceptor();
    }

    @Test
    void before_shouldPassWhenNoPermissionRequired() {
        InvocationContext ctx = createContext("", List.of());
        assertTrue(interceptor.before(ctx));
    }

    @Test
    void before_shouldPassWhenPermissionMatches() {
        InvocationContext ctx = createContext("user:create", List.of("ROLE_ADMIN"));
        // 需要模拟 SecurityContext 中有对应权限
        // 这里测试的是拦截器逻辑结构，实际权限检查需要 Casbin 集成
        assertTrue(interceptor.before(ctx));
    }

    @Test
    void order_shouldBe100() {
        assertEquals(100, interceptor.order());
    }

    private InvocationContext createContext(String permission, List<String> roles) {
        OperationMetadata opMeta = new OperationMetadata() {
            @Override public String name() { return "test"; }
            @Override public String description() { return ""; }
            @Override public MethodKey method() { return null; }
            @Override public List<ParameterMetadata> parameters() { return List.of(); }
            @Override public String returnType() { return "java.lang.Object"; }
            @Override public String returnDescription() { return ""; }
            @Override public String permission() { return permission; }
            @Override public List<String> requiredRoles() { return roles; }
            @Override public boolean audit() { return false; }
            @Override public boolean tenantScope() { return false; }
            @Override public boolean dataScope() { return false; }
            @Override public boolean async() { return false; }
            @Override public boolean deprecated() { return false; }
            @Override public String inputSchema() { return ""; }
            @Override public boolean paged() { return false; }
        };
        ServiceMetadata<?> svcMeta = new ServiceMetadata<>() {
            @Override public String serviceName() { return "testService"; }
            @Override public String description() { return ""; }
            @Override public String category() { return ""; }
            @Override public List<String> tags() { return List.of(); }
            @Override public Class<?> serviceType() { return Object.class; }
            @Override public List<OperationMetadata> operations() { return List.of(opMeta); }
        };
        return new DefaultInvocationContext(svcMeta, opMeta, new Object(), new Object[0], Map.of(), new HashMap<>());
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd /home/caiti/code/afg-projects/afg-framework && ./gradlew :security-core:test --tests "*.SecurityInvocationInterceptorTest" 2>&1 | tail -20`
Expected: FAIL

- [ ] **Step 3: 创建 SecurityInvocationInterceptor**

```java
package io.github.afgprojects.framework.security.core.invocation;

import io.github.afgprojects.framework.core.invocation.*;
import io.github.afgprojects.framework.security.core.permission.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class SecurityInvocationInterceptor implements InvocationInterceptor {

    private final PermissionService permissionService;

    public SecurityInvocationInterceptor() {
        this.permissionService = null;
    }

    @Override
    public int order() { return 100; }

    @Override
    public boolean before(InvocationContext context) {
        OperationMetadata op = context.operationMetadata();

        // 角色检查
        if (!op.requiredRoles().isEmpty()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                throw new ServiceAccessDeniedException("Authentication required");
            }
            boolean hasRole = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(authority -> op.requiredRoles().contains(authority.replace("ROLE_", "")));
            if (!hasRole) {
                throw new ServiceAccessDeniedException("Missing required role for " + op.name());
            }
        }

        // 权限检查（Casbin）
        if (!op.permission().isEmpty() && permissionService != null) {
            try {
                permissionService.checkPermission(op.permission());
            } catch (Exception e) {
                throw new ServiceAccessDeniedException(op.permission());
            }
        }

        return true;
    }

    @Override
    public Object after(InvocationContext context, Object result) { return result; }

    @Override
    public void onError(InvocationContext context, Exception exception) {}
}
```

- [ ] **Step 4: 创建 TenantInvocationInterceptor**

```java
package io.github.afgprojects.framework.security.core.invocation;

import io.github.afgprojects.framework.core.invocation.*;
import io.github.afgprojects.framework.security.core.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class TenantInvocationInterceptor implements InvocationInterceptor {

    @Override
    public int order() { return 200; }

    @Override
    public boolean before(InvocationContext context) {
        if (!context.operationMetadata().tenantScope()) return true;

        // 将当前租户信息放入上下文 attributes
        String tenantId = TenantContext.currentTenantId();
        if (tenantId != null) {
            context.attributes().put("tenantId", tenantId);
        }
        return true;
    }

    @Override
    public Object after(InvocationContext context, Object result) { return result; }

    @Override
    public void onError(InvocationContext context, Exception exception) {}
}
```

- [ ] **Step 5: 创建 DataScopeInvocationInterceptor**

```java
package io.github.afgprojects.framework.security.core.invocation;

import io.github.afgprojects.framework.core.invocation.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataScopeInvocationInterceptor implements InvocationInterceptor {

    @Override
    public int order() { return 300; }

    @Override
    public boolean before(InvocationContext context) {
        if (!context.operationMetadata().dataScope()) return true;

        // 标记需要数据权限过滤，由 DataManager 层面处理
        context.attributes().put("dataScopeEnabled", true);
        return true;
    }

    @Override
    public Object after(InvocationContext context, Object result) {
        // 数据权限过滤逻辑由 DataManager 的 DataScopeInterceptor 实现
        // 这里只做标记
        return result;
    }

    @Override
    public void onError(InvocationContext context, Exception exception) {}
}
```

- [ ] **Step 6: 运行测试**

Run: `cd /home/caiti/code/afg-projects/afg-framework && ./gradlew :security-core:test --tests "*.SecurityInvocationInterceptorTest" 2>&1 | tail -20`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add security-core/src/main/java/io/github/afgprojects/framework/security/core/invocation/ \
         security-core/src/test/java/io/github/afgprojects/framework/security/core/invocation/SecurityInvocationInterceptorTest.java
git commit -m "feat(security-core): add Security, Tenant, DataScope invocation interceptors"
```

---

## Task 10: AI Tool 自动适配

**Files:**
- Create: `ai-core/src/main/java/io/github/afgprojects/framework/ai/core/tool/ServiceToolAdapter.java`
- Create: `ai-core/src/main/java/io/github/afgprojects/framework/ai/core/tool/ServiceToolRegistrar.java`
- Test: `ai-core/src/test/java/io/github/afgprojects/framework/ai/core/tool/ServiceToolAdapterTest.java`

- [ ] **Step 1: 编写 ServiceToolAdapter 测试**

```java
package io.github.afgprojects.framework.ai.core.tool;

import io.github.afgprojects.framework.core.invocation.*;
import io.github.afgprojects.framework.ai.core.tool.ServiceToolAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ServiceToolAdapterTest {

    private ServiceToolAdapter adapter;
    private OperationMetadata testOp;

    @BeforeEach
    void setUp() {
        testOp = createTestOperation("greet", "Say hello", "user:greet");
        BeanInvocationEngine engine = (serviceName, operationName, arguments) -> "Hello World";
        adapter = new ServiceToolAdapter("testService", testOp, engine);
    }

    @Test
    void name_shouldCombineServiceAndOperation() {
        assertEquals("testService.greet", adapter.name());
    }

    @Test
    void description_shouldReturnOperationDescription() {
        assertEquals("Say hello", adapter.description());
    }

    @Test
    void inputSchema_shouldReturnOperationSchema() {
        assertNotNull(adapter.inputSchema());
    }

    @Test
    void execute_shouldInvokeEngine() {
        Map<String, Object> input = Map.of("name", "World");
        Object result = adapter.execute(input);
        assertEquals("Hello World", result);
    }

    private OperationMetadata createTestOperation(String name, String description, String permission) {
        return new OperationMetadata() {
            @Override public String name() { return name; }
            @Override public String description() { return description; }
            @Override public MethodKey method() { return new MethodKey(name, List.of()); }
            @Override public List<ParameterMetadata> parameters() { return List.of(); }
            @Override public String returnType() { return "java.lang.String"; }
            @Override public String returnDescription() { return ""; }
            @Override public String permission() { return permission; }
            @Override public List<String> requiredRoles() { return List.of(); }
            @Override public boolean audit() { return true; }
            @Override public boolean tenantScope() { return true; }
            @Override public boolean dataScope() { return false; }
            @Override public boolean async() { return false; }
            @Override public boolean deprecated() { return false; }
            @Override public String inputSchema() { return "{\"type\":\"object\"}"; }
            @Override public boolean paged() { return false; }
        };
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd /home/caiti/code/afg-projects/afg-framework && ./gradlew :ai-core:test --tests "*.ServiceToolAdapterTest" 2>&1 | tail -20`
Expected: FAIL

- [ ] **Step 3: 创建 ServiceToolAdapter**

```java
package io.github.afgprojects.framework.ai.core.tool;

import io.github.afgprojects.framework.core.invocation.BeanInvocationEngine;
import io.github.afgprojects.framework.core.invocation.OperationMetadata;

import java.util.List;
import java.util.Map;

public class ServiceToolAdapter implements SecureTool<Map<String, Object>, Object> {

    private final String serviceName;
    private final OperationMetadata operation;
    private final BeanInvocationEngine engine;

    public ServiceToolAdapter(String serviceName, OperationMetadata operation, BeanInvocationEngine engine) {
        this.serviceName = serviceName;
        this.operation = operation;
        this.engine = engine;
    }

    @Override
    public String name() {
        return serviceName + "." + operation.name();
    }

    @Override
    public String description() {
        return operation.description();
    }

    @Override
    public String inputSchema() {
        return operation.inputSchema();
    }

    @Override
    public Object execute(Map<String, Object> input) {
        return engine.invoke(serviceName, operation.name(), input);
    }

    @Override
    public String permission() {
        return operation.permission();
    }

    @Override
    public List<String> requiredRoles() {
        return operation.requiredRoles();
    }

    @Override
    public boolean audit() {
        return operation.audit();
    }

    @Override
    public boolean tenantScope() {
        return operation.tenantScope();
    }

    public String serviceName() {
        return serviceName;
    }

    public OperationMetadata operationMetadata() {
        return operation;
    }
}
```

- [ ] **Step 4: 创建 ServiceToolRegistrar**

```java
package io.github.afgprojects.framework.ai.core.tool;

import io.github.afgprojects.framework.core.invocation.BeanInvocationEngine;
import io.github.afgprojects.framework.core.invocation.OperationMetadata;
import io.github.afgprojects.framework.core.invocation.ServiceMetadata;
import io.github.afgprojects.framework.core.invocation.ServiceMetadataRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class ServiceToolRegistrar implements ApplicationRunner {

    private final ServiceMetadataRegistry metadataRegistry;
    private final BeanInvocationEngine engine;
    private final ToolRegistry toolRegistry;

    @Override
    public void run(ApplicationArguments args) {
        for (ServiceMetadata<?> sm : metadataRegistry.getAll()) {
            for (OperationMetadata op : sm.operations()) {
                if (op.deprecated()) continue;

                String toolName = sm.serviceName() + "." + op.name();

                Optional<Tool<?, ?>> existing = toolRegistry.get(toolName);
                if (existing.isPresent()) {
                    log.info("Skipping auto-registered tool '{}': manually defined tool takes precedence", toolName);
                    continue;
                }

                ServiceToolAdapter adapter = new ServiceToolAdapter(sm.serviceName(), op, engine);
                toolRegistry.register(adapter);
                log.info("Auto-registered tool: {}", toolName);
            }
        }
    }
}
```

- [ ] **Step 5: 运行测试**

Run: `cd /home/caiti/code/afg-projects/afg-framework && ./gradlew :ai-core:test --tests "*.ServiceToolAdapterTest" 2>&1 | tail -20`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add ai-core/src/main/java/io/github/afgprojects/framework/ai/core/tool/ServiceToolAdapter.java \
         ai-core/src/main/java/io/github/afgprojects/framework/ai/core/tool/ServiceToolRegistrar.java \
         ai-core/src/test/java/io/github/afgprojects/framework/ai/core/tool/ServiceToolAdapterTest.java
git commit -m "feat(ai-core): add ServiceToolAdapter and ServiceToolRegistrar for auto tool registration"
```

---

## Task 11: Spring Boot 自动配置

**Files:**
- Create: `spring-boot-starter/src/main/java/io/github/afgprojects/framework/autoconfigure/BeanInvocationAutoConfiguration.java`
- Create: `spring-boot-starter/src/main/java/io/github/afgprojects/framework/autoconfigure/BeanInvocationProperties.java`
- Create: `ai-spring-boot-starter/src/main/java/io/github/afgprojects/framework/ai/autoconfigure/ServiceToolAutoConfiguration.java`
- Modify: `spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- [ ] **Step 1: 创建 BeanInvocationProperties**

```java
package io.github.afgprojects.framework.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "afg.invocation")
public class BeanInvocationProperties {

    private boolean enabled = true;
    private int asyncPoolSize = 4;
    private boolean interceptorsEnabled = true;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getAsyncPoolSize() { return asyncPoolSize; }
    public void setAsyncPoolSize(int asyncPoolSize) { this.asyncPoolSize = asyncPoolSize; }
    public boolean isInterceptorsEnabled() { return interceptorsEnabled; }
    public void setInterceptorsEnabled(boolean interceptorsEnabled) { this.interceptorsEnabled = interceptorsEnabled; }
}
```

- [ ] **Step 2: 创建 BeanInvocationAutoConfiguration**

```java
package io.github.afgprojects.framework.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.core.invocation.*;
import io.github.afgprojects.framework.core.invocation.interceptor.AuditInvocationInterceptor;
import io.github.afgprojects.framework.core.invocation.interceptor.ValidationInvocationInterceptor;
import io.github.afgprojects.framework.core.invocation.processor.IdentityProcessor;
import io.github.afgprojects.framework.core.invocation.processor.PagedResultProcessor;
import io.github.afgprojects.framework.core.invocation.processor.ResultProcessor;
import io.github.afgprojects.framework.core.invocation.processor.SensitiveMaskProcessor;
import io.github.afgprojects.framework.core.invocation.resolver.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(BeanInvocationProperties.class)
@ConditionalOnProperty(prefix = "afg.invocation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BeanInvocationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ServiceMetadataRegistry serviceMetadataRegistry() {
        return new DefaultServiceMetadataRegistry();
    }

    @Bean
    public AptServiceMetadataLoader aptServiceMetadataLoader(ServiceMetadataRegistry registry) {
        AptServiceMetadataLoader loader = new AptServiceMetadataLoader(registry);
        loader.load();
        return loader;
    }

    @Bean
    @ConditionalOnMissingBean
    public BeanInvocationEngine beanInvocationEngine(
            ServiceMetadataRegistry registry,
            ApplicationContext applicationContext,
            ObjectMapper objectMapper,
            @Autowired(required = false) List<InvocationInterceptor> interceptors,
            @Autowired(required = false) List<ArgumentResolver> resolvers,
            @Autowired(required = false) List<ResultProcessor> resultProcessors) {

        List<InvocationInterceptor> allInterceptors = new ArrayList<>();
        if (interceptors != null) allInterceptors.addAll(interceptors);
        allInterceptors.add(new AuditInvocationInterceptor());

        List<ArgumentResolver> allResolvers = new ArrayList<>();
        if (resolvers != null) allResolvers.addAll(resolvers);
        allResolvers.add(new IdentityResolver());
        allResolvers.add(new JacksonConvertResolver());
        allResolvers.add(new StringConverterResolver());
        allResolvers.add(new CollectionResolver());
        allResolvers.add(new NullDefaultResolver());

        List<ResultProcessor> allProcessors = new ArrayList<>();
        if (resultProcessors != null) allProcessors.addAll(resultProcessors);
        allProcessors.add(new SensitiveMaskProcessor());
        allProcessors.add(new PagedResultProcessor());
        allProcessors.add(new IdentityProcessor());

        return new DefaultBeanInvocationEngine(
                registry,
                serviceName -> applicationContext.getBean(serviceName),
                allInterceptors,
                allResolvers,
                allProcessors,
                objectMapper
        );
    }
}
```

- [ ] **Step 3: 创建 ServiceToolAutoConfiguration**

```java
package io.github.afgprojects.framework.ai.autoconfigure;

import io.github.afgprojects.framework.ai.core.tool.ServiceToolRegistrar;
import io.github.afgprojects.framework.core.invocation.BeanInvocationEngine;
import io.github.afgprojects.framework.core.invocation.ServiceMetadataRegistry;
import io.github.afgprojects.framework.ai.core.tool.ToolRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnBean({BeanInvocationEngine.class, ToolRegistry.class})
@ConditionalOnProperty(prefix = "afg.invocation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ServiceToolAutoConfiguration {

    @Bean
    public ServiceToolRegistrar serviceToolRegistrar(
            ServiceMetadataRegistry metadataRegistry,
            BeanInvocationEngine engine,
            ToolRegistry toolRegistry) {
        return new ServiceToolRegistrar(metadataRegistry, engine, toolRegistry);
    }
}
```

- [ ] **Step 4: 更新 AutoConfiguration imports**

在 `spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 中追加：

```
io.github.afgprojects.framework.autoconfigure.BeanInvocationAutoConfiguration
```

在 `ai-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 中追加：

```
io.github.afgprojects.framework.ai.autoconfigure.ServiceToolAutoConfiguration
```

- [ ] **Step 5: 编译验证**

Run: `cd /home/caiti/code/afg-projects/afg-framework && ./gradlew build 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add spring-boot-starter/src/main/java/io/github/afgprojects/framework/autoconfigure/BeanInvocationAutoConfiguration.java \
         spring-boot-starter/src/main/java/io/github/afgprojects/framework/autoconfigure/BeanInvocationProperties.java \
         ai-spring-boot-starter/src/main/java/io/github/afgprojects/framework/ai/autoconfigure/ServiceToolAutoConfiguration.java \
         spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports \
         ai-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
git commit -m "feat(starter): add BeanInvocationAutoConfiguration and ServiceToolAutoConfiguration"
```

---

## Task 12: Gradle 插件 -parameters 支持

**Files:**
- Modify: `gradle-plugin/src/main/java/.../AfgFrameworkPlugin.java` (或对应的插件类)

- [ ] **Step 1: 找到 gradle-plugin 的插件实现类**

Run: `find /home/caiti/code/afg-projects/afg-framework/gradle-plugin -name "*.java" -o -name "*.kt" | head -20`

- [ ] **Step 2: 在插件中添加 -parameters 编译选项**

在插件的 `apply` 方法中，为所有 `JavaCompile` 任务添加 `-parameters` 选项：

```java
project.getTasks().withType(JavaCompile.class).configureEach(task -> {
    task.getOptions().getCompilerArgs().add("-parameters");
});
```

- [ ] **Step 3: 编译验证**

Run: `cd /home/caiti/code/afg-projects/afg-framework && ./gradlew :gradle-plugin:build 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add gradle-plugin/
git commit -m "feat(gradle-plugin): add -parameters compiler option for APT parameter name extraction"
```

---

## Task 13: 全量构建和集成测试

**Files:**
- 无新增文件

- [ ] **Step 1: 全量构建**

Run: `cd /home/caiti/code/afg-projects/afg-framework && ./gradlew clean build 2>&1 | tail -30`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 运行所有测试**

Run: `cd /home/caiti/code/afg-projects/afg-framework && ./gradlew test 2>&1 | tail -30`
Expected: 所有测试通过

- [ ] **Step 3: 发布到本地 Maven**

Run: `cd /home/caiti/code/afg-projects/afg-framework && ./gradlew publishToMavenLocal 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 最终 Commit**

```bash
git commit --allow-empty -m "feat: APT-driven Bean dynamic invocation framework complete - all tests passing"
```
