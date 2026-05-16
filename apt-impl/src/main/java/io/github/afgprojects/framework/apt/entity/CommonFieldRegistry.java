package io.github.afgprojects.framework.apt.entity;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.*;

/**
 * 通用字段注册表
 * <p>
 * 管理三层注册表：框架内置、配置文件、注解声明。
 * <p>
 * 优先级规则：框架内置 > 配置文件 > 注解声明
 * <ul>
 *   <li>框架内置字段不可覆盖（保证核心特性稳定）</li>
 *   <li>配置文件可覆盖注解声明（项目级 > 模块级）</li>
 * </ul>
 */
class CommonFieldRegistry {

    // 三层注册表（按注册名称索引）
    private final Map<String, CommonFieldInfo> frameworkFields = new LinkedHashMap<>();
    private final Map<String, CommonFieldInfo> configFields = new LinkedHashMap<>();
    private final Map<String, CommonFieldInfo> annotationFields = new LinkedHashMap<>();

    // 查找索引（按 propertyName:fieldType 索引）
    private final Map<String, CommonFieldInfo> lookupIndex = new HashMap<>();

    // 框架内置字段名称集合（用于快速判断）
    private static final Set<String> FRAMEWORK_FIELD_NAMES = Set.of(
        "createdAt", "updatedAt", "deleted", "deletedAt",
        "tenantId", "version", "createBy", "updateBy", "createdBy", "updatedBy"
    );

    private ProcessingEnvironment processingEnv;

    /**
     * 初始化注册表
     * <p>
     * 加载框架内置字段和配置文件字段。
     */
    void initialize(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;

        // 1. 加载框架内置字段
        loadFrameworkFields();

        // 2. 加载配置文件字段
        loadConfigFields();

        // 3. 构建查找索引
        buildLookupIndex();

        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
            "CommonFieldRegistry initialized with " + getTotalCount() + " fields");
    }

    /**
     * 加载框架内置字段
     */
    private void loadFrameworkFields() {
        // createdAt
        registerFrameworkField("CREATED_AT", "createdAt", "created_at",
            "java.time.LocalDateTime", false, false);

        // updatedAt
        registerFrameworkField("UPDATED_AT", "updatedAt", "updated_at",
            "java.time.LocalDateTime", false, false);

        // deleted
        registerFrameworkField("DELETED", "deleted", "deleted",
            "java.lang.Boolean", false, false);

        // deletedAt
        registerFrameworkField("DELETED_AT", "deletedAt", "deleted_at",
            "java.time.LocalDateTime", false, false);

        // tenantId
        registerFrameworkField("TENANT_ID", "tenantId", "tenant_id",
            "java.lang.String", false, false);

        // version (Long)
        registerFrameworkField("VERSION_LONG", "version", "version",
            "java.lang.Long", false, false);

        // version (Integer)
        registerFrameworkField("VERSION_INTEGER", "version", "version",
            "java.lang.Integer", false, false);

        // createBy
        registerFrameworkField("CREATE_BY", "createBy", "create_by",
            "java.lang.String", false, false);

        // updateBy
        registerFrameworkField("UPDATE_BY", "updateBy", "update_by",
            "java.lang.String", false, false);
    }

    /**
     * 注册框架内置字段
     */
    private void registerFrameworkField(String name, String propertyName, String columnName,
                                         String fieldType, boolean isId, boolean isGenerated) {
        CommonFieldInfo info = new CommonFieldInfo(
            name, propertyName, columnName, fieldType, isId, isGenerated, FieldSource.FRAMEWORK
        );
        frameworkFields.put(name, info);
    }

    /**
     * 加载配置文件字段
     */
    private void loadConfigFields() {
        CommonFieldConfigLoader loader = new CommonFieldConfigLoader(processingEnv);
        List<CommonFieldInfo> fields = loader.load();

        for (CommonFieldInfo field : fields) {
            // 检查是否与框架内置字段冲突
            if (isFrameworkField(field.propertyName())) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                    "Cannot override framework field '" + field.propertyName() + "' from config. Ignored.");
                continue;
            }

            // 检查是否重复
            if (configFields.containsKey(field.name())) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                    "Duplicate common field name '" + field.name() + "' in config. Using first definition.");
                continue;
            }

            configFields.put(field.name(), field);
        }
    }

    /**
     * 扫描注解并注册
     */
    void scanAnnotations(RoundEnvironment roundEnv) {
        // 扫描 @CommonFieldDefinition 注解
        for (Element element : roundEnv.getElementsAnnotatedWith(CommonFieldDefinition.class)) {
            CommonFieldDefinition annotation = element.getAnnotation(CommonFieldDefinition.class);
            registerAnnotationField(annotation, element);
        }

        // 扫描 @CommonFieldDefinitions 容器注解
        for (Element element : roundEnv.getElementsAnnotatedWith(CommonFieldDefinitions.class)) {
            CommonFieldDefinitions annotations = element.getAnnotation(CommonFieldDefinitions.class);
            for (CommonFieldDefinition annotation : annotations.value()) {
                registerAnnotationField(annotation, element);
            }
        }

        // 重建查找索引
        buildLookupIndex();
    }

    /**
     * 注册注解声明的字段
     */
    private void registerAnnotationField(CommonFieldDefinition annotation, Element element) {
        CommonFieldInfo info = CommonFieldInfo.fromAnnotation(annotation, FieldSource.ANNOTATION);

        // 检查是否与框架内置字段冲突
        if (isFrameworkField(info.propertyName())) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                "Cannot override framework field '" + info.propertyName() + "' from annotation. Ignored.", element);
            return;
        }

        // 检查是否与配置文件字段冲突（配置文件优先）
        if (configFields.containsKey(info.name())) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                "Common field '" + info.name() + "' already defined in config. Config takes priority.", element);
            return;
        }

        // 检查注解内部重复
        if (annotationFields.containsKey(info.name())) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                "Duplicate common field name '" + info.name() + "' in annotations. Using first definition.", element);
            return;
        }

        annotationFields.put(info.name(), info);
    }

    /**
     * 构建查找索引
     */
    private void buildLookupIndex() {
        lookupIndex.clear();

        // 按优先级添加：框架 > 配置 > 注解
        for (CommonFieldInfo field : frameworkFields.values()) {
            lookupIndex.put(field.lookupKey(), field);
        }

        for (CommonFieldInfo field : configFields.values()) {
            String key = field.lookupKey();
            if (!lookupIndex.containsKey(key)) {
                lookupIndex.put(key, field);
            }
        }

        for (CommonFieldInfo field : annotationFields.values()) {
            String key = field.lookupKey();
            if (!lookupIndex.containsKey(key)) {
                lookupIndex.put(key, field);
            }
        }
    }

    /**
     * 查找匹配的通用字段
     *
     * @param propertyName 属性名
     * @param fieldType    字段类型全限定名
     * @return 匹配的字段信息，未找到返回 null
     */
    CommonFieldInfo find(String propertyName, String fieldType) {
        String key = propertyName + ":" + fieldType;
        return lookupIndex.get(key);
    }

    /**
     * 检查是否为框架内置字段
     */
    boolean isFrameworkField(String propertyName) {
        return FRAMEWORK_FIELD_NAMES.contains(propertyName);
    }

    /**
     * 生成引用代码
     */
    String generateRef(CommonFieldInfo field) {
        return "CommonFieldMetadata." + field.name();
    }

    /**
     * 获取所有注册的字段（用于生成 CommonFieldMetadata 类）
     */
    List<CommonFieldInfo> getAllFields() {
        List<CommonFieldInfo> all = new ArrayList<>();
        all.addAll(frameworkFields.values());
        all.addAll(configFields.values());
        all.addAll(annotationFields.values());
        return all;
    }

    /**
     * 获取总字段数
     */
    int getTotalCount() {
        return frameworkFields.size() + configFields.size() + annotationFields.size();
    }
}
