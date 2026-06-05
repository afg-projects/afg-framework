package io.github.afgprojects.framework.data.core.autoconfigure;

import io.github.afgprojects.framework.data.core.transaction.TransactionAdapter;
import io.github.afgprojects.framework.data.core.autoconfigure.transaction.SpringTransactionAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 事务适配器自动配置
 * <p>
 * 当 PlatformTransactionManager 存在时，自动创建 SpringTransactionAdapter，
 * 供 DataManager 等组件使用。
 * <p>
 * 必须在 DataSourceTransactionManagerAutoConfiguration 之后执行，
 * 确保 PlatformTransactionManager Bean 已创建。
 */
@AutoConfiguration(afterName = "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration")
@ConditionalOnClass({PlatformTransactionManager.class, TransactionAdapter.class})
@ConditionalOnSingleCandidate(PlatformTransactionManager.class)
public class TransactionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TransactionAdapter.class)
    public SpringTransactionAdapter springTransactionAdapter(PlatformTransactionManager transactionManager) {
        return new SpringTransactionAdapter(transactionManager);
    }
}