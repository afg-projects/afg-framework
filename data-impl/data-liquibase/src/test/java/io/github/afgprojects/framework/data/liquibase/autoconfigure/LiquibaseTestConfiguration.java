package io.github.afgprojects.framework.data.liquibase.autoconfigure;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;

/**
 * Liquibase 测试配置类
 * <p>
 * 显式导入 Liquibase 依赖的自动配置链：DataSource -> TransactionManager -> Liquibase。
 * <p>
 * 注意：@ImportAutoConfiguration 不会自动解析 @AutoConfigureAfter 引用的配置类，
 * 因此需要显式列出所有前置自动配置。
 */
@SpringBootConfiguration
@ImportAutoConfiguration({
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    LiquibaseAutoConfiguration.class
})
public class LiquibaseTestConfiguration {
}
