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
package org.rhq.core.domain.util.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author John Mazzitelli
 * @author Joseph Marques
 */
public class ArrayUtils {

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

        // do not use Arrays.asList because returned list needs to be modifiable
        List<Integer> results = new ArrayList<Integer>();
        for (Integer next : intermediate) {
            results.add(next);
        }
        return results;
    }

}
