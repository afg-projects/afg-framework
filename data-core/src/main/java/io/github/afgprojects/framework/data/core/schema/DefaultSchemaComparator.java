package io.github.afgprojects.framework.data.core.schema;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 默认 Schema 比较器实现
 */
public class DefaultSchemaComparator implements SchemaComparator {

    @Override
    public SchemaDiff compare(SchemaMetadata source, SchemaMetadata target) {
        List<ColumnDiff> columnDiffs = compareColumns(source, target);
        IndexDiff indexDiff = compareIndexes(source, target);
        ForeignKeyDiff foreignKeyDiff = compareForeignKeys(source, target);

        return new SchemaDiff(
                source.getTableName(),
                true,
                columnDiffs,
                indexDiff,
                foreignKeyDiff
        );
    }

    @Override
    public ThreeWayDiff compareThreeWay(
            SchemaMetadata fromEntity,
            @Nullable SchemaMetadata fromDatabase,
            @Nullable SchemaMetadata fromChangeLog
    ) {
        SchemaDiff entityVsDatabase = fromDatabase != null
                ? compare(fromEntity, fromDatabase)
                : null;

        SchemaDiff entityVsChangeLog = fromChangeLog != null
                ? compare(fromEntity, fromChangeLog)
                : null;

        SchemaDiff databaseVsChangeLog = fromDatabase != null && fromChangeLog != null
                ? compare(fromDatabase, fromChangeLog)
                : null;

        ConflictAnalysis conflicts = analyzeConflicts(
                fromEntity, fromDatabase, fromChangeLog,
                entityVsDatabase, entityVsChangeLog, databaseVsChangeLog
        );

        return new ThreeWayDiff(
                fromEntity.getTableName(),
                entityVsDatabase,
                entityVsChangeLog,
                databaseVsChangeLog,
                conflicts
        );
    }

    private List<ColumnDiff> compareColumns(SchemaMetadata source, SchemaMetadata target) {
        List<ColumnDiff> diffs = new ArrayList<>();

        Map<String, ColumnMetadata> sourceColumns = source.getColumns().stream()
                .collect(Collectors.toMap(ColumnMetadata::getColumnName, Function.identity()));

        Map<String, ColumnMetadata> targetColumns = target.getColumns().stream()
                .collect(Collectors.toMap(ColumnMetadata::getColumnName, Function.identity()));

        // 检查新增和修改的列
        for (ColumnMetadata sourceCol : source.getColumns()) {
            ColumnMetadata targetCol = targetColumns.get(sourceCol.getColumnName());

            if (targetCol == null) {
                // 新增列
                diffs.add(new ColumnDiff(
                        sourceCol.getColumnName(),
                        DiffType.ADD,
                        sourceCol,
                        null,
                        List.of("Column not exists in target")
                ));
            } else {
                // 检查修改
                List<String> differences = compareColumnDetails(sourceCol, targetCol);
                if (!differences.isEmpty()) {
                    diffs.add(new ColumnDiff(
                            sourceCol.getColumnName(),
                            DiffType.MODIFY,
                            sourceCol,
                            targetCol,
                            differences
                    ));
                }
            }
        }

        // 检查删除的列
        for (ColumnMetadata targetCol : target.getColumns()) {
            if (!sourceColumns.containsKey(targetCol.getColumnName())) {
                diffs.add(new ColumnDiff(
                        targetCol.getColumnName(),
                        DiffType.DROP,
                        null,
                        targetCol,
                        List.of("Column not exists in source")
                ));
            }
        }

        return diffs;
    }

    private List<String> compareColumnDetails(ColumnMetadata source, ColumnMetadata target) {
        List<String> differences = new ArrayList<>();

        // 比较数据类型
        if (!normalizeDataType(source.getDataType()).equals(normalizeDataType(target.getDataType()))) {
            differences.add(String.format("Type: %s → %s", target.getDataType(), source.getDataType()));
        }

        // 比较可空性
        if (source.isNullable() != target.isNullable()) {
            differences.add(String.format("Nullable: %s → %s", target.isNullable(), source.isNullable()));
        }

        // 比较默认值
        String sourceDefault = source.getDefaultValue();
        String targetDefault = target.getDefaultValue();
        if (sourceDefault != null ? !sourceDefault.equals(targetDefault) : targetDefault != null) {
            differences.add(String.format("Default: %s → %s", targetDefault, sourceDefault));
        }

        // 比较唯一性
        if (source.isUnique() != target.isUnique()) {
            differences.add(String.format("Unique: %s → %s", target.isUnique(), source.isUnique()));
        }

        return differences;
    }

    private String normalizeDataType(String dataType) {
        // 统一类型格式，忽略大小写和括号内的长度差异
        String upper = dataType.toUpperCase();
        // 移除括号内容进行比较
        int parenIndex = upper.indexOf('(');
        if (parenIndex > 0) {
            return upper.substring(0, parenIndex);
        }
        return upper;
    }

    private @Nullable IndexDiff compareIndexes(SchemaMetadata source, SchemaMetadata target) {
        List<IndexMetadata> added = new ArrayList<>();
        List<IndexMetadata> dropped = new ArrayList<>();
        List<IndexMetadata> modified = new ArrayList<>();

        Map<String, IndexMetadata> sourceIndexes = source.getIndexes().stream()
                .collect(Collectors.toMap(IndexMetadata::getIndexName, Function.identity()));

        Map<String, IndexMetadata> targetIndexes = target.getIndexes().stream()
                .collect(Collectors.toMap(IndexMetadata::getIndexName, Function.identity()));

        for (IndexMetadata sourceIdx : source.getIndexes()) {
            IndexMetadata targetIdx = targetIndexes.get(sourceIdx.getIndexName());
            if (targetIdx == null) {
                added.add(sourceIdx);
            } else if (!indexesEqual(sourceIdx, targetIdx)) {
                modified.add(sourceIdx);
            }
        }

        for (IndexMetadata targetIdx : target.getIndexes()) {
            if (!sourceIndexes.containsKey(targetIdx.getIndexName())) {
                dropped.add(targetIdx);
            }
        }

        if (added.isEmpty() && dropped.isEmpty() && modified.isEmpty()) {
            return null;
        }

        return new IndexDiff(added, dropped, modified);
    }

    private boolean indexesEqual(IndexMetadata a, IndexMetadata b) {
        return a.isUnique() == b.isUnique()
                && a.getColumnNames().equals(b.getColumnNames());
    }

    private @Nullable ForeignKeyDiff compareForeignKeys(SchemaMetadata source, SchemaMetadata target) {
        List<ForeignKeyMetadata> added = new ArrayList<>();
        List<ForeignKeyMetadata> dropped = new ArrayList<>();
        List<ForeignKeyMetadata> modified = new ArrayList<>();

        Map<String, ForeignKeyMetadata> sourceFks = source.getForeignKeys().stream()
                .collect(Collectors.toMap(ForeignKeyMetadata::getConstraintName, Function.identity()));

        Map<String, ForeignKeyMetadata> targetFks = target.getForeignKeys().stream()
                .collect(Collectors.toMap(ForeignKeyMetadata::getConstraintName, Function.identity()));

        for (ForeignKeyMetadata sourceFk : source.getForeignKeys()) {
            ForeignKeyMetadata targetFk = targetFks.get(sourceFk.getConstraintName());
            if (targetFk == null) {
                added.add(sourceFk);
            } else if (!foreignKeysEqual(sourceFk, targetFk)) {
                modified.add(sourceFk);
            }
        }

        for (ForeignKeyMetadata targetFk : target.getForeignKeys()) {
            if (!sourceFks.containsKey(targetFk.getConstraintName())) {
                dropped.add(targetFk);
            }
        }

        if (added.isEmpty() && dropped.isEmpty() && modified.isEmpty()) {
            return null;
        }

        return new ForeignKeyDiff(added, dropped, modified);
    }

    private boolean foreignKeysEqual(ForeignKeyMetadata a, ForeignKeyMetadata b) {
        return a.getReferencedTableName().equals(b.getReferencedTableName())
                && a.getColumnNames().equals(b.getColumnNames())
                && a.getReferencedColumnNames().equals(b.getReferencedColumnNames());
    }

    private ConflictAnalysis analyzeConflicts(
            SchemaMetadata fromEntity,
            @Nullable SchemaMetadata fromDatabase,
            @Nullable SchemaMetadata fromChangeLog,
            @Nullable SchemaDiff entityVsDatabase,
            @Nullable SchemaDiff entityVsChangeLog,
            @Nullable SchemaDiff databaseVsChangeLog
    ) {
        ConflictAnalysis.Builder builder = new ConflictAnalysis.Builder();

        if (entityVsDatabase == null || entityVsChangeLog == null || databaseVsChangeLog == null) {
            return builder.build();
        }

        // 检查三方冲突：实体、数据库、ChangeLog 都不一致
        for (ColumnDiff entityDbDiff : entityVsDatabase.columnDiffs()) {
            if (entityDbDiff.diffType() == DiffType.MODIFY) {
                String columnName = entityDbDiff.columnName();

                // 检查 ChangeLog 是否也有差异
                ColumnDiff entityChangeLogDiff = entityVsChangeLog.columnDiffs().stream()
                        .filter(d -> d.columnName().equals(columnName))
                        .findFirst()
                        .orElse(null);

                if (entityChangeLogDiff != null && entityChangeLogDiff.diffType() == DiffType.MODIFY) {
                    // 三方都有差异，存在冲突
                    builder.addConflict(
                            columnName,
                            String.format("Entity, Database, and ChangeLog all have different definitions for column '%s'", columnName),
                            ConflictAnalysis.ConflictType.TYPE_MISMATCH,
                            ConflictAnalysis.ConflictResolution.MANUAL_RESOLUTION
                    );
                }
            }
        }

        // 检查删除冲突：实体要删除，但数据库有修改
        for (ColumnDiff dropDiff : entityVsDatabase.columnDiffs()) {
            if (dropDiff.diffType() == DiffType.DROP) {
                String columnName = dropDiff.columnName();

                // 检查数据库 vs ChangeLog 是否有差异
                boolean dbChanged = databaseVsChangeLog.columnDiffs().stream()
                        .anyMatch(d -> d.columnName().equals(columnName) && d.diffType() == DiffType.MODIFY);

                if (dbChanged) {
                    builder.addConflict(
                            columnName,
                            String.format("Column '%s' marked for drop, but database has uncommitted changes", columnName),
                            ConflictAnalysis.ConflictType.COLUMN_EXISTENCE,
                            ConflictAnalysis.ConflictResolution.MANUAL_RESOLUTION
                    );
                }
            }
        }

        return builder.build();
    }
}
