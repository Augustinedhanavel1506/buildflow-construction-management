package com.buildflow.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * On MySQL, Hibernate creates enum-typed Java fields as native ENUM columns, and
 * {@code ddl-auto: update} never widens an existing ENUM when a value is added later (a new role,
 * notification type or estimation basis). The insert then fails with "Data truncated for column"
 * and the application cannot start or save. This converts every ENUM column in the schema to
 * VARCHAR, which Hibernate reads and writes identically, so adding enum values keeps working.
 *
 * Runs before the other startup runners. It is idempotent, does nothing on other databases, and
 * keeps each column's nullability. The values already stored are unchanged.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EnumColumnMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EnumColumnMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public EnumColumnMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        String product = jdbcTemplate.execute((java.sql.Connection c) -> c.getMetaData().getDatabaseProductName());
        if (product == null || !product.toLowerCase().contains("mysql")) {
            return;
        }

        List<Map<String, Object>> columns = jdbcTemplate.queryForList("""
                select table_name, column_name, is_nullable
                from information_schema.columns
                where table_schema = database() and data_type = 'enum'
                """);
        for (Map<String, Object> column : columns) {
            String table = String.valueOf(column.get("TABLE_NAME") != null ? column.get("TABLE_NAME") : column.get("table_name"));
            String name = String.valueOf(column.get("COLUMN_NAME") != null ? column.get("COLUMN_NAME") : column.get("column_name"));
            Object nullable = column.get("IS_NULLABLE") != null ? column.get("IS_NULLABLE") : column.get("is_nullable");
            String nullClause = "YES".equalsIgnoreCase(String.valueOf(nullable)) ? "NULL" : "NOT NULL";
            jdbcTemplate.execute("alter table `" + table + "` modify column `" + name + "` varchar(64) " + nullClause);
            log.info("Converted enum column {}.{} to varchar(64)", table, name);
        }
    }
}
