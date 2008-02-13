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
package org.rhq.core.gui.util;

import java.util.List;

public class StringUtility {
    private static char CHAR_SPACE = ' ';

    public static <T extends Object> String getListAsDelimitedString(List<T> items) {
        return getListAsDelimitedString(items, ',');
    }

    public static <T extends Object> String getListAsDelimitedString(List<T> items, char delimiter) {
        if (items.size() < 1) {
            return "";
        }

        StringBuilder results = new StringBuilder();

        results.append(items.get(0).toString());
        for (int i = 1; i < items.size(); i++) {
            results.append(delimiter).append(CHAR_SPACE).append(items.get(i).toString());
        }

        return results.toString();
    }

    public static Integer[] getIntegerArray(List<String> list) {
        Integer[] results = new Integer[list.size()];
        int i = 0;
        for (String item : list) {
            results[i++] = Integer.valueOf(item);
        }

        return results;
    }

    public static Integer[] getIntegerArray(String[] list) {
        Integer[] results = new Integer[list.length];
        int i = 0;
        for (String item : list) {
            results[i++] = Integer.valueOf(item);
        }

        return results;
    }
}