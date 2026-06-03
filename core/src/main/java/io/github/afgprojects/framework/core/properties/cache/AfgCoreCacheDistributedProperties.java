package io.github.afgprojects.framework.core.properties.cache;

import lombok.Data;

/**
 * 分布式缓存配置。
 */
@Data
public class AfgCoreCacheDistributedProperties {

    private boolean enabled = true;
    private String keyPrefix = "afg:cache:";
    private long defaultTtl = 0;
}
