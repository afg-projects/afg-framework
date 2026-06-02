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
package io.github.afgprojects.framework.data.core.entity;

/**
 * 审计回调接口，用于在实体保存前后执行自定义审计逻辑。
 *
 * <p>与 {@link Auditable} 声明式标记接口不同，此接口提供了回调方法，
 * 允许实体在创建和更新时执行额外的审计操作（如记录审计日志、发送通知等）。
 *
 * <p>典型用法：实体同时实现 {@link Auditable} 和 {@link AuditableCallback}。
 *
 * @see Auditable
 */
public interface AuditableCallback {

    /**
     * 实体创建时的审计回调
     *
     * @param context 审计上下文
     */
    void onCreate(AuditContext context);

    /**
     * 实体更新时的审计回调
     *
     * @param context 审计上下文
     */
    void onUpdate(AuditContext context);

    /**
     * 审计上下文，提供当前操作者和时间信息
     */
    interface AuditContext {

        /**
         * 获取当前操作者
         *
         * @return 操作者标识
         */
        String getCurrentUser();

        /**
         * 获取当前操作时间
         *
         * @return 操作时间（UTC）
         */
        java.time.Instant getCurrentTime();
    }
}
