package io.github.afgprojects.framework.core.gradle.service

import java.nio.file.Path

/**
 * API 文档生成服务接口
 */
interface ApiDocService {

    /**
     * 生成 OpenAPI 文档
     *
     * @param sourcePath 源代码路径
     * @param basePackage 扫描的包名
     * @param options 生成选项
     * @return OpenAPI 文档内容
     */
    fun generate(sourcePath: Path, basePackage: String?, options: ApiDocOptions = ApiDocOptions()): ApiDocResult
}

/**
 * API 文档生成选项
 */
data class ApiDocOptions(
    val format: OutputFormat = OutputFormat.YAML,
    val includeDeprecated: Boolean = false,
    val groupByTag: Boolean = true,
    val defaultMediaType: String = "application/json"
)

/**
 * 输出格式
 */
enum class OutputFormat {
    JSON,
    YAML
}

/**
 * API 文档生成结果
 */
data class ApiDocResult(
    val success: Boolean,
    val content: String?,
    val endpoints: List<EndpointInfo>,
    val message: String
)

/**
 * 端点信息
 */
data class EndpointInfo(
    val path: String,
    val method: String,
    val operationId: String?,
    val summary: String?,
    val tags: List<String>,
    val parameters: List<ParameterInfo>,
    val requestBody: RequestBodyInfo?,
    val responses: Map<String, ResponseInfo>
)

/**
 * 参数信息
 */
data class ParameterInfo(
    val name: String,
    val `in`: String,  // path, query, header, cookie
    val required: Boolean,
    val description: String?,
    val schema: SchemaInfo?
)

/**
 * 请求体信息
 */
data class RequestBodyInfo(
    val required: Boolean,
    val description: String?,
    val content: Map<String, MediaTypeInfo>
)

/**
 * 响应信息
 */
data class ResponseInfo(
    val description: String,
    val content: Map<String, MediaTypeInfo>?
)

/**
 * 媒体类型信息
 */
data class MediaTypeInfo(
    val schema: SchemaInfo?
)

/**
 * Schema 信息
 */
data class SchemaInfo(
    val type: String,
    val format: String?,
    val description: String?,
    val properties: Map<String, SchemaInfo>?,
    val items: SchemaInfo?,
    val required: List<String>?,
    val `enum`: List<String>?,
    val example: Any?
)