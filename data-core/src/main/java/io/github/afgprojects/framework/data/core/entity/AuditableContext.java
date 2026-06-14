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

import org.jspecify.annotations.Nullable;

/**
 * 审计上下文 SPI 接口
 * <p>
 * 提供当前操作用户的身份信息，供 {@link io.github.afgprojects.framework.data.core.metadata.EntityTrait#AUDITABLE}
 * 特征自动填充使用。框架在 INSERT 时自动填充 createBy 和 updateBy，
 * UPDATE 时自动填充 updateBy。
 *
 * <h3>自定义实现</h3>
 * <p>
 * 业务应用可以实现此接口，从 Spring Security、JWT Token 或其他认证体系中获取当前用户 ID。
 * <pre>
 * &#064;Component
 * public class SecurityAuditableContext implements AuditableContext {
 *     &#064;Override
 *     public String getCurrentUserId() {
 *         return SecurityContextHolder.getContext().getAuthentication().getName();
 *     }
 * }
 * </pre>
 *
 * <h3>NoOp 降级</h3>
 * <p>
 * 框架内置 {@link NoOpAuditableContext} 作为默认降级实现，返回 "system"。
 * 审计字段会被自动填充为 "system"，表示由系统自动创建/修改。
 *
 * @see NoOpAuditableContext
 * @see io.github.afgprojects.framework.data.core.metadata.EntityTrait#AUDITABLE
 */
public interface AuditableContext {

    /**
     * 获取当前操作用户 ID
     * <p>
     * 返回值将用于填充实体的 createBy/updateBy 字段。
     * 如果返回 null，审计字段不会被自动填充。
     *
     * @return 当前用户 ID，可能为 null（未登录或无法获取）
     */
    @Nullable String getCurrentUserId();
}
