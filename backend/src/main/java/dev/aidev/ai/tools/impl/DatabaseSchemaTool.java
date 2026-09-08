package dev.aidev.ai.tools.impl;

import dev.aidev.ai.tools.AiTool;
import dev.aidev.ai.tools.ToolContext;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** database_schema - describes tables/columns of the project's MySQL database. */
@Component
public class DatabaseSchemaTool implements AiTool {

    public String name() { return "database_schema"; }

    public String label() { return "Database schema"; }

    public String description() {
        return "List the tables and columns of the project's dedicated MySQL database, "
                + "so generated code matches the real schema.";
    }

    public String parametersSchema() {
        return "{\"type\":\"object\",\"properties\":{},\"required\":[]}";
    }

    public Object execute(Map<String, Object> args, ToolContext ctx) throws Exception {
        String url = ctx.jdbcUrl();
        if (url == null || url.isBlank()) {
            return Map.of("error", "No project database provisioned for this project");
        }
        try (Connection conn = DriverManager.getConnection(url, ctx.dbUser(), ctx.dbPassword())) {
            List<Map<String, Object>> tables = new ArrayList<>();
            String catalog = conn.getCatalog();
            try (ResultSet rs = conn.getMetaData().getTables(catalog, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    Map<String, Object> table = new LinkedHashMap<>();
                    String tableName = rs.getString("TABLE_NAME");
                    table.put("name", tableName);
                    List<Map<String, Object>> cols = new ArrayList<>();
                    try (ResultSet cs = conn.getMetaData().getColumns(catalog, null, tableName, "%")) {
                        while (cs.next()) {
                            Map<String, Object> c = new LinkedHashMap<>();
                            c.put("name", cs.getString("COLUMN_NAME"));
                            c.put("type", cs.getString("TYPE_NAME"));
                            c.put("nullable", "YES".equalsIgnoreCase(cs.getString("IS_NULLABLE")));
                            c.put("size", cs.getInt("COLUMN_SIZE"));
                            cols.add(c);
                        }
                    }
                    table.put("columns", cols);
                    tables.add(table);
                }
            }
            return Map.of("tables", tables);
        } catch (Exception e) {
            return Map.of("error", String.valueOf(e.getMessage()));
        }
    }
}
