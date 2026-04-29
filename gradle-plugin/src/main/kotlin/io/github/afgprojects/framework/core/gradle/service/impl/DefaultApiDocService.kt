package io.github.afgprojects.framework.core.gradle.service.impl

import io.github.afgprojects.framework.core.gradle.service.*
import io.github.classgraph.ClassGraph
import java.nio.file.Path
import kotlin.io.path.*

/**
 * API 文档生成服务实现
 */
class DefaultApiDocService : ApiDocService {

    override fun generate(sourcePath: Path, basePackage: String?, options: ApiDocOptions): ApiDocResult {
        try {
            val endpoints = mutableListOf<EndpointInfo>()

            // 扫描 Controller 类
            val controllers = scanControllers(sourcePath, basePackage)

            for (controller in controllers) {
                endpoints.addAll(extractEndpoints(controller))
            }

            // 生成 OpenAPI 文档
            val openApi = generateOpenApiDoc(endpoints, options)

            val content = when (options.format) {
                OutputFormat.JSON -> toJson(openApi)
                OutputFormat.YAML -> toYaml(openApi)
            }

            return ApiDocResult(
                success = true,
                content = content,
                endpoints = endpoints,
                message = "生成了 ${endpoints.size} 个 API 端点"
            )
        } catch (e: Exception) {
            return ApiDocResult(
                success = false,
                content = null,
                endpoints = emptyList(),
                message = "生成失败: ${e.message}"
            )
        }
    }

    private data class ControllerInfo(
        val className: String,
        val packageName: String,
        val basePath: String,
        val methods: List<MethodInfo>
    )

    private data class MethodInfo(
        val name: String,
        val httpMethod: String,
        val path: String,
        val parameters: List<ParamInfo>,
        val returnType: String?
    )

    private data class ParamInfo(
        val name: String,
        val type: String,
        val annotation: String?
    )

    private fun scanControllers(sourcePath: Path, basePackage: String?): List<ControllerInfo> {
        val controllers = mutableListOf<ControllerInfo>()

        // 简单的文件扫描（不依赖编译）
        sourcePath.toFile().walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .forEach { file ->
                val content = file.readText()
                if (isController(content)) {
                    val info = parseController(file.nameWithoutExtension, content)
                    if (info != null) {
                        // 过滤包名
                        if (basePackage == null || info.packageName.startsWith(basePackage)) {
                            controllers.add(info)
                        }
                    }
                }
            }

        return controllers
    }

    private fun isController(content: String): Boolean {
        return content.contains("@RestController") || content.contains("@Controller")
    }

    private fun parseController(className: String, content: String): ControllerInfo? {
        // 提取包名
        val packageMatch = Regex("package\\s+([\\w.]+);").find(content)
        val packageName = packageMatch?.groupValues?.get(1) ?: ""

        // 提取基础路径
        val requestMappingMatch = Regex("@RequestMapping\\s*\\(?\\s*[\"']([^\"']+)[\"']").find(content)
        val basePath = requestMappingMatch?.groupValues?.get(1) ?: ""

        // 提取方法
        val methods = mutableListOf<MethodInfo>()
        val methodPattern = Regex(
            "@(Get|Post|Put|Delete|Patch)Mapping\\s*\\(?[^)]*\\)?\\s*" +
            "public\\s+(\\w+(?:<[^>]+>)?)\\s+(\\w+)\\s*\\(([^)]*)\\)"
        )

        methodPattern.findAll(content).forEach { match ->
            val httpMethod = match.groupValues[1].uppercase()
            val returnType = match.groupValues[2]
            val methodName = match.groupValues[3]
            val params = match.groupValues[4]

            // 提取路径
            val pathMatch = Regex("[\"']([^\"']+)[\"']").find(match.value)
            val path = pathMatch?.groupValues?.get(1) ?: ""

            // 解析参数
            val paramList = parseParameters(params)

            methods.add(MethodInfo(methodName, httpMethod, path, paramList, returnType))
        }

        return ControllerInfo(className, packageName, basePath, methods)
    }

    private fun parseParameters(params: String): List<ParamInfo> {
        if (params.isBlank()) return emptyList()

        val result = mutableListOf<ParamInfo>()
        val paramPattern = Regex("@?(\\w+)?\\s+(\\w+(?:<[^>]+>)?)\\s+(\\w+)")

        paramPattern.findAll(params).forEach { match ->
            val annotation = match.groupValues[1].ifEmpty { null }
            val type = match.groupValues[2]
            val name = match.groupValues[3]
            result.add(ParamInfo(name, type, annotation))
        }

        return result
    }

    private fun extractEndpoints(controller: ControllerInfo): List<EndpointInfo> {
        return controller.methods.map { method ->
            val fullPath = controller.basePath + method.path

            EndpointInfo(
                path = fullPath,
                method = method.httpMethod,
                operationId = method.name,
                summary = "${method.name} - ${controller.className}",
                tags = listOf(controller.className.removeSuffix("Controller")),
                parameters = method.parameters.map { param ->
                    ParameterInfo(
                        name = param.name,
                        `in` = when (param.annotation) {
                            "PathVariable" -> "path"
                            "RequestParam" -> "query"
                            "RequestHeader" -> "header"
                            else -> "body"
                        },
                        required = param.annotation == "PathVariable",
                        description = null,
                        schema = SchemaInfo(type = mapJavaType(param.type), format = null, description = null,
                            properties = null, items = null, required = null, `enum` = null, example = null)
                    )
                },
                requestBody = if (method.parameters.any { it.annotation == "RequestBody" }) {
                    val bodyParam = method.parameters.first { it.annotation == "RequestBody" }
                    RequestBodyInfo(
                        required = true,
                        description = null,
                        content = mapOf(
                            "application/json" to MediaTypeInfo(
                                schema = SchemaInfo(
                                    type = "object",
                                    format = null,
                                    description = null,
                                    properties = null,
                                    items = null,
                                    required = null,
                                    `enum` = null,
                                    example = null
                                )
                            )
                        )
                    )
                } else null,
                responses = mapOf(
                    "200" to ResponseInfo(
                        description = "成功",
                        content = mapOf(
                            "application/json" to MediaTypeInfo(
                                schema = SchemaInfo(
                                    type = "object",
                                    format = null,
                                    description = null,
                                    properties = null,
                                    items = null,
                                    required = null,
                                    `enum` = null,
                                    example = null
                                )
                            )
                        )
                    )
                )
            )
        }
    }

    private fun mapJavaType(javaType: String): String {
        return when {
            javaType.contains("String") -> "string"
            javaType.contains("Integer") || javaType.contains("int") -> "integer"
            javaType.contains("Long") || javaType.contains("long") -> "integer"
            javaType.contains("Double") || javaType.contains("double") -> "number"
            javaType.contains("Boolean") || javaType.contains("boolean") -> "boolean"
            javaType.contains("LocalDate") -> "string"
            javaType.contains("LocalDateTime") -> "string"
            javaType.contains("List") -> "array"
            else -> "object"
        }
    }

    private fun generateOpenApiDoc(endpoints: List<EndpointInfo>, options: ApiDocOptions): Map<String, Any> {
        val paths = mutableMapOf<String, MutableMap<String, Any>>()

        for (endpoint in endpoints) {
            val pathItem = paths.getOrPut(endpoint.path) { mutableMapOf() }

            val operation = mutableMapOf<String, Any>(
                "operationId" to (endpoint.operationId ?: ""),
                "summary" to (endpoint.summary ?: ""),
                "tags" to endpoint.tags
            )

            if (endpoint.parameters.isNotEmpty()) {
                operation["parameters"] = endpoint.parameters.map { param ->
                    mapOf(
                        "name" to param.name,
                        "in" to param.`in`,
                        "required" to param.required,
                        "schema" to mapOf("type" to (param.schema?.type ?: "string"))
                    )
                }
            }

            if (endpoint.requestBody != null) {
                operation["requestBody"] = mapOf(
                    "required" to endpoint.requestBody.required,
                    "content" to endpoint.requestBody.content.mapValues { (_, media) ->
                        mapOf("schema" to (media.schema?.let { s ->
                            mapOf("type" to s.type)
                        } ?: emptyMap()))
                    }
                )
            }

            operation["responses"] = endpoint.responses.mapValues { (_, response) ->
                mapOf<String, Any>(
                    "description" to response.description,
                    "content" to (response.content?.mapValues { (_, media) ->
                        mapOf<String, Any>("schema" to (media.schema?.let { s ->
                            mapOf<String, Any>("type" to s.type)
                        } ?: emptyMap<String, Any>()))
                    } ?: emptyMap<String, Any>())
                )
            }

            pathItem[endpoint.method.lowercase()] = operation
        }

        return mapOf(
            "openapi" to "3.0.3",
            "info" to mapOf(
                "title" to "API Documentation",
                "version" to "1.0.0"
            ),
            "paths" to paths
        )
    }

    private fun toJson(doc: Map<String, Any>): String {
        return buildString {
            appendLine("{")
            appendLine("  \"openapi\": \"${doc["openapi"]}\",")
            appendLine("  \"info\": {")
            val info = doc["info"] as Map<*, *>
            appendLine("    \"title\": \"${info["title"]}\",")
            appendLine("    \"version\": \"${info["version"]}\"")
            appendLine("  },")
            appendLine("  \"paths\": {")
            val paths = doc["paths"] as Map<*, *>
            val pathEntries = paths.entries.toList()
            pathEntries.forEachIndexed { index, (path, methods) ->
                append("    \"$path\": {")
                val methodMap = methods as Map<*, *>
                val methodEntries = methodMap.entries.toList()
                methodEntries.forEachIndexed { mIndex, (method, operation) ->
                    append("\"$method\": {}")
                    if (mIndex < methodEntries.size - 1) append(", ")
                }
                append("}")
                if (index < pathEntries.size - 1) appendLine(",")
                else appendLine()
            }
            appendLine("  }")
            append("}")
        }
    }

    private fun toYaml(doc: Map<String, Any>): String {
        return buildString {
            appendLine("openapi: ${doc["openapi"]}")
            appendLine("info:")
            val info = doc["info"] as Map<*, *>
            appendLine("  title: ${info["title"]}")
            appendLine("  version: ${info["version"]}")
            appendLine("paths:")
            val paths = doc["paths"] as Map<*, *>
            for ((path, methods) in paths) {
                appendLine("  $path:")
                val methodMap = methods as Map<*, *>
                for ((method, _) in methodMap) {
                    appendLine("    $method:")
                    appendLine("      operationId: placeholder")
                    appendLine("      responses:")
                    appendLine("        '200':")
                    appendLine("          description: Success")
                }
            }
        }
    }
}