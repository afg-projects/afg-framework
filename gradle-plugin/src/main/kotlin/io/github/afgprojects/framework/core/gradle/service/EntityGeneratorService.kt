package io.github.afgprojects.framework.core.gradle.service

import java.nio.file.Path

/**
 * 实体生成服务接口
 *
 * 提供实体类代码生成功能
 */
interface EntityGeneratorService {

    /**
     * 从数据库表生成实体类
     *
     * @param schema 表结构信息
     * @param basePackage 基础包名
     * @param outputPath 输出路径
     * @param options 生成选项
     * @return 生成的文件路径
     */
    fun generateFromTable(
        schema: TableSchema,
        basePackage: String,
        outputPath: Path,
        options: GeneratorOptions = GeneratorOptions()
    ): Path

    /**
     * 从 YAML/JSON 配置生成实体类
     *
     * @param config 配置内容
     * @param outputPath 输出路径
     * @return 生成的文件路径列表
     */
    fun generateFromConfig(config: String, outputPath: Path): List<Path>
}

/**
 * 表结构信息
 */
data class TableSchema(
    val tableName: String,
    val columns: List<TableColumn>,
    val primaryKey: List<String>,
    val indexes: List<TableIndex>,
    val foreignKeys: List<TableForeignKey>
)

/**
 * 表列信息
 */
data class TableColumn(
    val name: String,
    val sqlType: String,
    val javaType: String,
    val nullable: Boolean,
    val length: Int?,
    val precision: Int?,
    val scale: Int?,
    val defaultValue: String?,
    val autoIncrement: Boolean
)

/**
 * 表索引信息
 */
data class TableIndex(
    val name: String,
    val columns: List<String>,
    val unique: Boolean
)

/**
 * 外键信息
 */
data class TableForeignKey(
    val name: String,
    val columns: List<String>,
    val referencedTable: String,
    val referencedColumns: List<String>
)

/**
 * 生成选项
 */
data class GeneratorOptions(
    val useLombok: Boolean = true,
    val useJsr305: Boolean = true,
    val useValidation: Boolean = true,
    val generateBuilder: Boolean = true,
    val generateToString: Boolean = true,
    val generateEqualsHashCode: Boolean = true,
    val useRecords: Boolean = false,
    val javaVersion: Int = 21
)