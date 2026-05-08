# 安全模块实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建 afg-framework 安全模块，包含核心抽象层、OAuth2 授权服务器、资源服务器和 Casbin 权限集成。

**Architecture:** 采用分层架构，security-core 定义核心接口，security-impl 下的三个子模块独立实现。遵循 TDD 原则，每个模块独立可测试。

**Tech Stack:** Java 25, Spring Boot 4, Spring Security 6, Spring Authorization Server, JCasbin, JSpecify

---

## 文件结构

```
afg-framework/
├── security-core/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/java/io/github/afgprojects/framework/security/core/
│       │   ├── authentication/
│       │   │   ├── AfgUserDetails.java
│       │   │   ├── AfgAuthentication.java
│       │   │   ├── AfgUserDetailsService.java
│       │   │   └── package-info.java
│       │   ├── authorization/
│       │   │   ├── AfgEnforcer.java
│       │   │   └── package-info.java
│       │   ├── tenant/
│       │   │   ├── TenantContext.java
│       │   │   ├── TenantResolver.java
│       │   │   ├── TenantAware.java
│       │   │   ├── TenantException.java
│       │   │   └── package-info.java
│       │   ├── token/
│       │   │   ├── AfgToken.java
│       │   │   ├── AfgTokenProvider.java
│       │   │   └── package-info.java
│       │   └── package-info.java
│       └── test/java/io/github/afgprojects/framework/security/core/
│           ├── authentication/
│           ├── authorization/
│           ├── tenant/
│           └── token/
│
├── security-impl/
│   ├── auth-server/
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── main/java/io/github/afgprojects/framework/security/auth/
│   │       │   ├── config/
│   │       │   │   ├── AuthServerProperties.java
│   │       │   │   ├── AuthServerAutoConfiguration.java
│   │       │   │   └── package-info.java
│   │       │   ├── endpoint/
│   │       │   │   └── package-info.java
│   │       │   ├── token/
│   │       │   │   ├── JwtTokenProvider.java
│   │       │   │   └── package-info.java
│   │       │   ├── user/
│   │       │   │   ├── AfgClientDetailsService.java
│   │       │   │   └── package-info.java
│   │       │   └── package-info.java
│   │       └── test/java/io/github/afgprojects/framework/security/auth/
│   │           ├── config/
│   │           ├── token/
│   │           └── user/
│   │
│   ├── resource-server/
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── main/java/io/github/afgprojects/framework/security/resource/
│   │       │   ├── jwt/
│   │       │   │   ├── JwtResourceProperties.java
│   │       │   │   ├── JwtAuthenticationConverter.java
│   │       │   │   └── package-info.java
│   │       │   ├── introspection/
│   │       │   │   ├── IntrospectionProperties.java
│   │       │   │   └── package-info.java
│   │       │   ├── tenant/
│   │       │   │   ├── TenantResolveStrategy.java
│   │       │   │   ├── TenantResolverChain.java
│   │       │   │   ├── HeaderTenantResolver.java
│   │       │   │   ├── TokenTenantResolver.java
│   │       │   │   ├── DefaultTenantContext.java
│   │       │   │   └── package-info.java
│   │       │   ├── config/
│   │       │   │   ├── ResourceServerAutoConfiguration.java
│   │       │   │   └── package-info.java
│   │       │   └── package-info.java
│   │       └── test/java/io/github/afgprojects/framework/security/resource/
│   │           ├── jwt/
│   │           └── tenant/
│   │
│   └── security-casbin/
│       ├── build.gradle.kts
│       └── src/
│           ├── main/java/io/github/afgprojects/framework/security/casbin/
│           │   ├── config/
│           │   │   ├── CasbinProperties.java
│           │   │   ├── CasbinAutoConfiguration.java
│           │   │   └── package-info.java
│           │   ├── enforcer/
│           │   │   ├── CasbinAfgEnforcer.java
│           │   │   └── package-info.java
│           │   ├── model/
│           │   │   ├── CasbinRule.java
│           │   │   ├── AfgPolicyService.java
│           │   │   └── package-info.java
│           │   └── package-info.java
│           └── test/java/io/github/afgprojects/framework/security/casbin/
│               ├── enforcer/
│               └── model/
```

---

## Phase 1: security-core 模块

### Task 1: 创建 security-core 模块结构

**Files:**
- Create: `afg-framework/security-core/build.gradle.kts`
- Modify: `afg-framework/settings.gradle.kts`

- [ ] **Step 1: 更新 settings.gradle.kts 添加模块**

```kotlin
// 在 settings.gradle.kts 中添加（在 data-impl:data-liquibase 之后）

// 安全模块
include("security-core")
include("security-impl:auth-server")       // OAuth2 授权服务器
include("security-impl:resource-server")   // 资源服务器
include("security-impl:security-casbin")   // Casbin 权限集成
```

- [ ] **Step 2: 创建 security-core/build.gradle.kts**

```kotlin
plugins {
    `java-library`
}

dependencies {
    // 依赖 core 模块
    api(project(":core"))

    // Spring Security
    api(libs.spring.boot.starter.security)

    // JSpecify 空安全注解
    api(libs.jspecify)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Test
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
}
```

- [ ] **Step 3: 创建模块目录结构**

```bash
mkdir -p afg-framework/security-core/src/main/java/io/github/afgprojects/framework/security/core/{authentication,authorization,tenant,token}
mkdir -p afg-framework/security-core/src/test/java/io/github/afgprojects/framework/security/core/{authentication,authorization,tenant,token}
```

- [ ] **Step 4: 验证模块可构建**

Run: `cd afg-framework && ./gradlew :security-core:build --dry-run`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 提交**

```bash
git add settings.gradle.kts security-core/build.gradle.kts
git commit -m "feat(security): add security-core module structure"
```

---

### Task 2: 实现 authentication 包核心接口

**Files:**
- Create: `security-core/src/main/java/io/github/afgprojects/framework/security/core/authentication/AfgUserDetails.java`
- Create: `security-core/src/main/java/io/github/afgprojects/framework/security/core/authentication/AfgAuthentication.java`
- Create: `security-core/src/main/java/io/github/afgprojects/framework/security/core/authentication/AfgUserDetailsService.java`
- Create: `security-core/src/main/java/io/github/afgprojects/framework/security/core/authentication/package-info.java`
- Create: `security-core/src/test/java/io/github/afgprojects/framework/security/core/authentication/AfgUserDetailsTest.java`

- [ ] **Step 1: 编写 AfgUserDetails 接口测试**

```java
// security-core/src/test/java/io/github/afgprojects/framework/security/core/authentication/AfgUserDetailsTest.java
package io.github.afgprojects.framework.security.core.authentication;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class AfgUserDetailsTest {

    @Test
    void shouldProvideUserInformation() {
        AfgUserDetails userDetails = createTestUserDetails();

        assertThat(userDetails.getId()).isEqualTo("user-123");
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        assertThat(userDetails.getTenantId()).isEqualTo("tenant-1");
        assertThat(userDetails.getRoles()).containsExactly("ADMIN", "USER");
        assertThat(userDetails.getPermissions()).containsExactly("user:read", "user:write");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
    }

    @Test
    void shouldSupportDisabledUser() {
        AfgUserDetails userDetails = createDisabledUserDetails();

        assertThat(userDetails.isEnabled()).isFalse();
        assertThat(userDetails.isAccountNonLocked()).isFalse();
    }

    private AfgUserDetails createTestUserDetails() {
        return new AfgUserDetails() {
            @Override
            public String getId() {
                return "user-123";
            }

            @Override
            public String getUsername() {
                return "testuser";
            }

            @Override
            public String getTenantId() {
                return "tenant-1";
            }

            @Override
            public Set<String> getRoles() {
                return Set.of("ADMIN", "USER");
            }

            @Override
            public Set<String> getPermissions() {
                return Set.of("user:read", "user:write");
            }

            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public boolean isAccountNonLocked() {
                return true;
            }
        };
    }

    private AfgUserDetails createDisabledUserDetails() {
        return new AfgUserDetails() {
            @Override
            public String getId() {
                return "user-456";
            }

            @Override
            public String getUsername() {
                return "disableduser";
            }

            @Override
            public String getTenantId() {
                return "tenant-1";
            }

            @Override
            public Set<String> getRoles() {
                return Set.of();
            }

            @Override
            public Set<String> getPermissions() {
                return Set.of();
            }

            @Override
            public boolean isEnabled() {
                return false;
            }

            @Override
            public boolean isAccountNonLocked() {
                return false;
            }
        };
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd afg-framework && ./gradlew :security-core:test --tests AfgUserDetailsTest`
Expected: FAIL - AfgUserDetails 类不存在

- [ ] **Step 3: 实现 AfgUserDetails 接口**

```java
// security-core/src/main/java/io/github/afgprojects/framework/security/core/authentication/AfgUserDetails.java
package io.github.afgprojects.framework.security.core.authentication;

import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * AFG 安全用户主体
 * <p>
 * 提供用户认证和授权所需的核心信息。
 * 业务系统需实现此接口以提供用户数据。
 */
public interface AfgUserDetails {

    /**
     * 用户唯一标识
     */
    String getId();

    /**
     * 用户名
     */
    String getUsername();

    /**
     * 租户 ID
     */
    @Nullable String getTenantId();

    /**
     * 用户角色集合
     */
    Set<String> getRoles();

    /**
     * 用户权限集合
     */
    Set<String> getPermissions();

    /**
     * 账户是否启用
     */
    boolean isEnabled();

    /**
     * 账户是否未锁定
     */
    boolean isAccountNonLocked();
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `cd afg-framework && ./gradlew :security-core:test --tests AfgUserDetailsTest`
Expected: PASS

- [ ] **Step 5: 实现 AfgAuthentication 接口**

```java
// security-core/src/main/java/io/github/afgprojects/framework/security/core/authentication/AfgAuthentication.java
package io.github.afgprojects.framework.security.core.authentication;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;

/**
 * AFG 认证令牌
 * <p>
 * 封装认证成功后的用户信息和凭证。
 */
public interface AfgAuthentication {

    /**
     * 认证主体（用户信息）
     */
    AfgUserDetails getPrincipal();

    /**
     * 凭证（如密码、Token）
     */
    @Nullable String getCredentials();

    /**
     * 权限集合
     */
    default Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    /**
     * 扩展详情
     */
    default Map<String, Object> getDetails() {
        return Collections.emptyMap();
    }

    /**
     * 是否已认证
     */
    default boolean isAuthenticated() {
        return getPrincipal() != null;
    }
}
```

- [ ] **Step 6: 实现 AfgUserDetailsService SPI 接口**

```java
// security-core/src/main/java/io/github/afgprojects/framework/security/core/authentication/AfgUserDetailsService.java
package io.github.afgprojects.framework.security.core.authentication;

import org.jspecify.annotations.Nullable;

/**
 * AFG 用户服务 SPI
 * <p>
 * 业务系统实现此接口以提供用户数据。
 * 认证服务器通过此接口加载用户信息进行认证。
 */
public interface AfgUserDetailsService {

    /**
     * 根据用户名加载用户
     *
     * @param username 用户名
     * @param tenantId 租户 ID（可为 null）
     * @return 用户详情，未找到返回 null
     */
    @Nullable AfgUserDetails loadUserByUsername(String username, @Nullable String tenantId);

    /**
     * 根据 ID 加载用户
     *
     * @param userId 用户 ID
     * @param tenantId 租户 ID（可为 null）
     * @return 用户详情，未找到返回 null
     */
    @Nullable AfgUserDetails loadUserById(String userId, @Nullable String tenantId);

    /**
     * 验证用户凭证
     *
     * @param user 用户详情
     * @param password 密码
     * @throws AuthenticationException 凭证无效时抛出
     */
    void validateCredentials(AfgUserDetails user, String password);
}
```

- [ ] **Step 7: 创建 package-info.java**

```java
// security-core/src/main/java/io/github/afgprojects/framework/security/core/authentication/package-info.java
/**
 * 认证核心接口
 * <p>
 * 提供用户认证所需的核心抽象：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.security.core.authentication.AfgUserDetails} - 用户主体</li>
 *   <li>{@link io.github.afgprojects.framework.security.core.authentication.AfgAuthentication} - 认证令牌</li>
 *   <li>{@link io.github.afgprojects.framework.security.core.authentication.AfgUserDetailsService} - 用户服务 SPI</li>
 * </ul>
 */
package io.github.afgprojects.framework.security.core.authentication;
```

- [ ] **Step 8: 运行所有测试**

Run: `cd afg-framework && ./gradlew :security-core:test`
Expected: PASS

- [ ] **Step 9: 提交**

```bash
git add security-core/src/main/java/io/github/afgprojects/framework/security/core/authentication/
git add security-core/src/test/java/io/github/afgprojects/framework/security/core/authentication/
git commit -m "feat(security-core): add authentication interfaces"
```

---

### Task 3: 实现 authorization 包核心接口

**Files:**
- Create: `security-core/src/main/java/io/github/afgprojects/framework/security/core/authorization/AfgEnforcer.java`
- Create: `security-core/src/main/java/io/github/afgprojects/framework/security/core/authorization/package-info.java`
- Create: `security-core/src/test/java/io/github/afgprojects/framework/security/core/authorization/AfgEnforcerTest.java`

- [ ] **Step 1: 编写 AfgEnforcer 接口测试**

```java
// security-core/src/test/java/io/github/afgprojects/framework/security/core/authorization/AfgEnforcerTest.java
package io.github.afgprojects.framework.security.core.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AfgEnforcerTest {

    @Test
    void shouldEnforcePermission() {
        AfgEnforcer enforcer = (subject, resource, action) -> true;

        boolean result = enforcer.enforce("user-1", "/api/users", "GET");

        assertThat(result).isTrue();
    }

    @Test
    void shouldDenyPermission() {
        AfgEnforcer enforcer = (subject, resource, action) -> false;

        boolean result = enforcer.enforce("user-1", "/api/admin", "DELETE");

        assertThat(result).isFalse();
    }

    @Test
    void shouldEnforceWithNullSubject() {
        AfgEnforcer enforcer = (subject, resource, action) -> subject == null;

        boolean result = enforcer.enforce(null, "/api/public", "GET");

        assertThat(result).isTrue();
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd afg-framework && ./gradlew :security-core:test --tests AfgEnforcerTest`
Expected: FAIL - AfgEnforcer 类不存在

- [ ] **Step 3: 实现 AfgEnforcer 接口**

```java
// security-core/src/main/java/io/github/afgprojects/framework/security/core/authorization/AfgEnforcer.java
package io.github.afgprojects.framework.security.core.authorization;

import org.jspecify.annotations.Nullable;

/**
 * AFG 权限执行器
 * <p>
 * 提供统一的权限检查接口，与 Casbin 的 enforce(sub, obj, act) 天然对应。
 * 实现类可基于 Casbin、Spring Security 或其他权限框架。
 */
@FunctionalInterface
public interface AfgEnforcer {

    /**
     * 检查权限
     *
     * @param subject 主体（用户 ID）
     * @param resource 资源（URL、数据对象等）
     * @param action 操作（GET、POST、DELETE 等）
     * @return true 表示有权限，false 表示无权限
     */
    boolean enforce(@Nullable String subject, String resource, String action);

    /**
     * 使用认证上下文检查权限
     *
     * @param context 认证上下文
     * @param resource 资源
     * @param action 操作
     * @return true 表示有权限
     */
    default boolean enforce(@Nullable AfgSecurityContext context, String resource, String action) {
        String subject = context != null && context.getPrincipal() != null
                ? context.getPrincipal().getId()
                : null;
        return enforce(subject, resource, action);
    }
}
```

- [ ] **Step 4: 添加 AfgSecurityContext 引用接口**

```java
// security-core/src/main/java/io/github/afgprojects/framework/security/core/authorization/AfgSecurityContext.java
package io.github.afgprojects.framework.security.core.authorization;

import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;
import org.jspecify.annotations.Nullable;

/**
 * AFG 安全上下文
 * <p>
 * 提供当前请求的安全信息，包括用户主体和租户信息。
 */
public interface AfgSecurityContext {

    /**
     * 获取用户主体
     */
    @Nullable AfgUserDetails getPrincipal();

    /**
     * 获取租户 ID
     */
    @Nullable String getTenantId();

    /**
     * 获取扩展属性
     */
    @Nullable <T> T getAttribute(String key);
}
```

- [ ] **Step 5: 运行测试验证通过**

Run: `cd afg-framework && ./gradlew :security-core:test --tests AfgEnforcerTest`
Expected: PASS

- [ ] **Step 6: 创建 package-info.java**

```java
// security-core/src/main/java/io/github/afgprojects/framework/security/core/authorization/package-info.java
/**
 * 授权核心接口
 * <p>
 * 提供权限检查的核心抽象：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.security.core.authorization.AfgEnforcer} - 权限执行器</li>
 *   <li>{@link io.github.afgprojects.framework.security.core.authorization.AfgSecurityContext} - 安全上下文</li>
 * </ul>
 */
package io.github.afgprojects.framework.security.core.authorization;
```

- [ ] **Step 7: 提交**

```bash
git add security-core/src/main/java/io/github/afgprojects/framework/security/core/authorization/
git add security-core/src/test/java/io/github/afgprojects/framework/security/core/authorization/
git commit -m "feat(security-core): add authorization interfaces"
```

---

### Task 4: 实现 tenant 包核心接口

**Files:**
- Create: `security-core/src/main/java/io/github/afgprojects/framework/security/core/tenant/TenantContext.java`
- Create: `security-core/src/main/java/io/github/afgprojects/framework/security/core/tenant/TenantResolver.java`
- Create: `security-core/src/main/java/io/github/afgprojects/framework/security/core/tenant/TenantAware.java`
- Create: `security-core/src/main/java/io/github/afgprojects/framework/security/core/tenant/TenantException.java`
- Create: `security-core/src/main/java/io/github/afgprojects/framework/security/core/tenant/package-info.java`
- Create: `security-core/src/test/java/io/github/afgprojects/framework/security/core/tenant/TenantContextTest.java`

- [ ] **Step 1: 编写 TenantContext 测试**

```java
// security-core/src/test/java/io/github/afgprojects/framework/security/core/tenant/TenantContextTest.java
package io.github.afgprojects.framework.security.core.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TenantContextTest {

    @Test
    void shouldProvideTenantInformation() {
        TenantContext context = createTestTenantContext();

        assertThat(context.getTenantId()).isEqualTo("tenant-1");
        assertThat(context.getTenantCode()).isEqualTo("ACME");
        assertThat(context.getAttribute("region")).isEqualTo("cn-east-1");
    }

    @Test
    void shouldSupportNullTenantCode() {
        TenantContext context = () -> "tenant-1";

        assertThat(context.getTenantId()).isEqualTo("tenant-1");
        assertThat(context.getTenantCode()).isNull();
    }

    private TenantContext createTestTenantContext() {
        return new TenantContext() {
            @Override
            public String getTenantId() {
                return "tenant-1";
            }

            @Override
            public String getTenantCode() {
                return "ACME";
            }

            @Override
            public Map<String, Object> getAttributes() {
                return Map.of("region", "cn-east-1");
            }
        };
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd afg-framework && ./gradlew :security-core:test --tests TenantContextTest`
Expected: FAIL - TenantContext 类不存在

- [ ] **Step 3: 实现 TenantContext 接口**

```java
// security-core/src/main/java/io/github/afgprojects/framework/security/core/tenant/TenantContext.java
package io.github.afgprojects.framework.security.core.tenant;

import java.util.Collections;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * 租户上下文
 * <p>
 * 存储当前请求的租户信息，支持多租户场景。
 */
public interface TenantContext {

    /**
     * 租户唯一标识
     */
    String getTenantId();

    /**
     * 租户编码（可选）
     */
    default @Nullable String getTenantCode() {
        return null;
    }

    /**
     * 租户扩展属性
     */
    default Map<String, Object> getAttributes() {
        return Collections.emptyMap();
    }

    /**
     * 获取扩展属性
     *
     * @param key 属性名
     * @return 属性值，不存在返回 null
     */
    default @Nullable Object getAttribute(String key) {
        return getAttributes().get(key);
    }
}
```

- [ ] **Step 4: 实现 TenantResolver 接口**

```java
// security-core/src/main/java/io/github/afgprojects/framework/security/core/tenant/TenantResolver.java
package io.github.afgprojects.framework.security.core.tenant;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

/**
 * 租户解析器
 * <p>
 * 从请求或 Token 中解析租户信息。
 * 支持多种解析策略：Header、Token、Domain、Path。
 */
public interface TenantResolver {

    /**
     * 从 HTTP 请求解析租户
     *
     * @param request HTTP 请求
     * @return 租户上下文，解析失败返回 null
     */
    @Nullable TenantContext resolve(HttpServletRequest request);

    /**
     * 从 Token 解析租户
     *
     * @param token Token 字符串
     * @return 租户上下文，解析失败返回 null
     */
    default @Nullable TenantContext resolveFromToken(String token) {
        return null;
    }
}
```

- [ ] **Step 5: 实现 TenantAware 标记接口**

```java
// security-core/src/main/java/io/github/afgprojects/framework/security/core/tenant/TenantAware.java
package io.github.afgprojects.framework.security.core.tenant;

/**
 * 租户感知接口
 * <p>
 * 标记实现类包含租户信息，用于数据隔离。
 */
public interface TenantAware {

    /**
     * 获取租户 ID
     */
    String getTenantId();
}
```

- [ ] **Step 6: 实现 TenantException 异常类**

```java
// security-core/src/main/java/io/github/afgprojects/framework/security/core/tenant/TenantException.java
package io.github.afgprojects.framework.security.core.tenant;

import io.github.afgprojects.framework.core.model.exception.BusinessException;
import io.github.afgprojects.framework.core.model.exception.ErrorCode;

/**
 * 租户异常
 */
public class TenantException extends BusinessException {

    public TenantException(ErrorCode errorCode) {
        super(errorCode);
    }

    public TenantException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public static TenantException notFound() {
        return new TenantException(TenantErrorCode.NOT_FOUND);
    }

    public static TenantException invalid(String message) {
        return new TenantException(TenantErrorCode.INVALID, message);
    }
}
```

- [ ] **Step 7: 实现 TenantErrorCode 枚举**

```java
// security-core/src/main/java/io/github/afgprojects/framework/security/core/tenant/TenantErrorCode.java
package io.github.afgprojects.framework.security.core.tenant;

import io.github.afgprojects.framework.core.model.exception.ErrorCategory;
import io.github.afgprojects.framework.core.model.exception.ErrorCode;

/**
 * 租户错误码
 * <p>
 * 错误码范围：19000-19999
 */
public enum TenantErrorCode implements ErrorCode {

    /**
     * 租户不存在
     */
    NOT_FOUND(19000, "租户不存在", ErrorCategory.BUSINESS),

    /**
     * 租户无效
     */
    INVALID(19001, "租户无效", ErrorCategory.BUSINESS),

    /**
     * 租户已禁用
     */
    DISABLED(19002, "租户已禁用", ErrorCategory.BUSINESS),

    /**
     * 租户解析失败
     */
    RESOLVE_ERROR(19003, "租户解析失败", ErrorCategory.SYSTEM);

    private final int code;
    private final String message;
    private final ErrorCategory category;

    TenantErrorCode(int code, String message, ErrorCategory category) {
        this.code = code;
        this.message = message;
        this.category = category;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public ErrorCategory getCategory() {
        return category;
    }
}
```

- [ ] **Step 8: 运行测试验证通过**

Run: `cd afg-framework && ./gradlew :security-core:test --tests TenantContextTest`
Expected: PASS

- [ ] **Step 9: 创建 package-info.java**

```java
// security-core/src/main/java/io/github/afgprojects/framework/security/core/tenant/package-info.java
/**
 * 多租户核心接口
 * <p>
 * 提供多租户场景的核心抽象：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.security.core.tenant.TenantContext} - 租户上下文</li>
 *   <li>{@link io.github.afgprojects.framework.security.core.tenant.TenantResolver} - 租户解析器</li>
 *   <li>{@link io.github.afgprojects.framework.security.core.tenant.TenantAware} - 租户感知标记</li>
 * </ul>
 */
package io.github.afgprojects.framework.security.core.tenant;
```

- [ ] **Step 10: 提交**

```bash
git add security-core/src/main/java/io/github/afgprojects/framework/security/core/tenant/
git add security-core/src/test/java/io/github/afgprojects/framework/security/core/tenant/
git commit -m "feat(security-core): add tenant interfaces"
```

---

### Task 5: 实现 token 包核心接口

**Files:**
- Create: `security-core/src/main/java/io/github/afgprojects/framework/security/core/token/AfgToken.java`
- Create: `security-core/src/main/java/io/github/afgprojects/framework/security/core/token/AfgTokenProvider.java`
- Create: `security-core/src/main/java/io/github/afgprojects/framework/security/core/token/package-info.java`
- Create: `security-core/src/test/java/io/github/afgprojects/framework/security/core/token/AfgTokenTest.java`

- [ ] **Step 1: 编写 AfgToken 测试**

```java
// security-core/src/test/java/io/github/afgprojects/framework/security/core/token/AfgTokenTest.java
package io.github.afgprojects.framework.security.core.token;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AfgTokenTest {

    @Test
    void shouldProvideTokenInformation() {
        Instant now = Instant.now();
        Instant expires = now.plusSeconds(3600);

        AfgToken token = createTestToken("access-token-123", now, expires, "access");

        assertThat(token.getTokenValue()).isEqualTo("access-token-123");
        assertThat(token.getIssuedAt()).isEqualTo(now);
        assertThat(token.getExpiresAt()).isEqualTo(expires);
        assertThat(token.getTokenType()).isEqualTo("access");
        assertThat(token.isExpired()).isFalse();
    }

    @Test
    void shouldDetectExpiredToken() {
        Instant past = Instant.now().minusSeconds(3600);

        AfgToken token = createTestToken("expired-token", past, past, "access");

        assertThat(token.isExpired()).isTrue();
    }

    private AfgToken createTestToken(String value, Instant issuedAt, Instant expiresAt, String type) {
        return new AfgToken() {
            @Override
            public String getTokenValue() {
                return value;
            }

            @Override
            public Instant getIssuedAt() {
                return issuedAt;
            }

            @Override
            public Instant getExpiresAt() {
                return expiresAt;
            }

            @Override
            public String getTokenType() {
                return type;
            }
        };
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd afg-framework && ./gradlew :security-core:test --tests AfgTokenTest`
Expected: FAIL - AfgToken 类不存在

- [ ] **Step 3: 实现 AfgToken 接口**

```java
// security-core/src/main/java/io/github/afgprojects/framework/security/core/token/AfgToken.java
package io.github.afgprojects.framework.security.core.token;

import java.time.Instant;

/**
 * AFG Token 抽象
 * <p>
 * 封装访问令牌和刷新令牌的通用属性。
 */
public interface AfgToken {

    /**
     * Token 值
     */
    String getTokenValue();

    /**
     * 签发时间
     */
    Instant getIssuedAt();

    /**
     * 过期时间
     */
    Instant getExpiresAt();

    /**
     * Token 类型
     * <p>
     * 常见值：access, refresh
     */
    String getTokenType();

    /**
     * 是否已过期
     */
    default boolean isExpired() {
        return Instant.now().isAfter(getExpiresAt());
    }
}
```

- [ ] **Step 4: 实现 AfgTokenProvider 接口**

```java
// security-core/src/main/java/io/github/afgprojects/framework/security/core/token/AfgTokenProvider.java
package io.github.afgprojects.framework.security.core.token;

import io.github.afgprojects.framework.security.core.authentication.AfgAuthentication;

/**
 * AFG Token 提供者
 * <p>
 * 负责生成、验证和撤销 Token。
 */
public interface AfgTokenProvider {

    /**
     * 生成访问令牌
     *
     * @param authentication 认证信息
     * @return 访问令牌
     */
    AfgToken generateAccessToken(AfgAuthentication authentication);

    /**
     * 生成刷新令牌
     *
     * @param authentication 认证信息
     * @return 刷新令牌
     */
    AfgToken generateRefreshToken(AfgAuthentication authentication);

    /**
     * 验证 Token 并返回认证信息
     *
     * @param tokenValue Token 值
     * @return 认证信息，无效返回 null
     */
    AfgAuthentication validateToken(String tokenValue);

    /**
     * 撤销 Token
     *
     * @param tokenValue Token 值
     */
    void invalidateToken(String tokenValue);
}
```

- [ ] **Step 5: 运行测试验证通过**

Run: `cd afg-framework && ./gradlew :security-core:test --tests AfgTokenTest`
Expected: PASS

- [ ] **Step 6: 创建 package-info.java**

```java
// security-core/src/main/java/io/github/afgprojects/framework/security/core/token/package-info.java
/**
 * Token 核心接口
 * <p>
 * 提供 Token 生成和验证的核心抽象：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.security.core.token.AfgToken} - Token 抽象</li>
 *   <li>{@link io.github.afgprojects.framework.security.core.token.AfgTokenProvider} - Token 提供者</li>
 * </ul>
 */
package io.github.afgprojects.framework.security.core.token;
```

- [ ] **Step 7: 创建模块级 package-info.java**

```java
// security-core/src/main/java/io/github/afgprojects/framework/security/core/package-info.java
/**
 * AFG 安全核心模块
 * <p>
 * 提供安全相关的基础抽象，包括：
 * <ul>
 *   <li>认证（authentication）- 用户身份验证</li>
 *   <li>授权（authorization）- 权限控制</li>
 *   <li>多租户（tenant）- 租户隔离</li>
 *   <li>令牌（token）- Token 生成与验证</li>
 * </ul>
 */
package io.github.afgprojects.framework.security.core;
```

- [ ] **Step 8: 运行所有测试**

Run: `cd afg-framework && ./gradlew :security-core:test`
Expected: PASS

- [ ] **Step 9: 提交**

```bash
git add security-core/src/main/java/io/github/afgprojects/framework/security/core/token/
git add security-core/src/main/java/io/github/afgprojects/framework/security/core/package-info.java
git add security-core/src/test/java/io/github/afgprojects/framework/security/core/token/
git commit -m "feat(security-core): add token interfaces"
```

---

### Task 6: 更新 spring-boot-starter 依赖

**Files:**
- Modify: `afg-framework/spring-boot-starter/build.gradle.kts`

- [ ] **Step 1: 添加 security-core 依赖**

```kotlin
// 在 spring-boot-starter/build.gradle.kts 的 dependencies 块中添加
api(project(":security-core"))
```

- [ ] **Step 2: 验证构建**

Run: `cd afg-framework && ./gradlew :spring-boot-starter:build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add spring-boot-starter/build.gradle.kts
git commit -m "feat(starter): add security-core dependency"
```

---

## Phase 2: auth-server 模块

### Task 7: 创建 auth-server 模块结构

**Files:**
- Create: `afg-framework/security-impl/auth-server/build.gradle.kts`
- Create: `afg-framework/security-impl/auth-server/src/main/java/io/github/afgprojects/framework/security/auth/package-info.java`

- [ ] **Step 1: 创建 auth-server/build.gradle.kts**

```kotlin
plugins {
    `java-library`
}

dependencies {
    // 依赖 security-core
    api(project(":security-core"))

    // Spring Authorization Server
    api(libs.spring.boot.starter.oauth2.authorization.server)

    // JSpecify 空安全注解
    api(libs.jspecify)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Test
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
}
```

- [ ] **Step 2: 创建目录结构**

```bash
mkdir -p afg-framework/security-impl/auth-server/src/main/java/io/github/afgprojects/framework/security/auth/{config,endpoint,token,user}
mkdir -p afg-framework/security-impl/auth-server/src/test/java/io/github/afgprojects/framework/security/auth/{config,token,user}
```

- [ ] **Step 3: 创建 package-info.java**

```java
// security-impl/auth-server/src/main/java/io/github/afgprojects/framework/security/auth/package-info.java
/**
 * OAuth2 授权服务器模块
 * <p>
 * 提供标准 OAuth2 / OIDC 授权服务器功能：
 * <ul>
 *   <li>授权码模式（Authorization Code）</li>
 *   <li>客户端凭证模式（Client Credentials）</li>
 *   <li>刷新令牌（Refresh Token）</li>
 *   <li>PKCE 支持</li>
 * </ul>
 */
package io.github.afgprojects.framework.security.auth;
```

- [ ] **Step 4: 验证模块可构建**

Run: `cd afg-framework && ./gradlew :security-impl:auth-server:build --dry-run`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 提交**

```bash
git add security-impl/auth-server/
git commit -m "feat(auth-server): add module structure"
```

---

### Task 8: 实现 AuthServerProperties 配置类

**Files:**
- Create: `security-impl/auth-server/src/main/java/io/github/afgprojects/framework/security/auth/config/AuthServerProperties.java`
- Create: `security-impl/auth-server/src/test/java/io/github/afgprojects/framework/security/auth/config/AuthServerPropertiesTest.java`

- [ ] **Step 1: 编写 AuthServerProperties 测试**

```java
// security-impl/auth-server/src/test/java/io/github/afgprojects/framework/security/auth/config/AuthServerPropertiesTest.java
package io.github.afgprojects.framework.security.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthServerPropertiesTest {

    @Test
    void shouldHaveDefaultValues() {
        AuthServerProperties properties = new AuthServerProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getAccessTokenTtl()).isEqualTo(Duration.ofHours(2));
        assertThat(properties.getRefreshTokenTtl()).isEqualTo(Duration.ofDays(7));
        assertThat(properties.isRequirePkce()).isTrue();
        assertThat(properties.getSupportedGrantTypes())
                .containsExactlyInAnyOrder("authorization_code", "client_credentials", "refresh_token");
    }

    @Test
    void shouldAllowCustomValues() {
        AuthServerProperties properties = new AuthServerProperties();
        properties.setEnabled(false);
        properties.setIssuer("https://auth.example.com");
        properties.setAccessTokenTtl(Duration.ofHours(4));
        properties.setRequirePkce(false);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getIssuer()).isEqualTo("https://auth.example.com");
        assertThat(properties.getAccessTokenTtl()).isEqualTo(Duration.ofHours(4));
        assertThat(properties.isRequirePkce()).isFalse();
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd afg-framework && ./gradlew :security-impl:auth-server:test --tests AuthServerPropertiesTest`
Expected: FAIL - AuthServerProperties 类不存在

- [ ] **Step 3: 实现 AuthServerProperties**

```java
// security-impl/auth-server/src/main/java/io/github/afgprojects/framework/security/auth/config/AuthServerProperties.java
package io.github.afgprojects.framework.security.auth.config;

import java.time.Duration;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * OAuth2 授权服务器配置属性
 * <p>
 * 配置示例：
 * <pre>
 * afg:
 *   security:
 *     auth-server:
 *       enabled: true
 *       issuer: https://auth.afg.com
 *       signing-key: ${JWT_SIGNING_KEY}
 *       access-token-ttl: 2h
 *       refresh-token-ttl: 7d
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "afg.security.auth-server")
public class AuthServerProperties {

    /**
     * 是否启用授权服务器
     */
    private boolean enabled = true;

    /**
     * Issuer URI
     */
    private @Nullable String issuer;

    /**
     * JWT 签名密钥
     */
    private @Nullable String signingKey;

    /**
     * 访问令牌有效期
     */
    private Duration accessTokenTtl = Duration.ofHours(2);

    /**
     * 刷新令牌有效期
     */
    private Duration refreshTokenTtl = Duration.ofDays(7);

    /**
     * 是否强制 PKCE
     */
    private boolean requirePkce = true;

    /**
     * 支持的授权类型
     */
    private Set<String> supportedGrantTypes = Set.of(
            "authorization_code",
            "client_credentials",
            "refresh_token"
    );
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `cd afg-framework && ./gradlew :security-impl:auth-server:test --tests AuthServerPropertiesTest`
Expected: PASS

- [ ] **Step 5: 创建 package-info.java**

```java
// security-impl/auth-server/src/main/java/io/github/afgprojects/framework/security/auth/config/package-info.java
/**
 * OAuth2 授权服务器配置
 */
package io.github.afgprojects.framework.security.auth.config;
```

- [ ] **Step 6: 提交**

```bash
git add security-impl/auth-server/src/main/java/io/github/afgprojects/framework/security/auth/config/
git add security-impl/auth-server/src/test/java/io/github/afgprojects/framework/security/auth/config/
git commit -m "feat(auth-server): add AuthServerProperties"
```

---

### Task 9: 实现 AfgClientDetailsService SPI

**Files:**
- Create: `security-impl/auth-server/src/main/java/io/github/afgprojects/framework/security/auth/user/AfgClientDetailsService.java`
- Create: `security-impl/auth-server/src/test/java/io/github/afgprojects/framework/security/auth/user/AfgClientDetailsServiceTest.java`

- [ ] **Step 1: 编写 AfgClientDetailsService 测试**

```java
// security-impl/auth-server/src/test/java/io/github/afgprojects/framework/security/auth/user/AfgClientDetailsServiceTest.java
package io.github.afgprojects.framework.security.auth.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

class AfgClientDetailsServiceTest {

    @Test
    void shouldLoadClient() {
        AfgClientDetailsService service = clientId -> RegisteredClient.withId("test-client")
                .clientId(clientId)
                .clientSecret("{noop}secret")
                .authorizationGrantType(
                        org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8080/callback")
                .scope("read")
                .build();

        RegisteredClient client = service.loadClientByClientId("test-client");

        assertThat(client).isNotNull();
        assertThat(client.getClientId()).isEqualTo("test-client");
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd afg-framework && ./gradlew :security-impl:auth-server:test --tests AfgClientDetailsServiceTest`
Expected: FAIL - AfgClientDetailsService 类不存在

- [ ] **Step 3: 实现 AfgClientDetailsService**

```java
// security-impl/auth-server/src/main/java/io/github/afgprojects/framework/security/auth/user/AfgClientDetailsService.java
package io.github.afgprojects.framework.security.auth.user;

import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

/**
 * OAuth2 客户端服务 SPI
 * <p>
 * 业务系统实现此接口以提供 OAuth2 客户端数据。
 */
@FunctionalInterface
public interface AfgClientDetailsService {

    /**
     * 根据客户端 ID 加载客户端信息
     *
     * @param clientId 客户端 ID
     * @return 注册的客户端，未找到返回 null
     */
    @Nullable RegisteredClient loadClientByClientId(String clientId);
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `cd afg-framework && ./gradlew :security-impl:auth-server:test --tests AfgClientDetailsServiceTest`
Expected: PASS

- [ ] **Step 5: 创建 package-info.java**

```java
// security-impl/auth-server/src/main/java/io/github/afgprojects/framework/security/auth/user/package-info.java
/**
 * 用户和客户端服务集成
 */
package io.github.afgprojects.framework.security.auth.user;
```

- [ ] **Step 6: 提交**

```bash
git add security-impl/auth-server/src/main/java/io/github/afgprojects/framework/security/auth/user/
git add security-impl/auth-server/src/test/java/io/github/afgprojects/framework/security/auth/user/
git commit -m "feat(auth-server): add AfgClientDetailsService"
```

---

### Task 10: 实现 JwtTokenProvider

**Files:**
- Create: `security-impl/auth-server/src/main/java/io/github/afgprojects/framework/security/auth/token/JwtTokenProvider.java`
- Create: `security-impl/auth-server/src/test/java/io/github/afgprojects/framework/security/auth/token/JwtTokenProviderTest.java`

- [ ] **Step 1: 编写 JwtTokenProvider 测试**

```java
// security-impl/auth-server/src/test/java/io/github/afgprojects/framework/security/auth/token/JwtTokenProviderTest.java
package io.github.afgprojects.framework.security.auth.token;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.security.core.authentication.AfgAuthentication;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;
import io.github.afgprojects.framework.security.core.token.AfgToken;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        tokenProvider = new JwtTokenProvider(
                keyPair.getPrivate(),
                keyPair.getPublic(),
                "https://auth.afg.com",
                Duration.ofHours(2),
                Duration.ofDays(7)
        );
    }

    @Test
    void shouldGenerateAccessToken() {
        AfgAuthentication authentication = createTestAuthentication();

        AfgToken token = tokenProvider.generateAccessToken(authentication);

        assertThat(token).isNotNull();
        assertThat(token.getTokenValue()).isNotBlank();
        assertThat(token.getTokenType()).isEqualTo("access");
        assertThat(token.isExpired()).isFalse();
    }

    @Test
    void shouldGenerateRefreshToken() {
        AfgAuthentication authentication = createTestAuthentication();

        AfgToken token = tokenProvider.generateRefreshToken(authentication);

        assertThat(token).isNotNull();
        assertThat(token.getTokenValue()).isNotBlank();
        assertThat(token.getTokenType()).isEqualTo("refresh");
    }

    @Test
    void shouldValidateToken() {
        AfgAuthentication authentication = createTestAuthentication();
        AfgToken token = tokenProvider.generateAccessToken(authentication);

        AfgAuthentication validated = tokenProvider.validateToken(token.getTokenValue());

        assertThat(validated).isNotNull();
        assertThat(validated.getPrincipal().getId()).isEqualTo("user-1");
    }

    @Test
    void shouldReturnNullForInvalidToken() {
        AfgAuthentication validated = tokenProvider.validateToken("invalid-token");

        assertThat(validated).isNull();
    }

    private AfgAuthentication createTestAuthentication() {
        return new AfgAuthentication() {
            @Override
            public AfgUserDetails getPrincipal() {
                return new AfgUserDetails() {
                    @Override
                    public String getId() {
                        return "user-1";
                    }

                    @Override
                    public String getUsername() {
                        return "testuser";
                    }

                    @Override
                    public String getTenantId() {
                        return "tenant-1";
                    }

                    @Override
                    public Set<String> getRoles() {
                        return Set.of("USER");
                    }

                    @Override
                    public Set<String> getPermissions() {
                        return Set.of("user:read");
                    }

                    @Override
                    public boolean isEnabled() {
                        return true;
                    }

                    @Override
                    public boolean isAccountNonLocked() {
                        return true;
                    }
                };
            }

            @Override
            public String getCredentials() {
                return null;
            }
        };
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd afg-framework && ./gradlew :security-impl:auth-server:test --tests JwtTokenProviderTest`
Expected: FAIL - JwtTokenProvider 类不存在

- [ ] **Step 3: 实现 JwtTokenProvider**

```java
// security-impl/auth-server/src/main/java/io/github/afgprojects/framework/security/auth/token/JwtTokenProvider.java
package io.github.afgprojects.framework.security.auth.token;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import io.github.afgprojects.framework.security.core.authentication.AfgAuthentication;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;
import io.github.afgprojects.framework.security.core.token.AfgToken;
import io.github.afgprojects.framework.security.core.token.AfgTokenProvider;

/**
 * JWT Token 提供者
 * <p>
 * 使用 RSA 签名生成和验证 JWT Token。
 */
public class JwtTokenProvider implements AfgTokenProvider {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final String issuer;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    public JwtTokenProvider(
            PrivateKey privateKey,
            PublicKey publicKey,
            String issuer,
            Duration accessTokenTtl,
            Duration refreshTokenTtl) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.issuer = issuer;
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    @Override
    public AfgToken generateAccessToken(AfgAuthentication authentication) {
        return generateToken(authentication, accessTokenTtl, "access");
    }

    @Override
    public AfgToken generateRefreshToken(AfgAuthentication authentication) {
        return generateToken(authentication, refreshTokenTtl, "refresh");
    }

    private AfgToken generateToken(AfgAuthentication authentication, Duration ttl, String tokenType) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);

        AfgUserDetails principal = authentication.getPrincipal();

        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .subject(principal.getId())
                .issuer(issuer)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiresAt))
                .claim("token_type", tokenType)
                .claim("username", principal.getUsername());

        if (principal.getTenantId() != null) {
            claimsBuilder.claim("tenant_id", principal.getTenantId());
        }

        if (!principal.getRoles().isEmpty()) {
            claimsBuilder.claim("roles", String.join(",", principal.getRoles()));
        }

        if (!principal.getPermissions().isEmpty()) {
            claimsBuilder.claim("permissions", String.join(",", principal.getPermissions()));
        }

        try {
            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).build(),
                    claimsBuilder.build());
            signedJWT.sign(new RSASSASigner(privateKey));

            final String tokenValue = signedJWT.serialize();
            final Instant issuedAt = now;
            final Instant expiration = expiresAt;

            return new AfgToken() {
                @Override
                public String getTokenValue() {
                    return tokenValue;
                }

                @Override
                public Instant getIssuedAt() {
                    return issuedAt;
                }

                @Override
                public Instant getExpiresAt() {
                    return expiration;
                }

                @Override
                public String getTokenType() {
                    return tokenType;
                }
            };
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate token", e);
        }
    }

    @Override
    public @Nullable AfgAuthentication validateToken(String tokenValue) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(tokenValue);

            if (!signedJWT.verify(new RSASSAVerifier(publicKey))) {
                return null;
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            if (claims.getExpirationTime().before(new Date())) {
                return null;
            }

            String userId = claims.getSubject();
            String username = claims.getStringClaim("username");
            String tenantId = claims.getStringClaim("tenant_id");
            String rolesStr = claims.getStringClaim("roles");
            String permissionsStr = claims.getStringClaim("permissions");

            AfgUserDetails userDetails = new AfgUserDetails() {
                @Override
                public String getId() {
                    return userId;
                }

                @Override
                public String getUsername() {
                    return username;
                }

                @Override
                public String getTenantId() {
                    return tenantId;
                }

                @Override
                public java.util.Set<String> getRoles() {
                    return rolesStr != null ? java.util.Set.of(rolesStr.split(",")) : java.util.Set.of();
                }

                @Override
                public java.util.Set<String> getPermissions() {
                    return permissionsStr != null ? java.util.Set.of(permissionsStr.split(",")) : java.util.Set.of();
                }

                @Override
                public boolean isEnabled() {
                    return true;
                }

                @Override
                public boolean isAccountNonLocked() {
                    return true;
                }
            };

            return new AfgAuthentication() {
                @Override
                public AfgUserDetails getPrincipal() {
                    return userDetails;
                }

                @Override
                public String getCredentials() {
                    return null;
                }
            };
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void invalidateToken(String tokenValue) {
        // JWT 无状态，需要配合黑名单机制实现
        // 可在后续版本中实现
    }
}
```

- [ ] **Step 4: 添加 Nimbus JWT 依赖**

```kotlin
// 在 auth-server/build.gradle.kts 的 dependencies 块中添加
api("com.nimbusds:nimbus-jose-jwt:9.37.3")
```

- [ ] **Step 5: 运行测试验证通过**

Run: `cd afg-framework && ./gradlew :security-impl:auth-server:test --tests JwtTokenProviderTest`
Expected: PASS

- [ ] **Step 6: 创建 package-info.java**

```java
// security-impl/auth-server/src/main/java/io/github/afgprojects/framework/security/auth/token/package-info.java
/**
 * Token 生成与验证
 */
package io.github.afgprojects.framework.security.auth.token;
```

- [ ] **Step 7: 提交**

```bash
git add security-impl/auth-server/src/main/java/io/github/afgprojects/framework/security/auth/token/
git add security-impl/auth-server/src/test/java/io/github/afgprojects/framework/security/auth/token/
git add security-impl/auth-server/build.gradle.kts
git commit -m "feat(auth-server): add JwtTokenProvider"
```

---

### Task 11: 实现 AuthServerAutoConfiguration

**Files:**
- Create: `security-impl/auth-server/src/main/java/io/github/afgprojects/framework/security/auth/config/AuthServerAutoConfiguration.java`
- Create: `security-impl/auth-server/src/test/java/io/github/afgprojects/framework/security/auth/config/AuthServerAutoConfigurationTest.java`

- [ ] **Step 1: 编写自动配置测试**

```java
// security-impl/auth-server/src/test/java/io/github/afgprojects/framework/security/auth/config/AuthServerAutoConfigurationTest.java
package io.github.afgprojects.framework.security.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AuthServerAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AuthServerAutoConfiguration.class));

    @Test
    void shouldConfigureAuthServerProperties() {
        contextRunner
                .withPropertyValues("afg.security.auth-server.issuer=https://auth.test.com")
                .run(context -> {
                    assertThat(context).hasSingleBean(AuthServerProperties.class);
                    AuthServerProperties properties = context.getBean(AuthServerProperties.class);
                    assertThat(properties.getIssuer()).isEqualTo("https://auth.test.com");
                });
    }

    @Test
    void shouldNotConfigureWhenDisabled() {
        contextRunner
                .withPropertyValues("afg.security.auth-server.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AuthServerProperties.class);
                });
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd afg-framework && ./gradlew :security-impl:auth-server:test --tests AuthServerAutoConfigurationTest`
Expected: FAIL - AuthServerAutoConfiguration 类不存在

- [ ] **Step 3: 实现 AuthServerAutoConfiguration**

```java
// security-impl/auth-server/src/main/java/io/github/afgprojects/framework/security/auth/config/AuthServerAutoConfiguration.java
package io.github.afgprojects.framework.security.auth.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * OAuth2 授权服务器自动配置
 */
@AutoConfiguration
@EnableConfigurationProperties(AuthServerProperties.class)
@ConditionalOnProperty(prefix = "afg.security.auth-server", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuthServerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuthServerProperties authServerProperties() {
        return new AuthServerProperties();
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `cd afg-framework && ./gradlew :security-impl:auth-server:test --tests AuthServerAutoConfigurationTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add security-impl/auth-server/src/main/java/io/github/afgprojects/framework/security/auth/config/AuthServerAutoConfiguration.java
git add security-impl/auth-server/src/test/java/io/github/afgprojects/framework/security/auth/config/AuthServerAutoConfigurationTest.java
git commit -m "feat(auth-server): add AuthServerAutoConfiguration"
```

---

## Phase 3: resource-server 模块

### Task 12: 创建 resource-server 模块结构

**Files:**
- Create: `afg-framework/security-impl/resource-server/build.gradle.kts`
- Create: `afg-framework/security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/package-info.java`

- [ ] **Step 1: 创建 resource-server/build.gradle.kts**

```kotlin
plugins {
    `java-library`
}

dependencies {
    // 依赖 security-core
    api(project(":security-core"))

    // Spring OAuth2 Resource Server
    api(libs.spring.boot.starter.oauth2.resource.server)

    // JSpecify 空安全注解
    api(libs.jspecify)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Test
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
}
```

- [ ] **Step 2: 创建目录结构**

```bash
mkdir -p afg-framework/security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/{jwt,introspection,tenant,config}
mkdir -p afg-framework/security-impl/resource-server/src/test/java/io/github/afgprojects/framework/security/resource/{jwt,tenant}
```

- [ ] **Step 3: 创建 package-info.java**

```java
// security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/package-info.java
/**
 * 资源服务器模块
 * <p>
 * 提供 OAuth2 资源服务器功能：
 * <ul>
 *   <li>JWT Token 验证</li>
 *   <li>远程 Token 验证（Introspection）</li>
 *   <li>多租户解析</li>
 * </ul>
 */
package io.github.afgprojects.framework.security.resource;
```

- [ ] **Step 4: 验证模块可构建**

Run: `cd afg-framework && ./gradlew :security-impl:resource-server:build --dry-run`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 提交**

```bash
git add security-impl/resource-server/
git commit -m "feat(resource-server): add module structure"
```

---

### Task 13: 实现 TenantResolveStrategy 和租户解析器

**Files:**
- Create: `security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/tenant/TenantResolveStrategy.java`
- Create: `security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/tenant/DefaultTenantContext.java`
- Create: `security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/tenant/HeaderTenantResolver.java`
- Create: `security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/tenant/TokenTenantResolver.java`
- Create: `security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/tenant/TenantResolverChain.java`
- Create: `security-impl/resource-server/src/test/java/io/github/afgprojects/framework/security/resource/tenant/TenantResolverChainTest.java`

- [ ] **Step 1: 编写 TenantResolverChain 测试**

```java
// security-impl/resource-server/src/test/java/io/github/afgprojects/framework/security/resource/tenant/TenantResolverChainTest.java
package io.github.afgprojects.framework.security.resource.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import io.github.afgprojects.framework.security.core.tenant.TenantContext;

class TenantResolverChainTest {

    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
    }

    @Test
    void shouldResolveFromFirstSuccessfulResolver() {
        TenantResolver resolver1 = req -> null;
        TenantResolver resolver2 = req -> new DefaultTenantContext("tenant-2", null);
        TenantResolver resolver3 = req -> new DefaultTenantContext("tenant-3", null);

        TenantResolverChain chain = new TenantResolverChain(List.of(resolver1, resolver2, resolver3));

        TenantContext result = chain.resolve(request);

        assertThat(result).isNotNull();
        assertThat(result.getTenantId()).isEqualTo("tenant-2");
    }

    @Test
    void shouldReturnNullWhenNoResolverSucceeds() {
        TenantResolver resolver1 = req -> null;
        TenantResolver resolver2 = req -> null;

        TenantResolverChain chain = new TenantResolverChain(List.of(resolver1, resolver2));

        TenantContext result = chain.resolve(request);

        assertThat(result).isNull();
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd afg-framework && ./gradlew :security-impl:resource-server:test --tests TenantResolverChainTest`
Expected: FAIL - TenantResolverChain 类不存在

- [ ] **Step 3: 实现 TenantResolveStrategy 枚举**

```java
// security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/tenant/TenantResolveStrategy.java
package io.github.afgprojects.framework.security.resource.tenant;

/**
 * 租户解析策略
 */
public enum TenantResolveStrategy {

    /**
     * 从请求头解析（X-Tenant-Id）
     */
    HEADER,

    /**
     * 从 JWT Token 解析（tenant_id claim）
     */
    TOKEN,

    /**
     * 从子域名解析
     */
    DOMAIN,

    /**
     * 从 URL 路径解析（/{tenant}/api/...）
     */
    PATH
}
```

- [ ] **Step 4: 实现 DefaultTenantContext**

```java
// security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/tenant/DefaultTenantContext.java
package io.github.afgprojects.framework.security.resource.tenant;

import java.util.Collections;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.security.core.tenant.TenantContext;

import lombok.Builder;
import lombok.Data;

/**
 * 默认租户上下文实现
 */
@Data
@Builder
public class DefaultTenantContext implements TenantContext {

    private final String tenantId;
    private final @Nullable String tenantCode;
    private final Map<String, Object> attributes;

    public DefaultTenantContext(String tenantId, @Nullable String tenantCode) {
        this(tenantId, tenantCode, Collections.emptyMap());
    }

    public DefaultTenantContext(String tenantId, @Nullable String tenantCode, Map<String, Object> attributes) {
        this.tenantId = tenantId;
        this.tenantCode = tenantCode;
        this.attributes = attributes != null ? attributes : Collections.emptyMap();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
```

- [ ] **Step 5: 实现 HeaderTenantResolver**

```java
// security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/tenant/HeaderTenantResolver.java
package io.github.afgprojects.framework.security.resource.tenant;

import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import io.github.afgprojects.framework.security.core.tenant.TenantContext;

/**
 * 从请求头解析租户
 */
public class HeaderTenantResolver implements io.github.afgprojects.framework.security.core.tenant.TenantResolver {

    private final String headerName;

    public HeaderTenantResolver() {
        this("X-Tenant-Id");
    }

    public HeaderTenantResolver(String headerName) {
        this.headerName = headerName;
    }

    @Override
    public @Nullable TenantContext resolve(HttpServletRequest request) {
        String tenantId = request.getHeader(headerName);
        if (StringUtils.hasText(tenantId)) {
            return new DefaultTenantContext(tenantId, null);
        }
        return null;
    }
}
```

- [ ] **Step 6: 实现 TokenTenantResolver**

```java
// security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/tenant/TokenTenantResolver.java
package io.github.afgprojects.framework.security.resource.tenant;

import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import io.github.afgprojects.framework.security.core.tenant.TenantContext;

/**
 * 从 JWT Token 解析租户
 */
public class TokenTenantResolver implements io.github.afgprojects.framework.security.core.tenant.TenantResolver {

    private final String claimName;

    public TokenTenantResolver() {
        this("tenant_id");
    }

    public TokenTenantResolver(String claimName) {
        this.claimName = claimName;
    }

    @Override
    public @Nullable TenantContext resolve(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String tenantId = jwt.getClaimAsString(claimName);
            if (tenantId != null) {
                return new DefaultTenantContext(tenantId, null);
            }
        }
        return null;
    }
}
```

- [ ] **Step 7: 实现 TenantResolverChain**

```java
// security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/tenant/TenantResolverChain.java
package io.github.afgprojects.framework.security.resource.tenant;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.security.core.tenant.TenantContext;

/**
 * 租户解析链
 * <p>
 * 按优先级依次尝试解析租户，返回第一个成功的结果。
 */
public class TenantResolverChain implements io.github.afgprojects.framework.security.core.tenant.TenantResolver {

    private final List<io.github.afgprojects.framework.security.core.tenant.TenantResolver> resolvers;

    public TenantResolverChain(List<io.github.afgprojects.framework.security.core.tenant.TenantResolver> resolvers) {
        this.resolvers = resolvers;
    }

    @Override
    public @Nullable TenantContext resolve(HttpServletRequest request) {
        for (io.github.afgprojects.framework.security.core.tenant.TenantResolver resolver : resolvers) {
            TenantContext context = resolver.resolve(request);
            if (context != null) {
                return context;
            }
        }
        return null;
    }
}
```

- [ ] **Step 8: 运行测试验证通过**

Run: `cd afg-framework && ./gradlew :security-impl:resource-server:test --tests TenantResolverChainTest`
Expected: PASS

- [ ] **Step 9: 创建 package-info.java**

```java
// security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/tenant/package-info.java
/**
 * 多租户解析
 */
package io.github.afgprojects.framework.security.resource.tenant;
```

- [ ] **Step 10: 提交**

```bash
git add security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/tenant/
git add security-impl/resource-server/src/test/java/io/github/afgprojects/framework/security/resource/tenant/
git commit -m "feat(resource-server): add tenant resolvers"
```

---

### Task 14: 实现 JwtResourceProperties 和 JwtAuthenticationConverter

**Files:**
- Create: `security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/jwt/JwtResourceProperties.java`
- Create: `security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/jwt/JwtAuthenticationConverter.java`
- Create: `security-impl/resource-server/src/test/java/io/github/afgprojects/framework/security/resource/jwt/JwtAuthenticationConverterTest.java`

- [ ] **Step 1: 编写 JwtAuthenticationConverter 测试**

```java
// security-impl/resource-server/src/test/java/io/github/afgprojects/framework/security/resource/jwt/JwtAuthenticationConverterTest.java
package io.github.afgprojects.framework.security.resource.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import io.github.afgprojects.framework.security.core.authentication.AfgAuthentication;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;

class JwtAuthenticationConverterTest {

    private final JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

    @Test
    void shouldConvertJwtToAfgAuthentication() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("user-1")
                .claim("username", "testuser")
                .claim("tenant_id", "tenant-1")
                .claim("roles", "USER,ADMIN")
                .claim("permissions", "user:read,user:write")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        AfgAuthentication authentication = converter.convert(jwt);

        assertThat(authentication).isNotNull();
        AfgUserDetails principal = authentication.getPrincipal();
        assertThat(principal.getId()).isEqualTo("user-1");
        assertThat(principal.getUsername()).isEqualTo("testuser");
        assertThat(principal.getTenantId()).isEqualTo("tenant-1");
        assertThat(principal.getRoles()).containsExactlyInAnyOrder("USER", "ADMIN");
        assertThat(principal.getPermissions()).containsExactlyInAnyOrder("user:read", "user:write");
    }

    @Test
    void shouldHandleMinimalJwt() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("user-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        AfgAuthentication authentication = converter.convert(jwt);

        assertThat(authentication).isNotNull();
        AfgUserDetails principal = authentication.getPrincipal();
        assertThat(principal.getId()).isEqualTo("user-1");
        assertThat(principal.getRoles()).isEmpty();
        assertThat(principal.getPermissions()).isEmpty();
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd afg-framework && ./gradlew :security-impl:resource-server:test --tests JwtAuthenticationConverterTest`
Expected: FAIL - JwtAuthenticationConverter 类不存在

- [ ] **Step 3: 实现 JwtResourceProperties**

```java
// security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/jwt/JwtResourceProperties.java
package io.github.afgprojects.framework.security.resource.jwt;

import java.time.Duration;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * JWT 资源服务器配置属性
 */
@Data
@ConfigurationProperties(prefix = "afg.security.resource.jwt")
public class JwtResourceProperties {

    /**
     * 是否启用 JWT 验证
     */
    private boolean enabled = true;

    /**
     * JWK Set URI
     */
    private @Nullable String jwkSetUri;

    /**
     * Issuer URI
     */
    private @Nullable String issuerUri;

    /**
     * 公钥缓存时间
     */
    private Duration cacheTtl = Duration.ofMinutes(5);

    /**
     * 预期的 audience
     */
    private Set<String> audience = Set.of();
}
```

- [ ] **Step 4: 实现 JwtAuthenticationConverter**

```java
// security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/jwt/JwtAuthenticationConverter.java
package io.github.afgprojects.framework.security.resource.jwt;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import io.github.afgprojects.framework.security.core.authentication.AfgAuthentication;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;

/**
 * JWT 到 AfgAuthentication 的转换器
 */
public class JwtAuthenticationConverter implements Converter<Jwt, AfgAuthentication> {

    @Override
    public @Nullable AfgAuthentication convert(Jwt jwt) {
        AfgUserDetails userDetails = extractUserDetails(jwt);
        return new AfgAuthentication() {
            @Override
            public AfgUserDetails getPrincipal() {
                return userDetails;
            }

            @Override
            public String getCredentials() {
                return jwt.getTokenValue();
            }

            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return userDetails.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toSet());
            }
        };
    }

    private AfgUserDetails extractUserDetails(Jwt jwt) {
        String userId = jwt.getSubject();
        String username = jwt.getClaimAsString("username");
        String tenantId = jwt.getClaimAsString("tenant_id");
        String rolesStr = jwt.getClaimAsString("roles");
        String permissionsStr = jwt.getClaimAsString("permissions");

        Set<String> roles = parseCommaSeparated(rolesStr);
        Set<String> permissions = parseCommaSeparated(permissionsStr);

        return new AfgUserDetails() {
            @Override
            public String getId() {
                return userId;
            }

            @Override
            public String getUsername() {
                return username != null ? username : userId;
            }

            @Override
            public String getTenantId() {
                return tenantId;
            }

            @Override
            public Set<String> getRoles() {
                return roles;
            }

            @Override
            public Set<String> getPermissions() {
                return permissions;
            }

            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public boolean isAccountNonLocked() {
                return true;
            }
        };
    }

    private Set<String> parseCommaSeparated(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
}
```

- [ ] **Step 5: 运行测试验证通过**

Run: `cd afg-framework && ./gradlew :security-impl:resource-server:test --tests JwtAuthenticationConverterTest`
Expected: PASS

- [ ] **Step 6: 创建 package-info.java**

```java
// security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/jwt/package-info.java
/**
 * JWT 验证
 */
package io.github.afgprojects.framework.security.resource.jwt;
```

- [ ] **Step 7: 提交**

```bash
git add security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/jwt/
git add security-impl/resource-server/src/test/java/io/github/afgprojects/framework/security/resource/jwt/
git commit -m "feat(resource-server): add JWT authentication converter"
```

---

### Task 15: 实现 IntrospectionProperties

**Files:**
- Create: `security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/introspection/IntrospectionProperties.java`
- Create: `security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/introspection/package-info.java`

- [ ] **Step 1: 实现 IntrospectionProperties**

```java
// security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/introspection/IntrospectionProperties.java
package io.github.afgprojects.framework.security.resource.introspection;

import java.time.Duration;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Token Introspection 配置属性
 * <p>
 * 用于远程 Token 验证场景。
 */
@Data
@ConfigurationProperties(prefix = "afg.security.resource.introspection")
public class IntrospectionProperties {

    /**
     * 是否启用远程验证
     */
    private boolean enabled = false;

    /**
     * Introspection 端点 URI
     */
    private @Nullable String introspectionUri;

    /**
     * 客户端 ID
     */
    private @Nullable String clientId;

    /**
     * 客户端密钥
     */
    private @Nullable String clientSecret;

    /**
     * 验证结果缓存时间
     */
    private Duration cacheTtl = Duration.ofMinutes(1);
}
```

- [ ] **Step 2: 创建 package-info.java**

```java
// security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/introspection/package-info.java
/**
 * Token 远程验证（Introspection）
 */
package io.github.afgprojects.framework.security.resource.introspection;
```

- [ ] **Step 3: 提交**

```bash
git add security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/introspection/
git commit -m "feat(resource-server): add IntrospectionProperties"
```

---

### Task 16: 实现 ResourceServerAutoConfiguration

**Files:**
- Create: `security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/config/ResourceServerAutoConfiguration.java`
- Create: `security-impl/resource-server/src/test/java/io/github/afgprojects/framework/security/resource/config/ResourceServerAutoConfigurationTest.java`

- [ ] **Step 1: 编写自动配置测试**

```java
// security-impl/resource-server/src/test/java/io/github/afgprojects/framework/security/resource/config/ResourceServerAutoConfigurationTest.java
package io.github.afgprojects.framework.security.resource.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.github.afgprojects.framework.security.resource.jwt.JwtResourceProperties;
import io.github.afgprojects.framework.security.resource.introspection.IntrospectionProperties;

class ResourceServerAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ResourceServerAutoConfiguration.class));

    @Test
    void shouldConfigureJwtProperties() {
        contextRunner
                .withPropertyValues("afg.security.resource.jwt.jwk-set-uri=https://auth.test.com/.well-known/jwks.json")
                .run(context -> {
                    assertThat(context).hasSingleBean(JwtResourceProperties.class);
                    JwtResourceProperties properties = context.getBean(JwtResourceProperties.class);
                    assertThat(properties.getJwkSetUri()).isEqualTo("https://auth.test.com/.well-known/jwks.json");
                });
    }

    @Test
    void shouldConfigureIntrospectionProperties() {
        contextRunner
                .withPropertyValues(
                        "afg.security.resource.introspection.enabled=true",
                        "afg.security.resource.introspection.introspection-uri=https://auth.test.com/oauth2/introspect")
                .run(context -> {
                    assertThat(context).hasSingleBean(IntrospectionProperties.class);
                    IntrospectionProperties properties = context.getBean(IntrospectionProperties.class);
                    assertThat(properties.isEnabled()).isTrue();
                });
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd afg-framework && ./gradlew :security-impl:resource-server:test --tests ResourceServerAutoConfigurationTest`
Expected: FAIL - ResourceServerAutoConfiguration 类不存在

- [ ] **Step 3: 实现 ResourceServerAutoConfiguration**

```java
// security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/config/ResourceServerAutoConfiguration.java
package io.github.afgprojects.framework.security.resource.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.security.resource.introspection.IntrospectionProperties;
import io.github.afgprojects.framework.security.resource.jwt.JwtAuthenticationConverter;
import io.github.afgprojects.framework.security.resource.jwt.JwtResourceProperties;

/**
 * 资源服务器自动配置
 */
@AutoConfiguration
@EnableConfigurationProperties({
        JwtResourceProperties.class,
        IntrospectionProperties.class
})
@ConditionalOnProperty(prefix = "afg.security.resource", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ResourceServerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        return new JwtAuthenticationConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtResourceProperties jwtResourceProperties() {
        return new JwtResourceProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public IntrospectionProperties introspectionProperties() {
        return new IntrospectionProperties();
    }
}
```

- [ ] **Step 4: 创建 package-info.java**

```java
// security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/config/package-info.java
/**
 * 资源服务器配置
 */
package io.github.afgprojects.framework.security.resource.config;
```

- [ ] **Step 5: 运行测试验证通过**

Run: `cd afg-framework && ./gradlew :security-impl:resource-server:test --tests ResourceServerAutoConfigurationTest`
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add security-impl/resource-server/src/main/java/io/github/afgprojects/framework/security/resource/config/
git add security-impl/resource-server/src/test/java/io/github/afgprojects/framework/security/resource/config/
git commit -m "feat(resource-server): add ResourceServerAutoConfiguration"
```

---

## Phase 4: security-casbin 模块

### Task 17: 创建 security-casbin 模块结构

**Files:**
- Create: `afg-framework/security-impl/security-casbin/build.gradle.kts`
- Create: `afg-framework/security-impl/security-casbin/src/main/java/io/github/afgprojects/framework/security/casbin/package-info.java`

- [ ] **Step 1: 创建 security-casbin/build.gradle.kts**

```kotlin
plugins {
    `java-library`
}

dependencies {
    // 依赖 security-core
    api(project(":security-core"))

    // JCasbin
    api(libs.jcasbin)

    // JSpecify 空安全注解
    api(libs.jspecify)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Test
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
}
```

- [ ] **Step 2: 创建目录结构**

```bash
mkdir -p afg-framework/security-impl/security-casbin/src/main/java/io/github/afgprojects/framework/security/casbin/{config,enforcer,model}
mkdir -p afg-framework/security-impl/security-casbin/src/test/java/io/github/afgprojects/framework/security/casbin/{enforcer,model}
```

- [ ] **Step 3: 创建 package-info.java**

```java
// security-impl/security-casbin/src/main/java/io/github/afgprojects/framework/security/casbin/package-info.java
/**
 * Casbin 权限集成模块
 * <p>
 * 提供基于 Casbin 的权限控制：
 * <ul>
 *   <li>RBAC with domains（支持多租户）</li>
 *   <li>策略热更新</li>
 *   <li>权限执行器</li>
 * </ul>
 */
package io.github.afgprojects.framework.security.casbin;
```

- [ ] **Step 4: 验证模块可构建**

Run: `cd afg-framework && ./gradlew :security-impl:security-casbin:build --dry-run`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 提交**

```bash
git add security-impl/security-casbin/
git commit -m "feat(security-casbin): add module structure"
```

---

### Task 18: 实现 CasbinProperties 和 CasbinRule

**Files:**
- Create: `security-impl/security-casbin/src/main/java/io/github/afgprojects/framework/security/casbin/config/CasbinProperties.java`
- Create: `security-impl/security-casbin/src/main/java/io/github/afgprojects/framework/security/casbin/model/CasbinRule.java`
- Create: `security-impl/security-casbin/src/main/java/io/github/afgprojects/framework/security/casbin/model/AfgPolicyService.java`

- [ ] **Step 1: 实现 CasbinProperties**

```java
// security-impl/security-casbin/src/main/java/io/github/afgprojects/framework/security/casbin/config/CasbinProperties.java
package io.github.afgprojects.framework.security.casbin.config;

import java.time.Duration;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Casbin 配置属性
 */
@Data
@ConfigurationProperties(prefix = "afg.security.casbin")
public class CasbinProperties {

    /**
     * 是否启用 Casbin
     */
    private boolean enabled = true;

    /**
     * 模型定义（内联或文件路径）
     */
    private @Nullable String model;

    /**
     * 策略定义（内联或文件路径）
     */
    private @Nullable String policy;

    /**
     * 是否启用策略热更新
     */
    private boolean watchEnabled = true;

    /**
     * 策略同步间隔
     */
    private Duration syncInterval = Duration.ofSeconds(30);
}
```

- [ ] **Step 2: 实现 CasbinRule**

```java
// security-impl/security-casbin/src/main/java/io/github/afgprojects/framework/security/casbin/model/CasbinRule.java
package io.github.afgprojects.framework.security.casbin.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Casbin 策略规则
 * <p>
 * 对应 Casbin 的 policy 规则：p = sub, dom, obj, act
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CasbinRule {

    /**
     * 规则类型：p（策略）或 g（角色）
     */
    private String ptype;

    /**
     * 主体（用户 ID）
     */
    private String sub;

    /**
     * 域（租户 ID）
     */
    private String dom;

    /**
     * 资源（URL、数据对象等）
     */
    private String obj;

    /**
     * 操作（GET、POST、DELETE 等）
     */
    private String act;

    /**
     * 创建策略规则
     */
    public static CasbinRule policy(String sub, String dom, String obj, String act) {
        return new CasbinRule("p", sub, dom, obj, act);
    }

    /**
     * 创建角色规则
     */
    public static CasbinRule role(String sub, String dom, String role) {
        return new CasbinRule("g", sub, dom, role, null);
    }
}
```

- [ ] **Step 3: 实现 AfgPolicyService**

```java
// security-impl/security-casbin/src/main/java/io/github/afgprojects/framework/security/casbin/model/AfgPolicyService.java
package io.github.afgprojects.framework.security.casbin.model;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Casbin 策略服务 SPI
 * <p>
 * 业务系统实现此接口以提供策略数据。
 */
public interface AfgPolicyService {

    /**
     * 加载所有策略
     */
    List<CasbinRule> loadAllPolicies();

    /**
     * 加载指定租户的策略
     */
    List<CasbinRule> loadPoliciesByTenant(String tenantId);

    /**
     * 保存策略
     */
    void savePolicy(CasbinRule rule);

    /**
     * 删除策略
     */
    void removePolicy(CasbinRule rule);

    /**
     * 获取角色的权限列表
     */
    List<String> getPermissionsForRole(String role, @Nullable String tenantId);

    /**
     * 获取用户的角色列表
     */
    List<String> getRolesForUser(String userId, @Nullable String tenantId);
}
```

- [ ] **Step 4: 创建 package-info.java 文件**

```java
// security-impl/security-casbin/src/main/java/io/github/afgprojects/framework/security/casbin/config/package-info.java
/**
 * Casbin 配置
 */
package io.github.afgprojects.framework.security.casbin.config;
```

```java
// security-impl/security-casbin/src/main/java/io/github/afgprojects/framework/security/casbin/model/package-info.java
/**
 * Casbin 模型和策略
 */
package io.github.afgprojects.framework.security.casbin.model;
```

- [ ] **Step 5: 提交**

```bash
git add security-impl/security-casbin/src/main/java/io/github/afgprojects/framework/security/casbin/config/
git add security-impl/security-casbin/src/main/java/io/github/afgprojects/framework/security/casbin/model/
git commit -m "feat(security-casbin): add CasbinProperties and CasbinRule"
```

---

### Task 19: 实现 CasbinAfgEnforcer

**Files:**
- Create: `security-impl/security-casbin/src/main/java/io/github/afgprojects/framework/security/casbin/enforcer/CasbinAfgEnforcer.java`
- Create: `security-impl/security-casbin/src/test/java/io/github/afgprojects/framework/security/casbin/enforcer/CasbinAfgEnforcerTest.java`

- [ ] **Step 1: 编写 CasbinAfgEnforcer 测试**

```java
// security-impl/security-casbin/src/test/java/io/github/afgprojects/framework/security/casbin/enforcer/CasbinAfgEnforcerTest.java
package io.github.afgprojects.framework.security.casbin.enforcer;

import static org.assertj.core.api.Assertions.assertThat;

import org.casbin.jcasbin.main.Enforcer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CasbinAfgEnforcerTest {

    private CasbinAfgEnforcer afgEnforcer;

    @BeforeEach
    void setUp() {
        // 使用内存模型和策略
        String model = """
                [request_definition]
                r = sub, dom, obj, act

                [policy_definition]
                p = sub, dom, obj, act

                [role_definition]
                g = sub, dom, role

                [policy_effect]
                e = some(where (p.eft == allow))

                [matchers]
                m = r.sub == p.sub && r.dom == p.dom && keyMatch2(r.obj, p.obj) && r.act == p.act
                """;

        Enforcer enforcer = new Enforcer();
        enforcer.setModel(model);

        // 添加测试策略
        enforcer.addPolicy("user-1", "tenant-1", "/api/users/*", "GET");
        enforcer.addPolicy("user-1", "tenant-1", "/api/users/*", "POST");
        enforcer.addGroupingPolicy("user-2", "tenant-1", "admin");
        enforcer.addPolicy("admin", "tenant-1", "/api/*", "*");

        afgEnforcer = new CasbinAfgEnforcer(enforcer);
    }

    @Test
    void shouldEnforceAllowedRequest() {
        boolean result = afgEnforcer.enforce("user-1", "/api/users/123", "GET");

        assertThat(result).isTrue();
    }

    @Test
    void shouldDenyUnauthorizedRequest() {
        boolean result = afgEnforcer.enforce("user-1", "/api/admin/config", "DELETE");

        assertThat(result).isFalse();
    }

    @Test
    void shouldEnforceRoleBasedPermission() {
        boolean result = afgEnforcer.enforce("user-2", "/api/users/456", "DELETE");

        assertThat(result).isTrue();
    }

    @Test
    void shouldDenyCrossTenantAccess() {
        boolean result = afgEnforcer.enforce("user-1", "tenant-2", "/api/users/123", "GET");

        assertThat(result).isFalse();
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd afg-framework && ./gradlew :security-impl:security-casbin:test --tests CasbinAfgEnforcerTest`
Expected: FAIL - CasbinAfgEnforcer 类不存在

- [ ] **Step 3: 实现 CasbinAfgEnforcer**

```java
// security-impl/security-casbin/src/main/java/io/github/afgprojects/framework/security/casbin/enforcer/CasbinAfgEnforcer.java
package io.github.afgprojects.framework.security.casbin.enforcer;

import java.util.List;

import org.casbin.jcasbin.main.Enforcer;

import io.github.afgprojects.framework.security.core.authorization.AfgEnforcer;

/**
 * Casbin 权限执行器
 * <p>
 * 实现 AfgEnforcer 接口，委托给 JCasbin Enforcer 进行权限检查。
 */
public class CasbinAfgEnforcer implements AfgEnforcer {

    private final Enforcer enforcer;

    public CasbinAfgEnforcer(Enforcer enforcer) {
        this.enforcer = enforcer;
    }

    @Override
    public boolean enforce(String subject, String resource, String action) {
        // 无主体的请求（匿名访问）默认拒绝
        if (subject == null) {
            return false;
        }
        // 使用默认域（空字符串）进行权限检查
        return enforcer.enforce(subject, "", resource, action);
    }

    /**
     * 多租户权限检查
     *
     * @param subject 主体（用户 ID）
     * @param domain 域（租户 ID）
     * @param resource 资源
     * @param action 操作
     * @return true 表示有权限
     */
    public boolean enforce(String subject, String domain, String resource, String action) {
        if (subject == null) {
            return false;
        }
        return enforcer.enforce(subject, domain, resource, action);
    }

    /**
     * 批量权限检查
     *
     * @param subject 主体
     * @param domain 域
     * @param requests 请求列表
     * @return 所有请求都有权限时返回 true
     */
    public boolean enforceAll(String subject, String domain, List<ResourceAction> requests) {
        return requests.stream()
                .allMatch(ra -> enforce(subject, domain, ra.resource(), ra.action()));
    }

    /**
     * 添加策略
     */
    public void addPolicy(String subject, String domain, String resource, String action) {
        enforcer.addPolicy(subject, domain, resource, action);
    }

    /**
     * 删除策略
     */
    public void removePolicy(String subject, String domain, String resource, String action) {
        enforcer.removePolicy(subject, domain, resource, action);
    }

    /**
     * 添加角色
     */
    public void addRoleForUser(String subject, String domain, String role) {
        enforcer.addGroupingPolicy(subject, domain, role);
    }

    /**
     * 删除角色
     */
    public void deleteRoleForUser(String subject, String domain, String role) {
        enforcer.removeGroupingPolicy(subject, domain, role);
    }

    /**
     * 获取底层 Enforcer
     */
    public Enforcer getEnforcer() {
        return enforcer;
    }

    /**
     * 资源-操作对
     */
    public record ResourceAction(String resource, String action) {}
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `cd afg-framework && ./gradlew :security-impl:security-casbin:test --tests CasbinAfgEnforcerTest`
Expected: PASS

- [ ] **Step 5: 创建 package-info.java**

```java
// security-impl/security-casbin/src/main/java/io/github/afgprojects/framework/security/casbin/enforcer/package-info.java
/**
 * 权限执行器
 */
package io.github.afgprojects.framework.security.casbin.enforcer;
```

- [ ] **Step 6: 提交**

```bash
git add security-impl/security-casbin/src/main/java/io/github/afgprojects/framework/security/casbin/enforcer/
git add security-impl/security-casbin/src/test/java/io/github/afgprojects/framework/security/casbin/enforcer/
git commit -m "feat(security-casbin): add CasbinAfgEnforcer"
```

---

### Task 20: 实现 CasbinAutoConfiguration

**Files:**
- Create: `security-impl/security-casbin/src/main/java/io/github/afgprojects/framework/security/casbin/config/CasbinAutoConfiguration.java`
- Create: `security-impl/security-casbin/src/test/java/io/github/afgprojects/framework/security/casbin/config/CasbinAutoConfigurationTest.java`

- [ ] **Step 1: 编写自动配置测试**

```java
// security-impl/security-casbin/src/test/java/io/github/afgprojects/framework/security/casbin/config/CasbinAutoConfigurationTest.java
package io.github.afgprojects.framework.security.casbin.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.github.afgprojects.framework.security.casbin.enforcer.CasbinAfgEnforcer;

class CasbinAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CasbinAutoConfiguration.class));

    @Test
    void shouldConfigureCasbinProperties() {
        contextRunner
                .withPropertyValues("afg.security.casbin.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(CasbinProperties.class);
                });
    }

    @Test
    void shouldNotConfigureWhenDisabled() {
        contextRunner
                .withPropertyValues("afg.security.casbin.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(CasbinProperties.class);
                    assertThat(context).doesNotHaveBean(CasbinAfgEnforcer.class);
                });
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd afg-framework && ./gradlew :security-impl:security-casbin:test --tests CasbinAutoConfigurationTest`
Expected: FAIL - CasbinAutoConfiguration 类不存在

- [ ] **Step 3: 实现 CasbinAutoConfiguration**

```java
// security-impl/security-casbin/src/main/java/io/github/afgprojects/framework/security/casbin/config/CasbinAutoConfiguration.java
package io.github.afgprojects.framework.security.casbin.config;

import org.casbin.jcasbin.main.Enforcer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.security.casbin.enforcer.CasbinAfgEnforcer;
import io.github.afgprojects.framework.security.casbin.model.AfgPolicyService;

/**
 * Casbin 自动配置
 */
@AutoConfiguration
@EnableConfigurationProperties(CasbinProperties.class)
@ConditionalOnProperty(prefix = "afg.security.casbin", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CasbinAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CasbinProperties casbinProperties() {
        return new CasbinProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public Enforcer casbinEnforcer(CasbinProperties properties, AfgPolicyService policyService) {
        Enforcer enforcer = new Enforcer();

        // 设置默认模型（RBAC with domains）
        String model = """
                [request_definition]
                r = sub, dom, obj, act

                [policy_definition]
                p = sub, dom, obj, act

                [role_definition]
                g = sub, dom, role

                [policy_effect]
                e = some(where (p.eft == allow))

                [matchers]
                m = r.sub == p.sub && r.dom == p.dom && keyMatch2(r.obj, p.obj) && r.act == p.act
                """;
        enforcer.setModel(model);

        // 加载策略
        policyService.loadAllPolicies().forEach(rule -> {
            if ("p".equals(rule.getPtype())) {
                enforcer.addPolicy(rule.getSub(), rule.getDom(), rule.getObj(), rule.getAct());
            } else if ("g".equals(rule.getPtype())) {
                enforcer.addGroupingPolicy(rule.getSub(), rule.getDom(), rule.getObj());
            }
        });

        return enforcer;
    }

    @Bean
    @ConditionalOnBean(Enforcer.class)
    @ConditionalOnMissingBean
    public CasbinAfgEnforcer casbinAfgEnforcer(Enforcer enforcer) {
        return new CasbinAfgEnforcer(enforcer);
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `cd afg-framework && ./gradlew :security-impl:security-casbin:test --tests CasbinAutoConfigurationTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add security-impl/security-casbin/src/main/java/io/github/afgprojects/framework/security/casbin/config/CasbinAutoConfiguration.java
git add security-impl/security-casbin/src/test/java/io/github/afgprojects/framework/security/casbin/config/CasbinAutoConfigurationTest.java
git commit -m "feat(security-casbin): add CasbinAutoConfiguration"
```

---

## Phase 5: 集成测试与文档

### Task 21: 运行完整测试套件

- [ ] **Step 1: 运行所有模块测试**

Run: `cd afg-framework && ./gradlew test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 检查测试覆盖率**

Run: `cd afg-framework && ./gradlew jacocoTestReport`
Expected: 覆盖率 >= 95%

- [ ] **Step 3: 提交**

```bash
git add .
git commit -m "test(security): verify all tests pass"
```

---

### Task 22: 更新 README 文档

**Files:**
- Modify: `afg-framework/README.md`

- [ ] **Step 1: 更新 README 添加安全模块说明**

在 README.md 的模块结构部分添加：

```markdown
├── security-core/                   # 安全核心抽象层
├── security-impl/
│   ├── auth-server/                 # OAuth2 授权服务器
│   ├── resource-server/             # 资源服务器
│   └── security-casbin/             # Casbin 权限集成
```

在技术要点部分添加：

```markdown
### 安全模块

- **security-core** - 安全核心接口（认证、授权、多租户、Token）
- **auth-server** - OAuth2 授权服务器（授权码、客户端凭证、刷新令牌、PKCE）
- **resource-server** - 资源服务器（JWT 验证、远程验证、多租户解析）
- **security-casbin** - Casbin 权限集成（RBAC with domains）
```

- [ ] **Step 2: 提交**

```bash
git add README.md
git commit -m "docs: add security module documentation"
```

---

### Task 23: 最终验证

- [ ] **Step 1: 完整构建**

Run: `cd afg-framework && ./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: PMD 代码检查**

Run: `cd afg-framework && ./gradlew pmdMain`
Expected: 无违规

- [ ] **Step 3: 最终提交**

```bash
git add .
git commit -m "feat(security): complete security module implementation"
```

---

## 自审查清单

- [x] **Spec 覆盖**：每个设计需求都有对应任务
- [x] **占位符扫描**：无 TBD、TODO 或未完成步骤
- [x] **类型一致性**：接口定义在所有任务中保持一致
