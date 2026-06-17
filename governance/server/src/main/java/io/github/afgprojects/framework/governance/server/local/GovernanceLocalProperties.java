package io.github.afgprojects.framework.governance.server.local;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 本地模式属性配置
 * <p>
 * 当 governance-server 和 governance-client 在同一个 JVM 时，
 * 通过 Spring Bean 直接调用替代 gRPC 通信。
 *
 * @author afg-projects
 */
@Data
@ConfigurationProperties(prefix = "afg.governance.local")
public class GovernanceLocalProperties {

    /**
     * 是否启用本地模式
     */
    private boolean enabled = false;

    /**
     * 服务名称，默认取 spring.application.name
     */
    private String serviceName;

    /**
     * 环境，默认 dev
     */
    private String environment = "dev";

    /**
     * 是否自动注册服务，默认 true
     */
    private boolean autoRegister = true;
}
