package io.github.afgprojects.framework.core.gradle.service

import java.nio.file.Path
import java.sql.Connection

/**
 * 迁移服务接口
 *
 * 提供数据库迁移相关的核心功能
 */
interface MigrationService {

    /**
     * 执行数据库迁移
     *
     * @param connection 数据库连接
     * @param changeLogFile ChangeLog 文件路径
     * @param targetVersion 目标版本（可选，空表示最新）
     */
    fun migrate(connection: Connection, changeLogFile: String, targetVersion: String? = null)

    /**
     * 生成迁移脚本
     *
     * @param entityMetadata 实体元数据
     * @param connection 数据库连接（用于比对现有表结构）
     * @param changeLogFile ChangeLog 文件路径
     * @param author 作者
     * @param outputPath 输出路径
     * @return 是否有冲突
     */
    fun generateMigration(
        entityMetadata: EntityMetadata,
        connection: Connection?,
        changeLogFile: String?,
        author: String,
        outputPath: Path
    ): Boolean

    /**
     * 验证实体与数据库一致性
     *
     * @param entityMetadata 实体元数据
     * @param connection 数据库连接
     * @return 验证结果
     */
    fun validate(entityMetadata: EntityMetadata, connection: Connection): ValidationResult

    /**
     * 对比实体与数据库差异
     *
     * @param entityMetadata 实体元数据
     * @param connection 数据库连接
     * @return 差异报告
     */
    fun diff(entityMetadata: EntityMetadata, connection: Connection): DiffReport
}

/**
 * 实体元数据
 */
interface EntityMetadata {
    val entityClass: Class<*>
    val tableName: String
    val columns: List<ColumnMetadata>
}

/**
 * 列元数据
 */
interface ColumnMetadata {
    val name: String
    val javaType: Class<*>
    val sqlType: String
    val nullable: Boolean
    val length: Int?
    val precision: Int?
    val scale: Int?
    val defaultValue: String?
    val primaryKey: Boolean
    val autoIncrement: Boolean
    val unique: Boolean
}

/**
 * 验证结果
 */
data class ValidationResult(
    val valid: Boolean,
    val errors: List<ValidationError>,
    val warnings: List<String>
)

/**
 * 验证错误
 */
data class ValidationError(
    val type: ErrorType,
    val message: String,
    val columnName: String?,
    val expected: String?,
    val actual: String?
)

enum class ErrorType {
    MISSING_COLUMN,
    EXTRA_COLUMN,
    TYPE_MISMATCH,
    NULLABLE_MISMATCH,
    LENGTH_MISMATCH,
    PRIMARY_KEY_MISMATCH,
    UNIQUE_MISMATCH
}

/**
 * 差异报告
 */
data class DiffReport(
    val tableName: String,
    val addedColumns: List<ColumnMetadata>,
    val removedColumns: List<String>,
    val modifiedColumns: List<ColumnDiff>,
    val hasChanges: Boolean
)

/**
 * 列差异
 */
data class ColumnDiff(
    val columnName: String,
    val changes: List<PropertyChange>
)

/**
 * 属性变更
 */
data class PropertyChange(
    val property: String,
    val oldValue: Any?,
    val newValue: Any?
)