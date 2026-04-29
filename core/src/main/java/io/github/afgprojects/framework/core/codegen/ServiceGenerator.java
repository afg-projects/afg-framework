package io.github.afgprojects.framework.core.codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;

/**
 * Service 代码生成器
 *
 * <p>根据 Entity 生成 Service 接口和实现类
 *
 * @since 1.0.0
 */
public class ServiceGenerator implements CodeGenerator {

    @Override
    @NonNull
    public String generate(@NonNull GeneratorContext context) {
        StringBuilder sb = new StringBuilder();

        String entityName = context.getClassName();
        String entityVar = Character.toLowerCase(entityName.charAt(0)) + entityName.substring(1);

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
          .append(" * ").append(entityName).append(" Service\n")
          .append(" */\n");

        // 类注解
        sb.append("@Service\n")
          .append("@RequiredArgsConstructor\n");

        // 类声明
        sb.append("public class ").append(entityName).append("Service {\n\n");

        // Mapper 注入
        sb.append("    private final ").append(entityName).append("Mapper ").append(entityVar).append("Mapper;\n\n");

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
        return "ServiceGenerator";
    }

    @Override
    @NonNull
    public String getTemplateType() {
        return "service";
    }

    private List<String> collectImports(GeneratorContext context) {
        List<String> imports = new ArrayList<>();

        imports.add("org.springframework.stereotype.Service");
        imports.add("lombok.RequiredArgsConstructor");

        String entityName = context.getClassName();
        String basePackage = context.getPackageName().replace(".service", "");

        imports.add(basePackage + ".entity." + entityName);
        imports.add(basePackage + ".mapper." + entityName + "Mapper");
        imports.add(basePackage + ".dto." + entityName + "CreateRequest");
        imports.add(basePackage + ".dto." + entityName + "UpdateRequest");

        return imports.stream().distinct().sorted().collect(Collectors.toList());
    }

    private String generateCreateMethod(String entityName, String entityVar) {
        return "    public " + entityName + " create(" + entityName + "CreateRequest request) {\n" +
               "        " + entityName + " entity = " + entityName + "Mapper.INSTANCE.toEntity(request);\n" +
               "        " + entityVar + "Mapper.insert(entity);\n" +
               "        return entity;\n" +
               "    }\n\n";
    }

    private String generateGetByIdMethod(String entityName, String entityVar) {
        return "    public " + entityName + " getById(Long id) {\n" +
               "        return " + entityVar + "Mapper.selectById(id);\n" +
               "    }\n\n";
    }

    private String generateListMethod(String entityName, String entityVar) {
        return "    public List<" + entityName + "> list() {\n" +
               "        return " + entityVar + "Mapper.selectList(null);\n" +
               "    }\n\n";
    }

    private String generateUpdateMethod(String entityName, String entityVar) {
        return "    public " + entityName + " update(Long id, " + entityName + "UpdateRequest request) {\n" +
               "        " + entityName + " entity = " + entityVar + "Mapper.selectById(id);\n" +
               "        if (entity == null) {\n" +
               "            throw new RuntimeException(\"" + entityName + " not found\");\n" +
               "        }\n" +
               "        " + entityName + "Mapper.INSTANCE.updateFromRequest(request, entity);\n" +
               "        " + entityVar + "Mapper.updateById(entity);\n" +
               "        return entity;\n" +
               "    }\n\n";
    }

    private String generateDeleteMethod(String entityName, String entityVar) {
        return "    public void delete(Long id) {\n" +
               "        " + entityVar + "Mapper.deleteById(id);\n" +
               "    }\n\n";
    }
}