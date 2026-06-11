package io.github.afgprojects.framework.apt.entity;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 实体元数据注解处理器。
 * <p>
 * 扫描所有带有 @AfEntity 注解的类，生成完整的元数据类。
 * 生成的元数据类实现 DatabaseEntityMetadata 接口，提供编译时确定的实体元数据。
 *
 * <p>支持的特性：
 * <ul>
 *   <li>表名解析：@Table 注解或类名转 snake_case</li>
 *   <li>字段解析：@Column 注解或属性名转 snake_case</li>
 *   <li>主键识别：@Id 注解或名为 id 的字段</li>
 *   <li>关联关系：@ManyToOne、@OneToMany、@OneToOne、@ManyToMany</li>
 *   <li>特性检测：软删除、多租户、审计、版本化</li>
 *   <li>编译期校验：缺少 @Table、缺少主键、非 public 类、@EncryptedField 非 String 字段</li>
 * </ul>
 *
 * <h2>示例输入</h2>
 * <pre>{@code
 * @AfEntity
 * @Table(name = "sys_user")
 * public class User {
 *     @Id
 *     private Long id;
 *
 *     @Column(name = "user_name")
 *     private String userName;
 *
 *     @ManyToOne
 *     private Department department;
 * }
 * }</pre>
 *
 * <h2>生成输出</h2>
 * <pre>{@code
 * package io.github.example.metadata;
 *
 * public class UserMetadata implements DatabaseEntityMetadata<User> {
 *     // ... 自动生成的实现
 * }
 * }</pre>
 */
@SupportedAnnotationTypes({
    "io.github.afgprojects.framework.apt.entity.AfEntity",
    "io.github.afgprojects.framework.apt.entity.EncryptedField"
})
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class EntityMetadataProcessor extends AbstractProcessor {

    private CommonFieldRegistry commonFieldRegistry;
    private RelationMetadataGenerator relationMetadataGenerator;
    private FieldMetadataGenerator fieldMetadataGenerator;
    private MetadataCodeGenerator metadataCodeGenerator;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.commonFieldRegistry = new CommonFieldRegistry();
        this.commonFieldRegistry.initialize(processingEnv);

        // 初始化各个生成器
        this.relationMetadataGenerator = new RelationMetadataGenerator();
        this.fieldMetadataGenerator = new FieldMetadataGenerator(relationMetadataGenerator);
        this.metadataCodeGenerator = new MetadataCodeGenerator(
            processingEnv, commonFieldRegistry);

        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
            "AFG Entity Metadata Processor initialized");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }

        // 扫描通用字段注解
        commonFieldRegistry.scanAnnotations(roundEnv);

        // 校验 @EncryptedField 标注的字段必须是 String 类型
        validateEncryptedFields(roundEnv);

        // 处理实体
        for (TypeElement annotation : annotations) {
            if (!annotation.getQualifiedName().contentEquals("io.github.afgprojects.framework.apt.entity.AfEntity")) {
                continue;
            }
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (element instanceof TypeElement typeElement) {
                    try {
                        // 编译期校验
                        validateEntityElement(typeElement);

                        processEntityElement(typeElement);
                    } catch (Exception e) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Failed to process entity: " + e.getMessage(), element);
                    }
                }
            }
        }

        return true;
    }

    /**
     * 校验实体元素。
     * <p>
     * 执行以下编译期校验：
     * <ol>
     *   <li>@AfEntity 类缺少 @Table 注解 → ERROR</li>
     *   <li>@AfEntity 类缺少主键字段 → WARNING</li>
     *   <li>@AfEntity 类非 public → ERROR</li>
     * </ol>
     */
    private void validateEntityElement(TypeElement typeElement) {
        String className = typeElement.getQualifiedName().toString();

        // 校验1：@AfEntity 类必须有 @Table 注解
        boolean hasTableAnnotation = typeElement.getAnnotationMirrors().stream()
            .anyMatch(am -> am.getAnnotationType().toString().contains("Table"));
        if (!hasTableAnnotation) {
            // 检查 @AfEntity 是否显式指定了 tableName
            MetadataCodeGenerator.AfEntityConfig config = metadataCodeGenerator.extractAfEntityConfig(typeElement);
            if (config.tableName() == null || config.tableName().isEmpty()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "实体类 " + className + " 缺少 @Table 注解，请添加 @Table(name = \"xxx\") 指定表名",
                    typeElement);
            }
        }

        // 校验2：@AfEntity 类必须有主键字段
        boolean hasIdField = hasIdField(typeElement);
        if (!hasIdField) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                "实体类 " + className + " 未定义主键字段，DataManager 操作可能异常",
                typeElement);
        }

        // 校验3：@AfEntity 类必须是 public
        if (!typeElement.getModifiers().contains(Modifier.PUBLIC)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                "实体类 " + className + " 必须是 public",
                typeElement);
        }
    }

    /**
     * 检查类或其父类是否有主键字段（@Id 注解或名为 id 的字段）。
     */
    private boolean hasIdField(TypeElement typeElement) {
        TypeElement currentClass = typeElement;
        while (currentClass != null && !currentClass.getQualifiedName().toString().equals("java.lang.Object")) {
            for (VariableElement field : ElementFilter.fieldsIn(currentClass.getEnclosedElements())) {
                if (field.getModifiers().contains(Modifier.STATIC)) {
                    continue;
                }
                // 检查 @Id 注解
                for (AnnotationMirror am : field.getAnnotationMirrors()) {
                    if (am.getAnnotationType().toString().contains("Id")) {
                        return true;
                    }
                }
                // 检查字段名为 id
                if ("id".equals(field.getSimpleName().toString())) {
                    return true;
                }
            }

            // 移动到父类
            TypeMirror superclass = currentClass.getSuperclass();
            if (superclass.getKind() == TypeKind.DECLARED) {
                currentClass = (TypeElement) ((javax.lang.model.type.DeclaredType) superclass).asElement();
            } else {
                break;
            }
        }
        return false;
    }

    /**
     * 校验 @EncryptedField 标注的字段必须是 String 类型。
     */
    private void validateEncryptedFields(RoundEnvironment roundEnv) {
        TypeElement encryptedFieldAnnotation = processingEnv.getElementUtils()
            .getTypeElement("io.github.afgprojects.framework.apt.entity.EncryptedField");
        if (encryptedFieldAnnotation == null) {
            return;
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(encryptedFieldAnnotation)) {
            if (element instanceof VariableElement field) {
                TypeMirror fieldType = field.asType();
                String fieldTypeName = fieldType.toString();
                if (!fieldTypeName.equals("java.lang.String")) {
                    Element enclosingElement = field.getEnclosingElement();
                    String className = enclosingElement.getSimpleName().toString();
                    String fieldName = field.getSimpleName().toString();
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "@EncryptedField 只能标注 String 字段，" + className + "." + fieldName + " 的类型为 " + fieldTypeName,
                        field);
                }
            }
        }
    }

    /**
     * 处理实体元素，生成元数据类
     */
    private void processEntityElement(TypeElement typeElement) {
        String className = typeElement.getQualifiedName().toString();
        String packageName = extractPackageName(className);
        String simpleName = typeElement.getSimpleName().toString();
        String metadataClassName = simpleName + "Metadata";
        String metadataFullName = packageName + ".metadata." + metadataClassName;

        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
            "Generating metadata for: " + className, typeElement);

        try {
            String sourceCode = generateMetadataClass(typeElement, packageName, metadataClassName);
            writeSourceFile(metadataFullName, sourceCode);
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                "Failed to generate metadata: " + e.getMessage(), typeElement);
        }
    }

    /**
     * 生成元数据类源码
     */
    private String generateMetadataClass(TypeElement typeElement, String packageName, String metadataClassName) {
        // 读取 @AfEntity 注解属性
        MetadataCodeGenerator.AfEntityConfig afEntityConfig = metadataCodeGenerator.extractAfEntityConfig(typeElement);

        // 表名（优先使用 @AfEntity.tableName）
        String tableName = afEntityConfig.tableName() != null && !afEntityConfig.tableName().isEmpty()
            ? afEntityConfig.tableName()
            : metadataCodeGenerator.extractTableName(typeElement);

        // 提取字段和关联
        List<FieldMetadataGenerator.FieldInfo> fields = fieldMetadataGenerator.extractFields(typeElement);
        List<RelationMetadataGenerator.RelationInfo> relations = afEntityConfig.generateRelations()
            ? relationMetadataGenerator.extractRelations(typeElement, tableName)
            : Collections.emptyList();

        // 特性检测
        var features = EntityFeatureDetector.FeatureDetectionResult.detect(fields);

        // 生成代码
        return metadataCodeGenerator.generateMetadataClass(
            typeElement, packageName, metadataClassName, tableName, fields, relations, features);
    }

    /**
     * 写入源文件
     */
    private void writeSourceFile(String className, String sourceCode) throws IOException {
        JavaFileObject file = processingEnv.getFiler().createSourceFile(className);
        try (Writer writer = file.openWriter()) {
            writer.write(sourceCode);
        }
    }

    /**
     * 提取包名
     */
    private String extractPackageName(String fullName) {
        int lastDot = fullName.lastIndexOf('.');
        return lastDot > 0 ? fullName.substring(0, lastDot) : "";
    }
}