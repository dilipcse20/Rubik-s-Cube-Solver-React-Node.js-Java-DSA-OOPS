package com.example.rubik;

import java.util.List;
import java.util.Map;

/** Tiny helper that builds JSON text by hand, so we need no libraries. */
final class Json {

    private final StringBuilder text = new StringBuilder("{");
    private boolean isFirst = true;

    static Json object() {
        return new Json();
    }

    Json put(String key, String value) {
        return putRaw(key, value == null ? "null" : quote(value));
    }

    Json put(String key, long value) {
        return putRaw(key, String.valueOf(value));
    }

    Json put(String key, double value) {
        return putRaw(key, String.valueOf(value));
    }

    Json put(String key, boolean value) {
        return putRaw(key, String.valueOf(value));
    }

    Json putStrings(String key, List<String> values) {
        StringBuilder array = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                array.append(",");
            }
            array.append(quote(values.get(i)));
        }
        return putRaw(key, array.append("]").toString());
    }

    /** Writes a map such as {"U": "UUUUUUUUU", ...} as a JSON object. */
    Json putMap(String key, Map<String, String> values) {
        Json inner = Json.object();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            inner.put(entry.getKey(), entry.getValue());
        }
        return putRaw(key, inner.build());
    }

    String build() {
        return text + "}";
    }

    private Json putRaw(String key, String rawValue) {
        if (!isFirst) {
            text.append(",");
        }
        isFirst = false;
        text.append(quote(key)).append(":").append(rawValue);
        return this;
    }

    private static String quote(String value) {
        StringBuilder result = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\n' -> result.append("\\n");
                default -> result.append(c);
            }
        }
        return result.append("\"").toString();
    }
}
