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

import java.util.Arrays;

/**
* @author Thomas Segismont
*/
class InvokeSqlResultFormattedTextExporter implements InvokeSqlResultExporter {
    private static final char SEPARATOR_CHAR = '+';

    @Override
    public String export(InvokeSqlResult invokeSqlResult) {
        StringBuilder builder = new StringBuilder();

        appendSeparatorLine(builder, invokeSqlResult);
        builder.append(SEPARATOR_CHAR);
        for (int i = 0; i < invokeSqlResult.getColumnCount(); i++) {
            String columnHeader = invokeSqlResult.getColumnHeader(i);
            int totalSpace = 2 /* space before and after */+ invokeSqlResult.getColumnMaxLength(i)
                - columnHeader.length();
            int leftSpace = totalSpace / 2;
            int rightSpace = leftSpace + totalSpace % 2;
            appendSpace(builder, leftSpace);
            builder.append(columnHeader);
            appendSpace(builder, rightSpace);
            builder.append(SEPARATOR_CHAR);
        }
        builder.append(String.format("%n"));
        appendSeparatorLine(builder, invokeSqlResult);

        for (String[] row : invokeSqlResult.getRows()) {
            builder.append(SEPARATOR_CHAR);
            for (int i = 0; i < row.length; i++) {
                builder.append(String.format(" %" + invokeSqlResult.getColumnMaxLength(i) + "s ", row[i]));
                builder.append(SEPARATOR_CHAR);
            }
            builder.append(String.format("%n"));
        }

        appendSeparatorLine(builder, invokeSqlResult);

        return builder.toString();
    }

    private void appendSpace(StringBuilder builder, int length) {
        char[] chars = new char[length];
        Arrays.fill(chars, ' ');
        builder.append(chars);
    }

    private void appendSeparatorLine(StringBuilder builder, InvokeSqlResult invokeSqlResult) {
        builder.append(SEPARATOR_CHAR);
        for (int i = 0; i < invokeSqlResult.getColumnCount(); i++) {
            int columnSize = invokeSqlResult.getColumnMaxLength(i);
            char[] chars = new char[columnSize + 2 /* space before and after */+ 1 /* separator */];
            Arrays.fill(chars, SEPARATOR_CHAR);
            builder.append(chars);
        }
        builder.append(String.format("%n"));
    }
}
