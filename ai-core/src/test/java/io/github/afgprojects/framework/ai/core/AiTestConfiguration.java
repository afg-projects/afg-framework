package io.github.afgprojects.framework.ai.core;

import io.github.afgprojects.framework.ai.core.AiCoreModuleConfig;
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
import io.github.afgprojects.framework.core.autoconfigure.AfgAutoConfiguration;
import io.github.afgprojects.framework.core.autoconfigure.AfgCoreAutoConfiguration;
import io.github.afgprojects.framework.core.autoconfigure.IdGeneratorAutoConfiguration;
import io.github.afgprojects.framework.core.autoconfigure.ModuleWebAutoConfiguration;
import io.github.afgprojects.framework.core.autoconfigure.WebAutoConfiguration;
import io.github.afgprojects.framework.data.jdbc.autoconfigure.DataManagerAutoConfiguration;
import io.github.afgprojects.framework.data.liquibase.autoconfigure.LiquibaseAutoConfiguration;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.servlet.autoconfigure.HttpEncodingAutoConfiguration;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;

/**
 * AI 模块测试配置类
 *
 * <p>作为 @SpringBootTest 的配置入口（@SpringBootConfiguration），
 * 显式导入完整的自动配置链：DataSource → DataManager → Liquibase → Web → AI 全模块。
 *
 * <p>注意：@ImportAutoConfiguration 不会自动解析 @AutoConfigureAfter 引用的配置类，
 * 因此需要显式列出所有前置自动配置，包括 Web 服务器相关配置和 AFG Core 基础设施。
 */
@SpringBootConfiguration
@ImportAutoConfiguration({
    // Spring Boot JDBC 基础设施（Spring Boot 4 独立模块）
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    JdbcTemplateAutoConfiguration.class,
    // AFG Core 基础设施（ModuleRegistry、AfgCoreProperties 等）
    AfgAutoConfiguration.class,
    AfgCoreAutoConfiguration.class,
    // ID 生成器（Snowflake），显式列出：@ImportAutoConfiguration 不会自动发现 AutoConfiguration.imports
    IdGeneratorAutoConfiguration.class,
    // AI 模块配置（触发 @ComponentScan 扫描 controller/service 等组件）
    AiCoreModuleConfig.class,
    // AFG 数据层
    DataManagerAutoConfiguration.class,
    LiquibaseAutoConfiguration.class,
    // Web 服务器基础设施（@ImportAutoConfiguration 不自动解析 @AutoConfigureAfter）
    JacksonAutoConfiguration.class,
    HttpMessageConvertersAutoConfiguration.class,
    RestClientAutoConfiguration.class,
    HttpEncodingAutoConfiguration.class,
    TomcatServletWebServerAutoConfiguration.class,
    DispatcherServletAutoConfiguration.class,
    WebMvcAutoConfiguration.class,
    ModuleWebAutoConfiguration.class,
    WebAutoConfiguration.class,
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
