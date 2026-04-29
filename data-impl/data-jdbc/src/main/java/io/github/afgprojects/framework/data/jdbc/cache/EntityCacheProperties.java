package io.github.afgprojects.framework.data.jdbc.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 实体缓存配置属性
 * <p>
 * 配置示例：
 * <pre>{@code
 * afg:
 *   jdbc:
 *     cache:
 *       enabled: true
 *       ttl: 3600000      # 缓存过期时间（毫秒）
 *       max-size: 10000   # 最大缓存数量
 *       cache-null: true  # 是否缓存 null 值（防穿透）
 *       null-value-ttl: 60000  # 空值缓存过期时间（毫秒）
 * }</pre>
 * </p>
 */
@Data
@ConfigurationProperties(prefix = "afg.jdbc.cache")
public class EntityCacheProperties {

    /**
     * 是否启用二级缓存
     */
    private boolean enabled = false;

    /**
     * 缓存过期时间（毫秒）
     * <p>
     * 默认 0 表示永不过期
     * </p>
     */
    private long ttl = 0;

    /**
     * 最大缓存数量
     * <p>
     * 每个实体类型的最大缓存条目数
     * </p>
     */
    private int maxSize = 10000;

    /**
     * 是否缓存 null 值（防穿透）
     * <p>
     * 启用后，查询不到的实体也会缓存一个 null 标记，
     * 防止缓存穿透攻击
     * </p>
     */
    private boolean cacheNull = true;

    /**
     * 空值缓存过期时间（毫秒）
     * <p>
     * 为了防止缓存穿透，空值缓存使用较短的过期时间
     * </p>
     */
    private long nullValueTtl = 60000;
}