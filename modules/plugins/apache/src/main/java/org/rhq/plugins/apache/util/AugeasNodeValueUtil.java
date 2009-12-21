/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.plugins.apache.util;

/**
 * 
 * 
 * @author Lukas Krejci
 */
public class AugeasNodeValueUtil {

    private AugeasNodeValueUtil() {
        
    }
    
    /**
     * If the string is enclosed in double or single quotes, the
     * value inside the quotes is returned with all the escaped characters
     * unescaped.
     * 
     * @param value
     * @return
     */
    public static String unescape(String value) {
        if (value == null) return value;
        if (value.startsWith("\"")) {
            return value.substring(1, value.length() - 1).replaceAll("\\\"", "\"");
        } else if (value.startsWith("'")) {
            return value.substring(1, value.length() - 1).replaceAll("\\'", "'");
        }
        return value;
    }
    
    /**
     * If the supplied value contains single or double quotes, the returned
     * string is enclosed in quotes and the original qoutes "inside" are escaped.
     * 
     * @param value
     * @return
     */
    public static String escape(String value) {
        if (value.indexOf('"') >= 0) {
            return "\"" + value.replaceAll("\"", "\\\"") + "\"";
        } else if (value.indexOf('\'') >= 0) {
            return "'" + value.replaceAll("'", "\\'") + "'";
        }
        
        return value;
    }
}
