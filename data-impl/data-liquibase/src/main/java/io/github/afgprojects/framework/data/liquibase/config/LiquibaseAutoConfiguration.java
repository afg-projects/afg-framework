package io.github.afgprojects.framework.data.liquibase.config;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Liquibase 自动配置
 * <p>
 * 统一配置 SpringLiquibase，使用 db/changelog/changelog.xml 作为入口。
 * 各模块只需在 db/changelog/ 目录下创建模块目录和迁移脚本。
 */
@Configuration
public class LiquibaseAutoConfiguration {

    @Value("${spring.liquibase.enabled:true}")
    private boolean enabled;

    @Value("${spring.liquibase.drop-first:false}")
    private boolean dropFirst;

    @Bean
    @ConditionalOnMissingBean
    public SpringLiquibase liquibase(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:db/changelog/changelog.xml");
        liquibase.setShouldRun(enabled);
        liquibase.setDropFirst(dropFirst);
        return liquibase;
    }
}
