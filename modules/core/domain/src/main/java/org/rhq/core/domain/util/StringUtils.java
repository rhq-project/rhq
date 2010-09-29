/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.core.domain.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StringUtils {
    private static final Set<String> LOWERCASE_WORDS = new HashSet<String>();
    static {
        LOWERCASE_WORDS.add("And");
        LOWERCASE_WORDS.add("Or");
    }

    /*
     * Take something that is camel-cased, add spaces between the words, and capitalize each word.
     */
    public static String deCamelCase(String target) {
        if (target == null) {
            return null;
        }

        if (target.length() == 0) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        if (target.startsWith("-")) {
            target = target.substring(1);
        }

        if (target.endsWith("-")) {
            target = target.substring(0, target.length() - 1);
        }

        int nextDash;
        while ((nextDash = target.indexOf('-')) > 0) {
            target = target.substring(0, nextDash) + Character.toUpperCase(target.charAt(nextDash + 1))
                + target.substring(nextDash + 2);
        }

        result.append(Character.toUpperCase(target.charAt(0)));

        StringBuilder currentWord = new StringBuilder();
        char currentChar;
        char previousChar = target.charAt(0);
        for (int i = 1; i < target.length(); i++) {
            currentChar = target.charAt(i);

            // Obey multi-digit numbers and acronyms
            if ((Character.isDigit(currentChar) && !Character.isDigit(previousChar))
                || (Character.isUpperCase(currentChar) && (!Character.isUpperCase(previousChar)
                || ((i < (target.length() - 1)) && Character.isLowerCase(target.charAt(i + 1)))))) {
                // We're at the start of a new word.
                appendWord(result, currentWord);
                currentWord = new StringBuilder();
                // Append a space before the next word.
                result.append(' ');
            }

            currentWord.append(currentChar);
            previousChar = currentChar;
        }
        // Append the final word.
        appendWord(result, currentWord);

        return result.toString();
    }

    private static void appendWord(StringBuilder result, StringBuilder nextWord) {
        String word = nextWord.toString();
        if (LOWERCASE_WORDS.contains(word)) {
            result.append(word.toLowerCase());
        } else {
            result.append(word);
        }
    }

    public static List<String> getStringAsList(String input, String regexSplitter, boolean ignoreEmptyTokens) {
        List<String> results = new ArrayList<String>();

        if (input == null) {
            // gracefully return a 0-element list if the input is null
            return results;
        }

        for (String lineItem : input.split(regexSplitter)) {
            // allow user to visual separate data, but ignore blank lines
            if (ignoreEmptyTokens && lineItem.trim().equals("")) {
                continue;
            }

            results.add(lineItem);
        }

        return results;
    }

    public static String getListAsString(List<String> stringList, String seperatorFragment) {
        StringBuilder builder = new StringBuilder();
        for (String element : stringList) {
            if (builder.length() != 0) {
                builder.append(seperatorFragment);
            }

            builder.append(element);
        }

        return builder.toString();
    }
}