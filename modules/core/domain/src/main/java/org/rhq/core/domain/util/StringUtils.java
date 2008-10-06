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
package org.rhq.core.domain.util;

import java.util.ArrayList;
import java.util.List;

public class StringUtils {
    /*
     * Take somethin that is camel-cased, add spaces between the words, and capitalize each word
     */
    public static String deCamelCase(String target) {
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

        int nextDash = 0;
        while ((nextDash = target.indexOf('-')) > 0) {
            target = target.substring(0, nextDash) + Character.toUpperCase(target.charAt(nextDash + 1))
                + target.substring(nextDash + 2);
        }

        result.append(Character.toUpperCase(target.charAt(0)));

        char next;
        char last = target.charAt(0);
        for (int i = 1; i < target.length(); i++) {
            next = target.charAt(i);

            // Obey multi-digit numbers and acronyms
            if ((Character.isDigit(next) && !Character.isDigit(last))
                || (Character.isUpperCase(next) && (!Character.isUpperCase(last) || ((i < (target.length() - 1)) && Character
                    .isLowerCase(target.charAt(i + 1)))))) {
                // at the start of another word, add a space
                result.append(' ');
            }

            result.append(next);
            last = next;
        }

        return result.toString();
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

    @SuppressWarnings("unchecked")
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