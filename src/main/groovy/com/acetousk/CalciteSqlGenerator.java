package com.acetousk;

import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.SqlParser.Config;
//import org.apache.calcite.sql.parser.SqlParserImpl;

public class CalciteSqlGenerator {

    public static void main(String[] args) {
        String tableName = "employees";
        String columnName = "salary";
        double minSalary = 50000.0;

        // Define the SQL dialect (such as PostgreSQL, MySQL, etc.)
        SqlDialect dialect = SqlDialect.DatabaseProduct.POSTGRESQL.getDialect();

        // Generate SQL query using Calcite
        String sql = generateSql(dialect, tableName, columnName, minSalary);
        System.out.println("Generated SQL: " + sql);
    }

    public static String generateSql(SqlDialect dialect,
            String tableName, String columnName, double minSalary) {
//        String sql = "SELECT * FROM " + dialect.quoteIdentifier(tableName) +
//                " WHERE " + dialect.quoteIdentifier(columnName) + " > " + minSalary;

//        // Parse the generated SQL to make sure it's valid
//        SqlParser.Config parserConfig = SqlParser.config()
//                .withParserFactory(SqlParserImpl.FACTORY);
//        SqlParser parser = SqlParser.create(sql, parserConfig);
//        SqlNode sqlNode = parser.parseStmt();

        // Convert the parsed SQL node back to a formatted SQL string
//        return sqlNode.toSqlString(dialect).getSql();
        return null;
    }
}
