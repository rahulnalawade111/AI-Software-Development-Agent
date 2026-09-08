package dev.aidev.ai.tools.impl;

import java.util.List;
import java.util.Map;

/** Shared helpers for tool implementations. */
final class ToolUtil {

    private ToolUtil() {}

    static String str(Map<String, Object> args, String key) {
        Object v = args.get(key);
        return v == null ? "" : String.valueOf(v);
    }

    static String str(Map<String, Object> args, String key, String def) {
        Object v = args.get(key);
        return v == null || String.valueOf(v).isBlank() ? def : String.valueOf(v);
    }

    static double num(Map<String, Object> args, String key, double def) {
        Object v = args.get(key);
        if (v instanceof Number n) return n.doubleValue();
        if (v != null) {
            try { return Double.parseDouble(String.valueOf(v)); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    /** Rough changed-line count between two file versions (for compact tool results). */
    static int diffLinesChanged(String before, String after) {
        if (before == null || after == null) return 0;
        java.util.Set<String> b = new java.util.HashSet<>(List.of(before.split("\n", -1)));
        java.util.Set<String> a = new java.util.HashSet<>(List.of(after.split("\n", -1)));
        int changed = 0;
        for (String line : a) if (!b.contains(line)) changed++;
        for (String line : b) if (!a.contains(line)) changed++;
        return changed;
    }
}
