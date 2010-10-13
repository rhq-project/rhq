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

/**
 * A dumping ground for utility methods used by Criteria objects.  We specifically don't want to put these into the
 * Criteria abstract class because they would become part of the auto-completion candidate list in the interactive CLI.
 * Putting them here (in core/domain versus, say, core/util) will provide the necessary utility without requiring 
 * further module inheritance at the coregui layer. 
 * 
 * @author Joseph Marques
 */
public class CriteriaUtils {
    /*
     * remove nulls from the passed items, and return the resultant list.  return null if no non-null items exist.
     * this is useful in criteria addFilterXXX methods which take varargs, where a single null argument should be
     * interpreted as a null collection (instead of one-item collection whose first value is null). 
     */
    public static <T> List<T> getListIgnoringNulls(T[] items) {
        if (items == null) {
            return null;
        }

        List<T> results = new ArrayList<T>();
        for (T next : items) {
            if (next != null) {
                results.add(next);
            }
        }
        if (results.size() == 0) {
            return null;
        }
        return results;
    }
}
