@file:Suppress("UNCHECKED_CAST")

package io.github.afgprojects.framework.core.gradle.task

import io.github.classgraph.ClassGraph
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import java.lang.reflect.Field
import java.time.Instant

/**
 * 从实体类生成数据库 Schema 文档（Markdown 格式）。
 *
 * <p>扫描指定包路径下的实体类，提取表名、字段名、类型、约束、注释等信息，
 * 生成结构化的 Markdown 文档，方便开发者查阅数据库结构。
 *
 * <h3>信息来源</h3>
 * <ul>
 *   <li>@AfEntity 注解 — 表名（tableName 属性）</li>
 *   <li>@Table 注解 — 表名（name 属性）</li>
 *   <li>@Column 注解 — 列名、类型、约束</li>
 *   <li>实体继承链 — 从 BaseEntity/TenantEntity/SoftDeleteEntity 等基类推断字段</li>
 *   <li>Java 字段类型 — 推断数据库列类型</li>
 * </ul>
 *
 * <h3>使用方式</h3>
 * <pre>
 * afg {
 *     migration {
 *         entityPackages.set(listOf("com.example.entity"))
 *     }
 * }
 *
 * ./gradlew generateDbDoc
 * </pre>
 *
 * @since 1.0.0
 */
@DisableCachingByDefault(because = "Output depends on classpath entity state")
abstract class GenerateDbDocTask : DefaultTask() {

    /**
     * 实体包路径列表。
     *
     * <p>指定需要扫描的包路径，框架会在这些包下查找继承 BaseEntity 的实体类。
     * 默认使用 migration.entityPackages 配置。
     */
    @get:Input
    abstract val entityPackages: ListProperty<String>

    /**
     * 输出文件路径。
     *
     * <p>生成的 Markdown 文档输出路径，默认为 docs/db-schema.md。
     */
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    /**
     * 文档标题。
     *
     * <p>生成文档的一级标题，默认为"数据库 Schema 文档"。
     */
    @get:Input
    @get:Optional
    abstract val title: Property<String>

    /**
     * 是否包含基类字段。
     *
     * <p>如果为 true，文档中会包含从 BaseEntity、TenantEntity 等基类继承的字段。
     * 默认 true。
     */
    @get:Input
    @get:Optional
    abstract val includeBaseFields: Property<Boolean>

    init {
        outputFile.convention(project.layout.projectDirectory.file("docs/db-schema.md"))
        title.convention("数据库 Schema 文档")
        includeBaseFields.convention(true)
    }

    @TaskAction
    fun generate() {
        val packages = entityPackages.get()
        val output = outputFile.get().asFile

        logger.lifecycle("Generating database schema documentation...")
        logger.lifecycle("  Entity packages: $packages")
        logger.lifecycle("  Output file: ${output.absolutePath}")
        logger.lifecycle("  Title: ${title.get()}")
        logger.lifecycle("  Include base fields: ${includeBaseFields.getOrElse(true)}")

        if (packages.isEmpty()) {
            logger.warn("No entity packages specified, skipping database doc generation")
            return
        }

        // 1. 扫描实体类
        val entityClasses = scanEntityClasses(packages)
        logger.lifecycle("Found ${entityClasses.size} entity classes")

        if (entityClasses.isEmpty()) {
            logger.warn("No entity classes found in packages: $packages")
            return
        }

        // 2. 提取元数据
        val tables = entityClasses.mapNotNull { clazz ->
            try {
                extractTableMetadata(clazz)
            } catch (e: Exception) {
                logger.warn("Failed to extract metadata from ${clazz.name}: ${e.message}")
                null
            }
        }.sortedBy { it.tableName }

        // 3. 生成 Markdown 文档
        val markdown = generateMarkdown(tables)

        // 4. 写入文件
        output.parentFile.mkdirs()
        output.writeText(markdown)

        logger.lifecycle("Database schema documentation generated: ${output.absolutePath}")
        logger.lifecycle("  Tables documented: ${tables.size}")
    }

    // ========== 实体扫描 ==========

    private fun scanEntityClasses(packages: List<String>): List<Class<*>> {
        val result = mutableListOf<Class<*>>()
        val baseEntityClassName = "io.github.afgprojects.framework.data.core.entity.BaseEntity"

        for (pkg in packages) {
            logger.info("Scanning package: $pkg")

            ClassGraph()
                .enableClassInfo()
                .enableAnnotationInfo()
                .acceptPackages(pkg)
                .scan().use { scanResult ->
                    scanResult.allClasses.forEach { classInfo ->
                        if (classInfo.extendsSuperclass(baseEntityClassName) && !classInfo.isAbstract) {
                            try {
                                val clazz = Class.forName(classInfo.name)
                                result.add(clazz)
                                logger.info("Found entity class: ${classInfo.name}")
                            } catch (e: ClassNotFoundException) {
                                logger.warn("Could not load class: ${classInfo.name}")
                            } catch (e: Exception) {
                                logger.warn("Error loading class ${classInfo.name}: ${e.message}")
                            }
                        }
                    }
                }
        }

        return result
    }

    // ========== 元数据提取 ==========

    private data class TableMetadata(
        val className: String,
        val tableName: String,
        val tableComment: String,
        val columns: List<ColumnMetadata>,
        val baseClass: String,
        val traits: List<String>
    )

    private data class ColumnMetadata(
        val fieldName: String,
        val columnName: String,
        val javaType: String,
        val dbType: String,
        val nullable: Boolean,
        val unique: Boolean,
        val length: Int,
        val defaultValue: String?,
        val comment: String,
        val isPrimaryKey: Boolean,
        val isAutoIncrement: Boolean,
        val isBaseField: Boolean
    )

    private fun extractTableMetadata(clazz: Class<*>): TableMetadata {
        val tableName = resolveTableName(clazz)
        val tableComment = resolveTableComment(clazz)
        val traits = detectTraits(clazz)
        val baseClass = resolveBaseClassName(clazz)

        val columns = mutableListOf<ColumnMetadata>()
        val showBase = includeBaseFields.getOrElse(true)

        // 收集实体自身声明的字段
        collectDeclaredFields(clazz, columns, isBase = false)

        // 收集基类字段
        if (showBase) {
            collectInheritedFields(clazz, columns)
        }

        // 应用 @Column 注解覆盖
        applyColumnAnnotations(clazz, columns)

        // 检测 @Id 注解
        applyIdAnnotation(clazz, columns)

        return TableMetadata(
            className = clazz.simpleName,
            tableName = tableName,
            tableComment = tableComment,
            columns = columns,
            baseClass = baseClass,
            traits = traits
        )
    }

    private fun resolveTableName(clazz: Class<*>): String {
        // 1. 尝试 @AfEntity(tableName)
        val afEntity = findAnnotation(clazz, "io.github.afgprojects.framework.apt.entity.AfEntity")
        if (afEntity != null) {
            val afTableName = afEntity.javaClass.getMethod("tableName").invoke(afEntity) as String
            if (afTableName.isNotEmpty()) {
                return afTableName
            }
        }

        // 2. 尝试 @Table(name)
        val table = findAnnotation(clazz, "jakarta.persistence.Table")
        if (table != null) {
            val jpaTableName = table.javaClass.getMethod("name").invoke(table) as String
            if (jpaTableName.isNotEmpty()) {
                return jpaTableName
            }
        }

        // 3. 类名转 snake_case
        return camelToSnake(clazz.simpleName)
    }

    private fun resolveTableComment(clazz: Class<*>): String {
        val table = findAnnotation(clazz, "jakarta.persistence.Table")
        if (table != null) {
            try {
                val schema = table.javaClass.getMethod("schema").invoke(table) as String
                if (schema.isNotEmpty()) {
                    return schema
                }
            } catch (_: NoSuchMethodException) {
                // schema 方法不存在，忽略
            }
        }
        return ""
    }

    private fun resolveBaseClassName(clazz: Class<*>): String {
        var current = clazz.superclass
        while (current != null && current != Any::class.java) {
            if (current.name.startsWith("io.github.afgprojects.framework.data.core.entity")) {
                return current.simpleName
            }
            current = current.superclass
        }
        return "BaseEntity"
    }

    private fun detectTraits(clazz: Class<*>): List<String> {
        val traits = mutableListOf<String>()
        val traitInterfaces = mapOf(
            "io.github.afgprojects.framework.data.core.entity.SoftDeletable" to "SoftDeletable",
            "io.github.afgprojects.framework.data.core.entity.TimestampSoftDeletable" to "TimestampSoftDeletable",
            "io.github.afgprojects.framework.data.core.entity.Versioned" to "Versioned",
            "io.github.afgprojects.framework.data.core.entity.Auditable" to "Auditable",
            "io.github.afgprojects.framework.data.core.entity.Treeable" to "Treeable"
        )

        for ((ifaceName, traitName) in traitInterfaces) {
            try {
                val iface = Class.forName(ifaceName)
                if (iface.isAssignableFrom(clazz)) {
                    traits.add(traitName)
                }
            } catch (_: ClassNotFoundException) {
                // 接口不在 classpath 中，跳过
            }
        }

        // 检测 TenantEntity 继承（无接口，通过类名判断）
        var current: Class<*>? = clazz
        while (current != null && current != Any::class.java) {
            if (current.simpleName == "TenantEntity") {
                traits.add("TenantAware")
                break
            }
            current = current.superclass
        }

        return traits
    }

    private fun collectDeclaredFields(clazz: Class<*>, columns: MutableList<ColumnMetadata>, isBase: Boolean) {
        for (field in clazz.declaredFields) {
            if (shouldSkipField(field)) continue
            columns.add(createColumnMetadata(field, isBase))
        }
    }

    private fun collectInheritedFields(clazz: Class<*>, columns: MutableList<ColumnMetadata>) {
        val entityBasePackage = "io.github.afgprojects.framework.data.core.entity"
        var current = clazz.superclass

        while (current != null && current != Any::class.java) {
            if (current.name.startsWith(entityBasePackage)) {
                for (field in current.declaredFields) {
                    if (shouldSkipField(field)) continue
                    // 避免重复（子类可能覆盖父类字段）
                    if (columns.none { it.fieldName == field.name }) {
                        columns.add(createColumnMetadata(field, isBase = true))
                    }
                }
            }
            current = current.superclass
        }
    }

    private fun shouldSkipField(field: Field): Boolean {
        if (field.name.startsWith("\$") || field.name == "serialVersionUID") return true
        if (java.lang.reflect.Modifier.isStatic(field.modifiers)) return true
        if (java.lang.reflect.Modifier.isTransient(field.modifiers)) return true
        return false
    }

    private fun createColumnMetadata(field: Field, isBase: Boolean): ColumnMetadata {
        val fieldName = field.name
        val columnName = camelToSnake(fieldName)
        val javaType = field.type.simpleName
        val dbType = javaTypeToDbType(field.type, field)

        // 尝试从 @Column 注解获取更精确的信息
        var nullable = !field.type.isPrimitive
        var unique = false
        var length = 0
        var columnDefaultValue: String? = null
        val comment = ""

        // 读取字段的 @Column 注解
        val columnAnnotation = findFieldAnnotation(field, "jakarta.persistence.Column")
        if (columnAnnotation != null) {
            try {
                nullable = columnAnnotation.javaClass.getMethod("nullable").invoke(columnAnnotation) as Boolean
                unique = columnAnnotation.javaClass.getMethod("unique").invoke(columnAnnotation) as Boolean
                length = columnAnnotation.javaClass.getMethod("length").invoke(columnAnnotation) as Int
            } catch (_: Exception) {
                // 注解方法不存在，使用默认值
            }
        }

        // 尝试从字段默认值推断
        try {
            field.isAccessible = true
            val instance = field.declaringClass.getDeclaredConstructor().newInstance()
            val value = field.get(instance)
            if (value != null) {
                columnDefaultValue = value.toString()
            }
        } catch (_: Exception) {
            // 无法创建实例或获取默认值，忽略
        }

        // BaseEntity 的 id 是主键
        val isPrimaryKey = fieldName == "id"

        return ColumnMetadata(
            fieldName = fieldName,
            columnName = columnName,
            javaType = javaType,
            dbType = dbType,
            nullable = nullable,
            unique = unique,
            length = length,
            defaultValue = columnDefaultValue,
            comment = comment,
            isPrimaryKey = isPrimaryKey,
            isAutoIncrement = isPrimaryKey,
            isBaseField = isBase
        )
    }

    private fun applyColumnAnnotations(clazz: Class<*>, columns: MutableList<ColumnMetadata>) {
        for (field in clazz.declaredFields) {
            if (shouldSkipField(field)) continue

            val columnAnnotation = findFieldAnnotation(field, "jakarta.persistence.Column")
            if (columnAnnotation != null) {
                val idx = columns.indexOfFirst { it.fieldName == field.name }
                if (idx >= 0) {
                    val existing = columns[idx]
                    val updated = existing.copy(
                        columnName = resolveColumnName(field, existing.columnName),
                        nullable = resolveAnnotationBoolean(columnAnnotation, "nullable", existing.nullable),
                        unique = resolveAnnotationBoolean(columnAnnotation, "unique", existing.unique),
                        length = resolveAnnotationInt(columnAnnotation, "length", existing.length),
                        dbType = resolveColumnDbType(columnAnnotation, field, existing.dbType)
                    )
                    columns[idx] = updated
                }
            }
        }
    }

    private fun applyIdAnnotation(clazz: Class<*>, columns: MutableList<ColumnMetadata>) {
        for (field in clazz.declaredFields) {
            if (shouldSkipField(field)) continue

            val idAnnotation = findFieldAnnotation(field, "jakarta.persistence.Id")
            if (idAnnotation != null) {
                val idx = columns.indexOfFirst { it.fieldName == field.name }
                if (idx >= 0) {
                    columns[idx] = columns[idx].copy(isPrimaryKey = true, isAutoIncrement = true)
                }
            }
        }
    }

    private fun resolveColumnName(field: Field, defaultName: String): String {
        val columnAnnotation = findFieldAnnotation(field, "jakarta.persistence.Column")
            ?: return defaultName

        return try {
            val name = columnAnnotation.javaClass.getMethod("name").invoke(columnAnnotation) as String
            if (name.isNotEmpty()) name else defaultName
        } catch (_: Exception) {
            defaultName
        }
    }

    private fun resolveAnnotationBoolean(annotation: Annotation, methodName: String, default: Boolean): Boolean {
        return try {
            annotation.javaClass.getMethod(methodName).invoke(annotation) as Boolean
        } catch (_: Exception) {
            default
        }
    }

    private fun resolveAnnotationInt(annotation: Annotation, methodName: String, default: Int): Int {
        return try {
            annotation.javaClass.getMethod(methodName).invoke(annotation) as Int
        } catch (_: Exception) {
            default
        }
    }

    private fun resolveColumnDbType(annotation: Annotation, field: Field, default: String): String {
        val length = resolveAnnotationInt(annotation, "length", 0)
        return if (length > 0 && field.type == String::class.java) {
            "VARCHAR($length)"
        } else {
            default
        }
    }

    // ========== 注解查找（动态加载，避免编译期硬依赖） ==========

    /**
     * 在类上查找指定注解。
     *
     * <p>使用 Class.forName 动态加载注解类，因为 gradle-plugin 不直接依赖
     * apt-api 或 jakarta.persistence-api，这些注解仅在运行时通过项目 classpath 可用。
     */
    private fun findAnnotation(clazz: Class<*>, annotationClassName: String): Annotation? {
        return try {
            val annotationClass = Class.forName(annotationClassName) as Class<out Annotation>
            clazz.getAnnotation(annotationClass)
        } catch (_: ClassNotFoundException) {
            // 注解类不在 classpath 中，返回 null
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 在字段上查找指定注解。
     */
    private fun findFieldAnnotation(field: Field, annotationClassName: String): Annotation? {
        return try {
            val annotationClass = Class.forName(annotationClassName) as Class<out Annotation>
            field.getAnnotation(annotationClass)
        } catch (_: ClassNotFoundException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    // ========== 类型映射 ==========

    private fun javaTypeToDbType(type: Class<*>, field: Field? = null): String {
        return when (type) {
            Long::class.java, java.lang.Long::class.java -> "BIGINT"
            Integer::class.java, java.lang.Integer::class.java -> "INT"
            Int::class.javaPrimitiveType -> "INT"
            Long::class.javaPrimitiveType -> "BIGINT"
            String::class.java -> {
                // 尝试从 @Column 注解获取 length
                if (field != null) {
                    val columnAnnotation = findFieldAnnotation(field, "jakarta.persistence.Column")
                    if (columnAnnotation != null) {
                        try {
                            val length = columnAnnotation.javaClass.getMethod("length")
                                .invoke(columnAnnotation) as Int
                            if (length > 0) return "VARCHAR($length)"
                        } catch (_: Exception) {
                            // 使用默认
                        }
                    }
                }
                "VARCHAR(255)"
            }
            Boolean::class.java, java.lang.Boolean::class.java -> "BOOLEAN"
            Boolean::class.javaPrimitiveType -> "BOOLEAN"
            java.math.BigDecimal::class.java -> "DECIMAL(19,2)"
            Double::class.java, java.lang.Double::class.java -> "DOUBLE"
            Double::class.javaPrimitiveType -> "DOUBLE"
            Float::class.java, java.lang.Float::class.java -> "FLOAT"
            Float::class.javaPrimitiveType -> "FLOAT"
            Instant::class.java -> "TIMESTAMP"
            java.time.LocalDateTime::class.java -> "TIMESTAMP"
            java.time.LocalDate::class.java -> "DATE"
            java.time.LocalTime::class.java -> "TIME"
            java.util.UUID::class.java -> "UUID"
            java.util.Date::class.java -> "TIMESTAMP"
            ByteArray::class.java -> "BLOB"
            else -> {
                // 枚举类型
                if (type.isEnum) "VARCHAR(32)"
                else "VARCHAR(255)"
            }
        }
    }

    // ========== 文档生成 ==========

    private fun generateMarkdown(tables: List<TableMetadata>): String {
        val sb = StringBuilder()

        // 标题和目录
        sb.appendLine("# ${title.get()}")
        sb.appendLine()
        sb.appendLine("> 自动生成于 ${java.time.LocalDateTime.now()}，请勿手动编辑。")
        sb.appendLine()
        sb.appendLine("## 目录")
        sb.appendLine()

        for (table in tables) {
            sb.appendLine("- [${table.tableName}](#${table.tableName})")
        }

        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()

        // 每个表的详细文档
        for (table in tables) {
            generateTableSection(sb, table)
        }

        return sb.toString()
    }

    private fun generateTableSection(sb: StringBuilder, table: TableMetadata) {
        sb.appendLine("## ${table.tableName}")
        sb.appendLine()

        // 表信息
        if (table.tableComment.isNotEmpty()) {
            sb.appendLine("**说明**：${table.tableComment}")
            sb.appendLine()
        }

        sb.appendLine("| 属性 | 值 |")
        sb.appendLine("|------|------|")
        sb.appendLine("| Java 类 | `${table.className}` |")
        sb.appendLine("| 基类 | `${table.baseClass}` |")
        if (table.traits.isNotEmpty()) {
            sb.appendLine("| 特征 | ${table.traits.joinToString(", ") { "`$it`" }} |")
        }
        sb.appendLine()

        // 字段表格
        sb.appendLine("### 字段列表")
        sb.appendLine()
        sb.appendLine("| 列名 | Java 字段 | 类型 | 可空 | 约束 | 默认值 | 说明 |")
        sb.appendLine("|------|-----------|------|------|------|--------|------|")

        for (col in table.columns) {
            val constraints = buildConstraintsLabel(col)
            val defaultValue = col.defaultValue ?: ""
            val comment = col.comment.ifEmpty { if (col.isBaseField) "*(继承)*" else "" }

            sb.appendLine(
                "| `${col.columnName}` | `${col.fieldName}` | `${col.dbType}` | " +
                "${if (col.nullable) "YES" else "NO"} | $constraints | $defaultValue | $comment |"
            )
        }

        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()
    }

    private fun buildConstraintsLabel(col: ColumnMetadata): String {
        val labels = mutableListOf<String>()
        if (col.isPrimaryKey) labels.add("PK")
        if (col.isAutoIncrement) labels.add("AUTO_INCREMENT")
        if (col.unique) labels.add("UNIQUE")
        return if (labels.isEmpty()) "-" else labels.joinToString(" ")
    }

    // ========== 工具方法 ==========

    private fun camelToSnake(name: String): String {
        val result = StringBuilder()
        for (i in name.indices) {
            val c = name[i]
            if (i > 0 && c.isUpperCase()) {
                result.append('_')
            }
            result.append(c.lowercaseChar())
        }
        return result.toString()
    }
}
