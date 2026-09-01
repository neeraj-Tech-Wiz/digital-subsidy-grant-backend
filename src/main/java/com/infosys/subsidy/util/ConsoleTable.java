package com.infosys.subsidy.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for rendering formatted ASCII tables in the console.
 */
public class ConsoleTable {

    private final String[] headers;
    private final List<String[]> rows = new ArrayList<>();

    public ConsoleTable(String... headers) {
        this.headers = headers;
    }

    public void addRow(String... rowValues) {
        this.rows.add(rowValues);
    }

    public void print() {
        int columns = headers.length;
        int[] colWidths = new int[columns];

        for (int i = 0; i < columns; i++) {
            colWidths[i] = headers[i].length();
        }

        for (String[] row : rows) {
            for (int i = 0; i < columns && i < row.length; i++) {
                if (row[i] != null && row[i].length() > colWidths[i]) {
                    colWidths[i] = row[i].length();
                }
            }
        }

        StringBuilder divider = new StringBuilder("+");
        for (int w : colWidths) {
            divider.append("-".repeat(w + 2)).append("+");
        }

        System.out.println(divider);

        // Print header
        StringBuilder headerLine = new StringBuilder("|");
        for (int i = 0; i < columns; i++) {
            headerLine.append(String.format(" %-" + colWidths[i] + "s |", headers[i]));
        }
        System.out.println(headerLine);
        System.out.println(divider);

        // Print rows
        for (String[] row : rows) {
            StringBuilder rowLine = new StringBuilder("|");
            for (int i = 0; i < columns; i++) {
                String val = (i < row.length && row[i] != null) ? row[i] : "";
                rowLine.append(String.format(" %-" + colWidths[i] + "s |", val));
            }
            System.out.println(rowLine);
        }

        System.out.println(divider);
    }
}
