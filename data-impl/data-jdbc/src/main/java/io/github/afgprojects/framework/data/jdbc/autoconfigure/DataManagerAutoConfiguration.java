package io.github.afgprojects.framework.data.jdbc.autoconfigure;

import javax.sql.DataSource;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.context.TenantContextHolder;
import io.github.afgprojects.framework.data.core.entity.AuditableContext;
import io.github.afgprojects.framework.data.core.entity.FieldEncryptor;
import io.github.afgprojects.framework.data.core.entity.NoOpAuditableContext;
import io.github.afgprojects.framework.data.core.entity.NoOpFieldEncryptor;
import io.github.afgprojects.framework.data.core.mapper.TypeHandlerRegistry;
import io.github.afgprojects.framework.data.core.transaction.TransactionAdapter;
import io.github.afgprojects.framework.data.jdbc.JdbcDataManager;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * DataManager 自动配置
 * <p>
 * 当 DataSource 存在时自动配置 DataManager、TypeHandlerRegistry，
 * 并注入 TenantContextHolder 和 TransactionAdapter（如果可用）。
 * <p>
 * 必须在 DataSource 和 TransactionManager 自动配置之后执行。
 */
@AutoConfiguration(after = {
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    io.github.afgprojects.framework.data.core.autoconfigure.TenantContextAutoConfiguration.class,
    io.github.afgprojects.framework.data.core.autoconfigure.TransactionAutoConfiguration.class
})
@ConditionalOnClass(name = {"javax.sql.DataSource", "org.springframework.jdbc.core.JdbcTemplate"})
@ConditionalOnProperty(prefix = "afg.data", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataManagerAutoConfiguration {

    /**
     * 创建 TypeHandlerRegistry Bean（默认注册表）
     */
    @Bean
    @ConditionalOnMissingBean(TypeHandlerRegistry.class)
    public TypeHandlerRegistry typeHandlerRegistry() {
        return TypeHandlerRegistry.defaultRegistry();
    }

    /**
     * 创建 NoOpAuditableContext Bean（降级实现）
     * <p>
     * 当容器中不存在自定义 {@link AuditableContext} 实现时，
     * 使用 NoOp 降级实现，返回 null（不自动填充审计字段）。
     *
     * @return NoOpAuditableContext 实例
     */
    @Bean
    @ConditionalOnMissingBean(AuditableContext.class)
    public NoOpAuditableContext noOpAuditableContext() {
        return new NoOpAuditableContext();
    }

    /**
     * 创建 NoOpFieldEncryptor Bean（降级实现）
     * <p>
     * 当容器中不存在自定义 {@link FieldEncryptor} 实现时，
     * 使用 NoOp 降级实现，直接返回原文（不加密/解密）。
     *
     * @return NoOpFieldEncryptor 实例
     */
    @Bean
    @ConditionalOnMissingBean(FieldEncryptor.class)
    public NoOpFieldEncryptor noOpFieldEncryptor() {
        return new NoOpFieldEncryptor();
    }

    /**
     * 创建 JdbcDataManager Bean。
     *
     * <p>同时注册为 DataManager 和 JdbcDataManager 类型，
     * 以便其他配置类可以通过具体类型引用。
     *
     * <p>自动注入 TenantContextHolder、TransactionAdapter、AuditableContext 和 FieldEncryptor（如果容器中存在），
     * 确保与 TenantContextAutoConfiguration、TransactionAutoConfiguration、自定义审计上下文和加密实现协同工作。
     *
     * @param dataSource          数据源
     * @param typeHandlerRegistry 类型处理器注册表
     * @param transactionManager  Spring 事务管理器（自动注入）
     * @param tenantContextHolder 租户上下文持有者（可选，自动注入）
     * @param transactionAdapter  事务适配器（可选，自动注入）
     * @param auditableContext    审计上下文（可选，自动注入）
     * @param fieldEncryptor      字段加密器（可选，自动注入）
     * @return JdbcDataManager 实例
     */
    @Bean
    @ConditionalOnMissingBean({DataManager.class, JdbcDataManager.class})
    public JdbcDataManager dataManager(DataSource dataSource,
                                        TypeHandlerRegistry typeHandlerRegistry,
                                        PlatformTransactionManager transactionManager,
                                        @Nullable TenantContextHolder tenantContextHolder,
                                        @Nullable TransactionAdapter transactionAdapter,
                                        @Nullable AuditableContext auditableContext,
                                        @Nullable FieldEncryptor fieldEncryptor) {
        JdbcDataManager dm = new JdbcDataManager(dataSource);
        dm.setTypeHandlerRegistry(typeHandlerRegistry);
        dm.setTransactionManager(transactionManager);
        if (tenantContextHolder != null) {
            dm.setTenantContextHolder(tenantContextHolder);
        }
        if (transactionAdapter != null) {
            dm.setTransactionAdapter(transactionAdapter);
        }
        if (auditableContext != null) {
            dm.setAuditableContext(auditableContext);
        }
        if (fieldEncryptor != null) {
            dm.setFieldEncryptor(fieldEncryptor);
        }
        return dm;
    }
}
