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
package org.rhq.enterprise.server.util;

import java.util.List;

/**
 * @author John Mazzitelli
 */
public class ArrayUtils {

    // similar to Arrays.copyOfRange, but this allows execution on JDK5
    public static Integer[] copyOfRange(Integer[] arr, int from, int to) {
        if (to < from) {
            throw new IllegalArgumentException(to + "<" + from);
        }
        int newSize = Math.min(arr.length - from, to - from); // to prevent null items in returned array
        Integer[] copy = new Integer[newSize];
        System.arraycopy(arr, from, copy, 0, newSize);
        return copy;
    }

    public static int[] unwrapList(List<Integer> input) {
        Integer[] intermediate = input.toArray(new Integer[input.size()]);
        return unwrapArray(intermediate);
    }

    public static int[] unwrapArray(Integer[] input) {
        int[] output = new int[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = input[i];
        }
        return output;
    }
}
