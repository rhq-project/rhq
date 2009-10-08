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
package org.rhq.core.clientapi.util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class ArrayUtil {
    public static void dumpArray(PrintStream out, Object[] array) {
        out.print("{ ");
        for (int i = 0; i < array.length; i++) {
            out.print(array[i].toString());
            if (i != (array.length - 1)) {
                out.print(", ");
            }
        }

        out.print(" }");
    }

    public static Object[] merge(Object[][] arrays, Object[] arrType) {
        List res;
        int i;
        int size;

        size = 0;
        for (i = 0; i < arrays.length; i++) {
            size += arrays[i].length;
        }

        res = new ArrayList(size);
        for (i = 0; i < arrays.length; i++) {
            for (int j = 0; j < arrays[i].length; j++) {
                res.add(arrays[i][j]);
            }
        }

        return res.toArray(arrType);
    }

    public static Object[] merge(Object[] one, Object[] two, Object[] arrType) {
        return ArrayUtil.merge(new Object[][] { one, two }, arrType);
    }

    /**
     * Find the maximum value in an array of double values.
     *
     * @param  values Values to search for to find the max of
     *
     * @return The index of the maximum value, or -1 if 'values' was 0 length
     */
    public static int max(double[] values) {
        int maxIdx = -1;

        for (int i = 0; i < values.length; i++) {
            if ((maxIdx == -1) || (values[i] > values[maxIdx])) {
                maxIdx = i;
            }
        }

        return maxIdx;
    }

    /**
     * Find the maximum value in an array of int values.
     *
     * @param  values Values to search for to find the max of
     *
     * @return The index of the maximum value, or -1 if 'values' was 0 length
     */
    public static int max(int[] values) {
        int maxIdx = -1;

        for (int i = 0; i < values.length; i++) {
            if ((maxIdx == -1) || (values[i] > values[maxIdx])) {
                maxIdx = i;
            }
        }

        return maxIdx;
    }

    /**
     * Find the minimum value in an array of double values.
     *
     * @param  values Values to search for to find the min of
     *
     * @return The index of the minimum value, or -1 if 'values' was 0 length
     */
    public static int min(double[] values) {
        int minIdx = -1;

        for (int i = 0; i < values.length; i++) {
            if ((minIdx == -1) || (values[i] < values[minIdx])) {
                minIdx = i;
            }
        }

        return minIdx;
    }

    /**
     * Get the average of an array of values.
     */
    public static double average(double[] values) {
        double sum = 0;

        if (values.length == 0) {
            throw new IllegalArgumentException("Array length must be > 0");
        }

        for (int i = 0; i < values.length; i++) {
            sum += values[i];
        }

        return sum / values.length;
    }

    /**
     * Return an array of values where every value is unique.
     *
     * @param  values Values to get the unique of. Note that this array _must_ be sorted, prior to invoking this method
     *
     * @return a new array with values represented in the 'values' argument only a single time each
     */
    public static double[] uniq(double[] values) {
        ArrayList arr = new ArrayList();
        double[] res;
        double lastVal;
        int j;

        lastVal = 0;
        for (int i = 0; i < values.length; i++) {
            if ((i == 0) || (lastVal != values[i])) {
                arr.add(new Double(values[i]));
                lastVal = values[i];
            }
        }

        res = new double[arr.size()];
        j = 0;
        for (Iterator i = arr.iterator(); i.hasNext();) {
            Double d = (Double) i.next();

            res[j++] = d.doubleValue();
        }

        return res;
    }

    /**
     * Check to see if a _sorted_ array of values contains all unique values.
     */
    public static boolean isUniq(String[] values) {
        for (int i = 0; i < (values.length - 1); i++) {
            if (values[i].equals(values[i + 1])) {
                return false;
            }
        }

        return true;
    }

    /**
     * Array equality
     *
     * @return true if both arrays contain elements in the same positions that are also equal. Returns false if either
     *         (or both) arrays are null;
     */
    public static boolean equals(Object[] a1, Object[] a2) {
        if ((a1 == null) || (a2 == null)) {
            return false;
        }

        if (a1.length != a2.length) {
            return false;
        }

        for (int i = 0; i < a1.length; i++) {
            if (a1[i] == null) {
                if (a2[i] != null) {
                    return false;
                } else {
                    continue;
                }
            } else if (a2[i] == null) {
                return false;
            }

            if (!a1[i].equals(a2[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * Find the 2 values with the smallest difference between them. In the case of a tie, the earlier value in the array
     * will be returned. Ex: findMinDiff({1, 2, 3, 4, 5}) -> 0 findMinDiff({1, 2, 2.2, 3, 4, 5}) -> 1
     *
     * @param  values An array of _sorted_, _unique_ values
     *
     * @return The index of the first number in the pair with the smallest difference.
     */
    public static int findMinDiff(double[] values) {
        double minDiff;
        int minIdx;

        if (values.length < 2) {
            throw new IllegalArgumentException("Array length must be >= 2");
        }

        minIdx = 0;
        minDiff = values[1] - values[0];
        for (int i = 1; i < (values.length - 1); i++) {
            double diff = values[i + 1] - values[i];

            if (diff < minDiff) {
                minDiff = diff;
                minIdx = i;
            }
        }

        return minIdx;
    }

    /**
     * Return a boolean whether or not the element exists in the array
     *
     * @param  array   An array of objects
     * @param  element The element to look for in the array
     *
     * @return true if element is in the array
     */
    public static boolean exists(Object[] array, Object element) {
        // NULL objects are non-existent
        if (element == null) {
            return false;
        }

        HashSet set = new HashSet(Arrays.asList(array));
        return set.contains(element);
    }

    /**
     * Find the index of the first appearance of an object in the array
     */
    public static int find(Object[] array, Object element) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(element)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Convert a string array to an array of Integer. If <code>array</code> is null, null will be returned.
     *
     * @param  array string array
     *
     * @return the Integer array
     *
     * @throws NumberFormatException if an element in the String array is not parseable into an Integer
     */
    public static Integer[] stringToInteger(String[] array) {
        if (null != array) {
            Integer[] intArray = new Integer[array.length];
            for (int i = 0; i < array.length; ++i) {
                intArray[i] = new Integer(array[i]);
            }

            return intArray;
        } else {
            return null;
        }
    }

    /**
     * Convert a string array to an array of int. If <code>array</code> is null, null will be returned.
     *
     * @param  array string array
     *
     * @return the int array
     *
     * @throws NumberFormatException if an element in the String array is not parseable into an int
     */
    public static int[] stringToInt(String[] array) {
        if (null != array) {
            int[] intArray = new int[array.length];
            for (int i = 0; i < array.length; ++i) {
                intArray[i] = Integer.parseInt(array[i]);
            }

            return intArray;
        } else {
            return null;
        }
    }

    /**
     * Combine two arrays into a single, larger array. This method only returns null if both a1 and a2 are null. If a1
     * is not null, the array that is returned has the same class as a1. Otherwise it will be the the same class as a2.
     *
     * @param  a1 The first array. If this is null, a copy of the second array is returned (unless it's null too, then
     *            null is returned).
     * @param  a2 The second array. If this is null, a copy of the first array is returned (unless it's null too, then
     *            null is returned).
     *
     * @return An array that is effectively a2 appended to a1, or null if both a1 and a2 are null. The class of the
     *         array is the same as a1, unless a1 is null in which case the class of the array is the same as a2, unless
     *         a2 is also null, in which case this method returns null;
     */
    public static Object[] combine(Object[] a1, Object[] a2) {
        Object[] r;
        Class c;
        if (a1 == null) {
            if (a2 == null) {
                return null;
            }

            c = a2.getClass().getComponentType();
            r = (Object[]) java.lang.reflect.Array.newInstance(c, a2.length);
            System.arraycopy(a2, 0, r, 0, a2.length);
        } else if (a2 == null) {
            c = a1.getClass().getComponentType();
            r = (Object[]) java.lang.reflect.Array.newInstance(c, a1.length);
            System.arraycopy(a1, 0, r, 0, a1.length);
        } else {
            c = a1.getClass().getComponentType();
            r = (Object[]) java.lang.reflect.Array.newInstance(c, a1.length + a2.length);
            System.arraycopy(a1, 0, r, 0, a1.length);
            System.arraycopy(a2, 0, r, a1.length, a2.length);
        }

        return r;
    }
}