/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.gui.legacy.util;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

/**
 * Utilities class that provides convenience methods for taglib classes.
 */
public class TaglibUtils {
    /**
     * Set the value of the variable in the given scope with the given name as the given object. If the scope is
     * specified as <code>null</code>, the variable will be set in the page scope.
     *
     * @param pageContext the page context
     * @param scope       the scope of the variable ("application", "session", "request", or "page")
     * @param var         the name of the variable
     * @param value       the value of the variable
     */
    public static void setScopedVariable(PageContext pageContext, String scope, String var, Object value) {
        if (scope == null) {
            scope = " page";
        }

        if (scope.equalsIgnoreCase("application")) {
            pageContext.getServletContext().setAttribute(var, value);
        } else if (scope.equalsIgnoreCase("session")) {
            HttpSession session = pageContext.getSession();
            if (session != null) {
                session.setAttribute(var, value);
            }
        } else if (scope.equalsIgnoreCase("request")) {
            pageContext.getRequest().setAttribute(var, value);
        } else {
            pageContext.setAttribute(var, value);
        }
    }

    /**
     * @param  preChars  The maximum number of chars to appear before the "..."
     * @param  postChars The maximum number of chars to appear after the "..."
     *
     * @return The path, shortened by putting "..." in the middle
     */
    public static String shortenPath(String path, int preChars, int postChars, boolean strict) {
        if (!strict) {
            return shortenPath(path, preChars, postChars);
        }

        // If path is shorter than preChars + postChars + 3 ellipses, then
        // just return the original
        if (path.length() <= (preChars + postChars + 3)) {
            return path;
        }

        StringBuffer ret = new StringBuffer(path.substring(0, preChars)).append("...").append(
            path.substring(path.length() - postChars));

        return ret.toString();
    }

    public static String shortenPath(String path, int preChars, int postChars) {
        // Look for the first kind of slash to determine which kind this path uses.
        int slash1 = path.indexOf("/");
        int slash2 = path.indexOf("\\");
        char slash;

        // There must be some cleaner, simpler logic here.
        if (slash1 == -1) {
            if (slash2 == -1) {
                return path;
            }

            slash = '\\';
        } else {
            if (slash2 == -1) {
                slash = '/';
            } else if (slash1 < slash2) {
                slash = '/';
            } else {
                slash = '\\';
            }
        }

        // Easy cases
        try {
            if (path.length() <= (preChars + postChars)) {
                return path;
            }

            slash1 = path.substring(0, preChars).lastIndexOf(slash);
            if (slash1 == -1) {
                slash1 = path.indexOf(slash);
            }

            if (slash1 == -1) {
                return path;
            }

            String prefix = path.substring(0, slash1 + 1);

            if (postChars > path.length()) {
                postChars = path.length();
            }

            slash2 = path.substring(path.length() - postChars).indexOf(slash);
            if (slash2 == -1) {
                postChars = path.length();
                slash2 = path.lastIndexOf(slash);
            }

            if ((slash2 == -1) || (slash2 == slash1)) {
                return path;
            }

            String suffix = path.substring(path.length() - postChars).substring(slash2);
            return prefix + "..." + suffix;
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("SBE on shortenPath(" + path + "," + preChars + "," + postChars + ")");
            throw e;
        }
    }
}