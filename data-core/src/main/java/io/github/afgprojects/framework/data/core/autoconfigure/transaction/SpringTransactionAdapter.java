package io.github.afgprojects.framework.data.core.autoconfigure.transaction;

import io.github.afgprojects.framework.data.core.transaction.TransactionAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Spring {@link TransactionAdapter} 实现，基于 {@link PlatformTransactionManager}。
 */
public class SpringTransactionAdapter implements TransactionAdapter {

    private final TransactionTemplate transactionTemplate;

    public SpringTransactionAdapter(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T executeInTransaction(java.util.function.Supplier<T> action) {
        return transactionTemplate.execute(status -> action.get());
    }

    @Override
    public void executeInTransaction(Runnable action) {
        transactionTemplate.executeWithoutResult(status -> action.run());
    }

    @Override
    public <T> T executeInReadOnly(java.util.function.Supplier<T> action) {
        TransactionTemplate readOnlyTemplate = new TransactionTemplate(transactionTemplate.getTransactionManager());
        readOnlyTemplate.setReadOnly(true);
        return readOnlyTemplate.execute(status -> action.get());
    }
}