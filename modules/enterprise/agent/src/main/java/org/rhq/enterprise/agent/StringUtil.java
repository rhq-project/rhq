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
package org.rhq.enterprise.agent;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utilities that work with Strings.
 *
 * @author John Mazzitelli
 */
public class StringUtil {
    /**
     * Prevents instantiation.
     */
    private StringUtil() {
    }

    /**
     * Returns a string with the given key/value pairs formatted such that the key values are justified and ordered with
     * each pair separated with a newline. This is useful when displaying menu items, for example, and want to show them
     * in an orderly, formatted way. For example, if the given key value pairs are [first, This is the first], [second,
     * This is second] and [last one, This is last], this will return a string that looks like this:
     *
     * <pre>
     * first   : This is the first
     * second  : This is the second
     * last one: This is last
     *  </pre>
     *
     * @param  key_value_pairs
     *
     * @return formatted string
     */
    public static String justifyKeyValueStrings(Map<?, ?> key_value_pairs) {
        TreeMap<String, String> sorted_map = new TreeMap<String, String>();
        int longest_key = 0;

        // add the key/value pair objects to the sorted map and determine the longest key string
        for (Map.Entry<?, ?> nvp_entry : key_value_pairs.entrySet()) {
            String key = "" + nvp_entry.getKey();
            String value = "" + nvp_entry.getValue();

            if (key.length() > longest_key) {
                longest_key = key.length();
            }

            sorted_map.put(key, value);
        }

        // now format each key value pair and do so such that the keys are in ascending order
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos);
        String format = "%" + longest_key + "s: %s%n";

        for (Map.Entry<String, String> sorted_entry : sorted_map.entrySet()) {
            writer.printf(format, sorted_entry.getKey(), sorted_entry.getValue());
        }

        writer.flush();

        return baos.toString();
    }
}