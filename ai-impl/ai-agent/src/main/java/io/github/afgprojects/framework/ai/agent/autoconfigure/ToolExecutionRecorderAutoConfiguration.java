package io.github.afgprojects.framework.ai.agent.autoconfigure;

import io.github.afgprojects.framework.ai.agent.tool.execution.JdbcToolExecutionRecorder;
import io.github.afgprojects.framework.ai.agent.tool.execution.NoOpToolExecutionRecorder;
import io.github.afgprojects.framework.ai.core.api.tool.ToolExecutionRecorder;
import io.github.afgprojects.framework.data.core.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 工具执行记录器自动配置
 *
 * <p>配置工具执行记录器，用于记录工具执行的生命周期事件。
 *
 * <p>配置示例：
 * <pre>{@code
 * afg:
 *   ai:
 *     tool:
 *       execution-recorder:
 *         enabled: true
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "afg.ai.tool.execution-recorder", name = "enabled", havingValue = "true", matchIfMissing = false)
public class ToolExecutionRecorderAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutionRecorderAutoConfiguration.class);

    /**
     * 配置基于 DataManager 的工具执行记录器
     *
     * <p>当 DataManager 可用时，使用数据库持久化执行记录。
     *
     * @param dataManager 数据操作管理器
     * @return JDBC 工具执行记录器
     */
    @Bean
    @ConditionalOnBean(DataManager.class)
    @ConditionalOnMissingBean(ToolExecutionRecorder.class)
    public ToolExecutionRecorder jdbcToolExecutionRecorder(@Autowired DataManager dataManager) {
        log.info("Creating JDBC tool execution recorder with DataManager");
        return new JdbcToolExecutionRecorder(dataManager);
    }

    /**
     * 配置空操作工具执行记录器
     *
     * <p>当 DataManager 不可用时的降级实现。
     *
     * @return 空操作工具执行记录器
     */
    @Bean
    @ConditionalOnMissingBean({DataManager.class, ToolExecutionRecorder.class})
    public ToolExecutionRecorder noOpToolExecutionRecorder() {
        log.info("Creating NoOp tool execution recorder (DataManager not available)");
        return new NoOpToolExecutionRecorder();
    }
}
