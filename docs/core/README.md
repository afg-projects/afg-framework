# 核心功能

AFG Framework 提供了丰富的企业级核心功能。

## 缓存管理

### 本地缓存

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final CacheManager cacheManager;
    
    @Cacheable(cacheNames = "users", key = "#id")
    public User getById(Long id) {
        return userRepository.findById(id).orElseThrow();
    }
    
    @CacheEvict(cacheNames = "users", key = "#id")
    public void delete(Long id) {
        userRepository.deleteById(id);
    }
}
```

### 分布式缓存（Redis）

```java
@Configuration
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        return RedisCacheManager.builder(factory)
            .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)))
            .build();
    }
}
```

## 事件发布

### 定义事件

```java
public class UserCreatedEvent {
    
    private final Long userId;
    private final String username;
    private final LocalDateTime createdAt;
    
    public UserCreatedEvent(Long userId, String username) {
        this.userId = userId;
        this.username = username;
        this.createdAt = LocalDateTime.now();
    }
    
    // getters...
}
```

### 发布事件

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final ApplicationEventPublisher eventPublisher;
    
    public User create(User user) {
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new UserCreatedEvent(saved.getId(), saved.getUsername()));
        return saved;
    }
}
```

### 监听事件

```java
@Component
public class UserEventListener {
    
    @EventListener
    public void onUserCreated(UserCreatedEvent event) {
        log.info("User created: {}", event.getUsername());
        // 发送欢迎邮件等...
    }
    
    @Async
    @EventListener
    public void onUserCreatedAsync(UserCreatedEvent event) {
        // 异步处理
    }
}
```

## 异常处理

### 全局异常处理器

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(EntityNotFoundException.class)
    public Result<Void> handleEntityNotFound(EntityNotFoundException e) {
        return Result.error(ErrorCode.NOT_FOUND, e.getMessage());
    }
    
    @ExceptionHandler(DuplicateEntityException.class)
    public Result<Void> handleDuplicate(DuplicateEntityException e) {
        return Result.error(ErrorCode.CONFLICT, e.getMessage());
    }
    
    @ExceptionHandler(OptimisticLockException.class)
    public Result<Void> handleOptimisticLock(OptimisticLockException e) {
        return Result.error(ErrorCode.CONFLICT, "数据已被其他用户修改，请刷新后重试");
    }
}
```

## 安全防护

### XSS 防护

框架自动对请求参数进行 XSS 过滤。

### SQL 注入防护

使用参数化查询，自动防止 SQL 注入：

```java
// 安全：参数化查询
List<User> users = dataManager.entity(User.class)
    .query()
    .where(Conditions.builder(User.class)
        .eq(User::getUsername, username)  // 自动参数化
        .build())
    .list();
```

### 请求限流

```java
@RestController
@RequestMapping("/api")
public class ApiController {
    
    @RateLimit(value = 100, timeout = 60)  // 每分钟最多 100 次
    @GetMapping("/data")
    public Result<Data> getData() {
        return Result.success(dataService.getData());
    }
}
```

## 审计日志

### 自动审计

```java
@Getter
@Setter
public class User extends BaseEntity implements Auditable {
    
    private Long id;
    private String username;
    
    // Auditable 字段
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
```

### 审计日志查询

```java
@Service
@RequiredArgsConstructor
public class AuditService {
    
    private final AuditLogRepository auditLogRepository;
    
    public List<AuditLog> getEntityHistory(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }
}
```

## 分布式锁

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final DistributedLock lock;
    
    public Order createOrder(Long userId, OrderRequest request) {
        String lockKey = "order:create:" + userId;
        
        return lock.executeWithLock(lockKey, Duration.ofSeconds(30), () -> {
            // 在锁保护下执行业务逻辑
            return doCreateOrder(userId, request);
        });
    }
}
```

## 任务调度

```java
@Component
public class ScheduledTasks {
    
    @Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨 2 点执行
    public void cleanupExpiredData() {
        // 清理过期数据
    }
    
    @Scheduled(fixedRate = 300000)  // 每 5 分钟执行
    public void syncData() {
        // 数据同步
    }
}
```

## 链路追踪

框架自动集成 Micrometer Tracing，支持 Brave 和 OpenTelemetry。

```yaml
# application.yml
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0
```

## 健康检查

```java
@Component
public class CustomHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        // 自定义健康检查逻辑
        if (isHealthy()) {
            return Health.up().withDetail("service", "available").build();
        }
        return Health.down().withDetail("service", "unavailable").build();
    }
}
```
