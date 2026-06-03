package io.github.afgprojects.framework.core.properties.datasource;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import lombok.Data;

/**
 * 读写分离配置。
 */
@Data
public class AfgCoreReadWriteSeparationProperties {

    private boolean enabled = false;
    private List<String> readDatasources = new ArrayList<>();
    private @Nullable String writeDatasource;
    private AfgCoreLoadBalanceProperties loadBalance = new AfgCoreLoadBalanceProperties();
}
