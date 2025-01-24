package mainFiles.database;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.Dialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;

public class SQLiteDialect extends Dialect {

    public SQLiteDialect() {
        super();
    }

    @Override
    public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        var jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();

        jdbcTypeRegistry.addDescriptor(SqlTypes.BIT, jdbcTypeRegistry.getDescriptor(SqlTypes.BOOLEAN));
        jdbcTypeRegistry.addDescriptor(SqlTypes.TINYINT, jdbcTypeRegistry.getDescriptor(SqlTypes.INTEGER));
        jdbcTypeRegistry.addDescriptor(SqlTypes.SMALLINT, jdbcTypeRegistry.getDescriptor(SqlTypes.INTEGER));
        jdbcTypeRegistry.addDescriptor(SqlTypes.INTEGER, jdbcTypeRegistry.getDescriptor(SqlTypes.INTEGER));
        jdbcTypeRegistry.addDescriptor(SqlTypes.BIGINT, jdbcTypeRegistry.getDescriptor(SqlTypes.BIGINT));
        jdbcTypeRegistry.addDescriptor(SqlTypes.FLOAT, jdbcTypeRegistry.getDescriptor(SqlTypes.FLOAT));
        jdbcTypeRegistry.addDescriptor(SqlTypes.REAL, jdbcTypeRegistry.getDescriptor(SqlTypes.REAL));
        jdbcTypeRegistry.addDescriptor(SqlTypes.DOUBLE, jdbcTypeRegistry.getDescriptor(SqlTypes.DOUBLE));

        jdbcTypeRegistry.addDescriptor(SqlTypes.VARCHAR, jdbcTypeRegistry.getDescriptor(SqlTypes.VARCHAR));
        jdbcTypeRegistry.addDescriptor(SqlTypes.LONGVARCHAR, jdbcTypeRegistry.getDescriptor(SqlTypes.LONGVARCHAR));
        jdbcTypeRegistry.addDescriptor(SqlTypes.CLOB, jdbcTypeRegistry.getDescriptor(SqlTypes.LONGVARCHAR));

        jdbcTypeRegistry.addDescriptor(SqlTypes.BLOB, jdbcTypeRegistry.getDescriptor(SqlTypes.BLOB));
        jdbcTypeRegistry.addDescriptor(SqlTypes.BINARY, jdbcTypeRegistry.getDescriptor(SqlTypes.BLOB));
        jdbcTypeRegistry.addDescriptor(SqlTypes.VARBINARY, jdbcTypeRegistry.getDescriptor(SqlTypes.BLOB));
        jdbcTypeRegistry.addDescriptor(SqlTypes.LONGVARBINARY, jdbcTypeRegistry.getDescriptor(SqlTypes.BLOB));

        jdbcTypeRegistry.addDescriptor(SqlTypes.DATE, jdbcTypeRegistry.getDescriptor(SqlTypes.DATE));
        jdbcTypeRegistry.addDescriptor(SqlTypes.TIME, jdbcTypeRegistry.getDescriptor(SqlTypes.TIME));
        jdbcTypeRegistry.addDescriptor(SqlTypes.TIMESTAMP, jdbcTypeRegistry.getDescriptor(SqlTypes.TIMESTAMP));
        jdbcTypeRegistry.addDescriptor(SqlTypes.TIMESTAMP_WITH_TIMEZONE, jdbcTypeRegistry.getDescriptor(SqlTypes.TIMESTAMP));
        jdbcTypeRegistry.addDescriptor(SqlTypes.TIME_WITH_TIMEZONE, jdbcTypeRegistry.getDescriptor(SqlTypes.TIME));

        jdbcTypeRegistry.addDescriptor(SqlTypes.NUMERIC, jdbcTypeRegistry.getDescriptor(SqlTypes.NUMERIC));
        jdbcTypeRegistry.addDescriptor(SqlTypes.DECIMAL, jdbcTypeRegistry.getDescriptor(SqlTypes.DECIMAL));
        jdbcTypeRegistry.addDescriptor(SqlTypes.BOOLEAN, jdbcTypeRegistry.getDescriptor(SqlTypes.BOOLEAN));
        jdbcTypeRegistry.addDescriptor(SqlTypes.CHAR, jdbcTypeRegistry.getDescriptor(SqlTypes.VARCHAR));
        jdbcTypeRegistry.addDescriptor(SqlTypes.NVARCHAR, jdbcTypeRegistry.getDescriptor(SqlTypes.VARCHAR));
        jdbcTypeRegistry.addDescriptor(SqlTypes.NCHAR, jdbcTypeRegistry.getDescriptor(SqlTypes.VARCHAR));
    }

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        functionContributions.getFunctionRegistry().registerNamed(
                "concat",
                functionContributions.getTypeConfiguration().getBasicTypeForJavaType(String.class)
        );

        functionContributions.getFunctionRegistry().registerNamed(
                "mod",
                functionContributions.getTypeConfiguration().getBasicTypeForJavaType(Integer.class)
        );

        functionContributions.getFunctionRegistry().registerNamed(
                "quote",
                functionContributions.getTypeConfiguration().getBasicTypeForJavaType(String.class)
        );

        functionContributions.getFunctionRegistry().registerNamed(
                "random",
                functionContributions.getTypeConfiguration().getBasicTypeForJavaType(Double.class)
        );

        functionContributions.getFunctionRegistry().registerNamed(
                "round",
                functionContributions.getTypeConfiguration().getBasicTypeForJavaType(Double.class)
        );

        functionContributions.getFunctionRegistry().registerNamed(
                "substr",
                functionContributions.getTypeConfiguration().getBasicTypeForJavaType(String.class)
        );

        functionContributions.getFunctionRegistry().registerNamed(
                "trim",
                functionContributions.getTypeConfiguration().getBasicTypeForJavaType(String.class)
        );
    }

    @Override
    public boolean supportsCurrentTimestampSelection() {
        return true;
    }

    @Override
    public boolean isCurrentTimestampSelectStringCallable() {
        return false;
    }

    @Override
    public String getCurrentTimestampSelectString() {
        return "select current_timestamp";
    }

    @Override
    public boolean supportsUnionAll() {
        return true;
    }

    @Override
    public boolean hasAlterTable() {
        return false;
    }

    @Override
    public boolean dropConstraints() {
        return false;
    }

    @Override
    public String getForUpdateString() {
        return "";
    }

    @Override
    public boolean supportsOuterJoinForUpdate() {
        return false;
    }

    @Override
    public String getDropForeignKeyString() {
        throw new UnsupportedOperationException("SQLite не поддерживает внешние ключи");
    }

    @Override
    public String getAddForeignKeyConstraintString(String constraintName, String[] foreignKey,
                                                   String referencedTable, String[] primaryKey,
                                                   boolean referencesPrimaryKey) {
        throw new UnsupportedOperationException("SQLite не поддерживает внешние ключи");
    }

    @Override
    public String getAddPrimaryKeyConstraintString(String constraintName) {
        throw new UnsupportedOperationException("SQLite не поддерживает добавление первичных ключей");
    }

    @Override
    public boolean supportsIfExistsBeforeTableName() {
        return true;
    }

    @Override
    public boolean supportsTupleDistinctCounts() {
        return false;
    }

    @Override
    public String getSelectGUIDString() {
        return "select hex(randomblob(16))";
    }
}