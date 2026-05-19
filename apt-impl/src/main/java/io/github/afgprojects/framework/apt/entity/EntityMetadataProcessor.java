package io.github.afgprojects.framework.apt.entity;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 实体元数据注解处理器
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
 * </ul>
 *
 * <pre>
 * 示例输入：
 * {@code
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
 * }
 *
 * 生成输出：
 * {@code
 * package io.github.example.metadata;
 *
 * public class UserMetadata implements DatabaseEntityMetadata<User> {
 *     // ... 自动生成的实现
 * }
 * }
 * </pre>
 */
@SupportedAnnotationTypes("io.github.afgprojects.framework.apt.entity.AfEntity")
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class EntityMetadataProcessor extends AbstractProcessor {

    private Types typeUtils;
    private CommonFieldRegistry commonFieldRegistry;
    private RelationMetadataGenerator relationMetadataGenerator;
    private FieldMetadataGenerator fieldMetadataGenerator;
    private EntityFeatureDetector entityFeatureDetector;
    private MetadataCodeGenerator metadataCodeGenerator;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.typeUtils = processingEnv.getTypeUtils();
        this.commonFieldRegistry = new CommonFieldRegistry();
        this.commonFieldRegistry.initialize(processingEnv);

        // 初始化各个生成器
        this.relationMetadataGenerator = new RelationMetadataGenerator();
        this.fieldMetadataGenerator = new FieldMetadataGenerator(relationMetadataGenerator);
        this.entityFeatureDetector = new EntityFeatureDetector();
        this.metadataCodeGenerator = new MetadataCodeGenerator(
            processingEnv, commonFieldRegistry, relationMetadataGenerator);

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

        // 处理实体
        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (element instanceof TypeElement typeElement) {
                    try {
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