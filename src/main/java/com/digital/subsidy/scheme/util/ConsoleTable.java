package com.digital.subsidy.scheme.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility for rendering formatted ASCII tables in the terminal.
 */
public class ConsoleTable {

    private final String[] headers;
    private final List<String[]> rows = new ArrayList<>();
    private final int[] columnWidths;

    public ConsoleTable(String... headers) {
        this.headers = headers;
        this.columnWidths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            columnWidths[i] = headers[i].length();
        }
    }

    public ConsoleTable addRow(String... cells) {
        String[] row = new String[headers.length];
        for (int i = 0; i < headers.length; i++) {
            row[i] = (i < cells.length && cells[i] != null) ? cells[i] : "";
            if (row[i].length() > columnWidths[i]) {
                columnWidths[i] = row[i].length();
            }
        }
        rows.add(row);
        return this;
    }

    public void print() {
        printDivider("+", "-");
        printRow(headers);
        printDivider("+", "=");
        if (rows.isEmpty()) {
            System.out.println("|  (No data available to display) " + " ".repeat(Math.max(0, getTotalWidth() - 36)) + "|");
        } else {
            for (String[] row : rows) {
                printRow(row);
            }
        }
        printDivider("+", "-");
    }

    private void printRow(String[] cells) {
        StringBuilder sb = new StringBuilder("| ");
        for (int i = 0; i < headers.length; i++) {
            String cell = (i < cells.length) ? cells[i] : "";
            sb.append(String.format("%-" + columnWidths[i] + "s", cell));
            sb.append(" | ");
        }
        System.out.println(sb.toString().trim());
    }

    private void printDivider(String corner, String lineChar) {
        StringBuilder sb = new StringBuilder(corner);
        for (int width : columnWidths) {
            sb.append(lineChar.repeat(width + 2));
            sb.append(corner);
        }
        System.out.println(sb.toString());
    }

    private int getTotalWidth() {
        int w = 1;
        for (int width : columnWidths) {
            w += width + 3;
        }
        return w;
    }
}
