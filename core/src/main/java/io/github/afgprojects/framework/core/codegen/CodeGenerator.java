package io.github.afgprojects.framework.core.codegen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 代码生成器接口
 *
 * <p>定义代码生成的基本契约
 *
 * @since 1.0.0
 */
public interface CodeGenerator {

    /**
     * 生成代码
     *
     * @param context 生成上下文
     * @return 生成的代码内容
     */
    @NonNull String generate(@NonNull GeneratorContext context);

    /**
     * 获取生成器名称
     *
     * @return 生成器名称
     */
    @NonNull String getName();

    /**
     * 获取支持的模板类型
     *
     * @return 模板类型
     */
    @NonNull String getTemplateType();

    /**
     * 写入生成的代码到文件
     *
     * @param context  生成上下文
     * @param outputPath 输出路径
     * @throws IOException 如果写入失败
     */
    default void writeToFile(@NonNull GeneratorContext context, @NonNull Path outputPath) throws IOException {
        String code = generate(context);
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, code);
    }
}