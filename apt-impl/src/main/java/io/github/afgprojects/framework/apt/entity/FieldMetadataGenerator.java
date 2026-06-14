package io.github.afgprojects.framework.apt.entity;

import io.github.afgprojects.framework.commons.naming.NamingUtils;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 字段元数据生成器
 * <p>
 * 负责从实体类提取字段信息并生成字段元数据。
 * <p>
 * 功能：
 * <ul>
 *   <li>提取字段信息（属性名、列名、类型）</li>
 *   <li>识别主键字段（@Id 注解或名为 id 的字段）</li>
 *   <li>处理泛型类型参数</li>
 *   <li>跳过关联字段和静态字段</li>
 * </ul>
 */
class FieldMetadataGenerator {

    private final RelationMetadataGenerator relationMetadataGenerator;

    FieldMetadataGenerator(RelationMetadataGenerator relationMetadataGenerator) {
        this.relationMetadataGenerator = relationMetadataGenerator;
    }

    /**
     * 提取字段信息
     *
     * @param typeElement 实体类元素
     * @return 字段信息列表
     */
    List<FieldInfo> extractFields(TypeElement typeElement) {
        List<FieldInfo> fields = new ArrayList<>();

        // 构建类型参数映射（处理泛型父类）
        Map<String, TypeMirror> typeParamMap = buildTypeParameterMap(typeElement);

        // 遍历类层次结构
        TypeElement currentClass = typeElement;
        while (currentClass != null && !currentClass.getQualifiedName().toString().equals("java.lang.Object")) {
            for (VariableElement field : ElementFilter.fieldsIn(currentClass.getEnclosedElements())) {
                if (field.getModifiers().contains(Modifier.STATIC)) {
                    continue;
                }

                // 跳过关联字段
                if (relationMetadataGenerator.isRelationField(field)) {
                    continue;
                }

                // 跳过 @Transient 字段（非持久化字段，如 TreeEntity.children）
                if (isTransientField(field)) {
                    continue;
                }

                String propertyName = field.getSimpleName().toString();
                String columnName = extractColumnName(field);
                // 解析字段类型（处理泛型参数）
                String fieldType = resolveFieldType(field.asType(), typeParamMap);

                boolean isId = hasIdAnnotation(field);
                boolean isGenerated = isId;

                // 检测是否有自定义列名（@Column(name=...) 与默认 snake_case 不同）
                String defaultColumnName = NamingUtils.toSnakeCase(propertyName);
                boolean hasCustomColumnName = !defaultColumnName.equals(columnName);

                fields.add(new FieldInfo(
                    propertyName,
                    columnName,
                    fieldType,
                    isId,
                    isGenerated,
                    hasCustomColumnName
                ));
            }

            // 移动到父类
            TypeMirror superclass = currentClass.getSuperclass();
            if (superclass.getKind() == TypeKind.DECLARED) {
                currentClass = (TypeElement) ((DeclaredType) superclass).asElement();
            } else {
                break;
            }
        }

        return fields;
    }

    /**
     * 构建类型参数映射
     * <p>
     * 用于解析泛型父类中的类型参数。
     * 例如：AuthUserDevice extends BaseEntity&lt;Long&gt; → {"ID": Long}
     */
    private Map<String, TypeMirror> buildTypeParameterMap(TypeElement typeElement) {
        Map<String, TypeMirror> typeParamMap = new HashMap<>();

        TypeMirror superclass = typeElement.getSuperclass();
        if (superclass.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredSuper = (DeclaredType) superclass;
            TypeElement superElement = (TypeElement) declaredSuper.asElement();

            List<? extends TypeParameterElement> typeParams = superElement.getTypeParameters();
            List<? extends TypeMirror> typeArgs = declaredSuper.getTypeArguments();

            for (int i = 0; i < typeParams.size() && i < typeArgs.size(); i++) {
                typeParamMap.put(typeParams.get(i).getSimpleName().toString(), typeArgs.get(i));
            }
        }

        return typeParamMap;
    }

    /**
     * 解析字段类型（处理泛型类型参数）
     */
    private String resolveFieldType(TypeMirror fieldType, Map<String, TypeMirror> typeParamMap) {
        // 如果是类型变量（如 ID），查找实际类型
        if (fieldType.getKind() == TypeKind.TYPEVAR) {
            // 从字符串中提取纯类型变量名（移除注解）
            String typeStr = fieldType.toString();
            String varName = typeStr;

            // 移除类型注解（如 @org.jspecify.annotations.Nullable）
            if (typeStr.contains("@")) {
                varName = typeStr.replaceAll("@[\\w.]+\\s*", "");
            }

            if (typeParamMap.containsKey(varName)) {
                TypeMirror resolvedType = typeParamMap.get(varName);
                return normalizeFieldType(resolvedType.toString());
            }
            // 如果找不到映射，返回类型变量名
            return varName;
        } else if (fieldType.getKind() == TypeKind.DECLARED) {
            // 处理带泛型的声明类型
            DeclaredType declaredType = (DeclaredType) fieldType;
            TypeMirror enclosingType = declaredType.getEnclosingType();
            if (enclosingType != null && enclosingType.getKind() == TypeKind.DECLARED) {
                String result = resolveFieldType(enclosingType, typeParamMap);
                if (!result.equals(enclosingType.toString())) {
                    return result;
                }
            }
        }

        return normalizeFieldType(fieldType.toString());
    }

    /**
     * 提取列名
     */
    private String extractColumnName(VariableElement field) {
        // 检查 @Column 注解
        for (AnnotationMirror am : field.getAnnotationMirrors()) {
            if (am.getAnnotationType().toString().contains("Column")) {
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
                    if (entry.getKey().getSimpleName().contentEquals("name")) {
                        return entry.getValue().getValue().toString();
                    }
                }
            }
        }
        // 默认：camelCase → snake_case
        return NamingUtils.toSnakeCase(field.getSimpleName().toString());
    }

    /**
     * 检查是否有 @Id 注解
     */
    private boolean hasIdAnnotation(VariableElement field) {
        for (AnnotationMirror am : field.getAnnotationMirrors()) {
            if (am.getAnnotationType().toString().contains("Id")) {
                return true;
            }
        }
        return "id".equals(field.getSimpleName().toString());
    }

    /**
     * 检查字段是否标记为 @Transient（非持久化字段）
     * <p>
     * 支持 jakarta.persistence.Transient 注解。
     * @Transient 字段不映射到数据库列（如 TreeEntity.children），
     * 在元数据提取时应跳过。
     *
     * @param field 字段元素
     * @return 如果字段标记为 @Transient 则返回 true
     */
    private boolean isTransientField(VariableElement field) {
        for (AnnotationMirror am : field.getAnnotationMirrors()) {
            String annotationType = am.getAnnotationType().toString();
            if (annotationType.equals("jakarta.persistence.Transient")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 标准化字段类型
     */
    private String normalizeFieldType(String type) {
        String result = type;

        // 移除类型注解
        if (result.contains("@")) {
            int atIndex = result.indexOf('@');
            int spaceIndex = result.indexOf(' ', atIndex);
            if (spaceIndex > 0) {
                result = result.substring(0, atIndex) + result.substring(spaceIndex + 1);
            } else {
                result = result.substring(result.lastIndexOf(' ') + 1);
            }
        }

        // 处理泛型
        int genericIndex = result.indexOf('<');
        if (genericIndex > 0) {
            result = result.substring(0, genericIndex);
        }

        // 处理基本类型
        return switch (result) {
            case "long" -> "Long";
            case "int" -> "Integer";
            case "short" -> "Short";
            case "byte" -> "Byte";
            case "float" -> "Float";
            case "double" -> "Double";
            case "boolean" -> "Boolean";
            case "char" -> "Character";
            default -> result;
        };
    }

    /**
     * 字段信息
     */
    record FieldInfo(
        String propertyName,
        String columnName,
        String fieldType,
        boolean isId,
        boolean isGenerated,
        boolean hasCustomColumnName  // 是否有自定义列名（@Column(name=...)）
    ) {}
}
