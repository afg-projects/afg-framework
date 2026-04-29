package io.github.afgprojects.framework.core.autoconfigure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.creator.DataSourceProperty;
import com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator;
import com.baomidou.dynamic.datasource.provider.AbstractDataSourceProvider;
import com.baomidou.dynamic.datasource.provider.DynamicDataSourceProvider;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAutoConfiguration;

import io.github.afgprojects.framework.core.datasource.lb.LeastConnectionsStrategy;
import io.github.afgprojects.framework.core.datasource.lb.LoadBalanceStrategy;
import io.github.afgprojects.framework.core.datasource.lb.ReadDataSourceLoadBalancer;
import io.github.afgprojects.framework.core.datasource.lb.RoundRobinStrategy;
import io.github.afgprojects.framework.core.datasource.lb.WeightedStrategy;

/**
 * 多数据源自动配置类
 *
 * <p>基于 MyBatis-Plus Dynamic Datasource 实现多数据源支持
 *
 * <h3>功能特性</h3>
 * <ul>
 *   <li>支持多数据源动态切换</li>
 *   <li>支持读写分离</li>
 *   <li>支持分库分表</li>
 *   <li>基于注解的数据源切换</li>
 * </ul>
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   datasource:
 *     dynamic:
 *       enabled: true
 *       primary: master  # 默认数据源
 *       strict: false    # 严格模式，未匹配数据源时抛异常
 *       datasources:
 *         master:
 *           url: jdbc:mysql://localhost:3306/db_master
 *           username: root
 *           password: root
 *         slave_1:
 *           url: jdbc:mysql://localhost:3306/db_slave_1
 *           username: root
 *           password: root
 *         slave_2:
 *           url: jdbc:mysql://localhost:3306/db_slave_2
 *           username: root
 *           password: root
 * </pre>
 *
 * <h3>使用示例</h3>
 * <pre>
 * // 使用 @DS 注解切换数据源
 * @DS("slave_1")
 * public List&lt;User&gt; queryUsers() {
 *     return userMapper.selectList(null);
 * }
 *
 * // 使用编程式切换
 * DynamicDataSourceContextHolder.push("slave_2");
 * List&lt;User&gt; users = userMapper.selectList(null);
 * DynamicDataSourceContextHolder.poll();
 * </pre>
 *
 * @see com.baomidou.dynamic.datasource.annotation.DS
 * @since 1.0.0
 */
@AutoConfiguration(before = DynamicDataSourceAutoConfiguration.class)
@ConditionalOnClass(name = "com.baomidou.dynamic.datasource.DynamicRoutingDataSource")
@ConditionalOnProperty(prefix = "afg.datasource.dynamic", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(MultiDataSourceProperties.class)
public class MultiDataSourceAutoConfiguration {

    /**
     * 数据源创建器
     *
     * <p>负责创建各数据源实例
     */
    @Bean
    @ConditionalOnMissingBean
    public DefaultDataSourceCreator dataSourceCreator() {
        DefaultDataSourceCreator creator = new DefaultDataSourceCreator();
        creator.setLazy(false);
        return creator;
    }

    /**
     * 动态数据源提供者
     *
     * <p>从配置属性加载所有数据源
     */
    @Bean
    @ConditionalOnMissingBean
    public DynamicDataSourceProvider dynamicDataSourceProvider(
            MultiDataSourceProperties properties, DefaultDataSourceCreator creator) {
        return new AbstractDataSourceProvider(creator) {
            @Override
            public @NonNull Map<String, DataSource> loadDataSources() {
                Map<String, DataSourceProperty> dataSourceProperties = new HashMap<>();
                for (Map.Entry<String, MultiDataSourceProperties.DataSourceConfig> entry
                        : properties.getDatasources().entrySet()) {
                    DataSourceProperty prop = new DataSourceProperty();
                    prop.setUrl(entry.getValue().getUrl());
                    prop.setUsername(entry.getValue().getUsername());
                    prop.setPassword(entry.getValue().getPassword());
                    prop.setDriverClassName(entry.getValue().getDriverClassName());
                    prop.setLazy(entry.getValue().isLazyInit());
                    dataSourceProperties.put(entry.getKey(), prop);
                }
                return createDataSourceMap(dataSourceProperties);
            }
        };
    }

    /**
     * 动态路由数据源
     *
     * <p>作为主数据源，支持动态切换
     */
    @Bean(name = "dynamicDataSource")
    @Primary
    @ConditionalOnMissingBean(name = "dynamicDataSource")
    public DataSource dynamicDataSource(
            DynamicDataSourceProvider provider, MultiDataSourceProperties properties) {
        DynamicRoutingDataSource dataSource = new DynamicRoutingDataSource(List.of(provider));
        dataSource.setPrimary(properties.getPrimary());
        dataSource.setStrict(properties.isStrict());
        return dataSource;
    }

    /**
     * 读写分离配置
     *
     * <p>可选配置，用于自动路由读操作到从库
     */
    @Configuration
    @ConditionalOnProperty(prefix = "afg.datasource.dynamic.read-write-separation", name = "enabled", havingValue = "true")
    static class ReadWriteSeparationConfiguration {

        /**
         * 负载均衡策略
         *
         * <p>根据配置创建对应的负载均衡策略
         */
        @Bean
        @ConditionalOnMissingBean
        public LoadBalanceStrategy loadBalanceStrategy(MultiDataSourceProperties properties) {
            MultiDataSourceProperties.LoadBalanceConfig loadBalanceConfig =
                    properties.getReadWriteSeparation().getLoadBalance();

            return switch (loadBalanceConfig.getStrategy()) {
                case ROUND_ROBIN -> new RoundRobinStrategy();
                case WEIGHTED -> new WeightedStrategy(loadBalanceConfig.getWeights());
                case LEAST_CONNECTIONS -> new LeastConnectionsStrategy();
            };
        }

        /**
         * 读数据源负载均衡器
         *
         * <p>管理多个从库，根据策略选择从库，支持健康检查
         */
        @Bean
        @ConditionalOnMissingBean
        public ReadDataSourceLoadBalancer readDataSourceLoadBalancer(
                DataSource dynamicDataSource,
                MultiDataSourceProperties properties,
                LoadBalanceStrategy loadBalanceStrategy) {

            DynamicRoutingDataSource routingDataSource = (DynamicRoutingDataSource) dynamicDataSource;
            Map<String, DataSource> dataSourceMap = routingDataSource.getDataSources();

            String writeDatasource = properties.getReadWriteSeparation().getWriteDatasource();
            if (writeDatasource == null || writeDatasource.isEmpty()) {
                writeDatasource = properties.getPrimary();
            }

            List<String> readDatasources = properties.getReadWriteSeparation().getReadDatasources();
            long healthCheckInterval = properties.getReadWriteSeparation().getLoadBalance().getHealthCheckInterval();

            ReadDataSourceLoadBalancer loadBalancer = new ReadDataSourceLoadBalancer(
                    dataSourceMap,
                    writeDatasource,
                    readDatasources,
                    loadBalanceStrategy,
                    healthCheckInterval);

            // 如果启用健康检查，启动健康检查
            if (properties.getReadWriteSeparation().getLoadBalance().isHealthCheckEnabled()) {
                loadBalancer.startHealthCheck();
            }

            return loadBalancer;
        }

        /**
         * 读写分离路由策略
         *
         * <p>读操作自动路由到从库，写操作路由到主库
         */
        @Bean
        @ConditionalOnMissingBean
        public ReadWriteRoutingStrategy readWriteRoutingStrategy(
                ReadDataSourceLoadBalancer loadBalancer) {
            return new ReadWriteRoutingStrategy(
                    loadBalancer.getPrimaryDataSource(),
                    loadBalancer.getReadDataSources()) {
                @Override
                @NonNull
                public String getDatasource(@NonNull OperationType operationType) {
                    if (operationType == OperationType.READ) {
                        return loadBalancer.select();
                    }
                    return loadBalancer.getPrimaryDataSource();
                }
            };
        }
    }
}