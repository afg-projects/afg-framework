package io.github.afgprojects.framework.core.codegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 代码生成器管理器
 *
 * <p>管理所有代码生成器，提供统一的生成入口
 *
 * <h3>使用示例</h3>
 * <pre>
 * CodeGeneratorManager manager = CodeGeneratorManager.getInstance();
 *
 * // 生成 Entity
 * String entityCode = manager.generate("entity", context);
 *
 * // 生成 DTO
 * String dtoCode = manager.generate("dto", context);
 *
 * // 生成 Controller
 * String controllerCode = manager.generate("controller", context);
 * </pre>
 *
 * @since 1.0.0
 */
public class CodeGeneratorManager {

    private static final CodeGeneratorManager INSTANCE = new CodeGeneratorManager();

    private final Map<String, CodeGenerator> generators = new HashMap<>();

    private CodeGeneratorManager() {
        // 注册内置生成器
        register(new EntityGenerator());
        register(new DtoGenerator());
        register(new ControllerGenerator());
        register(new ServiceGenerator());

        // 通过 SPI 加载自定义生成器
        for (CodeGenerator generator : ServiceLoader.load(CodeGenerator.class)) {
            register(generator);
        }
    }

    /**
     * 获取单例实例
     *
     * @return CodeGeneratorManager 实例
     */
    @NonNull
    public static CodeGeneratorManager getInstance() {
        return INSTANCE;
    }

    /**
     * 注册生成器
     *
     * @param generator 代码生成器
     */
    public void register(@NonNull CodeGenerator generator) {
        generators.put(generator.getTemplateType(), generator);
    }

    /**
     * 生成代码
     *
     * @param templateType 模板类型
     * @param context      生成上下文
     * @return 生成的代码
     * @throws IllegalArgumentException 如果模板类型不支持
     */
    @NonNull
    public String generate(@NonNull String templateType, @NonNull GeneratorContext context) {
        CodeGenerator generator = generators.get(templateType);
        if (generator == null) {
            throw new IllegalArgumentException("Unsupported template type: " + templateType);
        }
        return generator.generate(context);
    }

    /**
     * 获取生成器
     *
     * @param templateType 模板类型
     * @return 代码生成器，不存在则返回 null
     */
    public @Nullable CodeGenerator getGenerator(@NonNull String templateType) {
        return generators.get(templateType);
    }

    /**
     * 获取所有支持的模板类型
     *
     * @return 模板类型列表
     */
    @NonNull
    public List<String> getSupportedTemplateTypes() {
        return new ArrayList<>(generators.keySet());
    }

    /**
     * 批量生成代码
     *
     * @param templateTypes 模板类型列表
     * @param context       生成上下文
     * @return 模板类型到生成代码的映射
     */
    @NonNull
    public Map<String, String> generateAll(@NonNull List<String> templateTypes, @NonNull GeneratorContext context) {
        Map<String, String> result = new HashMap<>();
        for (String type : templateTypes) {
            try {
                result.put(type, generate(type, context));
            } catch (IllegalArgumentException e) {
                // 跳过不支持的类型
            }
        }
        return result;
    }
}