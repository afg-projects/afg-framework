# 安全模块规格

> PRD 来源：§5.5 Security 安全模块（security-core + auth-server + resource-server）
> CLAUDE.md 来源：安全模块章节

## 1. 定位

企业级安全基础设施——OAuth2 授权服务器 + Casbin RBAC + 多租户 + 数据权限 + 登录策略 + 审计，开箱即用。

## 2. 模块结构

```
security-core/           # 安全 SPI 接口（认证、授权、租户、权限等抽象）
security-impl/
├── auth-server/         # 认证服务器实现（OAuth2、Token、登录、Casbin、权限、租户、审计）
└── resource-server/     # 资源服务器实现（JWT 验证、权限校验、租户解析）
```

### 2.1 依赖链

```
commons → security-core
security-core → auth-server ←── data-jdbc + core
security-core → resource-server ←── data-jdbc + core
```

## 3. Security Core SPI

包路径：`io.github.afgprojects.framework.security.core`

| 子包 | 核心接口 |
|------|----------|
| `authentication` | `AfgAuthentication`, `AfgUserDetails`, `AfgUserDetailsService` |
| `authorization` | `AfgEnforcer`, `AfgSecurityContext` |
| `login` | `LoginService`, `TokenService`, `CaptchaService`, `LoginStrategyFactory` |
| `oauth2` | `OAuth2AuthorizationService`, `OAuth2ClientService`, `AuthorizationCodeStorage` |
| `permission` | `PermissionService`, `RbacService`, `AbacService`, `DataScopeService` |
| `tenant` | `TenantResolver`, `TenantResolverChain`, `HeaderTenantResolver`, `AfgTenantService` |
| `security` | `LoginFailureTracker`, `DeviceLimiter`, `PasswordValidator`, `IpRestrictionChecker` |

## 4. 认证服务器（auth-server）

### 4.1 功能清单

用户名密码/手机号/邮箱登录、JWT Token 签发与验证、OAuth2 授权服务器、验证码、登录锁定、密码强度校验、IP 限制、设备绑定、Casbin RBAC 权限、数据权限、多租户、审计日志。

### 4.2 AutoConfiguration（9 个）

| AutoConfiguration | 功能 |
|-------------------|------|
| `AuthorizationServerAutoConfiguration` | OAuth2 授权服务器核心 |
| `LoginAutoConfiguration` | 登录流程（验证码、策略工厂、社交登录、2FA） |
| `OAuth2AutoConfiguration` | OAuth2 客户端、授权码存储 |
| `CasbinAutoConfiguration` | jCasbin RBAC 策略引擎 |
| `PermissionAutoConfiguration` | 权限管理（角色、资源、RBAC、@RequirePermission/@RequireRole） |
| `DataScopeAutoConfiguration` | 数据权限（5 种 DataScopeType + 自动条件注入） |
| `TenantAutoConfiguration` | 多租户（3 种隔离模式 + 解析器链 + 验证器 + 过滤器） |
| `SecurityStrategyAutoConfiguration` | 安全策略（登录锁定 + 设备限制 + 密码校验 + IP 限制） |
| `AuditAutoConfiguration` | 安全审计（登录日志 + 安全事件 + 告警） |

### 4.3 资源服务器 AutoConfiguration（2 个）

| AutoConfiguration | 功能 |
|-------------------|------|
| `ResourceServerAutoConfiguration` | 资源服务器（JWT 验证 + 远程权限校验） |
| `DefaultSecurityAutoConfiguration` | 默认安全配置（开发阶段放行所有请求） |

## 5. 登录策略

| 策略 | 说明 |
|------|------|
| `UsernamePasswordLoginStrategy` | 用户名密码 + 验证码 |
| `MobileCaptchaLoginStrategy` | 手机号 + 短信验证码 |
| `EmailCaptchaLoginStrategy` | 邮箱 + 验证码 |
| `WechatLoginStrategy` | 微信 OAuth2 登录 |
| `DingTalkLoginStrategy` | 钉钉 OAuth2 登录 |
| `FeishuLoginStrategy` | 飞书 OAuth2 登录 |
| `WeComLoginStrategy` | 企业微信 OAuth2 登录 |

社交登录（微信/钉钉/飞书/企微）只需配置 `appId`/`appSecret` 即可启用，框架提供完整的 `LoginStrategy` 实现。

## 6. AfgUserDetailsService——接入框架安全的唯一必须实现

业务项目只需实现 `AfgUserDetailsService` 接口，即可接入框架的完整安全体系：

```java
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements AfgUserDetailsService {

    private final DataManager dataManager;

    @Override
    public AfgUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = dataManager.findOne(User.class,
            builder(User.class).eq(User::getUsername, username).build())
            .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

        return AfgUserDetails.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .password(user.getPassword())
            .tenantId(user.getTenantId())
            .roles(user.getRoleCodes())
            .permissions(user.getPermissionCodes())
            .enabled(user.getStatus() == 1)
            .build();
    }
}
```

## 7. 权限控制

### 7.1 注解

- `@RequirePermission("order:delete")` —— 权限编码校验
- `@RequireRole(value = {"ADMIN", "MANAGER"}, logical = Logical.OR)` —— 角色编码校验

### 7.2 逻辑运算

- `Logical.AND` —— 全部满足（默认）
- `Logical.OR` —— 任一满足

### 7.3 动态 API 权限

通过治理中心配置 API 权限（如 `POST /orders → requireAuth=true, requirePermission="order:create"`），无需改代码。

## 8. 多租户

### 8.1 隔离模式

| 隔离模式 | 说明 | 适用场景 |
|---------|------|---------|
| 共享数据库 | `tenant_id` 列过滤 | 大多数 SaaS，成本最低 |
| 独立数据库 | 每个租户独立数据源 | 强隔离需求，合规要求 |
| 混合模式 | 按租户等级路由（大客户独立库，小客户共享库） | 混合场景 |

独立数据库和混合模式需要 `MultiDataSourceAutoConfiguration` 配合。

### 8.2 租户解析策略

| 策略 | 说明 |
|------|------|
| `TOKEN` | 从 JWT Token 中解析租户 ID |
| `HEADER` | 从 HTTP Header（默认 `X-Tenant-Id`）解析 |
| `DOMAIN` | 从请求域名解析 |

解析器链 `TenantResolverChain` 按配置的策略顺序依次尝试解析，优先使用成功解析的结果。

### 8.3 自动过滤

查询自动注入 `tenant_id` 条件，无需业务代码关心：

```java
dataManager.findAll(Order.class);  // → WHERE tenant_id = 'current-tenant'
```

## 9. 数据权限

### 9.1 DataScopeType

| DataScopeType | 自动注入条件 | 说明 |
|--------------|-------------|------|
| `ALL` | 无额外条件 | 看到所有数据 |
| `SELF` | `create_by = currentUserId` | 仅看自己创建的 |
| `DEPT` | `dept_id = currentDeptId` | 仅看本部门 |
| `DEPT_AND_CHILD` | `dept_id IN (currentDept + children)` | 本部门及子部门 |
| `CUSTOM` | 自定义策略 | 业务自定义条件 |

### 9.2 使用方式

```java
// 查询时指定
dataManager.entity(User.class).query().withDataScope().list();

// 或使用便捷方法
dataManager.findListWithDataScope(User.class, condition);
```

数据权限拦截器默认启用（`afg.security.auth-server.permission.data-scope-interceptor-enabled: true`），自动根据当前用户的数据权限范围注入条件。

## 10. 部署模式

通过 Spring 配置属性控制，**非** Spring Profile 机制：

| 模式 | 配置 | 说明 |
|------|------|------|
| AUTH_SERVER | `afg.security.auth-server.enabled: true` | 独立认证服务 |
| RESOURCE_SERVER | `afg.security.resource-server.enabled: true` | 只验证 Token，依赖外部认证 |
| MONOLITH | 两者同时启用 | 聚合部署 |

Gradle 插件的 `securityMode`（字符串：`"AUTH_SERVER"` / `"RESOURCE_SERVER"` / `"MONOLITH"`）仅影响 `afgInit` 任务生成的初始代码，不影响运行时。

## 11. OAuth2 授权码流程

1. 客户端重定向到 `/oauth2/authorize?response_type=code&client_id=xxx`
2. 用户授权后重定向回 `redirect_uri?code=xxx`
3. 客户端用 code 换 token：`POST /oauth2/token`

框架完整实现 OAuth2 授权服务器端点：`/authorize`, `/token`, `/introspect`, `/revoke`

## 12. 2FA（TOTP）

登录时如用户启用了 TOTP，需额外输入验证码。框架提供 QR 码生成 + TOTP 验证。

## 13. 密码重置流程

- `POST /auth-api/auth/password/reset-request` → 发送重置令牌到邮箱/手机
- `POST /auth-api/auth/password/reset` → 验证令牌 + 重置密码

## 14. 模块 Context-Path

安全模块的 context-path 为 `/auth-api`。

Spring Security 配置需匹配带模块前缀路径：

```java
.requestMatchers("/auth-api/auth/login").permitAll()
```

## 15. 配置

### 15.1 认证服务器配置

配置前缀：`afg.security.auth-server`

```yaml
afg:
  security:
    auth-server:
      enabled: true
      token:
        issuer: https://auth.example.com
        signing-key: my-secret-signing-key-at-least-256-bits
        access-token-ttl: 2h
        refresh-token-ttl: 7d
      oauth2:
        enabled: true
        authorization-code-ttl: 5m
        clients:
          - client-id: my-client
            client-secret: my-secret
            client-name: My Application
            redirect-uris: https://app.example.com/callback
            scopes: read,write
            grant-types: authorization_code,refresh_token
            require-pkce: true
      login:
        enabled: true
        captcha-ttl: 5m
        captcha-length: 4
      casbin:
        enabled: true
        model-type: rbac-domain
        policy-adapter-type: jdbc
        auto-save: true
        auto-build-role-links: true
      permission:
        enabled: true
        default-data-scope: ALL
        data-scope-interceptor-enabled: true
      tenant:
        enabled: true
        strategies: TOKEN,HEADER,DOMAIN,DEFAULT
        default-tenant: default
        header-name: X-Tenant-Id
      security:
        enabled: true
        max-login-failures: 5
        lock-duration: 30m
        max-devices: 5
      audit:
        enabled: true
        alert:
          login-failure-alert: true
          login-failure-threshold: 5
```

### 15.2 资源服务器配置

配置前缀：`afg.security.resource-server`

```yaml
afg:
  security:
    resource-server:
      enabled: true
      default-security: true    # 启用 DefaultSecurityAutoConfiguration
      jwt:
        enabled: true
        jwk-set-uri: https://auth.example.com/.well-known/jwks.json
        issuer-uri: https://auth.example.com
        cache-ttl: 5m
      permission:
        auth-server-url: http://auth-server:8080/auth-api/internal
        key-id: resource-server-1
        secret: shared-secret-key
      tenant:
        strategies: token,header
        header-name: X-Tenant-Id
        fail-if-unresolved: true
```

## 16. 与 Spring Boot 原生对比

| 能力 | Spring Boot 原生 | AFG 增强 |
|------|-----------------|---------|
| OAuth2 授权服务器 | 无 | 完整实现（/authorize, /token, /introspect, /revoke） |
| RBAC 策略引擎 | 无 | 集成 jCasbin |
| 多租户 | 无 | 三种隔离模式 + 解析器链 |
| 数据权限 | 无 | 行级自动注入（5 种 DataScopeType） |
| 社交登录 | 无 | 微信/钉钉/飞书/企微 |
| 2FA | 无 | TOTP |
| 登录策略 | 仅表单登录 | 7 种策略 |
| 安全审计 | 无 | 登录日志 + 安全事件 + 告警 |

## 17. 字段级访问控制（FieldAccessControl SPI）

包路径：`io.github.afgprojects.framework.data.core.security`

### 17.1 SPI 接口

```java
public interface FieldAccessControl {
    /** 返回当前用户可读取的字段名集合 */
    Set<String> getReadableFields(Class<?> entityClass);

    /** 返回当前用户可写入的字段名集合 */
    Set<String> getWritableFields(Class<?> entityClass);
}
```

### 17.2 默认实现

`NoOpFieldAccessControl` — 所有字段可读可写（返回空集表示无限制）。

### 17.3 双重防护机制

| 层级 | 机制 | 说明 |
|------|------|------|
| **主强制** | SQL 列排除 | 未授权列从 SELECT 列列表中排除（不查出来），与 DataScope SQL 重写模式一致 |
| **纵深防御** | Jackson `BeanSerializerModifier` | 序列化时二次检查，排除未授权字段 |
| **写保护** | UPDATE SET 列排除 | 未授权列从 UPDATE SET 子句中排除 |

### 17.4 Casbin 集成

通过 `AfgEnforcer.enforce()` 实现字段级权限校验，字段编码格式：`{entity}.{field}`（如 `sys_user.salary`）。

---

## 18. 数据脱敏（MaskingStrategy SPI）

包路径：`io.github.afgprojects.framework.data.core.sensitive`

### 18.1 注解

```java
@SensitiveField(SensitiveType.PHONE)
private String phone;
```

### 18.2 SPI 接口

| 接口 | 说明 |
|------|------|
| `MaskingStrategy` | 脱敏策略：`mask(String value, SensitiveType type)` |
| `MaskingContext` | 脱敏上下文：`shouldMask(String fieldName, SensitiveType type)` — 按角色差异化 |

### 18.3 脱敏类型

| SensitiveType | 规则 | 示例 |
|---------------|------|------|
| `PHONE` | 中间4位 `****` | `138****1234` |
| `ID_CARD` | 中间10位 `****` | `3201**********1234` |
| `EMAIL` | 用户名脱敏 | `u***@example.com` |
| `BANK_CARD` | 仅后4位 | `****1234` |
| `NAME` | 仅首字 | `张**` |
| `ADDRESS` | 仅省市区 | `北京市海淀区****` |
| `CUSTOM` | 自定义策略 | — |

### 18.4 默认实现

`DefaultMaskingStrategy` — GB/T 35273 标准脱敏规则。

### 18.5 脱敏时机

- 实体持有真实值（数据库存储真实数据或密文）
- Jackson 序列化时动态脱敏（`SensitiveFieldBeanSerializerModifier`）
- 导出层（Excel/PDF）同样生效

---

## 19. 审计追踪（AuditTrailStorage SPI）

包路径：`io.github.afgprojects.framework.data.core.event`

### 19.1 设计

事件驱动双表设计：
- `audit_log` — 操作记录（实体类型、操作类型、操作人、时间）
- `audit_field_diff` — 字段级变更明细（字段名、旧值、新值）

### 19.2 SPI 接口

```java
public interface AuditTrailStorage {
    void save(AuditEvent event);
}

public record FieldChangeDiff(
    String fieldName,
    String oldValue,
    String newValue
) {}
```

### 19.3 默认实现

`NoOpAuditTrailStorage` — 不记录审计日志。

### 19.4 AuditTrailEventListener

`AuditTrailEventListener` 监听实体变更事件，通过反射计算 UPDATE 操作的字段级 diff（比较 oldEntity vs newEntity 每个字段）。支持异步（默认）和同步模式。
