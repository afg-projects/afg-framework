package io.github.afgprojects.framework.governance.server.config;

import io.github.afgprojects.framework.core.autoconfigure.ModuleAutoConfiguration;
import io.github.afgprojects.framework.governance.server.properties.GovernanceServerProperties;
import io.github.afgprojects.framework.governance.server.properties.GovernanceServerSecurityProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.grpc.server.autoconfigure.GrpcServerProperties;
import org.springframework.context.annotation.Bean;

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
@Slf4j
@AutoConfiguration(
    afterName = {
        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
        "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration"
    },
    beforeName = {
        "org.springframework.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration"
    }
)
@ConditionalOnProperty(prefix = "afg.governance.server", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({GovernanceServerProperties.class, GovernanceServerSecurityProperties.class})
public class GovernanceServerAutoConfiguration {

    /**
     * 将 {@link GovernanceServerProperties#port} 映射到 {@link GrpcServerProperties#port}。
     *
     * <p>spring-grpc 使用 {@code spring.grpc.server.port} 配置 gRPC 监听端口（默认 9090），
     * 但 AFG 框架使用 {@code afg.governance.server.port} 作为统一配置入口。此 BeanPostProcessor
     * 在 {@link GrpcServerProperties} bean 初始化后，将 AFG 配置的端口同步到 spring-grpc 的配置中。
     *
     * <p>仅当 {@code afg.governance.server.port} 被显式设置时才覆盖 {@code spring.grpc.server.port}。
     * 如果未设置（null），则尊重 {@code spring.grpc.server.port} 的值，允许用户通过原生 gRPC 配置控制端口。
     *
     * <p>注意：不能通过提前注册 {@code GrpcServerProperties} bean 来覆盖，因为 spring-grpc 的
     * {@code GrpcServerAutoConfiguration} 使用 {@code @EnableConfigurationProperties} 绑定
     * {@code spring.grpc.server.*} 前缀，会覆盖我们手动创建的 bean 的属性值。
     *
     * <p>BeanPostProcessor 方式是最可靠的：在 spring-grpc 创建 {@link GrpcServerProperties} 并完成属性绑定后，
     * 我们再覆盖 port 字段，确保 gRPC server 使用 AFG 配置的端口。
     *
     * @param governanceServerProperties AFG 服务治理配置
     * @return BeanPostProcessor 用于修改 GrpcServerProperties 的端口
     */
    @Bean
    static GrpcPortMappingBeanPostProcessor grpcPortMappingBeanPostProcessor(
            GovernanceServerProperties governanceServerProperties) {
        return new GrpcPortMappingBeanPostProcessor(governanceServerProperties);
    }

    /**
     * 将 AFG 框架的 gRPC 端口配置映射到 spring-grpc 的 {@link GrpcServerProperties}。
     *
     * <p>声明为 static 是因为 {@link BeanPostProcessor} 需要提前实例化，不应依赖配置类实例。
     * {@link GovernanceServerProperties} 通过构造函数注入，在 BeanPostProcessor 创建时即已确定。
     */
    static class GrpcPortMappingBeanPostProcessor implements BeanPostProcessor {

        private final GovernanceServerProperties governanceServerProperties;

        GrpcPortMappingBeanPostProcessor(GovernanceServerProperties governanceServerProperties) {
            this.governanceServerProperties = governanceServerProperties;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            if (bean instanceof GrpcServerProperties grpcServerProperties) {
                Integer afgPort = governanceServerProperties.getPort();
                if (afgPort != null) {
                    int currentPort = grpcServerProperties.getPort();
                    if (afgPort != currentPort) {
                        log.info("Mapping gRPC server port: {} -> {} (from afg.governance.server.port)",
                                currentPort, afgPort);
                        grpcServerProperties.setPort(afgPort);
                    } else {
                        log.info("gRPC server port: {} (afg.governance.server.port matches spring.grpc.server.port)",
                                afgPort);
                    }
                } else {
                    log.debug("afg.governance.server.port not set, using spring.grpc.server.port: {}",
                            grpcServerProperties.getPort());
                }
            }
            return bean;
        }
    }
}
