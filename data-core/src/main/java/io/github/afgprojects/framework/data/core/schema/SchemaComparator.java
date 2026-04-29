package io.github.afgprojects.framework.data.core.schema;

import java.util.List;

/**
 * Schema 差异比对接口
 */
public interface SchemaComparator {

    /**
     * 比对两个 Schema 的差异
     *
     * @param source 源 Schema（通常是实体定义）
     * @param target 目标 Schema（通常是数据库或 ChangeLog）
     * @return 差异结果
     */
    SchemaDiff compare(SchemaMetadata source, SchemaMetadata target);

    /**
     * 三向比对：实体 vs 数据库 vs ChangeLog
     *
     * @param fromEntity    实体 Schema
     * @param fromDatabase  数据库 Schema（可为 null）
     * @param fromChangeLog ChangeLog Schema（可为 null）
     * @return 三向差异结果
     */
    ThreeWayDiff compareThreeWay(
            SchemaMetadata fromEntity,
            SchemaMetadata fromDatabase,
            SchemaMetadata fromChangeLog
    );
}
