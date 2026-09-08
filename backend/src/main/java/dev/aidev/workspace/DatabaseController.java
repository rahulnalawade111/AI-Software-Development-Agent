package dev.aidev.workspace;

import dev.aidev.project.ProjectService;
import dev.aidev.security.AppPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-oriented database browsing for the Database panel. Uses the same
 * per-project database as the AI database_query tool. Only SELECT is allowed
 * here; writes go through the AI tool with approval.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/database")
public class DatabaseController {

    private final ProjectService projects;

    @Value("${app.projects.database-url-template:jdbc:mysql://localhost:3306/}")
    private String jdbcUrlTemplate;

    @Value("${app.projects.database-user:}")
    private String dbUser;

    @Value("${app.projects.database-password:}")
    private String dbPassword;

    public DatabaseController(ProjectService projects) {
        this.projects = projects;
    }

    @GetMapping("/schema")
    public Map<String, Object> schema(@AuthenticationPrincipal AppPrincipal principal,
                                      @PathVariable Long projectId) throws Exception {
        projects.requireRead(projectId, principal);
        try (Connection conn = open(projectId)) {
            String catalog = conn.getCatalog();
            List<Map<String, Object>> tables = new ArrayList<>();
            try (ResultSet rs = conn.getMetaData().getTables(catalog, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    Map<String, Object> t = new LinkedHashMap<>();
                    String name = rs.getString("TABLE_NAME");
                    t.put("name", name);
                    List<Map<String, Object>> cols = new ArrayList<>();
                    try (ResultSet cs = conn.getMetaData().getColumns(catalog, null, name, "%")) {
                        while (cs.next()) {
                            Map<String, Object> c = new LinkedHashMap<>();
                            c.put("name", cs.getString("COLUMN_NAME"));
                            c.put("type", cs.getString("TYPE_NAME"));
                            cols.add(c);
                        }
                    }
                    t.put("columns", cols);
                    tables.add(t);
                }
            }
            return Map.of("tables", tables);
        } catch (Exception e) {
            return Map.of("error", String.valueOf(e.getMessage()));
        }
    }

    @PostMapping("/query")
    public Map<String, Object> query(@AuthenticationPrincipal AppPrincipal principal,
                                     @PathVariable Long projectId,
                                     @RequestBody Map<String, String> body) throws Exception {
        projects.requireRead(projectId, principal);
        String sql = body.get("sql") == null ? "" : body.get("sql").trim();
        String upper = sql.toUpperCase();
        if (!upper.startsWith("SELECT") && !upper.startsWith("SHOW") && !upper.startsWith("DESCRIBE")) {
            throw new IllegalArgumentException("Only SELECT/SHOW/DESCRIBE allowed from the panel");
        }
        try (Connection conn = open(projectId); Statement st = conn.createStatement()) {
            st.setQueryTimeout(30);
            try (ResultSet rs = st.executeQuery(sql)) {
                ResultSetMetaData md = rs.getMetaData();
                int cols = md.getColumnCount();
                List<String> columns = new ArrayList<>();
                for (int i = 1; i <= cols; i++) columns.add(md.getColumnLabel(i));
                List<Map<String, Object>> rows = new ArrayList<>();
                while (rs.next() && rows.size() < 200) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= cols; i++) row.put(columns.get(i - 1), rs.getObject(i));
                    rows.add(row);
                }
                return Map.of("columns", columns, "rows", rows, "rowCount", rows.size());
            }
        } catch (Exception e) {
            return Map.of("error", String.valueOf(e.getMessage()));
        }
    }

    private Connection open(Long projectId) throws Exception {
        String url = jdbcUrlTemplate.endsWith("/")
                ? jdbcUrlTemplate + "proj_" + projectId
                : jdbcUrlTemplate + "/proj_" + projectId;
        return DriverManager.getConnection(url, dbUser, dbPassword);
    }
}
