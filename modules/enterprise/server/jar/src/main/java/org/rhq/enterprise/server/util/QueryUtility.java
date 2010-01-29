/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.server.util;

import org.rhq.core.db.DatabaseTypeFactory;

public class QueryUtility {

    private static String ESCAPE_CHARACTER = null;
    private static String ESCAPED_PERCENT = null;
    private static String ESCAPED_UNDERSCORE = null;

    /**
     * Given the settings for the current DatabaseType, properly handle escaping special SQL characters.
     * 
     * @param value
     * @return the properly escaped value.
     */
    public static String escapeSearchParameter(String value) {
        if (value == null || value.trim().equals("")) {
            return null;
        }

        if (null == ESCAPED_UNDERSCORE) {
            ESCAPED_UNDERSCORE = getEscapeCharacter() + "_";
            ESCAPED_PERCENT = getEscapeCharacter() + "%";
        }

        // Now, escape LIKE's wildcard characters with escaped characters so that the user's input
        // will be matched literally
        value = value.replace("_", ESCAPED_UNDERSCORE);
        value = value.replace("%", ESCAPED_PERCENT);

        return value;
    }

    /**
     * Given the settings for the current DatabaseType, properly handle escaping special SQL characters as
     * well as upcasing the value (standard for rhq filter searches) and wrapping with SQL wildcard for
     * implicit "contains" (i.e. '%' characters)  
     * 
     * @param value
     * @return the properly escaped and formatted value.
     */
    public static String formatSearchParameter(String value) {
        if (value == null || value.trim().equals("")) {
            return null;
        }

        return "%" + escapeSearchParameter(value).toUpperCase() + "%";
    }

    /**
     * Get the proper LIKE operator escape clause for the current DatabaseType.
     * 
     * @return The escape clause buffered with single spaces. For example: " ESCAPE '\' "
     */
    public static String getEscapeClause() {
        return " ESCAPE '" + getEscapeCharacter() + "' ";
    }

    /**
     * Get the proper escape character for the current DatabaseType.
     * 
     * @return The escape character(s)
     */
    public static String getEscapeCharacter() {
        if (null == ESCAPE_CHARACTER) {
            ESCAPE_CHARACTER = DatabaseTypeFactory.getDefaultDatabaseType().getEscapeCharacter();
        }

        return ESCAPE_CHARACTER;
    }

    /**
     * If the current DatabaseType requires double escaping then ensure it is set correctly. This may be useful
     * if the search string has been constructed outside of QueryUtil.formatSearchParameter(String).
     * 
     * @param value single escaped search string value
     * @return The double escaped string
     */
    public static String handleDoubleEscaping(String value) {
        if (null == ESCAPE_CHARACTER) {
            ESCAPE_CHARACTER = DatabaseTypeFactory.getDefaultDatabaseType().getEscapeCharacter();
        }

        if ("\\\\".equals(ESCAPE_CHARACTER)) {
            value = value.replace("\\_", ESCAPED_UNDERSCORE);
            value = value.replace("\\%", ESCAPED_PERCENT);
        }

        return value;
    }

}
