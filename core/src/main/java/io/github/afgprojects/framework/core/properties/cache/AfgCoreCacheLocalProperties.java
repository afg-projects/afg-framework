package io.github.afgprojects.framework.core.properties.cache;

import java.time.Duration;

import org.jspecify.annotations.Nullable;

import lombok.Data;

/**
 * 本地缓存配置。
 */
@Data
public class AfgCoreCacheLocalProperties {

    private boolean enabled = true;
    private int initialCapacity = 128;
    private int maximumSize = 10000;
    private @Nullable Duration expireAfterWrite;
    private @Nullable Duration expireAfterAccess;
    private boolean recordStats = true;
}
