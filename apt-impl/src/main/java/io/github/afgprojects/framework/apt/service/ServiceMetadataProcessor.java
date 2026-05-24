package io.github.afgprojects.framework.apt.service;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service metadata annotation processor.
 * <p>
 * Scans all classes annotated with @AfService, extracts metadata from @AfOperation methods
 * and their @AfParam parameters, and generates ServiceMetadata implementation classes.
 * <p>
 * Also generates META-INF/afg/service-metadata.index listing all generated metadata classes
 * for runtime discovery by AptServiceMetadataLoader.
 *
 * <pre>
 * Example input:
 * {@code
 * @AfService(name = "userService", description = "User management service")
 * public class UserService {
 *     @AfOperation(name = "createUser", description = "Create a new user")
 *     public User createUser(@AfParam(name = "user") User user) { ... }
 * }
 * }
 *
 * Generated output:
 * - UserServiceServiceMetadata.java (implements ServiceMetadata<UserService>)
 * - META-INF/afg/service-metadata.index (contains fully qualified class name)
 * </pre>
 */
@SupportedAnnotationTypes("io.github.afgprojects.framework.apt.api.AfService")
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class ServiceMetadataProcessor extends AbstractProcessor {

    private ServiceMetadataCodeGenerator codeGenerator;
    private final List<String> generatedMetadataClasses = new ArrayList<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.codeGenerator = new ServiceMetadataCodeGenerator(processingEnv);

        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
            "AFG Service Metadata Processor initialized");
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
                    try {
                        processServiceElement(typeElement);
                    } catch (Exception e) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Failed to process service: " + e.getMessage(), element);
                    }
                }
            }
        }

        return true;
    }

    /**
     * Process a single @AfService-annotated class.
     */
    private void processServiceElement(TypeElement typeElement) {
        String className = typeElement.getQualifiedName().toString();
        String packageName = extractPackageName(className);
        String simpleName = typeElement.getSimpleName().toString();
        String metadataClassName = simpleName + "ServiceMetadata";
        String metadataFullName = packageName + ".metadata." + metadataClassName;

        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
            "Generating service metadata for: " + className, typeElement);

        // Extract service config from @AfService annotation
        AfServiceConfig serviceConfig = extractAfServiceConfig(typeElement);

        // Extract operations from @AfOperation methods
        List<OperationInfo> operations = extractOperations(typeElement);

        // Validate: check for duplicate operation names
        validateOperationNames(typeElement, operations);

        try {
            String sourceCode = codeGenerator.generateMetadataClass(
                typeElement, packageName, metadataClassName, serviceConfig, operations);
            writeSourceFile(metadataFullName, sourceCode);
            generatedMetadataClasses.add(metadataFullName);
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                "Failed to generate service metadata: " + e.getMessage(), typeElement);
        }
    }

    /**
     * Extract @AfService annotation configuration.
     */
    private AfServiceConfig extractAfServiceConfig(TypeElement typeElement) {
        String name = "";
        String description = "";
        String category = "";
        List<String> tags = List.of();
        boolean deprecated = false;

        for (AnnotationMirror am : typeElement.getAnnotationMirrors()) {
            if (am.getAnnotationType().toString().endsWith("AfService")) {
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
                    String key = entry.getKey().getSimpleName().toString();
                    Object value = entry.getValue().getValue();

                    switch (key) {
                        case "name" -> name = value.toString();
                        case "description" -> description = value.toString();
                        case "category" -> category = value.toString();
                        case "tags" -> {
                            @SuppressWarnings("unchecked")
                            List<? extends AnnotationValue> tagValues = (List<? extends AnnotationValue>) value;
                            tags = tagValues.stream().map(tv -> tv.getValue().toString()).toList();
                        }
                        case "deprecated" -> deprecated = (Boolean) value;
                    }
                }
                break;
            }
        }

        // Default name: decapitalized class simple name
        if (name.isEmpty()) {
            name = decapitalize(typeElement.getSimpleName().toString());
        }

        return new AfServiceConfig(name, description, category, tags, deprecated);
    }

    /**
     * Extract @AfOperation methods from the service class.
     */
    private List<OperationInfo> extractOperations(TypeElement typeElement) {
        List<OperationInfo> operations = new ArrayList<>();

        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed instanceof ExecutableElement method) {
                AfOperationConfig opConfig = extractAfOperationConfig(method);
                if (opConfig != null) {
                    List<ParameterInfo> parameters = extractParameters(method);
                    AfResultConfig resultConfig = extractAfResultConfig(method);
                    operations.add(new OperationInfo(opConfig, parameters, resultConfig, method));
                }
            }
        }

        return operations;
    }

    /**
     * Extract @AfOperation annotation configuration from a method.
     */
    private AfOperationConfig extractAfOperationConfig(ExecutableElement method) {
        String name = "";
        String description = "";
        boolean async = false;
        boolean deprecated = false;
        String permission = "";
        List<String> requiredRoles = List.of();
        boolean audit = true;
        boolean tenantScope = true;
        boolean dataScope = false;
        String inputSchema = "";
        boolean found = false;

        for (AnnotationMirror am : method.getAnnotationMirrors()) {
            if (am.getAnnotationType().toString().endsWith("AfOperation")) {
                found = true;
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
                    String key = entry.getKey().getSimpleName().toString();
                    Object value = entry.getValue().getValue();

                    switch (key) {
                        case "name" -> name = value.toString();
                        case "description" -> description = value.toString();
                        case "async" -> async = (Boolean) value;
                        case "deprecated" -> deprecated = (Boolean) value;
                        case "permission" -> permission = value.toString();
                        case "requiredRoles" -> {
                            @SuppressWarnings("unchecked")
                            List<? extends AnnotationValue> roleValues = (List<? extends AnnotationValue>) value;
                            requiredRoles = roleValues.stream().map(rv -> rv.getValue().toString()).toList();
                        }
                        case "audit" -> audit = (Boolean) value;
                        case "tenantScope" -> tenantScope = (Boolean) value;
                        case "dataScope" -> dataScope = (Boolean) value;
                        case "inputSchema" -> inputSchema = value.toString();
                    }
                }
                break;
            }
        }

        if (!found) {
            return null;
        }

        // Default name: method name
        if (name.isEmpty()) {
            name = method.getSimpleName().toString();
        }

        return new AfOperationConfig(name, description, async, deprecated, permission,
            requiredRoles, audit, tenantScope, dataScope, inputSchema);
    }

    /**
     * Extract parameter info from a method, including @AfParam annotation data.
     */
    private List<ParameterInfo> extractParameters(ExecutableElement method) {
        List<ParameterInfo> parameters = new ArrayList<>();
        List<? extends VariableElement> params = method.getParameters();

        for (int i = 0; i < params.size(); i++) {
            VariableElement param = params.get(i);
            String paramName = param.getSimpleName().toString();
            String paramType = param.asType().toString();
            boolean required = true;
            String defaultValue = "";
            String description = "";
            List<String> enumValues = List.of();
            boolean injected = false;

            // Check for @AfParam annotation
            for (AnnotationMirror am : param.getAnnotationMirrors()) {
                if (am.getAnnotationType().toString().endsWith("AfParam")) {
                    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
                        String key = entry.getKey().getSimpleName().toString();
                        Object value = entry.getValue().getValue();

                        switch (key) {
                            case "name" -> paramName = value.toString();
                            case "description" -> description = value.toString();
                            case "required" -> required = (Boolean) value;
                            case "defaultValue" -> defaultValue = value.toString();
                            case "enumValues" -> {
                                @SuppressWarnings("unchecked")
                                List<? extends AnnotationValue> evValues = (List<? extends AnnotationValue>) value;
                                enumValues = evValues.stream().map(ev -> ev.getValue().toString()).toList();
                            }
                        }
                    }
                    break;
                }
            }

            parameters.add(new ParameterInfo(paramName, paramType, required, defaultValue,
                i, description, enumValues, injected));
        }

        return parameters;
    }

    /**
     * Extract @AfResult annotation configuration from a method.
     */
    private AfResultConfig extractAfResultConfig(ExecutableElement method) {
        String description = "";
        boolean paged = false;
        boolean streaming = false;

        for (AnnotationMirror am : method.getAnnotationMirrors()) {
            if (am.getAnnotationType().toString().endsWith("AfResult")) {
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
                    String key = entry.getKey().getSimpleName().toString();
                    Object value = entry.getValue().getValue();

                    switch (key) {
                        case "description" -> description = value.toString();
                        case "paged" -> paged = (Boolean) value;
                        case "streaming" -> streaming = (Boolean) value;
                    }
                }
                break;
            }
        }

        return new AfResultConfig(description, paged, streaming);
    }

    /**
     * Validate that no two @AfOperation methods have the same name in the same @AfService class.
     */
    private void validateOperationNames(TypeElement typeElement, List<OperationInfo> operations) {
        Set<String> seen = new HashSet<>();
        for (OperationInfo op : operations) {
            String opName = op.operationConfig().name();
            if (!seen.add(opName)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Duplicate @AfOperation name '" + opName + "' in " + typeElement.getSimpleName(),
                    typeElement);
            }
        }
    }

    /**
     * Write the generated source file.
     */
    private void writeSourceFile(String className, String sourceCode) throws IOException {
        JavaFileObject file = processingEnv.getFiler().createSourceFile(className);
        try (Writer writer = file.openWriter()) {
            writer.write(sourceCode);
        }
    }

    /**
     * Write the META-INF/afg/service-metadata.index file listing all generated metadata classes.
     */
    private void writeIndexFile() {
        if (generatedMetadataClasses.isEmpty()) {
            return;
        }

        try {
            javax.tools.FileObject indexFile = processingEnv.getFiler()
                .createResource(javax.tools.StandardLocation.CLASS_OUTPUT, "",
                    "META-INF/afg/service-metadata.index");
            try (OutputStream out = indexFile.openOutputStream()) {
                for (String className : generatedMetadataClasses) {
                    out.write(className.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    out.write('\n');
                }
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                "Failed to write service-metadata.index: " + e.getMessage());
        }
    }

    /**
     * Extract package name from a fully qualified class name.
     */
    private String extractPackageName(String fullName) {
        int lastDot = fullName.lastIndexOf('.');
        return lastDot > 0 ? fullName.substring(0, lastDot) : "";
    }

    /**
     * Decapitalize a string (first letter lowercase).
     */
    private String decapitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * @AfService annotation configuration.
     */
    record AfServiceConfig(String name, String description, String category,
                           List<String> tags, boolean deprecated) {}

    /**
     * @AfOperation annotation configuration.
     */
    record AfOperationConfig(String name, String description, boolean async, boolean deprecated,
                             String permission, List<String> requiredRoles,
                             boolean audit, boolean tenantScope, boolean dataScope,
                             String inputSchema) {}

    /**
     * Parameter information extracted from method parameters and @AfParam.
     */
    record ParameterInfo(String name, String type, boolean required, String defaultValue,
                         int index, String description, List<String> enumValues, boolean injected) {}

    /**
     * @AfResult annotation configuration.
     */
    record AfResultConfig(String description, boolean paged, boolean streaming) {}

    /**
     * Combined operation information: annotation config + parameters + result config.
     */
    record OperationInfo(AfOperationConfig operationConfig, List<ParameterInfo> parameters,
                         AfResultConfig resultConfig, ExecutableElement method) {}
}
