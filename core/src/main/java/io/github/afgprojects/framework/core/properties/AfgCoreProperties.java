package io.github.afgprojects.framework.core.properties;

import io.github.afgprojects.framework.core.properties.audit.AfgCoreAuditProperties;
import io.github.afgprojects.framework.core.properties.batch.AfgCoreBatchProperties;
import io.github.afgprojects.framework.core.properties.cache.AfgCoreCacheProperties;
import io.github.afgprojects.framework.core.properties.cloudnative.AfgCoreCloudNativeProperties;
import io.github.afgprojects.framework.core.properties.datasource.AfgCoreDataSourceProperties;
import io.github.afgprojects.framework.core.properties.datascope.AfgCoreDataScopeProperties;
import io.github.afgprojects.framework.core.properties.encryption.AfgCoreEncryptionProperties;
import io.github.afgprojects.framework.core.properties.event.AfgCoreEventProperties;
import io.github.afgprojects.framework.core.properties.feature.AfgCoreFeatureProperties;
import io.github.afgprojects.framework.core.properties.health.AfgCoreHealthProperties;
import io.github.afgprojects.framework.core.properties.httpclient.AfgCoreHttpClientProperties;
import io.github.afgprojects.framework.core.properties.lock.AfgCoreLockProperties;
import io.github.afgprojects.framework.core.properties.logging.AfgCoreLoggingProperties;
import io.github.afgprojects.framework.core.properties.metrics.AfgCoreMetricsProperties;
import io.github.afgprojects.framework.core.properties.ratelimit.AfgCoreRateLimitProperties;
import io.github.afgprojects.framework.core.properties.scheduler.AfgCoreSchedulerProperties;
import io.github.afgprojects.framework.core.properties.security.AfgCoreSecurityProperties;
import io.github.afgprojects.framework.core.properties.shutdown.AfgCoreShutdownProperties;
import io.github.afgprojects.framework.core.properties.tracing.AfgCoreTracingProperties;
import io.github.afgprojects.framework.core.properties.virtualthread.AfgCoreVirtualThreadProperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * AFG Core 统一配置属性。
 *
 * <p>整合了缓存、事件、锁、数据源、安全、健康检查、限流、调度器、追踪等所有核心配置。
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   core:
 *     enabled: true
 *     cache:
 *       enabled: true
 *       type: multi-level
 *       default-ttl: 3600000
 *     event:
 *       enabled: true
 *       type: LOCAL
 *     lock:
 *       enabled: true
 *       key-prefix: "afg:lock"
 *     datasource:
 *       enabled: false
 *       primary: master
 *     security:
 *       xss:
 *         enabled: true
 *       signature:
 *         enabled: true
 *     health:
 *       liveness-enabled: true
 *       readiness-enabled: true
 *     rate-limit:
 *       enabled: true
 *       default-rate: 10
 *     scheduler:
 *       enabled: true
 *     tracing:
 *       enabled: true
 *     logging:
 *       mask-sensitive: true
 *     metrics:
 *       enabled: true
 *     virtual-thread:
 *       enabled: true
 * </pre>
 *
 * @since 1.1.0
 */
@Data
@ConfigurationProperties(prefix = "afg.core")
@SuppressWarnings("PMD.TooManyFields")
public class AfgCoreProperties {

    /**
     * 是否启用 AFG Core 功能。
     * 默认启用。
     */
    private boolean enabled = true;

    // ========== 缓存配置 ==========

    /**
     * 缓存配置。
     */
    private AfgCoreCacheProperties cache = new AfgCoreCacheProperties();

    // ========== 事件配置 ==========

    /**
     * 事件配置。
     */
    private AfgCoreEventProperties event = new AfgCoreEventProperties();

    // ========== 分布式锁配置 ==========

    /**
     * 分布式锁配置。
     */
    private AfgCoreLockProperties lock = new AfgCoreLockProperties();

    // ========== 数据源配置 ==========

    /**
     * 多数据源配置。
     */
    private AfgCoreDataSourceProperties datasource = new AfgCoreDataSourceProperties();

    // ========== 安全配置 ==========

    /**
     * 安全配置。
     */
    private AfgCoreSecurityProperties security = new AfgCoreSecurityProperties();

    // ========== 健康检查配置 ==========

    /**
     * 健康检查配置。
     */
    private AfgCoreHealthProperties health = new AfgCoreHealthProperties();

    // ========== 限流配置 ==========

    /**
     * 限流配置。
     */
    private AfgCoreRateLimitProperties rateLimit = new AfgCoreRateLimitProperties();

    // ========== 调度器配置 ==========

    /**
     * 调度器配置。
     */
    private AfgCoreSchedulerProperties scheduler = new AfgCoreSchedulerProperties();

    // ========== 追踪配置 ==========

    /**
     * 追踪配置。
     */
    private AfgCoreTracingProperties tracing = new AfgCoreTracingProperties();

    // ========== 日志配置 ==========

    /**
     * 日志配置。
     */
    private AfgCoreLoggingProperties logging = new AfgCoreLoggingProperties();

    // ========== 指标配置 ==========

    /**
     * 指标配置。
     */
    private AfgCoreMetricsProperties metrics = new AfgCoreMetricsProperties();

    // ========== 虚拟线程配置 ==========

    /**
     * 虚拟线程配置。
     */
    private AfgCoreVirtualThreadProperties virtualThread = new AfgCoreVirtualThreadProperties();

    // ========== 审计配置 ==========

    /**
     * 审计配置。
     */
    private AfgCoreAuditProperties audit = new AfgCoreAuditProperties();

    // ========== 批量操作配置 ==========

    /**
     * 批量操作配置。
     */
    private AfgCoreBatchProperties batch = new AfgCoreBatchProperties();

    // ========== HTTP 客户端配置 ==========

    /**
     * HTTP 客户端配置。
     */
    private AfgCoreHttpClientProperties httpClient = new AfgCoreHttpClientProperties();

    // ========== 云原生配置 ==========

    /**
     * 云原生配置。
     */
    private AfgCoreCloudNativeProperties cloudNative = new AfgCoreCloudNativeProperties();

    // ========== 功能开关配置 ==========

    /**
     * 功能开关配置。
     */
    private AfgCoreFeatureProperties feature = new AfgCoreFeatureProperties();

    // ========== 加密配置 ==========

    /**
     * 加密配置。
     */
    private AfgCoreEncryptionProperties encryption = new AfgCoreEncryptionProperties();

    // ========== 数据权限配置 ==========

    /**
     * 数据权限配置。
     */
    private AfgCoreDataScopeProperties dataScope = new AfgCoreDataScopeProperties();

    // ========== 优雅关闭配置 ==========

    /**
     * 优雅关闭配置。
     */
    private AfgCoreShutdownProperties shutdown = new AfgCoreShutdownProperties();
}
