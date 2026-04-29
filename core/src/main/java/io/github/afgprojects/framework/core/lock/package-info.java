/**
 * 分布式锁模块
 * <p>
 * 提供基于 Redisson 的分布式锁机制，支持声明式锁配置。
 * </p>
 *
 * <h2>核心组件</h2>
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.lock.DistributedLock} - 分布式锁接口</li>
 *   <li>{@code io.github.afgprojects.impl.redis.lock.RedisDistributedLock} - Redis 实现（在 afg-redis 模块中）</li>
 *   <li>{@link io.github.afgprojects.framework.core.lock.Lock} - 声明式锁注解</li>
 *   <li>{@link io.github.afgprojects.framework.core.lock.LockAspect} - 锁切面</li>
 *   <li>{@link io.github.afgprojects.framework.core.lock.LockProperties} - 配置属性</li>
 * </ul>
 *
 * <h2>支持的锁类型</h2>
 * <ul>
 *   <li>可重入锁（REENTRANT）- 默认，支持同一线程多次获取</li>
 *   <li>公平锁（FAIR）- 按请求顺序获取，避免线程饥饿</li>
 *   <li>读锁（READ）- 共享锁，多线程可同时持有</li>
 *   <li>写锁（WRITE）- 排他锁，同一时间只有一个线程持有</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 *
 * <h3>编程式使用</h3>
 * <pre>{@code
 * @Autowired
 * private DistributedLock distributedLock;
 *
 * public void process(String orderId) {
 *     boolean acquired = distributedLock.tryLock("order:" + orderId, 5000, 30000);
 *     if (acquired) {
 *         try {
 *             // 执行业务逻辑
 *         } finally {
 *             distributedLock.unlock("order:" + orderId);
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h3>声明式使用</h3>
 * <pre>{@code
 * @Lock(key = "#orderId", waitTime = 5000, leaseTime = 30000)
 * public void processOrder(String orderId) {
 *     // 执行业务逻辑
 * }
 * }</pre>
 *
 * <h3>读写锁使用</h3>
 * <pre>{@code
 * @Lock(key = "#userId", lockType = LockType.READ)
 * public User getUser(String userId) {
 *     // 读取用户信息
 * }
 *
 * @Lock(key = "#user.id", lockType = LockType.WRITE)
 * public void updateUser(User user) {
 *     // 更新用户信息
 * }
 * }</pre>
 *
 * <h2>配置项</h2>
 * <pre>
 * afg:
 *   lock:
 *     enabled: true
 *     key-prefix: "afg:lock"
 *     default-wait-time: 5000
 *     default-lease-time: -1  # -1 表示使用 watchdog 自动续期
 *     annotations:
 *       enabled: true
 * </pre>
 *
 * @see io.github.afgprojects.framework.core.lock.DistributedLock
 * @see io.github.afgprojects.framework.core.lock.Lock
 * @see io.github.afgprojects.framework.core.lock.LockType
 */
package io.github.afgprojects.framework.core.lock;
