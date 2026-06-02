package io.github.afgprojects.framework.ai.agent.autoconfigure;

import io.github.afgprojects.framework.ai.agent.tool.PersistentToolRegistry;
import io.github.afgprojects.framework.ai.core.api.tool.ToolRegistry;
import io.github.afgprojects.framework.data.core.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 持久化工具注册表自动配置
 *
 * <p>当 DataManager 可用且配置启用时，使用基于数据库的持久化工具注册表
 * 替代默认的内存注册表。
 *
 * <p>配置示例：
 * <pre>{@code
 * afg:
 *   ai:
 *     tool:
 *       persistent-registry:
 *         enabled: true
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnBean(DataManager.class)
@ConditionalOnProperty(prefix = "afg.ai.tool.persistent-registry", name = "enabled", havingValue = "true", matchIfMissing = false)
public class PersistentToolRegistryAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(PersistentToolRegistryAutoConfiguration.class);

    /**
     * 配置持久化工具注册表
     *
     * @param dataManager 数据操作管理器
     * @return 持久化工具注册表实例
     */
    @Bean
    @ConditionalOnMissingBean(ToolRegistry.class)
    public ToolRegistry persistentToolRegistry(DataManager dataManager) {
        log.info("Creating persistent tool registry with DataManager");
        return new PersistentToolRegistry(dataManager);
    }
}
