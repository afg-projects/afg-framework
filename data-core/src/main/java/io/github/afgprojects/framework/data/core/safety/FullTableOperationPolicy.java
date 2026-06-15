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

/**
 * 全表操作保护策略
 *
 * <p>当 updateAll/deleteByCondition 等操作没有指定条件时，
 * 根据 {@link FullTableOperationPolicy} 决定框架的行为：
 *
 * <ul>
 *   <li>{@link #BLOCK}（默认）— 抛出 {@link io.github.afgprojects.framework.commons.exception.BusinessException}，
 *       阻止全表操作执行，防止意外清空整表</li>
 *   <li>{@link #LIMIT} — 自动加 LIMIT 限制受影响行数，并日志警告</li>
 *   <li>{@link #WARN} — 仅日志警告，仍执行全表操作</li>
 * </ul>
 *
 * <p>配置属性：{@code afg.data.safety.full-table-operation-policy}
 */
public enum FullTableOperationPolicy {

    /**
     * 阻止全表操作 — 抛出 BusinessException，默认策略
     */
    BLOCK,

    /**
     * 自动加 LIMIT — 限制受影响行数，并日志警告
     */
    LIMIT,

    /**
     * 仅日志警告 — 仍执行全表操作
     */
    WARN
}
