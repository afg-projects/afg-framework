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
 * 审计标记接口，表示实体具有审计字段（createBy、updateBy）。
 *
 * <p>实现此接口的实体会在保存时自动填充创建人和修改人信息。
 * 这是一种声明式标记，框架会通过 {@link io.github.afgprojects.framework.data.core.metadata.EntityMetadata#hasTrait}
 * 检测此特征。
 *
 * <p>如果需要在保存前后执行自定义审计逻辑，请额外实现 {@link AuditableCallback} 接口。
 *
 * @see AuditableCallback
 * @see io.github.afgprojects.framework.data.core.metadata.EntityTrait#AUDITABLE
 */
public interface Auditable {
}
