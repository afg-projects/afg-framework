package io.github.afgprojects.framework.ai.langchain4j.autoconfigure;

import dev.langchain4j.agent.tool.ToolSpecification;
import io.github.afgprojects.framework.ai.core.api.tool.Tool;
import io.github.afgprojects.framework.ai.langchain4j.config.Lc4jProperties;
import io.github.afgprojects.framework.ai.langchain4j.tool.Lc4jToolAdapter;
import io.github.afgprojects.framework.ai.langchain4j.tool.Lc4jToolNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * LangChain4j Tool 自动配置
 *
 * <p>当 classpath 上存在 LangChain4j {@link ToolSpecification} 且
 * {@code afg.ai.langchain4j.enabled=true} 时自动激活。
 * 提供 AFG Tool 到 LangChain4j ToolSpecification 的适配能力。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(Lc4jProperties.class)
@ConditionalOnClass(name = "dev.langchain4j.agent.tool.ToolSpecification")
@ConditionalOnProperty(prefix = "afg.ai.langchain4j", name = "enabled", havingValue = "true", matchIfMissing = true)
public class Lc4jToolAutoConfiguration {

    /**
     * 将所有 AFG Tool 转换为 LangChain4j ToolSpecification 列表
     * <p>
     * 仅当存在 Tool Bean 时才创建
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(Tool.class)
    public List<ToolSpecification> lc4jToolSpecifications(List<Tool<?, ?>> tools) {
        log.info("Converting {} AFG Tools to LangChain4j ToolSpecifications", tools.size());
        List<Lc4jToolNode> nodes = Lc4jToolNode.fromTools(tools);
        return Lc4jToolNode.toToolSpecifications(nodes);
    }
}
