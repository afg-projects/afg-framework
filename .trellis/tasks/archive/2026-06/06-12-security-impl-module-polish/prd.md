# security-impl 模块精细化打磨 — auth-server + resource-server

## Goal

对 auth-server 和 resource-server 模块进行精细化打磨，重点：
1. AutoConfiguration 依赖排序 — @AutoConfigureAfter/Before 声明
2. IllegalArgumentException→BusinessException 替换

## What I already know

### auth-server
- 9 个 AutoConfiguration，仅 2 个有 @AutoConfigureAfter（LoginAutoConfiguration + AuditAutoConfiguration → DataManagerAutoConfiguration）
- 大量 IllegalArgumentException 需替换（登录策略 + DefaultLoginService + LoginController 约 20 处）
- 13 个测试文件

### resource-server
- 2 个 AutoConfiguration，均无 @AutoConfigureAfter
- 无 IllegalArgumentException 需替换
- 0 个测试文件

## Requirements

### T1: auth-server @AutoConfigureAfter 依赖排序

| AutoConfiguration | 添加 after |
|---|---|
| AuthorizationServerAutoConfiguration | AfgAutoConfiguration, LoginAutoConfiguration |
| LoginAutoConfiguration | 已有 DataManagerAutoConfiguration，补充 AfgAutoConfiguration |
| OAuth2AutoConfiguration | AfgAutoConfiguration, AuthorizationServerAutoConfiguration |
| CasbinAutoConfiguration | AfgAutoConfiguration, AuthorizationServerAutoConfiguration |
| PermissionAutoConfiguration | AfgAutoConfiguration, CasbinAutoConfiguration |
| DataScopeAutoConfiguration | AfgAutoConfiguration, PermissionAutoConfiguration |
| TenantAutoConfiguration | AfgAutoConfiguration, AuthorizationServerAutoConfiguration |
| SecurityStrategyAutoConfiguration | AfgAutoConfiguration, LoginAutoConfiguration |
| AuditAutoConfiguration | 已有 DataManagerAutoConfiguration，补充 AfgAutoConfiguration |

注意：跨模块依赖用 afterName 字符串引用：
- DataManagerAutoConfiguration → afterName = "io.github.afgprojects.framework.data.jdbc.autoconfigure.DataManagerAutoConfiguration"
- AfgAutoConfiguration → 直接用类引用（同模块 core 可编译期依赖）

### T2: resource-server @AutoConfigureAfter 依赖排序

| AutoConfiguration | 添加 after |
|---|---|
| ResourceServerAutoConfiguration | AfgAutoConfiguration (afterName) |
| DefaultSecurityAutoConfiguration | ResourceServerAutoConfiguration |

### T3: auth-server IllegalArgumentException→BusinessException 替换

**登录策略（3 个文件，16 处）：**

| 文件 | 错误信息 | 目标 ErrorCode |
|---|---|---|
| UsernamePasswordLoginStrategy | "验证码错误" | UNAUTHORIZED |
| UsernamePasswordLoginStrategy | "密码错误" | UNAUTHORIZED |
| UsernamePasswordLoginStrategy | "账号已被禁用" | ACCOUNT_DISABLED |
| UsernamePasswordLoginStrategy | "账号已被锁定" | ACCOUNT_LOCKED |
| UsernamePasswordLoginStrategy | "账号已过期" | ACCOUNT_DISABLED |
| UsernamePasswordLoginStrategy | "凭证已过期" | PASSWORD_EXPIRED |
| MobileCaptchaLoginStrategy | "验证码错误" | UNAUTHORIZED |
| MobileCaptchaLoginStrategy | "账号已被禁用" | ACCOUNT_DISABLED |
| MobileCaptchaLoginStrategy | "账号已被锁定" | ACCOUNT_LOCKED |
| MobileCaptchaLoginStrategy | "账号已过期" | ACCOUNT_DISABLED |
| EmailCaptchaLoginStrategy | "验证码错误" | UNAUTHORIZED |
| EmailCaptchaLoginStrategy | "账号已被禁用" | ACCOUNT_DISABLED |
| EmailCaptchaLoginStrategy | "账号已被锁定" | ACCOUNT_LOCKED |
| EmailCaptchaLoginStrategy | "账号已过期" | ACCOUNT_DISABLED |

**DefaultLoginService（7 处）：**

| 错误信息 | 目标 ErrorCode |
|---|---|
| "IP 已被限制" | FORBIDDEN |
| "不支持的登录类型" | PARAM_ERROR |
| "账户已被锁定" | ACCOUNT_LOCKED |
| "账号已被禁用" | ACCOUNT_DISABLED |
| "账号已被锁定" | ACCOUNT_LOCKED |
| "账号已过期" | ACCOUNT_DISABLED |
| "凭证已过期" | PASSWORD_EXPIRED |

**LoginController（1 处）：**

| 错误信息 | 目标 ErrorCode |
|---|---|
| "Invalid authorization header" | UNAUTHORIZED |

**保留 IllegalArgumentException：**
- DefaultDeviceLimiter "maxDevices must be greater than 0" — 纯参数校验

## Acceptance Criteria

- [ ] 9 个 auth-server AutoConfiguration 添加 @AutoConfigureAfter
- [ ] 2 个 resource-server AutoConfiguration 添加 @AutoConfigureAfter
- [ ] 约 24 处 IllegalArgumentException→BusinessException 替换
- [ ] 全量构建通过

## Definition of Done

- 代码修改完成
- ./gradlew build 通过
- commit 提交

## Out of Scope

- AutoConfiguration 测试覆盖
- resource-server 测试覆盖
- Controller 测试
