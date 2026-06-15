package io.github.afgprojects.framework.data.jdbc.safety;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.safety.FullTableOperationChecker;
import io.github.afgprojects.framework.data.core.safety.FullTableOperationPolicy;
import lombok.extern.slf4j.Slf4j;

/**
 * 全表操作检查器默认实现
 *
 * <p>根据 {@link DataSafetyProperties} 配置的策略，
 * 在条件更新/删除操作前检查条件是否为空（全表操作），并执行相应的保护策略：
 * <ul>
 *   <li>{@link FullTableOperationPolicy#BLOCK}（默认）— 抛出 {@link BusinessException}，阻止全表操作</li>
 *   <li>{@link FullTableOperationPolicy#LIMIT} — 允许执行但日志警告（LIMIT 由调用方处理）</li>
 *   <li>{@link FullTableOperationPolicy#WARN} — 仅日志警告，允许执行</li>
 * </ul>
 */
@Slf4j
public class DefaultFullTableOperationChecker implements FullTableOperationChecker {

    private final FullTableOperationPolicy policy;
    private final long limit;

    public DefaultFullTableOperationChecker(DataSafetyProperties properties) {
        this.policy = properties.getFullTableOperationPolicy();
        this.limit = properties.getFullTableOperationLimit();
    }

    /**
     * 创建使用默认配置（BLOCK 策略）的检查器
     */
    public DefaultFullTableOperationChecker() {
        this.policy = FullTableOperationPolicy.BLOCK;
        this.limit = 1000;
    }

    @Override
    public void check(String operation, Class<?> entityClass, Condition condition) {
        if (!isEmptyCondition(condition)) {
            return;
        }

        String entityName = entityClass.getSimpleName();
        switch (policy) {
            case BLOCK -> throw new BusinessException(CommonErrorCode.FULL_TABLE_OPERATION_NOT_ALLOWED,
                    String.format("Full table operation not allowed: %s on %s without condition. "
                            + "Set afg.data.safety.full-table-operation-policy to LIMIT or WARN to allow.",
                            operation, entityName));
            case LIMIT -> log.warn("Full table operation detected: {} on {} without condition. "
                    + "Applying LIMIT {} as configured. "
                    + "Consider adding a condition to narrow the scope.",
                    operation, entityName, limit);
            case WARN -> log.warn("Full table operation detected: {} on {} without condition. "
                    + "This will affect all rows in the table!",
                    operation, entityName);
        }
    }

    /**
     * 获取当前策略
     *
     * @return 全表操作保护策略
     */
    public FullTableOperationPolicy getPolicy() {
        return policy;
    }

    /**
     * 获取 LIMIT 模式下的限制行数
     *
     * @return 限制行数
     */
    public long getLimit() {
        return limit;
    }
}
