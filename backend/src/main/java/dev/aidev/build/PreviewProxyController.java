package dev.aidev.build;

import dev.aidev.project.ProjectService;
import dev.aidev.security.AppPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Proxies HTTP from the Preview panel to the project's running preview
 * process (managed by PreviewRegistry). Keeps the preview sandboxed behind
 * JWT auth — only project members can reach it.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/preview")
public class PreviewProxyController {

    private final ProjectService projects;
    private final PreviewRegistry preview;

    public PreviewProxyController(ProjectService projects, PreviewRegistry preview) {
        this.projects = projects;
        this.preview = preview;
    }

    @RequestMapping("/**")
    public ResponseEntity<byte[]> proxy(@AuthenticationPrincipal AppPrincipal principal,
                                        @PathVariable Long projectId,
                                        HttpMethod method,
                                        HttpServletRequest request,
                                        @RequestHeader Map<String, String> headers,
                                        @RequestBody(required = false) byte[] body) {
        projects.requireRead(projectId, principal);
        Integer port = preview.portOf(projectId);
        if (port == null || !preview.isRunning(projectId)) {
            return ResponseEntity.status(503).body("Preview not running".getBytes());
        }
        String suffix = request.getRequestURI()
                .replaceFirst("^/api/projects/" + projectId + "/preview", "");
        if (suffix.isEmpty()) suffix = "/";
        try {
            URL url = new URL("http://localhost:" + port + suffix
                    + (request.getQueryString() == null ? "" : "?" + request.getQueryString()));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method.name());
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(10000);
            headers.forEach((k, v) -> {
                if (!k.equalsIgnoreCase("host") && !k.equalsIgnoreCase("authorization")
                        && !k.equalsIgnoreCase("content-length")) {
                    conn.setRequestProperty(k, v);
                }
            });
            if (body != null && body.length > 0) {
                conn.setDoOutput(true);
                conn.getOutputStream().write(body);
            }
            int status = conn.getResponseCode();
            String contentType = conn.getContentType();
            byte[] out;
            try (InputStream is = status < 400 ? conn.getInputStream() : conn.getErrorStream()) {
                out = is == null ? new byte[0] : is.readAllBytes();
            }
            HttpHeaders respHeaders = new HttpHeaders();
            if (contentType != null) respHeaders.put(HttpHeaders.CONTENT_TYPE, List.of(contentType));
            return ResponseEntity.status(status).headers(respHeaders).body(out);
        } catch (Exception e) {
            return ResponseEntity.status(502)
                    .body(("Preview proxy error: " + e.getMessage()).getBytes());
        }
    }
}
