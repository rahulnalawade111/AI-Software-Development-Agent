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

/** database_query - executes SQL against the project's dedicated MySQL database. */
@Component
public class DatabaseQueryTool implements AiTool {

    public String name() { return "database_query"; }

    public String label() { return "Database query"; }

    public String description() {
        return "Execute SQL against the project's dedicated MySQL database (SELECT or DDL/DML). "
                + "CREATE, ALTER, DROP, INSERT, UPDATE, DELETE, TRUNCATE require user approval first. "
                + "Returns rows (for SELECT, max 200) and timing.";
    }

    public String parametersSchema() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"sql\":{\"type\":\"string\",\"description\":\"SQL statement to run\"}},"
                + "\"required\":[\"sql\"]}";
    }

    public boolean requiresApproval(Map<String, Object> args) {
        String sql = args.get("sql") instanceof String s ? s.toUpperCase() : "";
        String[] writeKeywords = {"CREATE", "ALTER", "DROP", "INSERT", "UPDATE",
                "DELETE", "TRUNCATE", "GRANT", "REVOKE", "REPLACE"};
        for (String keyword : writeKeywords) {
            if (sql.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public Object execute(Map<String, Object> args, ToolContext ctx) throws Exception {
        String sql = args.get("sql") instanceof String s ? s.trim() : "";
        if (sql.isEmpty()) {
            throw new IllegalArgumentException("sql is required");
        }
        String url = ctx.jdbcUrl();
        if (url == null || url.isBlank()) {
            return Map.of("error", "No project database provisioned for this project");
        }
        long start = System.currentTimeMillis();
        try (Connection conn = DriverManager.getConnection(url, ctx.dbUser(), ctx.dbPassword());
             Statement st = conn.createStatement()) {
            st.setQueryTimeout(30);
            boolean isQuery = st.execute(sql);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("sql", sql);
            if (isQuery) {
                try (ResultSet rs = st.getResultSet()) {
                    ResultSetMetaData md = rs.getMetaData();
                    int cols = md.getColumnCount();
                    List<String> columns = new ArrayList<>();
                    for (int i = 1; i <= cols; i++) {
                        columns.add(md.getColumnLabel(i));
                    }
                    List<Map<String, Object>> rows = new ArrayList<>();
                    while (rs.next() && rows.size() < 200) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= cols; i++) {
                            row.put(columns.get(i - 1), rs.getObject(i));
                        }
                        rows.add(row);
                    }
                    out.put("columns", columns);
                    out.put("rows", rows);
                    out.put("rowCount", rows.size());
                }
            } else {
                out.put("updateCount", st.getUpdateCount());
            }
            out.put("durationMs", System.currentTimeMillis() - start);
            return out;
        } catch (Exception e) {
            return Map.of("error", String.valueOf(e.getMessage()), "sql", sql);
        }
    }
}
