# 数据访问层

AFG Framework 提供了一套轻量级的 ORM 解决方案，无需 JPA 依赖，基于 Spring JdbcClient 实现。

## 核心概念

### DataManager

`DataManager` 是数据操作的统一入口，提供：

- `entity(Class)` - 获取实体操作代理
- `query()` - 创建 SQL 查询构建器
- `executeInTransaction()` - 事务管理
- `tenantScope()` - 多租户支持

### EntityProxy

`EntityProxy` 提供类型安全的实体操作：

- `save()` / `saveAll()` - 保存实体
- `findById()` / `findAll()` - 查询实体
- `deleteById()` / `deleteAll()` - 删除实体
- `query()` - 条件查询构建器

### EntityReader / EntityWriter / EntityQuery

接口职责分离：

- `EntityReader` - 只读操作
- `EntityWriter` - 写入操作
- `EntityQuery` - 条件查询

## 条件查询

### Lambda 风格（推荐）

```java
// 类型安全的条件构建
Condition condition = Conditions.builder(User.class)
    .eq(User::getStatus, "ACTIVE")
    .like(User::getUsername, "test")
    .gt(User::getAge, 18)
    .build();

List<User> users = dataManager.entity(User.class)
    .query()
    .where(condition)
    .list();
```

### 字符串字段名

```java
// 字符串字段名方式
Condition condition = Conditions.builder()
    .eq("status", "ACTIVE")
    .like("username", "test")
    .build();
```

### 支持的操作符

| 方法 | SQL 操作符 | 说明 |
|------|-----------|------|
| `eq` | = | 等于 |
| `ne` | != | 不等于 |
| `gt` | > | 大于 |
| `ge` | >= | 大于等于 |
| `lt` | < | 小于 |
| `le` | <= | 小于等于 |
| `like` | LIKE | 模糊匹配 |
| `in` | IN | 包含 |
| `notIn` | NOT IN | 不包含 |
| `isNull` | IS NULL | 为空 |
| `isNotNull` | IS NOT NULL | 不为空 |
| `between` | BETWEEN | 区间 |

## 分页查询

```java
// 创建分页请求
PageRequest pageRequest = PageRequest.of(1, 10, 
    Sort.by(Sort.Order.desc("createdAt")));

// 执行分页查询
Page<User> page = dataManager.entity(User.class)
    .query()
    .where(condition)
    .page(pageRequest);

// 获取分页信息
List<User> records = page.getContent();
long total = page.getTotal();
int totalPages = page.getTotalPages();
```

## 企业级特性

### 多租户

```java
// 设置租户上下文
try (var scope = dataManager.tenantScope("tenant-001")) {
    // 在租户上下文中执行操作
    List<User> users = dataManager.entity(User.class).findAll();
}
```

### 软删除

```java
// 实现 SoftDeletable 接口
public class User implements SoftDeletable {
    private Boolean deleted;
    
    @Override
    public Boolean getDeleted() {
        return deleted;
    }
    
    @Override
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}

// 删除操作自动变为软删除
dataManager.entity(User.class).deleteById(1L);

// 恢复删除
dataManager.entity(User.class).restoreById(1L);

// 包含已删除记录
List<User> allUsers = dataManager.entity(User.class)
    .query()
    .includeDeleted()
    .list();
```

### 乐观锁

```java
// 实现 Versioned 接口
public class Order implements Versioned {
    private Long version = 0L;
    
    @Override
    public Long getVersion() {
        return version;
    }
    
    @Override
    public void incrementVersion() {
        this.version++;
    }
}

// 更新时自动检查版本
Order order = dataManager.entity(Order.class).findById(1L).orElseThrow();
order.setStatus("COMPLETED");
dataManager.entity(Order.class).update(order);
// 如果版本冲突，抛出 OptimisticLockException
```

### 数据权限

```java
// 设置数据权限范围
List<User> users = dataManager.entity(User.class)
    .query()
    .withDataScope(DataScope.of("sys_user", "dept_id", DataScopeType.DEPT))
    .list();
```

## 事务管理

### 编程式事务

```java
// 使用 DataManager 的事务方法
dataManager.executeInTransaction(() -> {
    dataManager.entity(User.class).save(user);
    dataManager.entity(Order.class).save(order);
});

// 返回结果
User result = dataManager.executeInTransaction(() -> {
    User saved = dataManager.entity(User.class).save(user);
    return saved;
});

// 只读事务
List<User> users = dataManager.executeInReadOnly(() ->
    dataManager.entity(User.class).findAll()
);
```

### Spring 声明式事务

框架与 Spring `@Transactional` 完全兼容：

```java
@Service
public class UserService {
    
    private final DataManager dataManager;
    
    @Transactional
    public void createUserWithOrder(User user, Order order) {
        dataManager.entity(User.class).save(user);
        dataManager.entity(Order.class).save(order);
    }
}
```

## 异常处理

框架提供丰富的异常层次结构：

| 异常类 | 说明 |
|--------|------|
| `DataAccessException` | 数据访问基础异常 |
| `EntityNotFoundException` | 实体未找到 |
| `DuplicateEntityException` | 实体重复（唯一约束冲突） |
| `OptimisticLockException` | 乐观锁冲突 |
| `DataValidationException` | 数据验证失败 |
| `DataPermissionException` | 数据权限不足 |
| `MultiTenantException` | 多租户错误 |
| `BatchOperationException` | 批量操作失败 |
| `EntityMappingException` | 实体映射错误 |

```java
// 使用示例
try {
    User user = dataManager.entity(User.class)
        .findById(1L)
        .orElseThrow(() -> new EntityNotFoundException(User.class, 1L));
} catch (EntityNotFoundException e) {
    log.error("User not found: {}", e.getEntityId());
}
```

## 关联查询

```java
// 定义关联关系
public class User {
    
    @OneToMany(mappedBy = "user")
    private List<Order> orders;
    
    @ManyToOne
    private Department department;
}

// 急加载关联
List<User> users = dataManager.entity(User.class)
    .query()
    .withAssociation("orders")
    .withAssociation("department")
    .list();

// 按需加载
User user = dataManager.entity(User.class).findById(1L).orElseThrow();
List<Order> orders = dataManager.entity(User.class).fetch(user, "orders");
```

## Repository 模式

推荐在应用层封装 Repository：

```java
@Repository
@RequiredArgsConstructor
public class UserRepository {
    
    private final DataManager dataManager;
    
    private EntityProxy<User> entity() {
        return dataManager.entity(User.class);
    }
    
    public User save(User user) {
        return entity().save(user);
    }
    
    public Optional<User> findById(Long id) {
        return entity().findById(id);
    }
    
    public Optional<User> findByUsername(String username) {
        return entity().query()
            .where(Conditions.builder(User.class)
                .eq(User::getUsername, username)
                .build())
            .one();
    }
    
    public boolean existsByUsername(String username) {
        return entity().query()
            .where(Conditions.builder(User.class)
                .eq(User::getUsername, username)
                .build())
            .exists();
    }
    
    public Page<User> findAll(PageRequest pageRequest) {
        return entity().query()
            .where(Conditions.builder(User.class)
                .eq(User::getDeleted, false)
                .build())
            .page(pageRequest);
    }
}
```
