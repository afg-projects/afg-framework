package io.github.afgprojects.framework.data.jdbc.autoconfigure;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.jdbc.JdbcDataManager;

/**
 * DataManager 自动配置
 * <p>
 * 当 DataSource 和 JdbcTemplate 存在时自动配置 DataManager。
 * </p>
 */
@AutoConfiguration
@ConditionalOnClass({DataSource.class, JdbcTemplate.class})
@ConditionalOnProperty(prefix = "afg.data", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataManagerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DataManager.class)
    public DataManager dataManager(DataSource dataSource) {
        return new JdbcDataManager(dataSource);
    }
}
