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

        boolean handleEscapedBackslash = "\\\\".equals(getEscapeCharacter());

        if (handleEscapedBackslash) {
            value = ((String) value).replaceAll("\\_", "\\\\_");
        }

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

}
