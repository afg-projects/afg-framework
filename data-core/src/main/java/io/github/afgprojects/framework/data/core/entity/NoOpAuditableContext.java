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
 * NoOp 审计上下文实现（降级）
 * <p>
 * 返回 null，不提供任何用户身份信息。作为框架的默认降级实现，
 * 确保不引入 Spring Security 等安全框架时框架仍可正常运行。
 * <p>
 * 当业务应用注册了自定义 {@link AuditableContext} 实现后，
 * 此 NoOp 实现通过 {@code @ConditionalOnMissingBean} 自动退让。
 *
 * @see AuditableContext
 */
public class NoOpAuditableContext implements AuditableContext {

    @Override
    public String getCurrentUserId() {
        return null;
    }
}
