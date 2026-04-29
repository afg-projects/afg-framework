package io.github.afgprojects.framework.core.autoconfigure;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.NonNull;

/**
 * 读写分离路由策略
 *
 * <p>根据操作类型自动路由到对应的数据源
 *
 * <h3>路由规则</h3>
 * <ul>
 *   <li>写操作（INSERT/UPDATE/DELETE）→ 主库</li>
 *   <li>读操作（SELECT）→ 从库（负载均衡）</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class ReadWriteRoutingStrategy {

    private final String writeDatasource;
    private final List<String> readDatasources;
    private final AtomicInteger readCounter = new AtomicInteger(0);

    /**
     * 创建读写分离路由策略
     *
     * @param writeDatasource  写数据源名称
     * @param readDatasources  读数据源列表
     */
    public ReadWriteRoutingStrategy(@NonNull String writeDatasource, @NonNull List<String> readDatasources) {
        this.writeDatasource = writeDatasource;
        this.readDatasources = readDatasources.isEmpty() ? List.of(writeDatasource) : readDatasources;
    }

    /**
     * 根据操作类型获取数据源名称
     *
     * @param operationType 操作类型
     * @return 数据源名称
     */
    @NonNull
    public String getDatasource(@NonNull OperationType operationType) {
        if (operationType == OperationType.READ) {
            return getNextReadDatasource();
        }
        return writeDatasource;
    }

    /**
     * 获取下一个读数据源（轮询）
     *
     * @return 读数据源名称
     */
    @NonNull
    private String getNextReadDatasource() {
        if (readDatasources.size() == 1) {
            return readDatasources.get(0);
        }
        int index = readCounter.getAndIncrement() % readDatasources.size();
        return readDatasources.get(Math.abs(index));
    }

    /**
     * 操作类型
     */
    public enum OperationType {
        READ,
        WRITE
    }
}