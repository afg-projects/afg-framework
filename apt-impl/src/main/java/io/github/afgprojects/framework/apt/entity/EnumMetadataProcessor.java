package io.github.afgprojects.framework.apt.entity;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 枚举元数据注解处理器。
 * <p>
 * 扫描所有带有 @AfgEnum 注解的枚举类，生成枚举元数据类。
 * 生成的元数据类实现 EnumMetadata 接口，提供编译时确定的枚举元数据。
 *
 * <p>编译期校验：
 * <ul>
 *   <li>@AfgEnum 只能标注枚举类 → ERROR</li>
 * </ul>
 *
 * <h2>示例输入</h2>
 * <pre>{@code
 * @AfgEnum(valueField = "code", labelField = "description", i18nPrefix = "enum.user-status")
 * public enum UserStatus {
 *     ACTIVE(1, "活跃"),
 *     INACTIVE(0, "停用");
 *
 *     private final int code;
 *     private final String description;
 * }
 * }</pre>
 *
 * <h2>生成输出</h2>
 * <pre>{@code
 * public class UserStatusEnumMetadata implements EnumMetadata<UserStatus> {
 *     private static final List<EnumValue> VALUES;
 *     static {
 *         List<EnumValue> values = new ArrayList<>();
 *         for (UserStatus constant : UserStatus.values()) {
 *             Object value = getFieldValue(constant, "code");
 *             String label = getLabelValue(constant, "description");
 *             values.add(new EnumValue(value, label));
 *         }
 *         VALUES = Collections.unmodifiableList(values);
 *     }
 *     // ... 接口方法实现
 * }
 * }</pre>
 */
@SupportedAnnotationTypes("io.github.afgprojects.framework.apt.entity.AfgEnum")
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class EnumMetadataProcessor extends AbstractProcessor {

    private static final String INDEX_FILE = "META-INF/afg/enum-metadata.index";
    private final List<String> generatedMetadataClasses = new ArrayList<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            writeIndexFile();
            return false;
        }

        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (element instanceof TypeElement typeElement) {
                    // 校验：@AfgEnum 只能标注枚举类
                    if (typeElement.getKind() != ElementKind.ENUM) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "@AfgEnum 只能标注枚举类，" + typeElement.getQualifiedName() + " 不是枚举",
                            typeElement);
                        continue;
                    }

                    try {
                        processEnumElement(typeElement);
                    } catch (Exception e) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Failed to process enum: " + e.getMessage(), element);
                    }
                }
            }
        }

        return true;
    }

    /**
     * 处理枚举元素，生成元数据类
     */
    private void processEnumElement(TypeElement typeElement) {
        String className = typeElement.getQualifiedName().toString();
        String packageName = extractPackageName(className);
        String simpleName = typeElement.getSimpleName().toString();
        String metadataClassName = simpleName + "EnumMetadata";
        String metadataFullName = packageName + ".metadata." + metadataClassName;

        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
            "Generating enum metadata for: " + className, typeElement);

        // 提取 @AfgEnum 注解配置
        AfgEnumConfig config = extractAfgEnumConfig(typeElement);

        try {
            String sourceCode = generateMetadataClass(typeElement, packageName, metadataClassName, config);
            writeSourceFile(metadataFullName, sourceCode);
            generatedMetadataClasses.add(metadataFullName);
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                "Failed to generate enum metadata: " + e.getMessage(), typeElement);
        }
    }

    /**
     * 提取 @AfgEnum 注解配置
     */
    private AfgEnumConfig extractAfgEnumConfig(TypeElement typeElement) {
        String valueField = "value";
        String labelField = "label";
        String i18nPrefix = "";

        for (AnnotationMirror am : typeElement.getAnnotationMirrors()) {
            if (am.getAnnotationType().toString().endsWith("AfgEnum")) {
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
                    String key = entry.getKey().getSimpleName().toString();
                    Object value = entry.getValue().getValue();

                    switch (key) {
                        case "valueField" -> valueField = value.toString();
                        case "labelField" -> labelField = value.toString();
                        case "i18nPrefix" -> i18nPrefix = value.toString();
                        default -> {}
                    }
                }
                break;
            }
        }

        return new AfgEnumConfig(valueField, labelField, i18nPrefix);
    }

    /**
     * 生成元数据类源码
     * <p>
     * 由于 APT 阶段无法获取枚举常量的运行时字段值，
     * 生成的元数据类会在静态初始化时通过反射获取实际的 value 和 label。
     */
    private String generateMetadataClass(TypeElement typeElement, String packageName,
                                          String metadataClassName, AfgEnumConfig config) {
        ClassName enumClass = ClassName.get(typeElement);
        ClassName enumMetadata = ClassName.get("io.github.afgprojects.framework.apt.entity", "EnumMetadata");
        ClassName enumValueClass = ClassName.get("io.github.afgprojects.framework.apt.entity", "EnumValue");

        // 构建静态初始化块：在运行时通过反射获取实际的枚举值
        // VALUES = new ArrayList<>();
        // for (E constant : E.values()) {
        //     Object value = getFieldValue(constant, valueField);
        //     String label = getLabelValue(constant, labelField);
        //     VALUES.add(new EnumValue(value, label));
        // }
        // VALUES = Collections.unmodifiableList(VALUES);
        CodeBlock.Builder staticInitBuilder = CodeBlock.builder();
        staticInitBuilder.addStatement("$T values = new $T<>()",
            ParameterizedTypeName.get(ClassName.get(List.class), enumValueClass),
            ClassName.get(ArrayList.class));
        staticInitBuilder.beginControlFlow("for ($T constant : $T.values())", enumClass, enumClass);
        staticInitBuilder.addStatement("Object value = getFieldValue(constant, $S)", config.valueField());
        staticInitBuilder.addStatement("$T label = getLabelValue(constant, $S)", String.class, config.labelField());
        staticInitBuilder.addStatement("values.add(new $T(value, label))", enumValueClass);
        staticInitBuilder.endControlFlow();
        staticInitBuilder.addStatement("VALUES = $T.unmodifiableList(values)", ClassName.get(Collections.class));

        // 构建类
        TypeSpec classSpec = TypeSpec.classBuilder(metadataClassName)
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(ParameterizedTypeName.get(enumMetadata, enumClass))
            .addJavadoc("自动生成的枚举元数据类\n")
            .addJavadoc("@generated by AFG Framework APT\n")
            .addJavadoc("@see $L\n", typeElement.getQualifiedName().toString())
            // VALUES 字段 - 在静态初始化块中赋值
            .addField(FieldSpec.builder(
                    ParameterizedTypeName.get(ClassName.get(List.class), enumValueClass),
                    "VALUES",
                    Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .build())
            // 静态初始化块
            .addStaticBlock(staticInitBuilder.build())
            // getFieldValue 方法 - 通过反射获取枚举字段值
            .addMethod(MethodSpec.methodBuilder("getFieldValue")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(Object.class)
                .addParameter(enumClass, "constant")
                .addParameter(String.class, "fieldName")
                .beginControlFlow("try")
                .addStatement("$T field = constant.getClass().getDeclaredField(fieldName)",
                    java.lang.reflect.Field.class)
                .addStatement("field.setAccessible(true)")
                .addStatement("return field.get(constant)")
                .nextControlFlow("catch ($T e)", ReflectiveOperationException.class)
                .addStatement("return constant.name()")  // 降级：返回枚举常量名
                .endControlFlow()
                .build())
            // getLabelValue 方法 - 获取标签值，确保返回 String
            .addMethod(MethodSpec.methodBuilder("getLabelValue")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(String.class)
                .addParameter(enumClass, "constant")
                .addParameter(String.class, "fieldName")
                .beginControlFlow("try")
                .addStatement("$T field = constant.getClass().getDeclaredField(fieldName)",
                    java.lang.reflect.Field.class)
                .addStatement("field.setAccessible(true)")
                .addStatement("Object value = field.get(constant)")
                .addStatement("return value != null ? value.toString() : constant.name()")
                .nextControlFlow("catch ($T e)", ReflectiveOperationException.class)
                .addStatement("return constant.name()")  // 降级：返回枚举常量名
                .endControlFlow()
                .build())
            .addMethod(MethodSpec.methodBuilder("enumType")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Class.class), enumClass))
                .addStatement("return $T.class", enumClass)
                .build())
            .addMethod(MethodSpec.methodBuilder("valueField")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $S", config.valueField())
                .build())
            .addMethod(MethodSpec.methodBuilder("labelField")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $S", config.labelField())
                .build())
            .addMethod(MethodSpec.methodBuilder("i18nPrefix")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $S", config.i18nPrefix())
                .build())
            .addMethod(MethodSpec.methodBuilder("values")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), enumValueClass))
                .addStatement("return VALUES")
                .build())
            .build();

        JavaFile javaFile = JavaFile.builder(packageName + ".metadata", classSpec)
            .addFileComment("Auto-generated by AFG Framework APT")
            .build();

        return javaFile.toString();
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
     * 写入索引文件
     */
    private void writeIndexFile() {
        if (generatedMetadataClasses.isEmpty()) {
            return;
        }

        try {
            var fileObject = processingEnv.getFiler().createResource(
                StandardLocation.CLASS_OUTPUT,
                "",
                INDEX_FILE);

            try (OutputStream out = fileObject.openOutputStream()) {
                for (String className : generatedMetadataClasses) {
                    out.write(className.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    out.write('\n');
                }
            }

            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                "Generated AFG enum metadata index with " + generatedMetadataClasses.size() + " entries: " + INDEX_FILE);

        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                "Failed to generate AFG enum metadata index: " + e.getMessage());
        }
    }

    /**
     * 提取包名
     */
    private String extractPackageName(String fullName) {
        int lastDot = fullName.lastIndexOf('.');
        return lastDot > 0 ? fullName.substring(0, lastDot) : "";
    }

    /**
     * @AfgEnum 注解配置
     */
    record AfgEnumConfig(String valueField, String labelField, String i18nPrefix) {}
}
