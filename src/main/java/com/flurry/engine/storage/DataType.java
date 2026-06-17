package com.flurry.engine.storage;

public enum DataType {
    INT, LONG, DOUBLE, STRING, BOOLEAN;

    public static DataType infer(String raw) {
        if (raw == null || raw.isBlank()) return STRING;
        String s = raw.trim();
        if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false")) return BOOLEAN;
        try { Integer.parseInt(s); return INT; } catch (NumberFormatException ignored) {}
        try { Long.parseLong(s); return LONG; } catch (NumberFormatException ignored) {}
        try { Double.parseDouble(s); return DOUBLE; } catch (NumberFormatException ignored) {}
        return STRING;
    }
}