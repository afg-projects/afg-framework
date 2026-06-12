package io.github.afgprojects.framework.ai.langchain4j.autoconfigure;

import dev.langchain4j.model.chat.listener.ChatModelListener;
import io.github.afgprojects.framework.ai.core.api.observability.MetricsCollector;
import io.github.afgprojects.framework.ai.core.api.observability.Tracer;
import io.github.afgprojects.framework.ai.langchain4j.config.Lc4jProperties;
import io.github.afgprojects.framework.ai.langchain4j.observation.Lc4jObservationAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * LangChain4J Observation 自动配置
 *
 * <p>当 classpath 上存在 LangChain4J {@link ChatModelListener} 且
 * AFG {@link Tracer} 和 {@link MetricsCollector} 均可用时自动激活。
 * 注册 {@link Lc4jObservationAdapter} 作为 {@link ChatModelListener} 的实现，
 * 将 LangChain4J 的可观测性事件桥接到 AFG 的 Tracer 和 MetricsCollector。
 *
 * <p>配置示例：
 * <pre>{@code
 * afg:
 *   ai:
 *     langchain4j:
 *       enabled: true
 *     observability:
 *       enabled: true
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration(
    after = Lc4jChatAutoConfiguration.class,
    afterName = {
        "io.github.afgprojects.framework.core.autoconfigure.AfgAutoConfiguration",
        "io.github.afgprojects.framework.ai.core.autoconfigure.AiCoreAutoConfiguration"
    }
)
@EnableConfigurationProperties(Lc4jProperties.class)
@ConditionalOnClass(name = "dev.langchain4j.model.chat.listener.ChatModelListener")
@ConditionalOnProperty(prefix = "afg.ai.langchain4j", name = "enabled", havingValue = "true", matchIfMissing = true)
public class Lc4jObservationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ChatModelListener.class)
    @ConditionalOnBean({Tracer.class, MetricsCollector.class})
    public Lc4jObservationAdapter lc4jObservationAdapter(Tracer tracer,
                                                          MetricsCollector metricsCollector) {
        log.info("Creating Lc4jObservationAdapter bridging LC4J ChatModelListener to AFG Tracer and MetricsCollector");
        return new Lc4jObservationAdapter(tracer, metricsCollector);
    }
}
