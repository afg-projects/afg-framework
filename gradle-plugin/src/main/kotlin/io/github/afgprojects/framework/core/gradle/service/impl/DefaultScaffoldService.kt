package io.github.afgprojects.framework.core.gradle.service.impl

import io.github.afgprojects.framework.core.gradle.service.*
import java.nio.file.Path
import kotlin.io.path.*

/**
 * 脚手架服务实现
 *
 * 简化架构：Entity + Controller，使用 DataManager 提供 CRUD
 */
class DefaultScaffoldService : ScaffoldService {

    override fun generate(
        entityName: String,
        fields: List<FieldDefinition>,
        outputPath: Path,
        options: ScaffoldOptions
    ): ScaffoldResult {
        try {
            val packagePath = options.basePackage.replace('.', '/')
            val mainJava = outputPath.resolve("src/main/java/$packagePath")
            val resources = outputPath.resolve("src/main/resources")

            var entityFile: Path? = null
            var controllerFile: Path? = null
            var serviceFile: Path? = null
            var migrationFile: Path? = null
            val testFiles = mutableListOf<Path>()

            val tableName = options.tableName ?: inferTableName(entityName)
            val apiPath = options.apiPath ?: "/api/${tableName}"

            // 生成 Entity
            if (options.generateEntity) {
                val entityDir = mainJava.resolve("entity")
                entityDir.createDirectories()
                entityFile = entityDir.resolve("${entityName}Entity.java")
                entityFile.writeText(generateEntity(entityName, tableName, fields, options))
            }

            // 生成 Controller（直接使用 DataManager）
            if (options.generateController) {
                val controllerDir = mainJava.resolve("controller")
                controllerDir.createDirectories()
                controllerFile = controllerDir.resolve("${entityName}Controller.java")
                controllerFile.writeText(generateController(entityName, tableName, apiPath, fields, options))
            }

            // 生成 Service（可选，复杂业务逻辑时使用）
            if (options.generateService) {
                val serviceDir = mainJava.resolve("service")
                serviceDir.createDirectories()
                serviceFile = serviceDir.resolve("${entityName}Service.java")
                serviceFile.writeText(generateService(entityName, options))
            }

            // 生成迁移脚本
            if (options.generateMigration) {
                val dbDir = resources.resolve("db/changelog")
                dbDir.createDirectories()
                val timestamp = java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                )
                migrationFile = dbDir.resolve("${timestamp}_create_${tableName}_table.xml")
                migrationFile.writeText(generateMigration(tableName, fields, timestamp))
            }

            // 生成测试
            if (options.generateTests) {
                val testJava = outputPath.resolve("src/test/java/$packagePath")
                testFiles.addAll(generateTests(entityName, tableName, fields, testJava, options))
            }

            return ScaffoldResult(
                success = true,
                entityFile = entityFile,
                controllerFile = controllerFile,
                serviceFile = serviceFile,
                migrationFile = migrationFile,
                testFiles = testFiles,
                message = "脚手架生成成功: $entityName\n生成了 ${listOfNotNull(entityFile, controllerFile, serviceFile, migrationFile).size + testFiles.size} 个文件"
            )
        } catch (e: Exception) {
            return ScaffoldResult(
                success = false,
                entityFile = null,
                controllerFile = null,
                serviceFile = null,
                migrationFile = null,
                testFiles = emptyList(),
                message = "生成失败: ${e.message}"
            )
        }
    }

    private fun inferTableName(entityName: String): String {
        val result = StringBuilder()
        for (i in entityName.indices) {
            val c = entityName[i]
            if (i > 0 && c.isUpperCase()) {
                result.append('_')
            }
            result.append(c.lowercaseChar())
        }
        return result.toString()
    }

    private fun generateEntity(
        entityName: String,
        tableName: String,
        fields: List<FieldDefinition>,
        options: ScaffoldOptions
    ): String {
        return buildString {
            appendLine("package ${options.basePackage}.entity;")
            appendLine()

            if (options.useLombok) {
                appendLine("import lombok.Data;")
                appendLine("import lombok.Builder;")
                appendLine("import lombok.NoArgsConstructor;")
                appendLine("import lombok.AllArgsConstructor;")
                appendLine()
            }

            if (options.useValidation) {
                appendLine("import jakarta.validation.constraints.*;")
                appendLine()
            }

            appendLine("import io.github.afgprojects.framework.data.core.entity.BaseEntity;")
            appendLine("import jakarta.persistence.*;")
            appendLine()

            if (options.useSwagger) {
                appendLine("import io.swagger.v3.oas.annotations.media.Schema;")
                appendLine()
            }

            appendLine("/**")
            appendLine(" * 实体类: $entityName")
            appendLine(" * 对应表: $tableName")
            appendLine(" */")
            if (options.useLombok) {
                appendLine("@Data")
                appendLine("@Builder")
                appendLine("@NoArgsConstructor")
                appendLine("@AllArgsConstructor")
            }
            appendLine("@Entity")
            appendLine("@Table(name = \"$tableName\")")
            if (options.useSwagger) {
                appendLine("@Schema(description = \"$entityName 实体\")")
            }
            appendLine("public class ${entityName}Entity extends BaseEntity {")
            appendLine()

            for (field in fields) {
                appendLine(generateField(field, options))
            }

            if (!options.useLombok) {
                appendLine()
                for (field in fields) {
                    appendLine(generateGetterSetter(field))
                }
            }

            appendLine("}")
        }
    }

    private fun generateField(field: FieldDefinition, options: ScaffoldOptions): String {
        return buildString {
            append("    ")

            if (options.useValidation && !field.nullable) {
                when (field.type) {
                    FieldType.STRING -> append("@NotBlank(message = \"${field.name}不能为空\") ")
                    else -> append("@NotNull(message = \"${field.name}不能为空\") ")
                }
            }

            if (options.useValidation && field.type == FieldType.STRING && field.length != null) {
                append("@Size(max = ${field.length}, message = \"${field.name}长度不能超过${field.length}\") ")
            }

            if (options.useValidation && field.validation?.email == true) {
                append("@Email(message = \"${field.name}格式不正确\") ")
            }

            appendLine()
            append("    @Column(name = \"${field.name}\"")
            if (field.nullable) append(", nullable = true")
            if (field.unique) append(", unique = true")
            if (field.length != null && field.type == FieldType.STRING) append(", length = ${field.length}")
            append(")")

            if (options.useSwagger) {
                appendLine()
                append("    @Schema(description = \"${field.name}\")")
            }

            appendLine()
            append("    private ${field.type.javaType} ${field.name};")
            appendLine()
        }
    }

    private fun generateGetterSetter(field: FieldDefinition): String {
        val capitalizedName = field.name.replaceFirstChar { it.uppercaseChar() }
        return buildString {
            appendLine("    public ${field.type.javaType} get$capitalizedName() { return ${field.name}; }")
            appendLine("    public void set$capitalizedName(${field.type.javaType} ${field.name}) { this.${field.name} = ${field.name}; }")
            appendLine()
        }
    }

    /**
     * 生成 Controller - 直接使用 DataManager
     */
    private fun generateController(
        entityName: String,
        tableName: String,
        apiPath: String,
        fields: List<FieldDefinition>,
        options: ScaffoldOptions
    ): String {
        return buildString {
            appendLine("package ${options.basePackage}.controller;")
            appendLine()
            appendLine("import ${options.basePackage}.entity.${entityName}Entity;")
            appendLine("import io.github.afgprojects.framework.data.core.DataManager;")
            appendLine("import io.github.afgprojects.framework.data.core.query.Condition;")
            appendLine("import io.github.afgprojects.framework.data.core.query.Conditions;")
            appendLine("import io.github.afgprojects.framework.data.core.query.Page;")
            appendLine("import io.github.afgprojects.framework.data.core.page.PageRequest;")
            appendLine("import lombok.RequiredArgsConstructor;")
            appendLine("import org.springframework.http.ResponseEntity;")
            appendLine("import org.springframework.web.bind.annotation.*;")
            appendLine()

            if (options.useSwagger) {
                appendLine("import io.swagger.v3.oas.annotations.Operation;")
                appendLine("import io.swagger.v3.oas.annotations.tags.Tag;")
                appendLine()
            }

            appendLine("import java.util.List;")
            appendLine()

            appendLine("/**")
            appendLine(" * ${entityName} 控制器")
            appendLine(" * <p>")
            appendLine(" * 使用 DataManager 进行数据操作，无需 Repository/Service 层")
            appendLine(" */")
            appendLine("@RestController")
            appendLine("@RequestMapping(\"$apiPath\")")
            if (options.useSwagger) {
                appendLine("@Tag(name = \"$entityName\", description = \"${entityName} API\")")
            }
            appendLine("@RequiredArgsConstructor")
            appendLine("public class ${entityName}Controller {")
            appendLine()
            appendLine("    private final DataManager dataManager;")
            appendLine()

            // GET /{id}
            appendLine("    @GetMapping(\"/{id}\")")
            if (options.useSwagger) {
                appendLine("    @Operation(summary = \"根据ID查找\")")
            }
            appendLine("    public ResponseEntity<${entityName}Entity> getById(@PathVariable Long id) {")
            appendLine("        return dataManager.entity(${entityName}Entity.class)")
            appendLine("            .findById(id)")
            appendLine("            .map(ResponseEntity::ok)")
            appendLine("            .orElse(ResponseEntity.notFound().build());")
            appendLine("    }")
            appendLine()

            // GET / - 分页列表
            appendLine("    @GetMapping")
            if (options.useSwagger) {
                appendLine("    @Operation(summary = \"分页查询\")")
            }
            appendLine("    public Page<${entityName}Entity> list(")
            appendLine("        @RequestParam(defaultValue = \"0\") int page,")
            appendLine("        @RequestParam(defaultValue = \"20\") int size")
            appendLine("    ) {")
            appendLine("        return dataManager.entity(${entityName}Entity.class)")
            appendLine("            .findAll(Conditions.empty(), PageRequest.of(page, size));")
            appendLine("    }")
            appendLine()

            // GET /search - 条件搜索
            appendLine("    @GetMapping(\"/search\")")
            if (options.useSwagger) {
                appendLine("    @Operation(summary = \"条件搜索\")")
            }
            appendLine("    public List<${entityName}Entity> search(@RequestParam(required = false) String keyword) {")
            appendLine("        if (keyword == null || keyword.isBlank()) {")
            appendLine("            return dataManager.entity(${entityName}Entity.class).findAll();")
            appendLine("        }")
            appendLine("        // TODO: 根据实际字段构建查询条件")
            appendLine("        Condition condition = Conditions.builder()")
            appendLine("            // .like(\"name\", \"%\" + keyword + \"%\")")
            appendLine("            .build();")
            appendLine("        return dataManager.entity(${entityName}Entity.class).findAll(condition);")
            appendLine("    }")
            appendLine()

            // POST /
            appendLine("    @PostMapping")
            if (options.useSwagger) {
                appendLine("    @Operation(summary = \"创建\")")
            }
            appendLine("    public ResponseEntity<${entityName}Entity> create(@RequestBody ${entityName}Entity entity) {")
            appendLine("        ${entityName}Entity saved = dataManager.entity(${entityName}Entity.class).save(entity);")
            appendLine("        return ResponseEntity.ok(saved);")
            appendLine("    }")
            appendLine()

            // PUT /{id}
            appendLine("    @PutMapping(\"/{id}\")")
            if (options.useSwagger) {
                appendLine("    @Operation(summary = \"更新\")")
            }
            appendLine("    public ResponseEntity<${entityName}Entity> update(")
            appendLine("        @PathVariable Long id,")
            appendLine("        @RequestBody ${entityName}Entity entity")
            appendLine("    ) {")
            appendLine("        return dataManager.entity(${entityName}Entity.class)")
            appendLine("            .findById(id)")
            appendLine("            .map(existing -> {")
            appendLine("                entity.setId(id);")
            appendLine("                return ResponseEntity.ok(dataManager.entity(${entityName}Entity.class).save(entity));")
            appendLine("            })")
            appendLine("            .orElse(ResponseEntity.notFound().build());")
            appendLine("    }")
            appendLine()

            // DELETE /{id}
            appendLine("    @DeleteMapping(\"/{id}\")")
            if (options.useSwagger) {
                appendLine("    @Operation(summary = \"删除\")")
            }
            appendLine("    public ResponseEntity<Void> delete(@PathVariable Long id) {")
            appendLine("        dataManager.entity(${entityName}Entity.class).deleteById(id);")
            appendLine("        return ResponseEntity.noContent().build();")
            appendLine("    }")
            appendLine()

            appendLine("}")
        }
    }

    /**
     * 生成 Service - 可选，用于复杂业务逻辑
     */
    private fun generateService(entityName: String, options: ScaffoldOptions): String {
        return """
package ${options.basePackage}.service;

import ${options.basePackage}.entity.${entityName}Entity;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.Page;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * ${entityName} 服务类
 * <p>
 * 用于复杂业务逻辑，简单 CRUD 直接使用 DataManager
 */
@Service
@RequiredArgsConstructor
public class ${entityName}Service {

    private final DataManager dataManager;

    // ==================== 直接使用 DataManager 的示例 ====================
    // 简单 CRUD 可以直接在 Controller 中调用 DataManager，无需 Service 层

    /**
     * 复杂业务示例：带业务校验的保存
     */
    @Transactional
    public ${entityName}Entity saveWithValidation(${entityName}Entity entity) {
        // TODO: 业务校验逻辑
        return dataManager.entity(${entityName}Entity.class).save(entity);
    }

    /**
     * 复杂业务示例：批量操作
     */
    @Transactional
    public List<${entityName}Entity> batchSave(List<${entityName}Entity> entities) {
        return dataManager.entity(${entityName}Entity.class).saveAll(entities);
    }
}

        """.trimIndent()
    }

    private fun generateMigration(
        tableName: String,
        fields: List<FieldDefinition>,
        timestamp: String
    ): String {
        return buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            appendLine("<databaseChangeLog")
            appendLine("    xmlns=\"http://www.liquibase.org/xml/ns/dbchangelog\"")
            appendLine("    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")
            appendLine("    xsi:schemaLocation=\"http://www.liquibase.org/xml/ns/dbchangelog")
            appendLine("                        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.26.xsd\">")
            appendLine()
            appendLine("    <changeSet id=\"$timestamp\" author=\"scaffold\">")
            appendLine("        <createTable tableName=\"$tableName\">")
            appendLine("            <column name=\"id\" type=\"BIGINT\" autoIncrement=\"true\">")
            appendLine("                <constraints primaryKey=\"true\" nullable=\"false\"/>")
            appendLine("            </column>")
            appendLine("            <column name=\"created_at\" type=\"TIMESTAMP\">")
            appendLine("                <constraints nullable=\"false\"/>")
            appendLine("            </column>")
            appendLine("            <column name=\"updated_at\" type=\"TIMESTAMP\">")
            appendLine("                <constraints nullable=\"false\"/>")
            appendLine("            </column>")
            appendLine("            <column name=\"enabled\" type=\"BOOLEAN\" defaultValueBoolean=\"true\">")
            appendLine("                <constraints nullable=\"false\"/>")
            appendLine("            </column>")

            for (field in fields) {
                appendLine("            <column name=\"${field.name}\" type=\"${field.type.sqlType}\">")
                if (!field.nullable) {
                    appendLine("                <constraints nullable=\"false\"/>")
                }
                appendLine("            </column>")
            }

            appendLine("        </createTable>")
            appendLine("    </changeSet>")
            appendLine()
            appendLine("</databaseChangeLog>")
        }
    }

    private fun generateTests(
        entityName: String,
        tableName: String,
        fields: List<FieldDefinition>,
        testJava: Path,
        options: ScaffoldOptions
    ): List<Path> {
        val files = mutableListOf<Path>()

        // Controller Test
        val controllerTestDir = testJava.resolve("controller")
        controllerTestDir.createDirectories()
        val controllerTest = controllerTestDir.resolve("${entityName}ControllerTest.java")
        controllerTest.writeText("""
package ${options.basePackage}.controller;

import ${options.basePackage}.entity.${entityName}Entity;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.EntityProxy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ${entityName}Controller 测试
 */
@WebMvcTest(${entityName}Controller.class)
class ${entityName}ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataManager dataManager;

    @Test
    @DisplayName("GET /{id} - 成功")
    void getById_Success() throws Exception {
        // Given
        var entity = new ${entityName}Entity();
        entity.setId(1L);

        @SuppressWarnings("unchecked")
        var proxy = (EntityProxy<${entityName}Entity>) mock(EntityProxy.class);
        when(dataManager.entity(${entityName}Entity.class)).thenReturn(proxy);
        when(proxy.findById(1L)).thenReturn(Optional.of(entity));

        // When & Then
        mockMvc.perform(get("/api/${tableName}/1"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /{id} - 未找到")
    void getById_NotFound() throws Exception {
        // Given
        @SuppressWarnings("unchecked")
        var proxy = (EntityProxy<${entityName}Entity>) mock(EntityProxy.class);
        when(dataManager.entity(${entityName}Entity.class)).thenReturn(proxy);
        when(proxy.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/${tableName}/1"))
            .andExpect(status().isNotFound());
    }
}

        """.trimIndent())
        files.add(controllerTest)

        return files
    }
}