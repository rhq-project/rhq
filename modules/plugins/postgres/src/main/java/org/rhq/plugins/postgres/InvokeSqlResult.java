/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.plugins.postgres;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
* @author Thomas Segismont
*/
class InvokeSqlResult {
    private String[] columnHeaders;
    private int[] columnMaxLength;
    private List<String[]> rows;

    InvokeSqlResult(int columns) {
        columnHeaders = new String[columns];
        columnMaxLength = new int[columns];
        Arrays.fill(columnMaxLength, 0);
        rows = new ArrayList<String[]>(10);
    }

    int getColumnCount() {
        return columnHeaders.length;
    }

    String[] createRow() {
        return new String[columnHeaders.length];
    }

    void addRow(String[] row) {
        for (int i = 0; i < row.length; i++) {
            String column = row[i];
            if (column != null) {
                int columnLength = column.length();
                if (columnLength > columnMaxLength[i]) {
                    columnMaxLength[i] = columnLength;
                }
            }
        }
        rows.add(row);
    }

    String getColumnHeader(int index) {
        return columnHeaders[index];
    }

    public void setColumnHeader(int index, String header) {
        columnHeaders[index] = header;
    }

    int getColumnMaxLength(int index) {
        return Math.max(columnHeaders[index].length(), columnMaxLength[index]);
    }

    List<String[]> getRows() {
        return rows;
    }
}
