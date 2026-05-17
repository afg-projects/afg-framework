package io.github.afgprojects.framework.integration.governance.config;

import io.github.afgprojects.framework.integration.governance.client.GovernanceClient;
import io.github.afgprojects.framework.integration.governance.client.GovernanceClientProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import jakarta.annotation.PreDestroy;

/**
 * Governance Client 自动配置
 *
 * @author afg-projects
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(GovernanceClientProperties.class)
@ConditionalOnProperty(prefix = "afg.governance.client", name = "enabled", havingValue = "true")
public class GovernanceClientAutoConfiguration {

    private GovernanceClient governanceClient;

    @Bean
    public GovernanceClient governanceClient(GovernanceClientProperties properties) {
        log.info("Initializing GovernanceClient with server: {}", properties.getServerAddr());
        this.governanceClient = new GovernanceClient(properties);
        this.governanceClient.start();
        return this.governanceClient;
    }

    @PreDestroy
    public void destroy() {
        if (governanceClient != null) {
            governanceClient.stop();
        }
    }
}
