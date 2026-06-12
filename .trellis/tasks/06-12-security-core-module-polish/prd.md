# security-core SPI 模块精细化打磨 — 6 质量基线落地

## Goal

对 afg-framework security-core 模块进行精细化打磨。本模块是纯 SPI/API 模块（无 AutoConfiguration），重点在：
1. NoOp fallback — SPI 接口本地降级实现
2. 异常体系统一 — IllegalStateException/RuntimeException→BusinessException
3. 接口 default 方法的合理性

## What I already know

* 纯 SPI 模块，31 个 SPI 接口，0 个 AutoConfiguration
* 0 个 NoOp 实现（除 tenant 子包有 DefaultTenantContext/HeaderTenantResolver/TenantResolverChain）
* AfgSecurityContext 2 处 IllegalStateException→应替换为 BusinessException
* OAuth2Exception 和 TokenValidationException 直接继承 RuntimeException→应考虑统一
* AfgAuthentication.setAuthenticated 声明 throws IllegalArgumentException→Spring Security 接口约束，保留
* 34 个测试文件（主要是 record/model/enum 测试），SPI 接口 default 方法无测试
* TenantErrorCode 枚举已有良好实践（实现 ErrorCode 接口）

## Requirements

### T1: SPI NoOp fallback 补充

为 security-core 的核心 SPI 创建 NoOp 降级实现。由于本模块无 AutoConfiguration，NoOp 实现仅作为可用的降级类，不注册为 Bean（Bean 注册在 auth-server/resource-server 中处理）。

**需要创建的 NoOp 类：**

| SPI 接口 | NoOp 实现 | 说明 |
|---|---|---|
| `LoginService` | `NoOpLoginService` | 登录总是返回失败 |
| `CaptchaService` | `NoOpCaptchaService` | 验证码总是验证失败 |
| `TokenService` | `NoOpTokenService` | Token 操作返回空/无效 |
| `DeviceLimiter` | `NoOpDeviceLimiter` | 总是允许（不限制设备） |
| `IpRestrictionChecker` | `NoOpIpRestrictionChecker` | 总是允许（不限制 IP） |
| `LoginFailureTracker` | `NoOpLoginFailureTracker` | 总是记录 0 次失败（不追踪） |
| `PasswordValidator` | `NoOpPasswordValidator` | 总是验证通过 |
| `AfgCaptchaStorage` | `NoOpCaptchaStorage` | 存储操作返回空/不存在 |
| `AfgDeviceStorage` | `NoOpDeviceStorage` | 返回空设备列表 |
| `AfgLoginFailureStorage` | `NoOpLoginFailureStorage` | 返回 0 次失败 |
| `AfgRefreshTokenStorage` | `NoOpRefreshTokenStorage` | 返回空/不存在 |
| `AfgTokenBlacklist` | `NoOpTokenBlacklist` | 总是返回 false（不在黑名单） |

**不需要 NoOp 的接口（业务模块必须提供实现）：**
- AfgUserDetailsService — 业务必须实现，NoOp 无意义
- AfgEnforcer — 权限校验核心，NoOp 会在 core 模块 AfgSecurityAutoConfiguration 中已有 lambda 默认
- PermissionService/RbacService/AbacService/DataScopeService — 业务必须实现
- AlertService/LoginLogService/SecurityEventService — 审计核心，必须有实现
- OAuth2AuthorizationService/OAuth2ClientService/AuthorizationCodeStorage — OAuth2 核心，必须实现
- AfgTenantService/TenantValidator — 业务必须实现
- RolePermissionStorage — 权限核心，必须实现

### T2: IllegalStateException→BusinessException 替换

| 文件 | 当前 | 替换为 |
|---|---|---|
| AfgSecurityContext.getRequiredCurrentUser | IllegalStateException("No authenticated user found") | BusinessException(CommonErrorCode.UNAUTHORIZED, "No authenticated user found") |
| AfgSecurityContext.getRequiredCurrentUserId | IllegalStateException("No authenticated user found") | BusinessException(CommonErrorCode.UNAUTHORIZED, "No authenticated user found") |

注意：AfgAuthentication.setAuthenticated 的 IllegalArgumentException 是 Spring Security 接口约束，保留不动。

### T3: OAuth2Exception 统一异常体系

OAuth2Exception 当前直接继承 RuntimeException，应改为继承 BusinessException 以统一异常体系。

需要：
1. OAuth2Exception extends BusinessException
2. 保留原有 errorCode/errorDescription 字段
3. 适配 BusinessException 的构造函数

### T4: TokenValidationException 统一异常体系

TokenValidationException 当前直接继承 RuntimeException，应改为继承 BusinessException。

需要：
1. TokenValidationException extends BusinessException
2. 使用 CommonErrorCode.TOKEN_INVALID 或自定义 ErrorCode
3. 保留原有 message 信息

### T5: 测试补充（NoOp 实现测试）

为所有新增 NoOp 实现创建单元测试，验证降级行为正确。

## Acceptance Criteria

- [ ] 12 个 SPI NoOp 实现类创建完成
- [ ] AfgSecurityContext 的 IllegalStateException 替换为 BusinessException
- [ ] OAuth2Exception 继承 BusinessException
- [ ] TokenValidationException 继承 BusinessException
- [ ] NoOp 实现测试覆盖
- [ ] 全量构建通过

## Definition of Done

- 代码修改完成
- ./gradlew build 通过
- commit 提交

## Out of Scope

- AutoConfiguration 测试覆盖（本模块无 AutoConfiguration）
- SPI 接口的 default 方法测试
- 业务 SPI（AfgUserDetailsService 等）的 NoOp 实现

## Technical Notes

- 模块路径: security-core/src/main/java/io/github/afgprojects/framework/security/core/
- 依赖 commons 模块（BusinessException/CommonErrorCode）
- 依赖 core 模块
- 依赖 Spring Security Core
- NoOp 实现放在各 SPI 接口同一包下
