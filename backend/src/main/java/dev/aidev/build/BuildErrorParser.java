package dev.aidev.build;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses compiler/test output into structured errors grouped by file,
 * for feeding back to the agent's fix loop.
 */
public final class BuildErrorParser {

    private BuildErrorParser() {}

    // javac: /path/File.java:[12,34] message
    private static final Pattern JAVAC = Pattern.compile("([^\\s\\[]+\\.java):\\[(\\d+),(\\d+)\\]\\s+(.*)");
    // tsc / vite: src/App.tsx:12:34 - error TS2304: ...
    private static final Pattern TSC = Pattern.compile("([^\\s:]+\\.(tsx?|jsx?)):(\\d+):(\\d+)\\s*-\\s*error\\s+(.*)");

    public static List<Map<String, Object>> parse(String output) {
        Map<String, List<Map<String, Object>>> byFile = new LinkedHashMap<>();
        if (output != null && !output.isBlank()) {
            for (String line : output.split("\n")) {
                String file = null;
                Integer lineNo = null;
                String message = null;
                Matcher m = JAVAC.matcher(line);
                if (m.find()) {
                    file = shortName(m.group(1));
                    lineNo = Integer.valueOf(m.group(2));
                    message = m.group(4);
                } else {
                    Matcher t = TSC.matcher(line);
                    if (t.find()) {
                        file = shortName(t.group(1));
                        lineNo = Integer.valueOf(t.group(3));
                        message = t.group(5);
                    }
                }
                if (file != null) {
                    byFile.computeIfAbsent(file, k -> new ArrayList<>())
                            .add(Map.of("line", lineNo, "message", message == null ? "" : message));
                }
            }
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (var e : byFile.entrySet()) {
            out.add(Map.of("file", e.getKey(), "errors", e.getValue()));
        }
        out.sort((a, b) -> String.valueOf(a.get("file")).compareTo(String.valueOf(b.get("file"))));
        return out;
    }

    private static String shortName(String path) {
        int i = path.lastIndexOf('/');
        return i >= 0 ? path.substring(i + 1) : path;
    }
}
