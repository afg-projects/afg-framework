package io.github.afgprojects.framework.core.codegen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import lombok.Builder;
import lombok.Data;

/**
 * 代码生成器上下文
 *
 * <p>包含代码生成所需的所有信息
 *
 * @since 1.0.0
 */
@Data
@Builder
public class GeneratorContext {

    /**
     * 类名
     */
    private String className;

    /**
     * 包名
     */
    private String packageName;

    /**
     * 表名（用于 Entity 生成）
     */
    private @Nullable String tableName;

    /**
     * 类注释
     */
    private @Nullable String classComment;

    /**
     * 字段列表
     */
    private List<FieldDefinition> fields;

    /**
     * 方法列表
     */
    private @Nullable List<MethodDefinition> methods;

    /**
     * 导入列表
     */
    private @Nullable List<String> imports;

    /**
     * 父类
     */
    private @Nullable String superClass;

    /**
     * 实现的接口
     */
    private @Nullable List<String> interfaces;

    /**
     * 注解
     */
    private @Nullable List<String> annotations;

    /**
     * 额外属性
     */
    @Builder.Default
    private Map<String, Object> extraProperties = new HashMap<>();

    /**
     * 字段定义
     */
    @Data
    @Builder
    public static class FieldDefinition {

        /**
         * 字段名
         */
        private String name;

        /**
         * 字段类型
         */
        private String type;

        /**
         * 字段注释
         */
        private @Nullable String comment;

        /**
         * 是否为主键
         */
        private boolean primaryKey;

        /**
         * 是否为必填
         */
        private boolean required;

        /**
         * 默认值
         */
        private @Nullable String defaultValue;

        /**
         * 字段长度
         */
        private @Nullable Integer length;

        /**
         * 列名（数据库字段名）
         */
        private @Nullable String columnName;

        /**
         * 注解列表
         */
        private @Nullable List<String> annotations;
    }

    /**
     * 方法定义
     */
    @Data
    @Builder
    public static class MethodDefinition {

        /**
         * 方法名
         */
        private String name;

        /**
         * 返回类型
         */
        private String returnType;

        /**
         * 参数列表
         */
        private @Nullable List<ParameterDefinition> parameters;

        /**
         * 方法体
         */
        private @Nullable String body;

        /**
         * 方法注释
         */
        private @Nullable String comment;

        /**
         * 注解列表
         */
        private @Nullable List<String> annotations;
    }

    /**
     * 参数定义
     */
    @Data
    @Builder
    public static class ParameterDefinition {

        /**
         * 参数名
         */
        private String name;

        /**
         * 参数类型
         */
        private String type;
    }
}