package io.github.afgprojects.framework.core.properties.security;

import lombok.Data;

/**
 * SQL 注入防护配置。
 */
@Data
public class AfgCoreSqlInjectionProperties {

    private boolean enabled = true;
    private boolean rejectOnDetection = true;
    private boolean logDetection = true;
}
