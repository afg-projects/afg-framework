package io.github.afgprojects.framework.ai.core;

import io.github.afgprojects.framework.ai.core.autoconfigure.AiCoreAutoConfiguration;
import io.github.afgprojects.framework.ai.core.autoconfigure.AiChatAutoConfiguration;
import io.github.afgprojects.framework.ai.core.autoconfigure.AiAgentAutoConfiguration;
import io.github.afgprojects.framework.ai.core.autoconfigure.AiModelAutoConfiguration;
import io.github.afgprojects.framework.ai.core.autoconfigure.AiWorkflowAutoConfiguration;
import io.github.afgprojects.framework.ai.core.autoconfigure.AiPipelineAutoConfiguration;
import io.github.afgprojects.framework.ai.core.autoconfigure.AiPersistenceAutoConfiguration;
import io.github.afgprojects.framework.ai.core.autoconfigure.AiResilienceAutoConfiguration;
import io.github.afgprojects.framework.ai.core.autoconfigure.AiPerformanceAutoConfiguration;
import io.github.afgprojects.framework.ai.core.autoconfigure.AiSecurityAutoConfiguration;
import io.github.afgprojects.framework.ai.core.autoconfigure.AiObservabilityAutoConfiguration;
import io.github.afgprojects.framework.ai.core.autoconfigure.AiRagAutoConfiguration;
import io.github.afgprojects.framework.ai.core.autoconfigure.AiEtlAutoConfiguration;
import io.github.afgprojects.framework.ai.core.autoconfigure.AiToolAutoConfiguration;
import io.github.afgprojects.framework.ai.core.autoconfigure.AiSkillAutoConfiguration;
import io.github.afgprojects.framework.ai.core.autoconfigure.AiEntityAutoConfiguration;
import io.github.afgprojects.framework.data.jdbc.autoconfigure.DataManagerAutoConfiguration;
import io.github.afgprojects.framework.data.liquibase.autoconfigure.LiquibaseAutoConfiguration;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;

/**
 * AI 模块测试配置类
 *
 * <p>作为 @SpringBootTest 的配置入口（@SpringBootConfiguration），
 * 显式导入完整的自动配置链：DataSource → DataManager → Liquibase → AI 全模块。
 *
 * <p>Web 相关的自动配置（Servlet、Jackson、HttpMessageConverters）
 * 由 spring-boot-starter-web 自动激活，无需显式导入。
 *
 * <p>注意：@ImportAutoConfiguration 不会自动解析 @AutoConfigureAfter 引用的配置类，
 * 因此需要显式列出所有前置自动配置。
 */
@SpringBootConfiguration
@ImportAutoConfiguration({
    // Spring Boot JDBC 基础设施（Spring Boot 4 独立模块）
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    JdbcTemplateAutoConfiguration.class,
    // AFG 数据层
    DataManagerAutoConfiguration.class,
    LiquibaseAutoConfiguration.class,
    // AFG AI 模块
    AiCoreAutoConfiguration.class,
    AiChatAutoConfiguration.class,
    AiAgentAutoConfiguration.class,
    AiModelAutoConfiguration.class,
    AiWorkflowAutoConfiguration.class,
    AiPipelineAutoConfiguration.class,
    AiPersistenceAutoConfiguration.class,
    AiResilienceAutoConfiguration.class,
    AiPerformanceAutoConfiguration.class,
    AiSecurityAutoConfiguration.class,
    AiObservabilityAutoConfiguration.class,
    AiRagAutoConfiguration.class,
    AiEtlAutoConfiguration.class,
    AiToolAutoConfiguration.class,
    AiSkillAutoConfiguration.class,
    AiEntityAutoConfiguration.class
})
public class AiTestConfiguration {
}