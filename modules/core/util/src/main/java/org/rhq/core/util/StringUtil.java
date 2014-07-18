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
package org.rhq.core.util;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class StringUtil {

    private static final Log log = LogFactory.getLog(StringUtil.class);

    public static final String EMPTY_STRING = "";

    /**
     * @param  source  The source string to perform replacements on.
     * @param  find    The substring to find in source.
     * @param  replace The string to replace 'find' within source
     *
     * @return The source string, with all occurrences of 'find' replaced with 'replace'
     */
    public static String replace(String source, String find, String replace) {
        if ((source == null) || (find == null) || (replace == null)) {
            return source;
        }

        int sourceLen = source.length();
        int findLen = find.length();
        if ((sourceLen == 0) || (findLen == 0)) {
            return source;
        }

        StringBuilder buffer = new StringBuilder();

        int idx;
        int fromIndex;

        for (fromIndex = 0; (idx = source.indexOf(find, fromIndex)) != -1; fromIndex = idx + findLen) {
            buffer.append(source.substring(fromIndex, idx));
            buffer.append(replace);
        }

        if (fromIndex == 0) {
            return source;
        }

        buffer.append(source.substring(fromIndex));

        return buffer.toString();
    }

    /**
     * @param  source The source string to perform replacements on.
     * @param  find   The substring to find in source.
     *
     * @return The source string, with all occurrences of 'find' removed
     */
    public static String remove(String source, String find) {
        if ((source == null) || (find == null)) {
            return source;
        }

        String retVal = null;
        int sourceLen = source.length();
        int findLen = find.length();
        StringBuilder remove = new StringBuilder(source);

        try {
            if ((sourceLen > 0) && (findLen > 0)) {
                int fromIndex;
                int idx;

                for (fromIndex = 0, idx = 0; (fromIndex = source.indexOf(find, idx)) != -1; idx = fromIndex + findLen) {
                    remove.delete(fromIndex, findLen + fromIndex);
                }

                retVal = remove.toString();
            }
        } catch (Exception e) {
            log.error("This should not have happened.", e);
            retVal = null;
        }

        return retVal;
    }

    /**
     * Print out everything in an Iterator in a user-friendly string format.
     *
     * @param  i     An iterator to print out.
     * @param  delim The delimiter to use between elements.
     *
     * @return The Iterator's elements in a user-friendly string format.
     */
    public static String iteratorToString(Iterator<?> i, String delim) {
        return iteratorToString(i, delim, "");
    }

    /**
     * Print out everything in an Iterator in a user-friendly string format.
     *
     * @param  i         An iterator to print out.
     * @param  delim     The delimiter to use between elements.
     * @param  quoteChar The character to quote each element with.
     *
     * @return The Iterator's elements in a user-friendly string format.
     */
    public static String iteratorToString(Iterator<?> i, String delim, String quoteChar) {
        Object elt = null;
        StringBuilder rstr = new StringBuilder();
        String s;

        while (i.hasNext()) {
            if (rstr.length() > 0) {
                rstr.append(delim);
            }

            elt = i.next();
            if (elt == null) {
                rstr.append("NULL");
            } else {
                s = elt.toString();
                if (quoteChar != null) {
                    rstr.append(quoteChar).append(s).append(quoteChar);
                } else {
                    rstr.append(s);
                }
            }
        }

        return rstr.toString();
    }

    /**
     * Print out a List in a user-friendly string format.
     *
     * @param  list  A List to print out.
     * @param  delim The delimiter to use between elements.
     *
     * @return The List in a user-friendly string format.
     */
    public static String listToString(List<?> list, String delim) {
        if (list == null) {
            return "NULL";
        }

        Iterator<?> i = list.iterator();
        return iteratorToString(i, delim, null);
    }

    public static String collectionToString(Collection<?> collection, String delim) {
        if (collection == null) {
            return "NULL";
        }

        Iterator<?> i = collection.iterator();
        return iteratorToString(i, delim, null);
    }

    /**
     * Print out a List in a user-friendly string format.
     *
     * @param  list A List to print out.
     *
     * @return The List in a user-friendly string format.
     */
    public static String listToString(List<?> list) {
        return listToString(list, ",");
    }

    public static String collectionToString(Collection<?> collection) {
        return collectionToString(collection, ",");
    }

    /**
     * Print out an array as a String
     */
    public static String arrayToString(Object[] array) {
        return arrayToString(array, ',');
    }

    /**
     * Print out an array as a String
     */
    public static String arrayToString(boolean[] array) {
        if (array == null) {
            return "null";
        }

        String rstr = "";
        char delim = ',';
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                rstr += delim;
            }

            rstr += array[i];
        }

        return rstr;
    }

    /**
     * Print out an array as a String
     *
     * @param array The array to print out
     * @param delim The delimiter to use between elements.
     */
    public static String arrayToString(Object[] array, char delim) {
        if (array == null) {
            return "null";
        }

        StringBuilder rstr = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                rstr.append(delim);
            }

            rstr.append(array[i]);
        }

        return rstr.toString();
    }

    /**
     * Print out an array as a String
     */
    public static String arrayToString(int[] array) {
        if (array == null) {
            return "null";
        }

        StringBuilder rstr = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                rstr.append(",");
            }

            rstr.append(array[i]);
        }

        return rstr.toString();
    }

    /**
     * Create a string formulated by inserting a delimiter in between consecutive array elements.
     *
     * @param  objs  List of objects to implode (elements may not be null)
     * @param  delim String to place inbetween elements
     *
     * @return A string with objects in the list seperated by delim
     */
    public static String implode(List<?> objs, String delim) {
        StringBuilder buf = new StringBuilder();
        int size = objs.size();

        for (int i = 0; i < (size - 1); i++) {
            buf.append(objs.get(i).toString());
            buf.append(delim);
        }

        if (size != 0) {
            buf.append(objs.get(size - 1).toString());
        }

        return buf.toString();
    }

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

        StringTokenizer tok = new StringTokenizer(s, delim);

        while (tok.hasMoreTokens()) {
            res.add(tok.nextToken());
        }

        return res;
    }

    /**
     * Split a string on delimiter boundaries, and place each element into an Array.
     *
     * @param  toExplode String to split up
     * @param  delim     Delimiting token, ala StringTokenizer
     *
     * @return an Array comprised of elements split by the tokenizing
     */
    public static String[] explodeToArray(String toExplode, String delim) {
        List<String> strings = explode(toExplode, delim);
        String[] ret;
        ret = strings.toArray(new String[strings.size()]);
        return ret;
    }

    /**
     * Split a string up by whitespace, taking into account quoted subcomponents. If there is an uneven number of
     * quotes, a parse error will be thrown.
     *
     * @param  arg String to parse
     *
     * @return an array of elements, the argument was split into
     *
     * @throws IllegalArgumentException indicating there was a quoting error
     */

    public static String[] explodeQuoted(String arg) throws IllegalArgumentException {
        List<String> res = new ArrayList<String>();
        StringTokenizer quoteTok;
        boolean inQuote = false;

        arg = arg.trim();
        quoteTok = new StringTokenizer(arg, "\"", true);

        while (quoteTok.hasMoreTokens()) {
            String elem = (String) quoteTok.nextElement();

            if (elem.equals("\"")) {
                inQuote = !inQuote;
                continue;
            }

            if (inQuote) {
                res.add(elem);
            } else {
                StringTokenizer spaceTok = new StringTokenizer(elem.trim());

                while (spaceTok.hasMoreTokens()) {
                    res.add(spaceTok.nextToken());
                }
            }
        }

        if (inQuote) {
            throw new IllegalArgumentException("Unbalanced quotation marks");
        }

        return res.toArray(new String[res.size()]);
    }

    /**
     * Remove a prefix from a string. If value starts with prefix, it will be removed, the resultant string is trimmed
     * and returned.
     *
     * @return If value starts with prefix, then this method returns value with the prefix removed, and the resultant
     *         string trimmed. If value does not start with prefix, value is returned as-is.
     */
    public static String removePrefix(String value, String prefix) {
        if (!value.startsWith(prefix)) {
            return value;
        }

        return value.substring(prefix.length()).trim();
    }

    /**
     * @return the plural of word. This is done by applying a few rules. These cover most (but not all) cases: 1. If the
     *         word ends in s, ss, x, o, or ch, append es 2. If the word ends in a consonant followed by y, drop the y
     *         and add ies 3. Append an s and call it a day. The ultimate references is at
     *         http://en.wikipedia.org/wiki/English_plural
     */
    public static String pluralize(String word) {
        if (word.endsWith("s") || word.endsWith("x") || word.endsWith("o") || word.endsWith("ch")) {
            return word + "es";
        }

        if (word.endsWith("y")) {
            // Odd case to avoid StringIndexOutOfBounds later
            if (word.length() == 1) {
                return word;
            }

            // Check next-to-last letter
            char next2last = word.charAt(word.length() - 2);
            if ((next2last != 'a') && (next2last != 'e') && (next2last != 'i') && (next2last != 'o')
                && (next2last != 'u') && (next2last != 'y')) {
                return word.substring(0, word.length() - 1) + "ies";
            }
        }

        return word + "s";
    }

    /**
     * @return The stack trace for the given Throwable as a String.
     */
    public static String getStackTrace(Throwable t) {
        if (t == null) {
            return "THROWABLE-WAS-NULL (at " + getStackTrace(new Exception()) + ")";
        }

        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);

            t.printStackTrace(pw);

            Throwable cause = t.getCause();
            if (cause != null) {
                return sw.toString() + getStackTrace(cause);
            }

            return sw.toString();
        } catch (Exception e) {
            return "\n\nStringUtil.getStackTrace " + "GENERATED EXCEPTION: '" + e.toString() + "' \n\n";
        }
    }

    /**
     * @return The stack trace for the given Throwable as a String.
     */
    public static String getFirstStackTrace(Throwable t) {
        if (t == null) {
            return null;
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);

        return sw.toString();
    }

    /**
     * @param  s A string that might contain unix-style path separators.
     *
     * @return The correct path for this platform (i.e, if win32, replace / with \).
     */
    public static String normalizePath(String s) {
        return StringUtil.replace(s, "/", File.separator);
    }

    public static String formatDuration(long duration) {
        return formatDuration(duration, 0, false);
    }

    public static String formatDuration(long duration, int scale, boolean minDigits) {
        long hours;
        long mins;
        int digits;
        double millis;

        hours = duration / 3600000;
        duration -= hours * 3600000;

        mins = duration / 60000;
        duration -= mins * 60000;

        millis = (double) duration / 1000;

        StringBuilder buf = new StringBuilder();

        if ((hours > 0) || (minDigits == false)) {
            buf.append(((hours < 10) && (minDigits == false)) ? ("0" + hours) : String.valueOf(hours)).append(':');
            minDigits = false;
        }

        if ((mins > 0) || (minDigits == false)) {
            buf.append(((mins < 10) && (minDigits == false)) ? ("0" + mins) : String.valueOf(mins)).append(':');
            minDigits = false;
        }

        // Format seconds and milliseconds
        NumberFormat fmt = NumberFormat.getInstance();
        digits = (((minDigits == false) || ((scale == 0) && (millis >= 9.5))) ? 2 : 1);
        fmt.setMinimumIntegerDigits(digits);
        fmt.setMaximumIntegerDigits(2); // Max of 2
        fmt.setMinimumFractionDigits(0); // Don't need any
        fmt.setMaximumFractionDigits(scale);

        buf.append(fmt.format(millis));

        return buf.toString();
    }

    public static String repeatChars(char c, int nTimes) {
        char[] arr = new char[nTimes];

        for (int i = 0; i < nTimes; i++) {
            arr[i] = c;
        }

        return new String(arr);
    }

    /**
     * Capitalizes the first letter of str.
     *
     * @param  str The string to capitalize.
     *
     * @return A new string that is <code>str</code> capitalized. Returns <code>null</code> if str is null.
     */
    public static String capitalize(String str) {
        if (str == null) {
            return null;
        } else if (str.trim().equals("")) {
            return str;
        }

        String result = str.substring(0, 1).toUpperCase() + str.substring(1, str.length());

        return result;
    }

    /**
     * Ensure string does not exceed maxSize. First trims and then truncates as needed.
     * @param s
     * @param maxSize
     * @return s if null or not altered, otherwise a copy that has been trimmed and/or truncated.
     */
    public static String trim(String s, int maxLength) {
        if (null == s) {
            return null;
        }

        s = s.trim();
        return (s.length() > maxLength) ? s.substring(0, maxLength) : s;
    }

    public static String truncate(String s, int truncLength, boolean removeWhiteSpace) {
        String temp = ((s.length() > truncLength) ? (s.substring(0, truncLength) + "...") : s);
        if (removeWhiteSpace) {
            temp = temp.replaceAll("\\s+", " ");
        }

        return temp;
    }

    public static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

    public static boolean isNotEmpty(String s) {
        return !isEmpty(s);
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().length() == 0;
    }

    public static boolean isNotBlank(String s) {
        return !isBlank(s);
    }

}