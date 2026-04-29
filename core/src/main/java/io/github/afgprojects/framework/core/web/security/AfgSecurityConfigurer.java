package io.github.afgprojects.framework.core.web.security;

/**
 * AFG 模块级安全配置 SPI
 *
 * 各模块实现此接口贡献安全规则，由自动配置收集并应用。
 */
public interface AfgSecurityConfigurer {

    void configure(AfgSecurityConfiguration config);

    default int getOrder() {
        return 0;
    }
}
