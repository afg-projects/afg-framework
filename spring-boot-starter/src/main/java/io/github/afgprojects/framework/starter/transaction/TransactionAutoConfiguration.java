package io.github.afgprojects.framework.starter.transaction;

import io.github.afgprojects.framework.data.core.transaction.TransactionAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 事务自动配置类
 * <p>
 * 自动配置 Spring 事务适配器，使框架的 {@link TransactionAdapter}
 * 与 Spring 的声明式事务（@Transactional）兼容。
 * <p>
 * 自动配置条件：
 * <ul>
 *   <li>存在 {@link PlatformTransactionManager} Bean</li>
 *   <li>存在 {@link TransactionAdapter} 类（来自 afg-data-core）</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>
 * &#64;Autowired
 * private TransactionAdapter transactionAdapter;
 *
 * // 编程式事务
 * transactionAdapter.executeInTransaction(() -> {
 *     dataManager.entity(User.class).save(user);
 *     dataManager.entity(Order.class).save(order);
 * });
 *
 * // 只读事务
 * List&lt;User&gt; users = transactionAdapter.executeInReadOnly(() ->
 *     dataManager.entity(User.class).findAll()
 * );
 * </pre>
 * <p>
 * 如果用户需要自定义事务适配器，只需定义自己的 {@link TransactionAdapter} Bean，
 * 本自动配置将自动退避。
 *
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass({PlatformTransactionManager.class, TransactionAdapter.class})
@ConditionalOnSingleCandidate(PlatformTransactionManager.class)
public class TransactionAutoConfiguration {

    /**
     * 注册 Spring 事务适配器
     * <p>
     * 当存在唯一的 {@link PlatformTransactionManager} 时自动配置，
     * 如果用户已定义自己的 {@link TransactionAdapter}，则不创建。
     *
     * @param transactionManager Spring 事务管理器
     * @return Spring 事务适配器
     */
    @Bean
    @ConditionalOnMissingBean(TransactionAdapter.class)
    public SpringTransactionAdapter springTransactionAdapter(PlatformTransactionManager transactionManager) {
        return new SpringTransactionAdapter(transactionManager);
    }
}
