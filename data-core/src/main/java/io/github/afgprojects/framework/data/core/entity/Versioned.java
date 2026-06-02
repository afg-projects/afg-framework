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
 * 乐观锁接口，表示实体支持乐观锁机制。
 *
 * <p>实现此接口的实体在更新时会检查版本号，防止并发修改冲突。
 * 每次更新时版本号自动递增。
 *
 * <p>提供 default 方法 {@link #incrementVersion()}，
 * 允许实体自由组合多个特征接口。
 *
 * @see VersionedEntity
 */
public interface Versioned {

    /**
     * 获取版本号
     *
     * @return 版本号
     */
    Integer getVersion();

    /**
     * 设置版本号
     *
     * @param version 版本号
     */
    void setVersion(Integer version);

    /**
     * 递增版本号
     */
    default void incrementVersion() {
        Integer current = getVersion();
        setVersion(current == null ? 1 : current + 1);
    }
}
