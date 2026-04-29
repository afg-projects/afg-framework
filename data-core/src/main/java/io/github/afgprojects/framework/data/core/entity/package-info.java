/**
 * 实体抽象包
 * <p>
 * 提供不同类型的实体基类，支持：
 * - BaseEntity: 基础实体（id, createTime, updateTime）
 * - TenantEntity: 租户实体（tenantId）
 * - SoftDeleteEntity: 软删除实体（deleted - Boolean 模式）
 * - TimestampSoftDeleteEntity: 时间戳软删除实体（deletedAt - 时间戳模式）
 * - VersionedEntity: 乐观锁实体（version）
 * - FullEntity: 完整实体（所有特性）
 * <p>
 * 软删除策略：
 * - SoftDeleteStrategy.BOOLEAN: 使用 boolean 类型的 deleted 字段
 * - SoftDeleteStrategy.TIMESTAMP: 使用 LocalDateTime 类型的 deletedAt 字段
 */
package io.github.afgprojects.framework.data.core.entity;