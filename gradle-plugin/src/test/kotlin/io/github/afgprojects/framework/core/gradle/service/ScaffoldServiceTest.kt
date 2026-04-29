package io.github.afgprojects.framework.core.gradle.service

import io.github.afgprojects.framework.core.gradle.service.impl.DefaultScaffoldService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.test.assertTrue

/**
 * ScaffoldService 测试
 */
@DisplayName("ScaffoldService 测试")
class ScaffoldServiceTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var projectDir: Path
    private val service = DefaultScaffoldService()

    @BeforeEach
    fun setup() {
        projectDir = tempDir.resolve("test-project")
        projectDir.toFile().mkdirs()

        // 创建基本目录结构
        val srcDir = projectDir.resolve("src/main/java/com/example")
        srcDir.toFile().mkdirs()
        projectDir.resolve("src/main/resources/db/changelog").toFile().mkdirs()
        projectDir.resolve("src/test/java/com/example").toFile().mkdirs()
    }

    @Test
    @DisplayName("应该生成 Entity 文件")
    fun `should generate entity file`() {
        val fields = listOf(
            FieldDefinition("name", FieldType.STRING),
            FieldDefinition("email", FieldType.STRING, length = 100),
            FieldDefinition("age", FieldType.INTEGER)
        )

        val result = service.generate("User", fields, projectDir, ScaffoldOptions(basePackage = "com.example"))

        assertTrue(result.success)
        assertTrue(result.entityFile != null)
        assertTrue(result.entityFile!!.exists())

        val content = result.entityFile!!.readText()
        assertTrue(content.contains("class UserEntity"))
        assertTrue(content.contains("private String name"))
        assertTrue(content.contains("private String email"))
        assertTrue(content.contains("private Integer age"))
    }

    @Test
    @DisplayName("应该生成 Controller 文件")
    fun `should generate controller file`() {
        val fields = listOf(
            FieldDefinition("name", FieldType.STRING)
        )

        val result = service.generate("User", fields, projectDir, ScaffoldOptions(basePackage = "com.example"))

        assertTrue(result.success)
        assertTrue(result.controllerFile != null)
        assertTrue(result.controllerFile!!.exists())

        val content = result.controllerFile!!.readText()
        assertTrue(content.contains("class UserController"))
        assertTrue(content.contains("DataManager"))
        assertTrue(content.contains("@GetMapping"))
        assertTrue(content.contains("@PostMapping"))
    }

    @Test
    @DisplayName("应该生成迁移文件")
    fun `should generate migration file`() {
        val fields = listOf(
            FieldDefinition("name", FieldType.STRING),
            FieldDefinition("age", FieldType.INTEGER)
        )

        val result = service.generate("User", fields, projectDir, ScaffoldOptions(basePackage = "com.example"))

        assertTrue(result.success)
        assertTrue(result.migrationFile != null)
        assertTrue(result.migrationFile!!.exists())

        val content = result.migrationFile!!.readText()
        assertTrue(content.contains("createTable"))
        assertTrue(content.contains("tableName=\"user\""))
        assertTrue(content.contains("column name=\"name\""))
        assertTrue(content.contains("column name=\"age\""))
    }

    @Test
    @DisplayName("应该使用 Lombok 注解")
    fun `should use lombok annotations`() {
        val fields = listOf(FieldDefinition("name", FieldType.STRING))

        val result = service.generate("User", fields, projectDir, ScaffoldOptions(
            basePackage = "com.example",
            useLombok = true
        ))

        assertTrue(result.success)

        val content = result.entityFile!!.readText()
        assertTrue(content.contains("@Data"))
        assertTrue(content.contains("@Builder"))
    }

    @Test
    @DisplayName("应该添加验证注解")
    fun `should add validation annotations`() {
        val fields = listOf(
            FieldDefinition("name", FieldType.STRING, nullable = false),
            FieldDefinition("email", FieldType.STRING, nullable = false)
        )

        val result = service.generate("User", fields, projectDir, ScaffoldOptions(
            basePackage = "com.example",
            useValidation = true
        ))

        assertTrue(result.success)

        val content = result.entityFile!!.readText()
        assertTrue(content.contains("@NotBlank"))
    }

    @Test
    @DisplayName("应该推断正确的表名")
    fun `should infer correct table name`() {
        val fields = listOf(FieldDefinition("name", FieldType.STRING))

        val result = service.generate("UserOrder", fields, projectDir, ScaffoldOptions(basePackage = "com.example"))

        assertTrue(result.success)

        val entityContent = result.entityFile!!.readText()
        assertTrue(entityContent.contains("tableName = \"user_order\""))
    }

    @Test
    @DisplayName("应该生成测试文件")
    fun `should generate test files`() {
        val fields = listOf(FieldDefinition("name", FieldType.STRING))

        val result = service.generate("User", fields, projectDir, ScaffoldOptions(
            basePackage = "com.example",
            generateTests = true
        ))

        assertTrue(result.success)
        assertTrue(result.testFiles.isNotEmpty())
    }

    @Test
    @DisplayName("Controller 应该直接使用 DataManager")
    fun `controller should use DataManager directly`() {
        val fields = listOf(FieldDefinition("name", FieldType.STRING))

        val result = service.generate("User", fields, projectDir, ScaffoldOptions(basePackage = "com.example"))

        assertTrue(result.success)

        val content = result.controllerFile!!.readText()
        assertTrue(content.contains("private final DataManager dataManager"))
        assertTrue(content.contains("dataManager.entity(UserEntity.class)"))
    }
}