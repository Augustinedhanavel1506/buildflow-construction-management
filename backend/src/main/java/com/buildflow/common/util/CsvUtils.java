package com.buildflow.common.util;

import java.util.List;
import java.util.stream.Collectors;

public final class CsvUtils {

    private CsvUtils() {
    }

    public static String buildCsv(List<String> headers, List<List<String>> rows) {
        StringBuilder csv = new StringBuilder();
        csv.append(toCsvLine(headers));
        for (List<String> row : rows) {
            csv.append(toCsvLine(row));
        }
        return csv.toString();
    }

    private static String toCsvLine(List<String> values) {
        return values.stream()
                .map(CsvUtils::escape)
                .collect(Collectors.joining(",")) + "\r\n";
    }

    private static String escape(Object value) {
        String text = value == null ? "" : value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
