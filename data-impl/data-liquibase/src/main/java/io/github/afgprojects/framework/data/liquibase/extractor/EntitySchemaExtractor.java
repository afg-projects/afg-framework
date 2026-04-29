package io.github.afgprojects.framework.data.liquibase.extractor;

import io.github.afgprojects.framework.data.core.converter.SchemaConverter;
import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.metadata.FieldMetadata;
import io.github.afgprojects.framework.data.core.relation.RelationMetadata;
import io.github.afgprojects.framework.data.core.relation.RelationType;
import io.github.afgprojects.framework.data.core.schema.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 实体 Schema 提取器
 * <p>
 * 将 EntityMetadata 转换为 SchemaMetadata
 */
public class EntitySchemaExtractor implements SchemaConverter<EntityMetadata<?>, SchemaMetadata> {

    private final Dialect dialect;

    public EntitySchemaExtractor(Dialect dialect) {
        this.dialect = dialect;
    }

    @Override
    public SchemaMetadata convert(EntityMetadata<?> entityMetadata) {
        String tableName = entityMetadata.getTableName();

        SchemaMetadataImpl.Builder builder = SchemaMetadataImpl.builder()
                .tableName(tableName);

        // 提取列
        List<ColumnMetadata> columns = new ArrayList<>();
        FieldMetadata idField = entityMetadata.getIdField();

        for (FieldMetadata field : entityMetadata.getFields()) {
            ColumnMetadata column = toColumnMetadata(field, field == idField);
            columns.add(column);
            builder.addColumn(column);
        }

        // 提取主键
        if (idField != null) {
            PrimaryKeyMetadata primaryKey = PrimaryKeyMetadataImpl.builder()
                    .constraintName("pk_" + tableName)
                    .columnNames(List.of(idField.getColumnName()))
                    .build();
            builder.primaryKey(primaryKey);
        }

        // 提取外键（从关联关系）
        for (RelationMetadata relation : entityMetadata.getRelations()) {
            ForeignKeyMetadata fk = toForeignKeyMetadata(tableName, relation);
            if (fk != null) {
                builder.addForeignKey(fk);
            }
        }

        return builder.build();
    }

    private ColumnMetadata toColumnMetadata(FieldMetadata field, boolean isPrimaryKey) {
        String sqlType = dialect.getSqlType(field.getFieldType());

        return ColumnMetadataImpl.builder()
                .columnName(field.getColumnName())
                .dataType(sqlType)
                .nullable(!isPrimaryKey && !field.isId())
                .primaryKey(isPrimaryKey)
                .autoIncrement(field.isGenerated())
                .unique(false)
                .build();
    }

    private ForeignKeyMetadata toForeignKeyMetadata(String tableName, RelationMetadata relation) {
        // 只有 ManyToOne 和 OneToOne（拥有方）需要创建外键
        if (relation.getRelationType() == RelationType.MANY_TO_ONE) {
            return ForeignKeyMetadataImpl.builder()
                    .constraintName("fk_" + tableName + "_" + relation.getFieldName())
                    .columnNames(List.of(relation.getForeignKeyColumn()))
                    .referencedTableName(inferTableName(relation.getTargetEntityClass()))
                    .referencedColumnNames(List.of("id"))
                    .updateRule("NO ACTION")
                    .deleteRule("NO ACTION")
                    .build();
        }

        if (relation.getRelationType() == RelationType.ONE_TO_ONE && relation.isOwningSide()) {
            return ForeignKeyMetadataImpl.builder()
                    .constraintName("fk_" + tableName + "_" + relation.getFieldName())
                    .columnNames(List.of(relation.getForeignKeyColumn()))
                    .referencedTableName(inferTableName(relation.getTargetEntityClass()))
                    .referencedColumnNames(List.of("id"))
                    .updateRule("NO ACTION")
                    .deleteRule("NO ACTION")
                    .build();
        }

        return null;
    }

    private String inferTableName(Class<?> entityClass) {
        String className = entityClass.getSimpleName();
        StringBuilder tableName = new StringBuilder();
        for (int i = 0; i < className.length(); i++) {
            char c = className.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                tableName.append('_');
            }
            tableName.append(Character.toLowerCase(c));
        }
        return tableName.toString();
    }
}
