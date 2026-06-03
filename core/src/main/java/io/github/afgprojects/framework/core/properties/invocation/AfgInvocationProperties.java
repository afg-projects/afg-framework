package io.github.afgprojects.framework.core.properties.invocation;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Bean 调用配置属性。
 *
 * @since 1.1.0
 */
@Data
@ConfigurationProperties(prefix = "afg.invocation")
public class AfgInvocationProperties {

    private boolean enabled = true;
}