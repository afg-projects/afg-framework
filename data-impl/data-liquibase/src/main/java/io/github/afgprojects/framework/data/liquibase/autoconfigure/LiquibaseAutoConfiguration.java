package io.github.afgprojects.framework.data.liquibase.autoconfigure;

import liquibase.Scope;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;

/**
 * Liquibase 自动配置
 * <p>
 * 统一配置 SpringLiquibase，默认使用 classpath:db/changelog/changelog.xml 作为入口。
 * 各模块只需在 db/changelog/ 目录下创建模块目录和迁移脚本。
 * <p>
 * 必须在 DataSource 自动配置之后执行。
 * <p>
 * 配置项（前缀 afg.liquibase）：
 * - enabled: 是否启用（默认 true）
 * - change-log: ChangeLog 路径（默认 classpath:db/changelog/changelog.xml）
 * - drop-first: 是否先删除数据库（默认 false）
 * - contexts: 执行上下文过滤
 * - labels: 执行标签过滤
 * - default-schema: 默认 Schema
 */
@AutoConfiguration(afterName = {
    "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
    "io.github.afgprojects.framework.data.jdbc.autoconfigure.DataManagerAutoConfiguration"
})
@ConditionalOnClass(SpringLiquibase.class)
@ConditionalOnProperty(prefix = "afg.liquibase", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(LiquibaseProperties.class)
public class LiquibaseAutoConfiguration {

    /**
     * 创建 SpringLiquibase Bean
     * <p>
     * 使用 @Order 确保 Liquibase 在其他数据库初始化之后执行，
     * 避免与 Hibernate ddl-auto 等冲突。
     */
    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    @ConditionalOnMissingBean
    public SpringLiquibase liquibase(DataSource dataSource, LiquibaseProperties properties) {
        // Liquibase 5.x 默认 secureParsing=true，阻止 XSD 解析。
        // 通过系统属性禁用，让 Liquibase 的 EntityResolver 从 classpath 加载 XSD。
        System.setProperty("liquibase.secureParsing", "false");

        // 设置 Liquibase Scope 中的 ResourceAccessor，使用 Liquibase 自身的 ClassLoader。
        // 解决 Gradle bootRun / Java 25 环境下 ContextClassLoader 无法加载 jar 内
        // XSD 文件（www/liquibase.org/xml/ns/dbchangelog/）的问题。
        try {
            ClassLoader liquibaseCl = SpringLiquibase.class.getClassLoader();
            liquibase.resource.ResourceAccessor accessor =
                new liquibase.resource.ClassLoaderResourceAccessor(liquibaseCl);
            Scope.child(Scope.Attr.resourceAccessor, accessor, () -> {});
        } catch (Exception ignored) {
            // 如果 Scope 设置失败，Liquibase 会使用默认的 ResourceAccessor
        }

        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(properties.getChangeLog());
        liquibase.setShouldRun(properties.isEnabled());
        liquibase.setDropFirst(properties.isDropFirst());

        // 上下文和标签过滤
        if (properties.getContexts() != null && !properties.getContexts().isEmpty()) {
            liquibase.setContexts(properties.getContexts());
        }
        if (properties.getLabels() != null && !properties.getLabels().isEmpty()) {
            liquibase.setLabels(properties.getLabels());
        }

        // Schema 配置
        if (properties.getDefaultSchema() != null && !properties.getDefaultSchema().isEmpty()) {
            liquibase.setDefaultSchema(properties.getDefaultSchema());
        }

        // 表名配置
        liquibase.setDatabaseChangeLogTable(properties.getDatabaseChangeLogTable());
        liquibase.setDatabaseChangeLogLockTable(properties.getDatabaseChangeLogLockTable());

        return liquibase;
    }
}
