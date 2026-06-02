package io.github.afgprojects.framework.ai.agent.skill.autoconfigure;

import io.github.afgprojects.framework.ai.agent.skill.PersistentSkillRegistry;
import io.github.afgprojects.framework.ai.agent.skill.SkillRegistry;
import io.github.afgprojects.framework.data.core.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 持久化 Skill 注册表自动配置
 *
 * <p>当 {@link DataManager} 存在时，自动创建 {@link PersistentSkillRegistry}，
 * 优先级高于 {@link DefaultSkillRegistry}（内存实现）。
 *
 * <p>条件装配逻辑：
 * <ul>
 *   <li>必须存在 DataManager Bean（即项目引入了 data-jdbc 模块）</li>
 *   <li>必须不存在其他 SkillRegistry Bean（允许用户自定义覆盖）</li>
 *   <li>配置项 afg.ai.skill.persistent-enabled=true 时生效</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration(after = SkillAutoConfiguration.class)
@ConditionalOnBean(DataManager.class)
@ConditionalOnProperty(prefix = "afg.ai.skill", name = "persistent-enabled", havingValue = "true")
public class PersistentSkillRegistryAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(PersistentSkillRegistryAutoConfiguration.class);

    /**
     * 创建持久化 Skill 注册表
     *
     * @param dataManager 数据操作管理器
     * @return PersistentSkillRegistry 实例
     */
    @Bean
    @ConditionalOnMissingBean(SkillRegistry.class)
    public SkillRegistry persistentSkillRegistry(DataManager dataManager) {
        log.info("Creating persistent skill registry with DataManager");
        return new PersistentSkillRegistry(dataManager);
    }
}
