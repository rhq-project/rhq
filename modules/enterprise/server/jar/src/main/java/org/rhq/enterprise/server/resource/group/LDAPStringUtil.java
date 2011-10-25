/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.resource.group;

import java.io.UnsupportedEncodingException;

/**
 * A utility class containing static methods to perform common string 
 * manipulations or modifications for use with an LDAP implementation. 
 * 
 * @author loleary
 * @since 4.0.1
 *
 */
public class LDAPStringUtil {

    /**
     * <p>Encode a string so that it can be used in an LDAP search filter.</p> 
     *
     * <p>The following table shows the characters that are encoded and their 
     * encoded version.</p>
     * 
     * <table>
     * <tr><th align="center">Character</th><th>Encoded As</th></tr>
     * <tr><td align="center">*</td><td>\2a</td></tr>
     * <tr><td align="center">(</td><td>\28</td></tr>
     * <tr><td align="center">)</td><td>\29</td></tr>
     * <tr><td align="center">\</td><td>\5c</td></tr>
     * <tr><td align="center"><code>null</code></td><td>\00</td></tr>
     * </table>
     * 
     * <p>In addition to encoding the above characters, any non-ASCII character 
     * (any character with a hex value greater then <code>0x7f</code>) is also 
     * encoded and rewritten as a UTF-8 character or sequence of characters in 
     * hex notation.</p>
     *  
     * @param  filterString a string that is to be encoded
     * @return the encoded version of <code>filterString</code> suitable for use
     *         in a LDAP search filter
     * @see <a href="http://tools.ietf.org/html/rfc4515">RFC 4515</a>
     */
    public static String encodeForFilter(final String filterString) {
        if (filterString != null && filterString.length() > 0) {
            StringBuilder encString = new StringBuilder(filterString.length());
            for (int i = 0; i < filterString.length(); i++) {
                char ch = filterString.charAt(i);
                switch (ch) {
                case '*': // encode a wildcard * character
                    encString.append("\\2a");
                    break;
                case '(': // encode a open parenthesis ( character
                    encString.append("\\28");
                    break;
                case ')': // encode a close parenthesis ) character
                    encString.append("\\29");
                    break;
                case '\\': // encode a backslash \ character
                    encString.append("\\5c");
                    break;
                case '\u0000': // encode a null character
                    encString.append("\\00");
                    break;
                default:
                    if (ch <= 0x7f) { // an ASCII character
                        encString.append(ch);
                    } else if (ch >= 0x80) { // encode to UTF-8
                        try {
                            byte[] utf8bytes = String.valueOf(ch).getBytes("UTF8");
                            for (byte b : utf8bytes) {
                                encString.append(String.format("\\%02x", b));
                            }
                        } catch (UnsupportedEncodingException e) {
                            // ignore
                        }
                    }
                }
            }
            return encString.toString();
        }
        return filterString;
    }

}
