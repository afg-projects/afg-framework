package io.github.afgprojects.framework.apt.entity;

import io.github.afgprojects.framework.commons.naming.NamingUtils;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 关联元数据生成器
 * <p>
 * 负责从实体类提取关联关系信息并生成关联元数据。
 * <p>
 * 支持的关联类型：
 * <ul>
 *   <li>@ManyToOne - 多对一关联</li>
 *   <li>@OneToMany - 一对多关联</li>
 *   <li>@OneToOne - 一对一关联</li>
 *   <li>@ManyToMany - 多对多关联</li>
 * </ul>
 */
class RelationMetadataGenerator {

    /**
     * 提取关联关系
     *
     * @param typeElement 实体类元素
     * @param tableName   表名
     * @return 关联信息列表
     */
    List<RelationInfo> extractRelations(TypeElement typeElement, String tableName) {
        List<RelationInfo> relations = new ArrayList<>();

        TypeElement currentClass = typeElement;
        while (currentClass != null && !currentClass.getQualifiedName().toString().equals("java.lang.Object")) {
            for (VariableElement field : ElementFilter.fieldsIn(currentClass.getEnclosedElements())) {
                if (field.getModifiers().contains(Modifier.STATIC)) {
                    continue;
                }

                RelationInfo relation = extractRelationInfo(field, tableName);
                if (relation != null) {
                    relations.add(relation);
                }
            }

            // 移动到父类
            TypeMirror superclass = currentClass.getSuperclass();
            if (superclass.getKind() == TypeKind.DECLARED) {
                currentClass = (TypeElement) ((DeclaredType) superclass).asElement();
            } else {
                break;
            }
        }

        return relations;
    }

    /**
     * 从字段提取关联信息
     */
    private RelationInfo extractRelationInfo(VariableElement field, String tableName) {
        String fieldName = field.getSimpleName().toString();

        // 检查 @ManyToOne
        for (AnnotationMirror am : field.getAnnotationMirrors()) {
            String annotationType = am.getAnnotationType().toString();

            if (annotationType.endsWith("ManyToOne")) {
                return extractManyToOne(field, am, fieldName);
            }

            if (annotationType.endsWith("OneToMany")) {
                return extractOneToMany(field, am, fieldName, tableName);
            }

            if (annotationType.endsWith("OneToOne")) {
                return extractOneToOne(field, am, fieldName);
            }

            if (annotationType.endsWith("ManyToMany")) {
                return extractManyToMany(field, am, fieldName, tableName);
            }
        }

        return null;
    }

    /**
     * 提取 ManyToOne 关联信息
     */
    private RelationInfo extractManyToOne(VariableElement field, AnnotationMirror am, String fieldName) {
        String targetEntity = extractTargetEntity(field, am);
        String foreignKey = extractAnnotationValue(am, "foreignKey", fieldName + "_id");
        List<String> cascadeTypes = extractCascadeTypes(am);
        FetchType fetchType = extractFetchType(am, FetchType.EAGER);
        boolean optional = extractAnnotationValue(am, "optional", true);

        return new RelationInfo(
            "MANY_TO_ONE",
            targetEntity,
            fieldName,
            null, // mappedBy
            foreignKey,
            null, // joinTable
            null, // joinColumn
            null, // inverseJoinColumn
            cascadeTypes,
            fetchType,
            false, // orphanRemoval
            optional
        );
    }

    /**
     * 提取 OneToMany 关联信息
     */
    private RelationInfo extractOneToMany(VariableElement field, AnnotationMirror am, String fieldName, String tableName) {
        String targetEntity = extractGenericTargetEntity(field, am);
        String mappedBy = extractAnnotationValue(am, "mappedBy", "");
        String foreignKey = extractAnnotationValue(am, "foreignKey", tableName + "_id");
        List<String> cascadeTypes = extractCascadeTypes(am);
        FetchType fetchType = extractFetchType(am, FetchType.LAZY);
        boolean orphanRemoval = extractAnnotationValue(am, "orphanRemoval", false);

        return new RelationInfo(
            "ONE_TO_MANY",
            targetEntity,
            fieldName,
            mappedBy,
            foreignKey,
            null, // joinTable
            null, // joinColumn
            null, // inverseJoinColumn
            cascadeTypes,
            fetchType,
            orphanRemoval,
            true // optional
        );
    }

    /**
     * 提取 OneToOne 关联信息
     */
    private RelationInfo extractOneToOne(VariableElement field, AnnotationMirror am, String fieldName) {
        String targetEntity = extractTargetEntity(field, am);
        String mappedBy = extractAnnotationValue(am, "mappedBy", "");
        String foreignKey = extractAnnotationValue(am, "foreignKey", fieldName + "_id");
        List<String> cascadeTypes = extractCascadeTypes(am);
        FetchType fetchType = extractFetchType(am, FetchType.LAZY);

        return new RelationInfo(
            "ONE_TO_ONE",
            targetEntity,
            fieldName,
            mappedBy,
            foreignKey,
            null, // joinTable
            null, // joinColumn
            null, // inverseJoinColumn
            cascadeTypes,
            fetchType,
            false, // orphanRemoval
            true // optional
        );
    }

    /**
     * 提取 ManyToMany 关联信息
     */
    private RelationInfo extractManyToMany(VariableElement field, AnnotationMirror am, String fieldName, String tableName) {
        String targetEntity = extractGenericTargetEntity(field, am);
        String mappedBy = extractAnnotationValue(am, "mappedBy", "");
        String targetTableName = NamingUtils.toSnakeCase(targetEntity.substring(targetEntity.lastIndexOf('.') + 1));

        String joinTable = extractAnnotationValue(am, "joinTable", tableName + "_" + targetTableName);
        String joinColumn = extractAnnotationValue(am, "joinColumn", tableName + "_id");
        String inverseJoinColumn = extractAnnotationValue(am, "inverseJoinColumn", targetTableName + "_id");
        List<String> cascadeTypes = extractCascadeTypes(am);
        FetchType fetchType = extractFetchType(am, FetchType.LAZY);

        return new RelationInfo(
            "MANY_TO_MANY",
            targetEntity,
            fieldName,
            mappedBy,
            "", // foreignKeyColumn - not used for ManyToMany
            joinTable,
            joinColumn,
            inverseJoinColumn,
            cascadeTypes,
            fetchType,
            false, // orphanRemoval
            true // optional
        );
    }

    /**
     * 提取目标实体类（非泛型字段）
     */
    private String extractTargetEntity(VariableElement field, AnnotationMirror am) {
        // 先检查 targetEntity 属性
        String targetEntity = extractAnnotationValue(am, "targetEntity", "");
        if (!targetEntity.isEmpty() && !targetEntity.equals("void")) {
            return targetEntity;
        }

        // 使用字段类型
        return normalizeFieldType(field.asType().toString());
    }

    /**
     * 提取泛型目标实体类（Collection/Set/List 字段）
     */
    private String extractGenericTargetEntity(VariableElement field, AnnotationMirror am) {
        // 先检查 targetEntity 属性
        String targetEntity = extractAnnotationValue(am, "targetEntity", "");
        if (!targetEntity.isEmpty() && !targetEntity.equals("void")) {
            return targetEntity;
        }

        // 尝试从泛型参数提取
        TypeMirror fieldType = field.asType();
        if (fieldType.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) fieldType;
            List<? extends TypeMirror> typeArgs = declaredType.getTypeArguments();
            if (!typeArgs.isEmpty()) {
                return normalizeFieldType(typeArgs.get(0).toString());
            }
        }

        // 降级：使用字段类型本身
        return normalizeFieldType(fieldType.toString());
    }

    /**
     * 提取级联类型
     */
    private List<String> extractCascadeTypes(AnnotationMirror am) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().contentEquals("cascade")) {
                AnnotationValue value = entry.getValue();
                if (value.getValue() instanceof List<?> list) {
                    for (Object item : list) {
                        if (item instanceof AnnotationValue av) {
                            String cascadeStr = av.getValue().toString();
                            // 提取枚举名称，如 CascadeType.PERSIST -> PERSIST
                            if (cascadeStr.contains(".")) {
                                cascadeStr = cascadeStr.substring(cascadeStr.lastIndexOf('.') + 1);
                            }
                            result.add(cascadeStr);
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * 提取抓取策略
     */
    private FetchType extractFetchType(AnnotationMirror am, FetchType defaultValue) {
        String fetchTypeStr = extractAnnotationValue(am, "fetch", "");
        if (fetchTypeStr.isEmpty()) {
            return defaultValue;
        }
        // 提取枚举名称
        if (fetchTypeStr.contains(".")) {
            fetchTypeStr = fetchTypeStr.substring(fetchTypeStr.lastIndexOf('.') + 1);
        }
        return "EAGER".equals(fetchTypeStr) ? FetchType.EAGER : FetchType.LAZY;
    }

    /**
     * 提取注解属性值（类型安全版本）
     *
     * @param am            注解镜像
     * @param attributeName 属性名
     * @param defaultValue  默认值
     * @return 属性值
     */
    @SuppressWarnings("unchecked")
    private <T> T extractAnnotationValue(AnnotationMirror am, String attributeName, T defaultValue) {
        // 推断期望类型
        Class<?> expectedType = defaultValue != null ? defaultValue.getClass() : Object.class;

        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().contentEquals(attributeName)) {
                Object value = entry.getValue().getValue();
                if (value != null) {
                    // 类型检查
                    if (expectedType.isInstance(value)) {
                        return (T) value;
                    } else if (expectedType == String.class && value instanceof String) {
                        return (T) value;
                    } else if (expectedType == Boolean.class && value instanceof Boolean) {
                        return (T) value;
                    } else if (expectedType == Integer.class && value instanceof Integer) {
                        return (T) value;
                    }
                    // 类型不匹配，返回默认值
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }

    /**
     * 检查字段是否为关联字段
     */
    boolean isRelationField(VariableElement field) {
        for (AnnotationMirror am : field.getAnnotationMirrors()) {
            String annotationType = am.getAnnotationType().toString();
            if (annotationType.endsWith("ManyToOne")
                || annotationType.endsWith("OneToMany")
                || annotationType.endsWith("OneToOne")
                || annotationType.endsWith("ManyToMany")) {
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

        return result;
    }

    /**
     * 生成关联元数据创建代码
     */
    String generateRelationMetadata(RelationInfo relation, String entitySimpleName) {
        StringBuilder sb = new StringBuilder();
        sb.append("RelationMetadataImpl.builder()\n");
        sb.append("                .relationType(RelationType.").append(relation.relationType).append(")\n");
        sb.append("                .entityClass(").append(entitySimpleName).append(".class)\n");
        sb.append("                .targetEntityClass(").append(relation.targetEntity).append(".class)\n");
        sb.append("                .fieldName(\"").append(relation.fieldName).append("\")\n");

        if (relation.mappedBy != null && !relation.mappedBy.isEmpty()) {
            sb.append("                .mappedBy(\"").append(relation.mappedBy).append("\")\n");
        }

        sb.append("                .foreignKeyColumn(\"").append(relation.foreignKeyColumn).append("\")\n");

        if (relation.joinTable != null && !relation.joinTable.isEmpty()) {
            sb.append("                .joinTable(\"").append(relation.joinTable).append("\")\n");
        }
        if (relation.joinColumn != null && !relation.joinColumn.isEmpty()) {
            sb.append("                .joinColumn(\"").append(relation.joinColumn).append("\")\n");
        }
        if (relation.inverseJoinColumn != null && !relation.inverseJoinColumn.isEmpty()) {
            sb.append("                .inverseJoinColumn(\"").append(relation.inverseJoinColumn).append("\")\n");
        }

        // 级联类型
        sb.append("                .cascadeTypes(");
        if (relation.cascadeTypes.isEmpty()) {
            sb.append("Collections.emptySet()");
        } else {
            sb.append("EnumSet.of(");
            for (int i = 0; i < relation.cascadeTypes.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append("CascadeType.").append(relation.cascadeTypes.get(i));
            }
            sb.append(")");
        }
        sb.append(")\n");

        sb.append("                .fetchType(FetchType.").append(relation.fetchType).append(")\n");
        sb.append("                .orphanRemoval(").append(relation.orphanRemoval).append(")\n");
        sb.append("                .optional(").append(relation.optional).append(")\n");
        sb.append("                .build()");

        return sb.toString();
    }

    /**
     * 关联信息
     */
    record RelationInfo(
        String relationType,
        String targetEntity,
        String fieldName,
        String mappedBy,
        String foreignKeyColumn,
        String joinTable,
        String joinColumn,
        String inverseJoinColumn,
        List<String> cascadeTypes,
        FetchType fetchType,
        boolean orphanRemoval,
        boolean optional
    ) {}

    /**
     * 抓取策略枚举（内部使用）
     */
    enum FetchType {
        LAZY, EAGER
    }
}
