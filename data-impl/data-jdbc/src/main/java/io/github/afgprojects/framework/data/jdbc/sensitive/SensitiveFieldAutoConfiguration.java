package io.github.afgprojects.framework.data.jdbc.sensitive;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadataCache;
import io.github.afgprojects.framework.data.core.sensitive.DefaultMaskingStrategy;
import io.github.afgprojects.framework.data.core.sensitive.MaskingContext;
import io.github.afgprojects.framework.data.core.sensitive.MaskingStrategy;
import io.github.afgprojects.framework.data.core.sensitive.NoOpMaskingContext;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 数据脱敏自动配置。
 * <p>
 * 当 Jackson ObjectMapper 和 EntityMetadataCache 都存在时，
 * 自动注册 {@link SensitiveFieldBeanSerializerModifier} 到 ObjectMapper 的序列化器修饰器链中。
 *
 * <h3>配置</h3>
 * <pre>
 * afg:
 *   data:
 *     masking:
 *       enabled: true
 * </pre>
 */
@Slf4j
@AutoConfiguration(afterName = {
    "io.github.afgprojects.framework.data.jdbc.autoconfigure.DataManagerAutoConfiguration"
})
@ConditionalOnClass({ObjectMapper.class, EntityMetadataCache.class})
@ConditionalOnProperty(prefix = "afg.data.masking", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SensitiveFieldAutoConfiguration {

    /**
     * 创建默认脱敏策略 Bean。
     */
    @Bean
    @ConditionalOnMissingBean(MaskingStrategy.class)
    public DefaultMaskingStrategy defaultMaskingStrategy() {
        log.info("Using DefaultMaskingStrategy for data masking (GB/T 35273)");
        return new DefaultMaskingStrategy();
    }

    /**
     * 创建默认脱敏上下文 Bean（始终脱敏）。
     */
    @Bean
    @ConditionalOnMissingBean(MaskingContext.class)
    public NoOpMaskingContext noOpMaskingContext() {
        return new NoOpMaskingContext();
    }

    /**
     * 注册脱敏序列化器修饰器到 ObjectMapper。
     *
     * @param objectMapper   Jackson ObjectMapper
     * @param metadataCache  实体元数据缓存
     * @param maskingStrategy 脱敏策略
     * @param maskingContext  脱敏上下文
     * @return 配置后的 ObjectMapper
     */
    @Bean
    @ConditionalOnBean({ObjectMapper.class, EntityMetadataCache.class})
    public ObjectMapper sensitiveFieldObjectMapper(ObjectMapper objectMapper,
                                                    EntityMetadataCache metadataCache,
                                                    @Nullable MaskingStrategy maskingStrategy,
                                                    @Nullable MaskingContext maskingContext) {
        SensitiveFieldBeanSerializerModifier modifier = new SensitiveFieldBeanSerializerModifier(
            metadataCache, maskingStrategy, maskingContext);
        objectMapper.setSerializerFactory(
            objectMapper.getSerializerFactory().withSerializerModifier(modifier));
        log.info("Registered SensitiveFieldBeanSerializerModifier on ObjectMapper for data masking");
        return objectMapper;
    }
}
