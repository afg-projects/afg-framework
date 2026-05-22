package io.github.afgprojects.framework.data.jdbc.autoconfigure;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.jdbc.JdbcDataManager;

/**
 * DataManager 自动配置
 * <p>
 * 当 DataSource 存在时自动配置 DataManager。
 * DataSource 由 Spring Boot 的 DataSource 自动配置提供。
 * </p>
 */
@AutoConfiguration
@ConditionalOnClass(name = {"javax.sql.DataSource", "org.springframework.jdbc.core.JdbcTemplate"})
@ConditionalOnProperty(prefix = "afg.data", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataManagerAutoConfiguration {

    /**
     * 创建 JdbcDataManager Bean。
     *
     * <p>同时注册为 DataManager 和 JdbcDataManager 类型，
     * 以便其他配置类可以通过具体类型引用。
     *
     * @param dataSource 数据源
     * @return JdbcDataManager 实例
     */
    @Bean
    @ConditionalOnMissingBean({DataManager.class, JdbcDataManager.class})
    public JdbcDataManager dataManager(DataSource dataSource) {
        return new JdbcDataManager(dataSource);
    }
}
