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
package org.rhq.core.util.collection;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author John Mazzitelli
 * @author Joseph Marques
 */
public class ArrayUtils {

    // similar to Arrays.copyOfRange, but this allows execution on JDK5
    @SuppressWarnings("unchecked")
    public static <T> T[] copyOfRange(T[] arr, int from, int to) {
        if (to < from) {
            throw new IllegalArgumentException(to + "<" + from);
        }
        int newSize = Math.min(arr.length - from, to - from); // to prevent null items in returned array
        T[] copy = (T[]) Array.newInstance(arr.getClass().getComponentType(), newSize);
        System.arraycopy(arr, from, copy, 0, newSize);
        return copy;
    }

    public static int[] unwrapCollection(Collection<Integer> input) {
        if (input == null) {
            return null;
        }
        Integer[] intermediate = input.toArray(new Integer[input.size()]);
        return unwrapArray(intermediate);
    }

    public static int[] unwrapArray(Integer[] input) {
        if (input == null) {
            return null;
        }
        int[] output = new int[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = input[i];
        }
        return output;
    }

    public static Integer[] wrapInArray(int[] input) {
        if (input == null) {
            return null;
        }
        Integer[] output = new Integer[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = input[i];
        }
        return output;
    }

    public static List<Integer> wrapInList(int[] input) {
        if (input == null) {
            return null;
        }
        Integer[] intermediate = wrapInArray(input);
        return Arrays.asList(intermediate);
    }

}
