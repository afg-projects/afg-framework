package io.github.afgprojects.framework.governance.server.config;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.governance.server.GovernanceServerModuleConfig;
import io.github.afgprojects.framework.governance.server.properties.GovernanceServerProperties;
import io.github.afgprojects.framework.governance.server.properties.GovernanceServerSecurityProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * 服务治理服务端自动配置
 */
@AutoConfiguration(afterName = {
    "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
    "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration"
})
@ConditionalOnBean(DataManager.class)
@ConditionalOnProperty(prefix = "afg.governance.server", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({GovernanceServerProperties.class, GovernanceServerSecurityProperties.class})
@ComponentScan(basePackageClasses = GovernanceServerModuleConfig.class)
public class GovernanceServerAutoConfiguration {
}