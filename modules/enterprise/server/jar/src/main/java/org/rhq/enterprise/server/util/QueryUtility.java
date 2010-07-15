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
    private static String ESCAPE_CLAUSE_CHARACTER = null;
    private static String ESCAPED_ESCAPE = null;
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

        return doEscapeSearchParameter(value);
    }

    private static String doEscapeSearchParameter(String value) {
        init();

        // Escape LIKE's wildcard characters with escaped characters so that the user's input
        // will be matched literally
        value = value.replace(ESCAPE_CHARACTER, ESCAPED_ESCAPE);
        value = value.replace("_", ESCAPED_UNDERSCORE);
        value = value.replace("%", ESCAPED_PERCENT);
        value = value.replace("'", "''");

        return value;
    }

    /**
     * Given the settings for the current DatabaseType, properly handle escaping special SQL characters as
     * well as UPCASING the value (standard for rhq filter searches) and wrapping with SQL wildcard for
     * implicit "contains" (i.e. '%' characters)  
     * 
     * @param value
     * @return the properly escaped and formatted value.
     */
    public static String formatSearchParameter(String value) {
        if (value == null || value.trim().equals("")) {
            return null;
        }

        return "%" + doEscapeSearchParameter(value).toUpperCase() + "%";
    }

    /**
     * Get the proper LIKE operator escape clause for the current DatabaseType.
     * 
     * @return The escape clause buffered with single spaces. For example: " ESCAPE '\' "
     */
    public static String getEscapeClause() {
        init();

        return " ESCAPE '" + ESCAPE_CLAUSE_CHARACTER + "' ";
    }

    /**
     * Get the proper ESCAPE clause character for the current DatabaseType. This is for use when
     * constructing query strings to be parsed (it may itself escape the escape character for
     * proper parsing (like in Postgres when standard_conforming_strings is off).
     * Call getEscapeCharacterParam() when needed for setting a NamedQuery parameter.
     * 
     * @return The escape character as a String.  The string may actually be multiple character but
     * when parsed by the vendor it will parse out the single character. 
     */
    public static String getEscapeClauseCharacter() {
        init();

        return ESCAPE_CLAUSE_CHARACTER;
    }

    /**
     * Get the proper ESCAPE clause character for the current DatabaseType. This is for use when
     * setting a NamedQuery paramater (unparsed, guaranteed to be a single char). If constructing
     * query strings to be parsed  Call getEscapeCharacter()
     * 
     * @return The single escape character as a String.
     */
    public static String getEscapeCharacter() {
        init();

        return ESCAPE_CHARACTER;
    }

    private static void init() {
        if (null == ESCAPE_CLAUSE_CHARACTER) {
            ESCAPE_CLAUSE_CHARACTER = DatabaseTypeFactory.getDefaultDatabaseType().getEscapeCharacter();

            // The escape character should be a single character. In postgres and possibly other
            // db types the character itself may need to be escaped for proper parsing of the ESCAPE value.
            // (for example, ESCAPE '\\' in postgres because backslash in a string literal is
            // escaped by default. In such a case assume the last character is the true escape character.
            int len = ESCAPE_CLAUSE_CHARACTER.length();
            ESCAPE_CHARACTER = (len > 1) ? ESCAPE_CLAUSE_CHARACTER.substring(len - 1) : ESCAPE_CLAUSE_CHARACTER;

            ESCAPED_ESCAPE = ESCAPE_CHARACTER + ESCAPE_CHARACTER;
            ESCAPED_UNDERSCORE = ESCAPE_CHARACTER + "_";
            ESCAPED_PERCENT = ESCAPE_CHARACTER + "%";
        }
    }
}
