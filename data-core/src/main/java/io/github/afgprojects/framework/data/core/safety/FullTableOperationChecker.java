/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.afgprojects.framework.data.core.safety;

import io.github.afgprojects.framework.data.core.query.Condition;

/**
 * 全表操作检查器 SPI
 *
 * <p>在执行条件更新 ({@code updateAll}) 和条件删除 ({@code deleteByCondition}) 之前，
 * 检查条件是否为空（即全表操作），并根据 {@link FullTableOperationPolicy} 决定是否阻止、限制或警告。
 *
 * <p>默认实现：{@code io.github.afgprojects.framework.data.jdbc.safety.DefaultFullTableOperationChecker}
 *
 * <p>使用方式：
 * <pre>{@code
 * // 在条件更新/删除前调用
 * fullTableOperationChecker.check(operation, entityClass, condition);
 * }</pre>
 */
public interface FullTableOperationChecker {

    /**
     * 检查是否允许执行全表操作
     *
     * @param operation   操作类型（如 "updateAll" 或 "deleteByCondition"）
     * @param entityClass 实体类
     * @param condition   查询条件，可能为 null 或空
     * @throws io.github.afgprojects.framework.commons.exception.BusinessException
     *         当策略为 BLOCK 且条件为空时抛出
     */
    void check(String operation, Class<?> entityClass, Condition condition);

    /**
     * 判断条件是否为空（全表操作）
     *
     * @param condition 查询条件
     * @return 如果条件为 null 或为空条件，返回 true
     */
    default boolean isEmptyCondition(Condition condition) {
        return condition == null || condition.isEmpty();
    }
}
