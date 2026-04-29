package io.github.afgprojects.framework.core.web.security.signature;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 签名验证自动配置
 * <p>
 * 配置示例：
 * <pre>
 * afg:
 *   security:
 *     signature:
 *       enabled: true
 *       default-key-id: "default"
 *       timestamp-tolerance: 300
 *       nonce-cache-size: 10000
 *       keys:
 *         default:
 *           secret: "your-secret-key-at-least-32-chars"
 *         app1:
 *           secret: "app1-secret-key-at-least-32-chars"
 *           enabled: true
 * </pre>
 */
@AutoConfiguration
@EnableConfigurationProperties(SignatureProperties.class)
@ConditionalOnProperty(prefix = "afg.security.signature", name = "enabled", havingValue = "true", matchIfMissing = false)
public class SignatureAutoConfiguration implements WebMvcConfigurer {

    private final SignatureProperties properties;

    public SignatureAutoConfiguration(SignatureProperties properties) {
        this.properties = properties;
    }

    /**
     * 签名生成器
     */
    @Bean
    @ConditionalOnMissingBean
    public SignatureGenerator signatureGenerator() {
        return new SignatureGenerator();
    }

    /**
     * Nonce 缓存
     */
    @Bean
    @ConditionalOnMissingBean
    public NonceCache nonceCache() {
        return new NonceCache(properties.getNonceCacheSize());
    }

    /**
     * 签名拦截器
     */
    @Bean
    @ConditionalOnMissingBean
    public SignatureInterceptor signatureInterceptor(SignatureGenerator signatureGenerator, NonceCache nonceCache) {
        return new SignatureInterceptor(properties, signatureGenerator, nonceCache);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 签名验证拦截器需要在其他业务拦截器之前执行
        registry.addInterceptor(signatureInterceptor(signatureGenerator(), nonceCache()))
                .order(-100)
                .addPathPatterns("/**");
    }
}
