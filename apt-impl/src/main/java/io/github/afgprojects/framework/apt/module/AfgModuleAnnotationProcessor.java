package io.github.afgprojects.framework.apt.module;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;

/**
 * AFG 模块注解处理器
 *
 * 在编译时扫描所有带有 @AfgModuleAnnotation 注解的类，
 * 并生成模块索引文件 META-INF/afg-modules.index
 *
 * 索引文件格式：
 * - 每行一个模块配置类
 * - 格式: moduleId:configFile:className
 * - 例如: auth:module-auth.yml:io.github.afgprojects.auth.AuthModuleConfig
 *
 * 这样运行时可以直接读取索引文件，无需扫描整个 classpath，
 * 大幅提升启动性能（从 100-500ms 降至 约5ms） */
@SupportedAnnotationTypes("io.github.afgprojects.framework.apt.module.AfgModuleAnnotation")
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class AfgModuleAnnotationProcessor extends AbstractProcessor {

    private static final String INDEX_FILE = "META-INF/afg-modules.index";

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                "AFG Module Annotation Processor initialized");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }

        Set<String> moduleEntries = new HashSet<>();

        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (element instanceof TypeElement typeElement) {
                    String entry = processModuleElement(typeElement);
                    moduleEntries.add(entry);
                }
            }
        }

        if (!moduleEntries.isEmpty()) {
            generateIndexFile(moduleEntries);
        }

        return true;
    }

    /**
     * 处理模块元素，提取模块信息
     *
     * <p>索引格式: moduleId:configFile:className:contextPath:basePackage
     */
    private String processModuleElement(TypeElement typeElement) {
        String className = typeElement.getQualifiedName().toString();

        // 获取注解属性
        String moduleId = extractModuleId(typeElement);
        String configFile = extractConfigFile(typeElement);
        String contextPath = extractContextPath(typeElement);
        String basePackage = extractBasePackage(typeElement);

        // 如果没有指定 configFile，使用默认值 module-{moduleId}.yml
        if (configFile == null || configFile.isEmpty()) {
            configFile = "module-" + moduleId + ".yml";
        }

        // 如果没有指定 basePackage，使用注解类所在的包名
        if (basePackage == null || basePackage.isEmpty()) {
            PackageElement pkg = processingEnv.getElementUtils().getPackageOf(typeElement);
            basePackage = pkg.getQualifiedName().toString();
        }

        String entry = moduleId + ":" + configFile + ":" + className + ":" + contextPath + ":" + basePackage;

        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                "Found AFG module: " + className + " (id=" + moduleId + ", config=" + configFile + ", contextPath=" + contextPath + ", basePackage=" + basePackage + ")", typeElement);

        return entry;
    }

    /**
     * 提取模块 ID
     */
    private String extractModuleId(TypeElement typeElement) {
        try {
            var annotationMirror = typeElement.getAnnotationMirrors().stream()
                    .filter(am -> am.getAnnotationType().toString().contains("AfgModuleAnnotation"))
                    .findFirst();

            if (annotationMirror.isPresent()) {
                for (var entry : annotationMirror.get().getElementValues().entrySet()) {
                    String key = entry.getKey().getSimpleName().toString();
                    if ("id".equals(key) || "value".equals(key)) {
                        String value = entry.getValue().getValue().toString();
                        if (!value.isEmpty()) {
                            return value;
                        }
                    }
                }
            }

            // 默认使用包名最后一部分
            PackageElement pkg = processingEnv.getElementUtils().getPackageOf(typeElement);
            String packageName = pkg.getQualifiedName().toString();
            int lastDot = packageName.lastIndexOf('.');
            return lastDot >= 0 ? packageName.substring(lastDot + 1) : packageName;

        } catch (Exception e) {
            // 回退到包名，打印详细警告信息
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                "Failed to extract module ID from annotation for " + typeElement.getQualifiedName() +
                ", falling back to package name. Exception: " + e.getClass().getName() + " - " + e.getMessage(),
                typeElement);
            PackageElement pkg = processingEnv.getElementUtils().getPackageOf(typeElement);
            return pkg.getQualifiedName().toString();
        }
    }

    /**
     * 提取配置文件名
     */
    private String extractConfigFile(TypeElement typeElement) {
        try {
            var annotationMirror = typeElement.getAnnotationMirrors().stream()
                    .filter(am -> am.getAnnotationType().toString().contains("AfgModuleAnnotation"))
                    .findFirst();

            if (annotationMirror.isPresent()) {
                for (var entry : annotationMirror.get().getElementValues().entrySet()) {
                    String key = entry.getKey().getSimpleName().toString();
                    if ("configFile".equals(key)) {
                        return entry.getValue().getValue().toString();
                    }
                }
            }
        } catch (Exception e) {
            // 忽略
        }
        return "";
    }

    /**
     * 提取模块 context-path
     *
     * <p>如果没有指定，默认为 "/{moduleId}-api"
     */
    private String extractContextPath(TypeElement typeElement) {
        try {
            var annotationMirror = typeElement.getAnnotationMirrors().stream()
                    .filter(am -> am.getAnnotationType().toString().contains("AfgModuleAnnotation"))
                    .findFirst();

            if (annotationMirror.isPresent()) {
                for (var entry : annotationMirror.get().getElementValues().entrySet()) {
                    String key = entry.getKey().getSimpleName().toString();
                    if ("contextPath".equals(key)) {
                        String value = entry.getValue().getValue().toString();
                        if (!value.isEmpty()) {
                            return value;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 忽略
        }

        // 默认: /{moduleId}-api
        String moduleId = extractModuleId(typeElement);
        return "/" + moduleId + "-api";
    }

    /**
     * 提取模块 basePackage
     *
     * <p>如果没有指定，返回 null（由调用方使用类所在包名作为默认值）
     */
    private String extractBasePackage(TypeElement typeElement) {
        try {
            var annotationMirror = typeElement.getAnnotationMirrors().stream()
                    .filter(am -> am.getAnnotationType().toString().contains("AfgModuleAnnotation"))
                    .findFirst();

            if (annotationMirror.isPresent()) {
                for (var entry : annotationMirror.get().getElementValues().entrySet()) {
                    String key = entry.getKey().getSimpleName().toString();
                    if ("basePackage".equals(key)) {
                        String value = entry.getValue().getValue().toString();
                        if (!value.isEmpty()) {
                            return value;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 忽略
        }
        return null;
    }

    /**
     * 生成模块索引文件
     */
    private void generateIndexFile(Set<String> moduleEntries) {
        try {
            var fileObject = processingEnv.getFiler().createResource(
                    StandardLocation.CLASS_OUTPUT,
                    "",
                    INDEX_FILE);

            try (Writer writer = fileObject.openWriter()) {
                for (String entry : moduleEntries) {
                    writer.write(entry);
                    writer.write('\n');
                }
            }

            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                    "Generated AFG module index with " + moduleEntries.size() + " modules: " + INDEX_FILE);

        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Failed to generate AFG module index: " + e.getMessage());
        }
    }
}