package io.github.afgprojects.framework.starter.transaction;

import io.github.afgprojects.framework.data.core.transaction.TransactionAdapter;
import org.jspecify.annotations.NonNull;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * Spring 声明式事务适配器
 * <p>
 * 提供与 Spring {@link PlatformTransactionManager} 的集成，
 * 使框架的事务管理与 Spring 的声明式事务（@Transactional）兼容。
 * <p>
 * 使用示例：
 * <pre>
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
 */
public class SpringTransactionAdapter implements TransactionAdapter {

    private final PlatformTransactionManager transactionManager;
    private final TransactionTemplate transactionTemplate;

    /**
     * 创建事务适配器
     *
     * @param transactionManager Spring 事务管理器
     */
    public SpringTransactionAdapter(@NonNull PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void executeInTransaction(@NonNull Runnable action) {
        transactionTemplate.executeWithoutResult(status -> action.run());
    }

    @Override
    public <T> T executeInTransaction(@NonNull Supplier<T> action) {
        return transactionTemplate.execute(status -> action.get());
    }

    @Override
    public <T> T executeInReadOnly(@NonNull Supplier<T> action) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setReadOnly(true);
        TransactionTemplate readOnlyTemplate = new TransactionTemplate(transactionManager, def);
        return readOnlyTemplate.execute(status -> action.get());
    }

    /**
     * 在指定事务定义下执行操作
     *
     * @param definition 事务定义
     * @param action     要执行的操作
     */
    public void executeInTransaction(@NonNull TransactionDefinition definition, @NonNull Runnable action) {
        TransactionTemplate template = new TransactionTemplate(transactionManager, definition);
        template.executeWithoutResult(status -> action.run());
    }

    /**
     * 在指定事务定义下执行操作并返回结果
     *
     * @param definition 事务定义
     * @param action     要执行的操作
     * @param <T>        返回类型
     * @return 操作结果
     */
    public <T> T executeInTransaction(@NonNull TransactionDefinition definition, @NonNull Supplier<T> action) {
        TransactionTemplate template = new TransactionTemplate(transactionManager, definition);
        return template.execute(status -> action.get());
    }

    /**
     * 获取底层的事务管理器
     *
     * @return Spring 事务管理器
     */
    @NonNull
    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    /**
     * 获取事务模板
     *
     * @return 事务模板
     */
    @NonNull
    public TransactionTemplate getTransactionTemplate() {
        return transactionTemplate;
    }
}