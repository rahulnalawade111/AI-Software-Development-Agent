package dev.aidev.workspace;

import dev.aidev.project.ProjectService;
import dev.aidev.security.AppPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;

/**
 * Provisions the per-project MySQL database (schema proj_<id>) using the
 * platform root credentials, then grants the app DB user full rights on it.
 * Idempotent — called when the agent first needs database_query/schema or
 * when the Database panel is opened.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/database")
public class DatabaseProvisionController {

    private final ProjectService projects;

    @Value("${app.projects.database-url-template:jdbc:mysql://localhost:3306/}")
    private String jdbcUrlTemplate;

    @Value("${app.projects.database-user:}")
    private String appDbUser;

    @Value("${spring.datasource.url:}")
    private String platformUrl;

    @Value("${spring.datasource.username:}")
    private String platformUser;

    @Value("${spring.datasource.password:}")
    private String platformPassword;

    public DatabaseProvisionController(ProjectService projects) {
        this.projects = projects;
    }

    @PostMapping("/provision")
    public Map<String, Object> provision(@AuthenticationPrincipal AppPrincipal principal,
                                         @PathVariable Long projectId) {
        projects.requireWrite(projectId, principal);
        String dbName = "proj_" + projectId;
        try (Connection conn = DriverManager.getConnection(platformUrl, platformUser, platformPassword);
             var st = conn.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName);
            try (var g = conn.createStatement()) {
                g.executeUpdate("GRANT ALL PRIVILEGES ON " + dbName + ".* TO '" + appDbUser + "'@'%'");
                g.executeUpdate("FLUSH PRIVILEGES");
            }
            String url = jdbcUrlTemplate.endsWith("/")
                    ? jdbcUrlTemplate + dbName : jdbcUrlTemplate + "/" + dbName;
            return Map.of("provisioned", true, "database", dbName, "url", url);
        } catch (Exception e) {
            return Map.of("provisioned", false, "error", String.valueOf(e.getMessage()));
        }
    }
}
