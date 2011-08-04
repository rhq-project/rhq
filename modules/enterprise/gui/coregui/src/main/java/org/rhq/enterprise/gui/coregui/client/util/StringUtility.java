/*
 * RHQ Management Platform
 * Copyright 2010-2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.enterprise.gui.coregui.client.util;

import java.util.ArrayList;
import java.util.List;

/**
 * A collection of utility methods for working with Strings.
 *
 * @author Ian Springer
 */
public class StringUtility {

    /**
     * Split a string on delimiter boundaries, and place each element into a List.
     *
     * @param  s     String to split up
     * @param  delim Delimiting token, ala StringTokenizer
     *
     * @return a List comprised of elements split by the tokenizing
     */
    public static List<String> explode(String s, String delim) {
        List<String> res = new ArrayList<String>();
        if (s == null)
            return res;

        String[] tokens = s.split(delim);
        for (String token : tokens) {
            res.add(token);
        }

        return res;
    }

    // TODO: I18N. The logic here may need to be pluggable for different localizations.
    public static String pluralize(String singularNoun) {
        String pluralNoun;
        if (singularNoun.endsWith("y") && !singularNoun.endsWith("ay") && !singularNoun.endsWith("ey")
            && !singularNoun.endsWith("oy")) {
            pluralNoun = singularNoun.substring(0, singularNoun.length() - 1) + "ies";
        } else if (!singularNoun.endsWith("s")) {
            pluralNoun = singularNoun + "s";
        } else {
            pluralNoun = singularNoun;
        }
        return pluralNoun;
    }

    /**
     * Escapes HTML in a string to eliminate cross site scripting (XSS) vulnerabilities. Note, this impl is designed
     * to be highly efficient to minimize the impact on performance.
     *
     * @param string the string to be escaped
     *
     * @return the escaped string
     */
    public static String escapeHtml(String string) {
        if (string == null) {
            return null;
        }
        StringBuilder buffer = null;
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c == '&') {
                if (buffer == null) {
                    buffer = new StringBuilder(string.substring(0, i));
                }
                buffer.append("&amp;");
            } else if (c == '<') {
                if (buffer == null) {
                    buffer = new StringBuilder(string.substring(0, i));
                }
                buffer.append("&lt;");
            } else if (c == '>') {
                if (buffer == null) {
                    buffer = new StringBuilder(string.substring(0, i));
                }
                buffer.append("&gt;");
            } else {
                if (buffer != null) {
                    buffer.append(c);
                }
            }
        }
        return (buffer != null) ? buffer.toString() : string;
    }

    /**
     * Sanitizes HTML (i.e. removes unsafe HTML such as SCRIPT tags) in a string to eliminate cross site scripting (XSS)
     * vulnerabilities.
     *
     * @param string the string to be sanitized
     *
     * @return the sanitized string
     */
    // TODO (ips, 03/31/11): Replace this lame impl with a much more robust one - easiest way would be to upgrade to GWT
    //                       2.1 or later and use the new Safe HTML APIs. See also
    //                       http://tomerdoron.blogspot.com/2011/03/less-simple-safe-html-sanitizer.html.
    public static String sanitizeHtml(String string) {
        if (string == null) {
            return null;
        }

        return string.replaceAll("<script", "&lt;script").replaceAll("<SCRIPT", "&lt;SCRIPT");
    }

    private StringUtility() {
    }

}
