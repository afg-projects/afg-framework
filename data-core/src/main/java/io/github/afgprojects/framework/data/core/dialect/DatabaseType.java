package io.github.afgprojects.framework.data.core.dialect;

/**
 * 数据库类型枚举
 */
public enum DatabaseType {

    /** MySQL */
    MYSQL("mysql", "MySQL"),

    /** PostgreSQL */
    POSTGRESQL("postgresql", "PostgreSQL"),

    /** Oracle */
    ORACLE("oracle", "Oracle"),

    /** SQL Server */
    SQLSERVER("sqlserver", "Microsoft SQL Server"),

    /** SQLite */
    SQLITE("sqlite", "SQLite"),

    /** H2 */
    H2("h2", "H2"),

    /** OceanBase */
    OCEANBASE("oceanbase", "OceanBase"),

    /** openGauss */
    OPENGAUSS("opengauss", "openGauss"),

    /** 达梦 */
    DM("dm", "DM Database"),

    /** 金仓 */
    KINGBASE("kingbase", "KingbaseES"),

    /** GaussDB */
    GAUSSDB("gaussdb", "GaussDB"),

    /** 未知/自动检测 */
    UNKNOWN("unknown", "Unknown");

    private final String code;
    private final String name;

    DatabaseType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * 根据代码获取数据库类型
     */
    public static DatabaseType fromCode(String code) {
        if (code == null) {
            return UNKNOWN;
        }
        for (DatabaseType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return UNKNOWN;
    }

    /**
     * 是否是 MySQL 系列（包括 OceanBase）
     */
    public boolean isMySQLFamily() {
        return this == MYSQL || this == OCEANBASE;
    }

    /**
     * 是否是 PostgreSQL 系列（包括 openGauss、GaussDB）
     */
    public boolean isPostgreSQLFamily() {
        return this == POSTGRESQL || this == OPENGAUSS || this == GAUSSDB;
    }

    /**
     * 是否是国产数据库
     */
    public boolean isChineseDatabase() {
        return this == OCEANBASE || this == OPENGAUSS || this == DM || this == KINGBASE || this == GAUSSDB;
    }
}