package io.github.afgprojects.framework.core.properties.datasource;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

/**
 * 负载均衡配置。
 */
@Data
public class AfgCoreLoadBalanceProperties {

    private LoadBalanceStrategyType strategy = LoadBalanceStrategyType.ROUND_ROBIN;
    private long healthCheckInterval = 30000L;
    private boolean healthCheckEnabled = true;
    private Map<String, Integer> weights = new HashMap<>();
}
