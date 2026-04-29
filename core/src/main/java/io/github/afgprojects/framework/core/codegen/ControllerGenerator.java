package io.github.afgprojects.framework.core.codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;

/**
 * Controller 代码生成器
 *
 * <p>根据 Entity 生成 RESTful Controller
 *
 * @since 1.0.0
 */
public class ControllerGenerator implements CodeGenerator {

    @Override
    @NonNull
    public String generate(@NonNull GeneratorContext context) {
        StringBuilder sb = new StringBuilder();

        String entityName = context.getClassName();
        String entityVar = Character.toLowerCase(entityName.charAt(0)) + entityName.substring(1);
        String basePath = context.getTableName() != null
                ? "/" + context.getTableName().replace("t_", "").replace("_", "-")
                : "/" + entityVar + "s";

        // 包声明
        sb.append("package ").append(context.getPackageName()).append(";\n\n");

        // 导入
        List<String> imports = collectImports(context);
        for (String imp : imports) {
            sb.append("import ").append(imp).append(";\n");
        }
        sb.append("\n");

        // 类注释
        sb.append("/**\n")
          .append(" * ").append(entityName).append(" Controller\n")
          .append(" */\n");

        // 类注解
        sb.append("@RestController\n")
          .append("@RequestMapping(\"").append(basePath).append("\")\n")
          .append("@Tag(name = \"").append(entityName).append("\")\n");

        // 类声明
        sb.append("public class ").append(entityName).append("Controller {\n\n");

        // 依赖注入
        sb.append("    private final ").append(entityName).append("Service ").append(entityVar).append("Service;\n\n");

        sb.append("    public ").append(entityName).append("Controller(")
          .append(entityName).append("Service ").append(entityVar).append("Service) {\n")
          .append("        this.").append(entityVar).append("Service = ").append(entityVar).append("Service;\n")
          .append("    }\n\n");

        // CRUD 方法
        sb.append(generateCreateMethod(entityName, entityVar));
        sb.append(generateGetByIdMethod(entityName, entityVar));
        sb.append(generateListMethod(entityName, entityVar));
        sb.append(generateUpdateMethod(entityName, entityVar));
        sb.append(generateDeleteMethod(entityName, entityVar));

        sb.append("}\n");

        return sb.toString();
    }

    @Override
    @NonNull
    public String getName() {
        return "ControllerGenerator";
    }

    @Override
    @NonNull
    public String getTemplateType() {
        return "controller";
    }

    private List<String> collectImports(GeneratorContext context) {
        List<String> imports = new ArrayList<>();

        imports.add("io.swagger.v3.oas.annotations.Operation");
        imports.add("io.swagger.v3.oas.annotations.Parameter");
        imports.add("io.swagger.v3.oas.annotations.responses.ApiResponse");
        imports.add("io.swagger.v3.oas.annotations.tags.Tag");
        imports.add("org.springframework.web.bind.annotation.*");
        imports.add("org.springframework.validation.annotation.Validated");
        imports.add("jakarta.validation.Valid");
        imports.add("lombok.RequiredArgsConstructor");

        String entityName = context.getClassName();
        String basePackage = context.getPackageName().replace(".controller", "");

        imports.add(basePackage + ".entity." + entityName);
        imports.add(basePackage + ".service." + entityName + "Service");
        imports.add(basePackage + ".dto." + entityName + "CreateRequest");
        imports.add(basePackage + ".dto." + entityName + "UpdateRequest");
        imports.add("io.github.afgprojects.core.model.Result");
        imports.add("io.github.afgprojects.core.model.Results");

        return imports.stream().distinct().sorted().collect(Collectors.toList());
    }

    private String generateCreateMethod(String entityName, String entityVar) {
        return "    @Operation(summary = \"创建" + entityName + "\")\n" +
               "    @ApiResponse(responseCode = \"200\", description = \"创建成功\")\n" +
               "    @PostMapping\n" +
               "    public Result<" + entityName + "> create(\n" +
               "            @Valid @RequestBody " + entityName + "CreateRequest request) {\n" +
               "        return Results.success(" + entityVar + "Service.create(request));\n" +
               "    }\n\n";
    }

    private String generateGetByIdMethod(String entityName, String entityVar) {
        return "    @Operation(summary = \"根据ID查询" + entityName + "\")\n" +
               "    @ApiResponse(responseCode = \"200\", description = \"查询成功\")\n" +
               "    @GetMapping(\"/{id}\")\n" +
               "    public Result<" + entityName + "> getById(\n" +
               "            @Parameter(description = \"ID\") @PathVariable Long id) {\n" +
               "        return Results.success(" + entityVar + "Service.getById(id));\n" +
               "    }\n\n";
    }

    private String generateListMethod(String entityName, String entityVar) {
        return "    @Operation(summary = \"查询" + entityName + "列表\")\n" +
               "    @ApiResponse(responseCode = \"200\", description = \"查询成功\")\n" +
               "    @GetMapping\n" +
               "    public Result<List<" + entityName + ">> list() {\n" +
               "        return Results.success(" + entityVar + "Service.list());\n" +
               "    }\n\n";
    }

    private String generateUpdateMethod(String entityName, String entityVar) {
        return "    @Operation(summary = \"更新" + entityName + "\")\n" +
               "    @ApiResponse(responseCode = \"200\", description = \"更新成功\")\n" +
               "    @PutMapping(\"/{id}\")\n" +
               "    public Result<" + entityName + "> update(\n" +
               "            @Parameter(description = \"ID\") @PathVariable Long id,\n" +
               "            @Valid @RequestBody " + entityName + "UpdateRequest request) {\n" +
               "        return Results.success(" + entityVar + "Service.update(id, request));\n" +
               "    }\n\n";
    }

    private String generateDeleteMethod(String entityName, String entityVar) {
        return "    @Operation(summary = \"删除" + entityName + "\")\n" +
               "    @ApiResponse(responseCode = \"200\", description = \"删除成功\")\n" +
               "    @DeleteMapping(\"/{id}\")\n" +
               "    public Result<Void> delete(\n" +
               "            @Parameter(description = \"ID\") @PathVariable Long id) {\n" +
               "        " + entityVar + "Service.delete(id);\n" +
               "        return Results.success();\n" +
               "    }\n\n";
    }
}