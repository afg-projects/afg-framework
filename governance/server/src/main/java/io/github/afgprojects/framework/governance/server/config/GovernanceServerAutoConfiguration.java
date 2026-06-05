package io.github.afgprojects.framework.governance.server.config;

import io.github.afgprojects.framework.governance.server.properties.GovernanceServerProperties;
import io.github.afgprojects.framework.governance.server.properties.GovernanceServerSecurityProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 服务治理服务端自动配置
 *
 * <p>仅声明条件判断和配置属性注册。组件扫描由 {@link ModuleAutoConfiguration} 通过
 * {@code afg-modules.index} 发现 {@code GovernanceServerModuleConfig}（@AfgModuleAnnotation =
 * @Configuration + @ComponentScan）自动完成。
 *
 * <p>注意：Spring Boot 4 不允许 @AutoConfiguration 类同时使用 @ComponentScan（直接或通过 @Import 间接）
 * 和 @ConditionalOnBean，会导致 REGISTER_BEAN 阶段冲突。因此本类不再包含 @Import 或 @ComponentScan，
 * 组件扫描由模块索引机制独立处理。
 *
 * <p>不使用 @ConditionalOnBean(DataManager.class)，因为 Properties 注册需要在 ComponentScan
 * 扫描到的组件之前完成，而 DataManager bean 可能在组件之后才创建。
 */
@AutoConfiguration(afterName = {
    "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
    "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration"
})
@ConditionalOnProperty(prefix = "afg.governance.server", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({GovernanceServerProperties.class, GovernanceServerSecurityProperties.class})
public class GovernanceServerAutoConfiguration {
}