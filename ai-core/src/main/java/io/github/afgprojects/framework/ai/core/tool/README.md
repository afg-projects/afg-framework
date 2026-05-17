# AI 工具安全集成

本模块提供 AI 工具调用与安全模块的集成，包括权限校验、数据权限、审计日志和内容安全。

## 快速开始

### 1. 添加依赖

```kotlin
dependencies {
    implementation("io.github.afg-projects:afg-framework-ai-spring-boot-starter:1.0.0-SNAPSHOT")
}
```

### 2. 配置

```yaml
afg:
  ai:
    tool:
      security:
        enabled: true
        max-iterations: 10
        timeout-ms: 30000
        permission-checker:
          enabled: true
        audit:
          enabled: true
          table-name: ai_tool_audit
```

### 3. 创建安全工具

```java
public class UserQueryTool extends DataQueryTool<User, UserQueryInput, UserQueryOutput> {

    public UserQueryTool(DataManager dataManager) {
        super(dataManager, User.class);
    }

    @Override
    public String name() {
        return "query_users";
    }

    @Override
    public String requiredPermission() {
        return "user:read";  // 定义所需权限
    }

    @Override
    public DataScope getDataScope(ToolContext context) {
        // 定义数据权限
        return DataScope.of("sys_user", "dept_id", DataScopeType.DEPT_AND_CHILD);
    }

    @Override
    protected Condition buildCondition(UserQueryInput input, ToolContext context) {
        return Conditions.builder(User.class)
            .eqIf(User::getStatus, input.status())
            .likeIf(User::getName, input.keyword())
            .build();
    }

    @Override
    protected UserQueryOutput convertResult(List<User> entities, ToolContext context) {
        // 过滤敏感字段
        List<UserInfo> infos = entities.stream()
            .map(u -> new UserInfo(u.getId(), u.getName()))
            .toList();
        return new UserQueryOutput(infos, entities.size());
    }
}
```

### 4. 注册工具

```java
@Configuration
public class ToolConfiguration {

    @Bean
    public UserQueryTool userQueryTool(DataManager dataManager) {
        return new UserQueryTool(dataManager);
    }
}
```

## 核心接口

### ToolContext

工具执行上下文，包含用户信息、租户信息等：

```java
ToolContext context = ToolContext.builder()
    .userId("user-001")
    .tenantId("tenant-001")
    .sessionId("session-001")
    .attribute("customKey", "customValue")
    .build();
```

### SecureTool

安全工具接口，扩展 `Tool` 接口：

| 方法 | 说明 |
|------|------|
| `requiredPermission()` | 所需权限（如 "user:read"） |
| `requiredRoles()` | 所需角色集合 |
| `isSensitive()` | 是否敏感操作 |
| `isAuditable()` | 是否需要审计 |
| `getDataScope(context)` | 数据权限配置 |
| `execute(input, context)` | 带上下文执行 |
| `validate(input, context)` | 输入校验 |
| `filterOutput(output, context)` | 输出过滤 |

### DataQueryTool

数据查询工具基类，自动应用数据权限：

```java
public abstract class DataQueryTool<T, I, O> implements SecureTool<I, O> {
    // 自动应用租户隔离
    // 自动应用数据权限
    // 提供查询条件构建钩子
}
```

## 安全特性

### 权限校验

工具执行前校验用户权限：

1. 检查用户是否已认证
2. 管理员直接放行
3. 检查 `requiredRoles()` - 满足任一角色即可
4. 检查 `requiredPermission()` - 必须具有该权限

### 数据权限

自动应用数据权限范围：

```java
@Override
public DataScope getDataScope(ToolContext context) {
    return DataScope.of("sys_user", "dept_id", DataScopeType.DEPT_AND_CHILD);
}
```

支持的数据范围类型：

| 类型 | 说明 |
|------|------|
| `ALL` | 全部数据 |
| `SELF` | 仅本人数据 |
| `DEPT` | 本部门数据 |
| `DEPT_AND_CHILD` | 本部门及子部门数据 |
| `CUSTOM` | 自定义条件 |

### 审计日志

记录工具调用的完整过程：

- 调用者信息（用户 ID、租户 ID、会话 ID）
- 工具信息（名称、参数）
- 执行结果（输出、错误）
- 执行时间（开始、结束、耗时）

查询审计日志：

```java
@Autowired
private ToolAuditLogger auditLogger;

List<ToolAuditEntry> logs = auditLogger.query(
    new JdbcToolAuditLogger.DefaultToolAuditQuery()
        .userId("user-001")
        .toolName("query_users")
        .page(0)
        .size(20)
);
```

### 内容安全

对 AI 输入/输出进行安全检查：

- 敏感词检测
- PII（个人身份信息）检测
- 有害内容检测

## 配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `afg.ai.tool.security.enabled` | 是否启用安全工具 | `true` |
| `afg.ai.tool.security.max-iterations` | 最大迭代次数 | `10` |
| `afg.ai.tool.security.timeout-ms` | 执行超时（毫秒） | `30000` |
| `afg.ai.tool.security.permission-checker.enabled` | 是否启用权限检查 | `true` |
| `afg.ai.tool.security.audit.enabled` | 是否启用审计日志 | `true` |
| `afg.ai.tool.security.audit.table-name` | 审计日志表名 | `ai_tool_audit` |

## 数据库表

审计日志表结构：

```sql
CREATE TABLE ai_tool_audit (
    id VARCHAR(64) PRIMARY KEY,
    call_id VARCHAR(64) NOT NULL,
    tool_name VARCHAR(100) NOT NULL,
    user_id VARCHAR(64),
    tenant_id VARCHAR(64),
    session_id VARCHAR(64),
    arguments TEXT,
    output TEXT,
    error TEXT,
    status VARCHAR(20) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    duration_ms BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tool_audit_user ON ai_tool_audit(user_id);
CREATE INDEX idx_tool_audit_tenant ON ai_tool_audit(tenant_id);
CREATE INDEX idx_tool_audit_tool ON ai_tool_audit(tool_name);
CREATE INDEX idx_tool_audit_start_time ON ai_tool_audit(start_time);
```
