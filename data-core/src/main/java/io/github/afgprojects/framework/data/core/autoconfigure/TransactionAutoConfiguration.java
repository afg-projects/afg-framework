package io.github.afgprojects.framework.data.core.autoconfigure;

import io.github.afgprojects.framework.data.core.transaction.TransactionAdapter;
import io.github.afgprojects.framework.data.core.autoconfigure.transaction.SpringTransactionAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

@AutoConfiguration
@ConditionalOnClass({PlatformTransactionManager.class, TransactionAdapter.class})
@ConditionalOnSingleCandidate(PlatformTransactionManager.class)
public class TransactionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TransactionAdapter.class)
    public SpringTransactionAdapter springTransactionAdapter(PlatformTransactionManager transactionManager) {
        return new SpringTransactionAdapter(transactionManager);
    }
}