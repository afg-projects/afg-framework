package io.github.afgprojects.framework.data.jdbc;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;

import io.github.afgprojects.framework.data.jdbc.autoconfigure.DataManagerAutoConfiguration;
import io.github.afgprojects.framework.data.jdbc.encryption.EncryptionAutoConfiguration;
import io.github.afgprojects.framework.data.liquibase.autoconfigure.LiquibaseAutoConfiguration;

/**
 * 测试配置类
 * <p>
 * 作为 @SpringBootTest 的配置入口（@SpringBootConfiguration），
 * 显式导入完整的 JDBC 自动配置链：
 * DataSource → TransactionManager → JdbcTemplate → DataManager → Liquibase。
 * </p>
 * <p>
 * 注意：@ImportAutoConfiguration 不会自动解析 @AutoConfigureAfter 引用的配置类，
 * 因此需要显式列出所有前置自动配置。
 * </p>
 */
@SpringBootConfiguration
@ImportAutoConfiguration({
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    JdbcTemplateAutoConfiguration.class,
    DataManagerAutoConfiguration.class,
    EncryptionAutoConfiguration.class,
    LiquibaseAutoConfiguration.class
})
public class JdbcDataTestConfiguration {
}