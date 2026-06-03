package io.github.afgprojects.framework.data.jdbc.properties.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 实体缓存配置属性
 */
@Data
@ConfigurationProperties(prefix = "afg.jdbc.cache")
public class EntityCacheProperties {

    private boolean enabled = false;
    private long ttl = 0;
    private int maxSize = 10000;
    private boolean cacheNull = true;
    private long nullValueTtl = 60000;
}
