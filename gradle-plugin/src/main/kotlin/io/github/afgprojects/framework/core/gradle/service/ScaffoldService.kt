package io.github.afgprojects.framework.core.gradle.service

import java.nio.file.Path

/**
 * 脚手架服务接口
 *
 * 生成 CRUD 代码骨架
 */
interface ScaffoldService {

    /**
     * 生成脚手架代码
     *
     * @param entityName 实体名称
     * @param fields 字段定义
     * @param outputPath 输出路径
     * @param options 生成选项
     * @return 生成的文件列表
     */
    fun generate(
        entityName: String,
        fields: List<FieldDefinition>,
        outputPath: Path,
        options: ScaffoldOptions = ScaffoldOptions()
    ): ScaffoldResult
}

/**
 * 字段定义
 */
data class FieldDefinition(
    val name: String,
    val type: FieldType,
    val nullable: Boolean = false,
    val length: Int? = null,
    val precision: Int? = null,
    val unique: Boolean = false,
    val defaultValue: String? = null,
    val validation: FieldValidation? = null
) {
    companion object {
        /**
         * 从字符串解析字段定义
         * 格式: name:Type 或 name:Type(length) 或 name:Type(length,precision)
         */
        fun parse(spec: String): FieldDefinition {
            val parts = spec.split(":")
            val name = parts[0].trim()

            if (parts.size == 1) {
                return FieldDefinition(name, FieldType.STRING)
            }

            val typePart = parts[1].trim()
            val typeMatch = Regex("^(\\w+)(?:\\(([^)]+)\\))?$").find(typePart)
                ?: return FieldDefinition(name, FieldType.STRING)

            val typeName = typeMatch.groupValues[1].uppercase()
            val params = typeMatch.groupValues[2].takeIf { it.isNotEmpty() }

            val type = FieldType.values().find { it.name == typeName } ?: FieldType.STRING
            val (length, precision) = parseParams(params, type)

            return FieldDefinition(name, type, length = length, precision = precision)
        }

        private fun parseParams(params: String?, type: FieldType): Pair<Int?, Int?> {
            if (params == null) return Pair(null, null)

            val values = params.split(",").map { it.trim().toIntOrNull() }
            return when {
                values.size >= 2 -> Pair(values[0], values[1])
                values.size == 1 -> Pair(values[0], null)
                else -> Pair(null, null)
            }
        }
    }
}

/**
 * 字段类型
 */
enum class FieldType(val javaType: String, val sqlType: String, val defaultValue: String) {
    STRING("String", "VARCHAR(255)", "''"),
    TEXT("String", "TEXT", "''"),
    INTEGER("Integer", "INT", "0"),
    LONG("Long", "BIGINT", "0"),
    DOUBLE("Double", "DOUBLE", "0.0"),
    DECIMAL("BigDecimal", "DECIMAL(19,4)", "0"),
    BOOLEAN("Boolean", "BOOLEAN", "false"),
    DATE("LocalDate", "DATE", "null"),
    TIME("LocalTime", "TIME", "null"),
    DATETIME("LocalDateTime", "TIMESTAMP", "null"),
    UUID("UUID", "UUID", "null"),
    JSON("String", "JSON", "null"),
    BLOB("byte[]", "BLOB", "null")
}

/**
 * 字段验证规则
 */
data class FieldValidation(
    val notBlank: Boolean = false,
    val notEmpty: Boolean = false,
    val min: Double? = null,
    val max: Double? = null,
    val minSize: Int? = null,
    val maxSize: Int? = null,
    val pattern: String? = null,
    val email: Boolean = false,
    val custom: String? = null
)

/**
 * 脚手架生成选项
 */
data class ScaffoldOptions(
    val basePackage: String = "com.example",
    val generateEntity: Boolean = true,
    val generateController: Boolean = true,
    val generateMigration: Boolean = true,
    val generateService: Boolean = false,  // 默认不生成 Service，DataManager 已提供 CRUD
    val generateTests: Boolean = true,
    val useLombok: Boolean = true,
    val useValidation: Boolean = true,
    val useSwagger: Boolean = true,
    val tableName: String? = null,
    val apiPath: String? = null
)

/**
 * 脚手架生成结果
 */
data class ScaffoldResult(
    val success: Boolean,
    val entityFile: Path?,
    val controllerFile: Path?,
    val serviceFile: Path?,
    val migrationFile: Path?,
    val testFiles: List<Path>,
    val message: String
)