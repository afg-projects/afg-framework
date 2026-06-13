package io.github.afgprojects.framework.core.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.core.api.webhook.InMemoryWebhookRepository;
import io.github.afgprojects.framework.core.api.webhook.LocalWebhookService;
import io.github.afgprojects.framework.core.api.webhook.NoOpWebhookRepository;
import io.github.afgprojects.framework.core.api.webhook.NoOpWebhookService;
import io.github.afgprojects.framework.core.api.webhook.WebhookRepository;
import io.github.afgprojects.framework.core.api.webhook.WebhookService;
import io.github.afgprojects.framework.core.config.AfgCoreProperties;

/**
 * Webhook 自动配置
 * <p>
 * 自动配置 Webhook 分发功能，默认使用 {@link InMemoryWebhookRepository} + {@link LocalWebhookService}（HTTP 分发）。
 * 持久化仓库和分布式分发实现由集成模块提供，通过 {@code @ConditionalOnMissingBean} 自动升级。
 * </p>
 * <p>
 * 配置示例：
 * <pre>
 * afg:
 *   core:
 *     webhook:
 *       enabled: true
 *       connect-timeout: 5000
 *       read-timeout: 10000
 *       max-retries: 3
 *       retry-interval-ms: 1000
 *       signature-algorithm: HmacSHA256
 *       signature-header: X-Webhook-Signature
 * </pre>
 * </p>
 *
 * @since 1.0.0
 */
@AutoConfiguration(after = AfgAutoConfiguration.class)
@ConditionalOnProperty(prefix = "afg.core.webhook", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AfgCoreProperties.class)
public class WebhookAutoConfiguration {

    /**
     * 内存 Webhook 仓库
     * <p>
     * 当没有 JDBC 等持久化仓库实现时，提供基于 ConcurrentHashMap 的内存注册管理。
     * 应用重启后注册信息丢失，生产环境应替换为持久化实现。
     *
     * @return 内存 Webhook 仓库实例
     */
    @Bean
    @ConditionalOnMissingBean(WebhookRepository.class)
    public WebhookRepository inMemoryWebhookRepository() {
        return new InMemoryWebhookRepository();
    }

    /**
     * 本地 Webhook 服务
     * <p>
     * 组合 WebhookRepository 查找订阅者，通过 HTTP POST 逐个投递事件。
     * 支持 HMAC-SHA256 签名验证。
     *
     * @param repository Webhook 仓库
     * @param properties 核心配置属性
     * @return 本地 Webhook 服务实例
     */
    @Bean
    @ConditionalOnMissingBean(WebhookService.class)
    public WebhookService localWebhookService(WebhookRepository repository, AfgCoreProperties properties) {
        return new LocalWebhookService(repository, properties);
    }

    /**
     * NoOp Webhook 仓库降级实现
     * <p>
     * 当 InMemoryWebhookRepository 也不满足条件时使用。
     *
     * @return NoOp Webhook 仓库实例
     */
    @Bean
    @ConditionalOnMissingBean(WebhookRepository.class)
    public WebhookRepository noOpWebhookRepository() {
        return new NoOpWebhookRepository();
    }

    /**
     * NoOp Webhook 服务降级实现
     * <p>
     * 当 LocalWebhookService 也不满足条件时使用。
     *
     * @return NoOp Webhook 服务实例
     */
    @Bean
    @ConditionalOnMissingBean(WebhookService.class)
    public WebhookService noOpWebhookService() {
        return new NoOpWebhookService();
    }
}
