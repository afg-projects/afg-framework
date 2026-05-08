# 安全模块设计文档

## 概述

重新定义 afg-framework 的安全模块，采用与 data 模块相同的分层架构：
- `security-core` - 安全核心抽象层（接口、基础类型）
- `security-impl/` - 实现目录，包含认证服务器、资源服务器、Casbin 权限集成

## 需求总结

| 需求项 | 决策 |
|--------|------|
| 认证方式 | OAuth2 / OIDC（标准协议） |
| 授权类型 | 授权码、客户端凭证、刷新令牌、PKCE |
| 用户存储 | 可配置（业务系统实现 SPI） |
| Token 验证 | JWT + 远程验证（introspection）+ 黑名单机制 |
| 多租户 | 核心需求，支持多种解析策略 |
| 权限控制 | Casbin RBAC with domains |
| 部署模式 | 微服务独立部署 |

## 模块结构

```
afg-framework/
├── security-core/                        # 安全核心抽象层
│   └── src/main/java/.../security/core/
│       ├── authentication/               # 认证核心接口
│       │   ├── AfgUserDetails.java
│       │   ├── AfgAuthentication.java
│       │   └── AfgUserDetailsService.java
│       ├── authorization/                # 授权核心接口
│       │   └── AfgEnforcer.java
│       ├── tenant/                       # 多租户抽象
│       │   ├── TenantContext.java
│       │   ├── TenantResolver.java
│       │   └── TenantAware.java
│       └── token/                        # Token 抽象
│           ├── AfgToken.java
│           └── AfgTokenProvider.java
│
├── security-impl/
│   ├── auth-server/                      # OAuth2 授权服务器
│   │   └── src/main/java/.../security/auth/
│   │       ├── config/                   # OAuth2 配置
│   │       ├── endpoint/                 # 授权端点
│   │       ├── token/                    # Token 生成
│   │       └── user/                     # 用户服务集成
│   │
│   ├── resource-server/                  # 资源服务器
│   │   └── src/main/java/.../security/resource/
│   │       ├── jwt/                      # JWT 验证
│   │       ├── introspection/            # 远程验证
│   │       └── tenant/                   # 租户解析
│   │
│   └── security-casbin/                  # Casbin 权限集成
│       └── src/main/java/.../security/casbin/
│           ├── enforcer/                 # 权限执行器
│           └── model/                    # 模型配置
```

## security-core 核心接口

### 认证相关

```java
// 认证主体
public interface AfgUserDetails {
    String getId();
    String getUsername();
    String getTenantId();
    Set<String> getRoles();
    Set<String> getPermissions();
    boolean isEnabled();
    boolean isAccountNonLocked();
}

// 认证令牌
public interface AfgAuthentication {
    AfgUserDetails getPrincipal();
    String getCredentials();
    Collection<? extends GrantedAuthority> getAuthorities();
    Map<String, Object> getDetails();
}

// 用户服务 SPI（供业务系统实现）
public interface AfgUserDetailsService {
    AfgUserDetails loadUserByUsername(String username, String tenantId);
    AfgUserDetails loadUserById(String userId, String tenantId);
    void validateCredentials(AfgUserDetails user, String password);
}
```

### 多租户相关

```java
// 租户上下文
public interface TenantContext {
    String getTenantId();
    String getTenantCode();
    Map<String, Object> getAttributes();
}

// 租户解析器
public interface TenantResolver {
    TenantContext resolve(HttpServletRequest request);
    TenantContext resolveFromToken(String token);
}

// 租户感知接口标记
public interface TenantAware {
    String getTenantId();
}
```

### Token 相关

```java
// Token 抽象
public interface AfgToken {
    String getTokenValue();
    Instant getIssuedAt();
    Instant getExpiresAt();
    String getTokenType();  // access, refresh
}

// Token 提供者
public interface AfgTokenProvider {
    AfgToken generateAccessToken(AfgAuthentication authentication);
    AfgToken generateRefreshToken(AfgAuthentication authentication);
    AfgAuthentication validateToken(String tokenValue);
    void invalidateToken(String tokenValue);
}
```

## auth-server 模块

### OAuth2 配置

```java
@ConfigurationProperties(prefix = "afg.security.auth-server")
public class AuthServerProperties {
    private boolean enabled = true;
    private String issuer;                          // 如 https://auth.afg.com
    private String signingKey;                      // JWT 签名密钥
    private Duration accessTokenTtl = Duration.ofHours(2);
    private Duration refreshTokenTtl = Duration.ofDays(7);
    private boolean requirePkce = true;             // 强制 PKCE
    private Set<String> supportedGrantTypes = Set.of(
        "authorization_code", "client_credentials", "refresh_token"
    );
}
```

### 核心端点

| 端点 | 路径 | 说明 |
|------|------|------|
| 授权端点 | `/oauth2/authorize` | 授权码流程入口 |
| Token 端点 | `/oauth2/token` | 获取 Access Token |
| JWK Set | `/.well-known/jwks.json` | 公钥端点 |
| UserInfo | `/userinfo` | 用户信息端点 |
| Introspection | `/oauth2/introspect` | Token 验证（资源服务器调用） |
| 撤销端点 | `/oauth2/revoke` | 撤销 Token |

### 用户服务集成 SPI

```java
// 业务系统实现此接口，提供用户数据
public interface AfgUserDetailsService {
    AfgUserDetails loadUserByUsername(String username, String tenantId);
    AfgUserDetails loadUserById(String userId, String tenantId);
    void validateCredentials(AfgUserDetails user, String password);
    void onLoginSuccess(AfgUserDetails user, HttpServletRequest request);
    void onLoginFailure(AfgUserDetails user, String reason);
}

// 业务系统实现此接口，提供客户端数据
public interface AfgClientDetailsService {
    RegisteredClient loadClientByClientId(String clientId);
}
```

## resource-server 模块

### JWT 验证

```java
@ConfigurationProperties(prefix = "afg.security.resource.jwt")
public class JwtResourceProperties {
    private boolean enabled = true;
    private String jwkSetUri;                      // JWK 端点
    private String issuerUri;                       // Issuer URI
    private Duration cacheTtl = Duration.ofMinutes(5);
    private Set<String> audience = Set.of();       // 预期 audience
}
```

### 远程验证

```java
@ConfigurationProperties(prefix = "afg.security.resource.introspection")
public class IntrospectionProperties {
    private boolean enabled = false;
    private String introspectionUri;
    private String clientId;
    private String clientSecret;
    private Duration cacheTtl = Duration.ofMinutes(1);
}
```

### 多租户解析

```java
// 租户解析策略（按优先级）
public enum TenantResolveStrategy {
    HEADER,           // X-Tenant-Id 请求头
    TOKEN,            // JWT 中的 tenant_id claim
    DOMAIN,           // 子域名解析
    PATH              // URL 路径 /{tenant}/api/...
}

// 租户解析链
public class TenantResolverChain implements TenantResolver {
    private final List<TenantResolver> resolvers;  // 按优先级排序

    public TenantContext resolve(HttpServletRequest request) {
        for (TenantResolver resolver : resolvers) {
            TenantContext ctx = resolver.resolve(request);
            if (ctx != null) return ctx;
        }
        throw new TenantNotFoundException();
    }
}
```

## security-casbin 模块

### Casbin 配置

```java
@ConfigurationProperties(prefix = "afg.security.casbin")
public class CasbinProperties {
    private boolean enabled = true;
    private String model;                          // 模型定义
    private String policy;                         // 策略定义
    private boolean watchEnabled = true;           // 策略热更新
    private Duration syncInterval = Duration.ofSeconds(30);
}
```

### 权限执行器

```java
public class CasbinAfgEnforcer implements AfgEnforcer {
    private final Enforcer enforcer;

    @Override
    public boolean enforce(String subject, String resource, String action) {
        return enforcer.enforce(subject, resource, action);
    }

    // 批量权限检查
    public boolean enforceAll(String subject, List<ResourceAction> requests);

    // 添加/删除策略（管理接口）
    public void addPolicy(String subject, String resource, String action);
    public void removePolicy(String subject, String resource, String action);
}
```

### Casbin 模型定义（RBAC with domains，支持多租户）

```ini
[request_definition]
r = sub, dom, obj, act

[policy_definition]
p = sub, dom, obj, act

[role_definition]
r = sub, dom, role

[policy_effect]
e = some(where (p.eft == allow))

[matchers]
m = r.sub == p.sub && r.dom == p.dom && keyMatch2(r.obj, p.obj) && r.act == p.act
```

### 策略存储 SPI

```java
public interface AfgPolicyService {
    // 从数据库加载策略
    List<CasbinRule> loadAllPolicies();
    List<CasbinRule> loadPoliciesByTenant(String tenantId);

    // 策略变更
    void savePolicy(CasbinRule rule);
    void removePolicy(CasbinRule rule);

    // 角色-权限映射
    List<String> getPermissionsForRole(String role, String tenantId);
    List<String> getRolesForUser(String userId, String tenantId);
}
```

## 与现有代码的关系

### 保留在 `core/web/security/` 的接口

| 接口 | 原因 |
|------|------|
| `AfgSecurityContext` | 通用安全上下文，不依赖具体实现 |
| `AfgSecurityContextBridge` | 桥接器模式，业务可扩展 |
| `AfgSecurityConfigurer` | 模块级安全配置 SPI |
| `AfgSecurityConfiguration` | 配置收集器 |

### 迁移到 `security-core` 的接口

| 接口 | 新位置 | 原因 |
|------|--------|------|
| `AfgPrincipal` | `security-core/authentication/AfgUserDetails` | 更完整的用户信息定义 |
| `AfgEnforcer` | `security-core/authorization/AfgEnforcer` | 授权核心接口 |

### 保持不变

- `core/security/datascope/` - 数据权限（行级过滤）与认证授权是独立关注点

## 模块依赖关系

```
                    ┌─────────────────┐
                    │  spring-boot-   │
                    │    starter      │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              ▼              ▼
       ┌──────────┐   ┌───────────┐   ┌──────────┐
       │  auth-   │   │ resource- │   │ security │
       │  server  │   │  server   │   │ -casbin  │
       └────┬─────┘   └─────┬─────┘   └────┬─────┘
            │               │              │
            └───────────────┼──────────────┘
                            │
                            ▼
                    ┌───────────────┐
                    │ security-core │
                    └───────┬───────┘
                            │
                            ▼
                    ┌───────────────┐
                    │     core      │
                    └───────────────┘
```

**依赖说明：**

- `security-core` 依赖 `core`（使用异常、缓存等基础设施）
- `auth-server` 依赖 `security-core`
- `resource-server` 依赖 `security-core`
- `security-casbin` 依赖 `security-core`，实现 `AfgEnforcer`
- `spring-boot-starter` 自动配置三个实现模块

## Spring Boot Starter 自动配置

```java
@AutoConfiguration
@EnableConfigurationProperties({
    AuthServerProperties.class,
    JwtResourceProperties.class,
    IntrospectionProperties.class,
    CasbinProperties.class
})
public class AfgSecurityAutoConfiguration {

    // 认证服务器自动配置
    @Configuration
    @ConditionalOnClass(AuthServerMarker.class)
    @ConditionalOnProperty(prefix = "afg.security.auth-server", name = "enabled", havingValue = "true")
    static class AuthServerConfiguration { }

    // 资源服务器自动配置
    @Configuration
    @ConditionalOnClass(ResourceServerMarker.class)
    @ConditionalOnProperty(prefix = "afg.security.resource", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class ResourceServerConfiguration { }

    // Casbin 自动配置
    @Configuration
    @ConditionalOnClass(CasbinMarker.class)
    @ConditionalOnProperty(prefix = "afg.security.casbin", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class CasbinConfiguration { }
}
```

## 配置示例

```yaml
afg:
  security:
    # 认证服务器配置
    auth-server:
      enabled: true
      issuer: https://auth.afg.com
      signing-key: ${JWT_SIGNING_KEY}
      access-token-ttl: 2h
      refresh-token-ttl: 7d

    # 资源服务器配置
    resource:
      jwt:
        jwk-set-uri: https://auth.afg.com/.well-known/jwks.json
        issuer-uri: https://auth.afg.com
      tenant:
        strategies: header, token
        header-name: X-Tenant-Id

    # Casbin 配置
    casbin:
      enabled: true
      model: rbac_with_domains
      watch-enabled: true
```

## 测试策略

| 模块 | 测试重点 |
|------|----------|
| `security-core` | 接口契约测试、默认实现测试 |
| `auth-server` | OAuth2 流程测试（MockMvc）、Token 生成/验证、PKCE 验证 |
| `resource-server` | JWT 解析、租户解析链、Security Filter Chain |
| `security-casbin` | 权限规则测试、策略热更新、性能基准 |

**测试覆盖率要求：** 95%

## 发布坐标

| 模块 | Maven 坐标 |
|------|------------|
| security-core | `io.github.afg-projects:afg-framework-security-core` |
| auth-server | `io.github.afg-projects:afg-framework-auth-server` |
| resource-server | `io.github.afg-projects:afg-framework-resource-server` |
| security-casbin | `io.github.afg-projects:afg-framework-security-casbin` |

## Gradle 配置示例

```kotlin
// settings.gradle.kts
include("security-core")
include("security-impl:auth-server")
include("security-impl:resource-server")
include("security-impl:security-casbin")
```

```kotlin
// security-core/build.gradle.kts
dependencies {
    api(project(":core"))
    api(libs.spring.boot.starter.security)
    api(libs.jspecify)
}

// security-impl/auth-server/build.gradle.kts
dependencies {
    api(project(":security-core"))
    api(libs.spring.boot.starter.oauth2.authorization.server)
}

// security-impl/resource-server/build.gradle.kts
dependencies {
    api(project(":security-core"))
    api(libs.spring.boot.starter.oauth2.resource.server)
}

// security-impl/security-casbin/build.gradle.kts
dependencies {
    api(project(":security-core"))
    api(libs.jcasbin)
}
```
