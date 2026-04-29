package io.github.afgprojects.framework.integration.websocket.interceptor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * WebSocket 拦截器自动配置
 * <p>
 * 提供 WebSocket 相关拦截器的默认配置。
 * </p>
 */
@Configuration
public class WebSocketInterceptorConfiguration {

    /**
     * 配置认证通道拦截器
     * <p>
     * 如果没有自定义实现，使用默认的认证拦截器。
     * </p>
     *
     * @return 认证通道拦截器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthChannelInterceptor authChannelInterceptor() {
        return new AuthChannelInterceptor();
    }
}
