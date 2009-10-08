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
package org.rhq.core.domain.util.serial;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HibernateUtil {

    /**
     * If the pass-in List is a Hibernate type, replace it with a java type. This is typically used
     * for ensuring that we don't serialize Hibernate types.
     * @param <T>
     * @param list
     * @return
     */
    public static <T> List<T> safeList(List<T> list) {
        if (null == list) {
            return null;
        }

        return (list.getClass().getName().contains("hibernate") ? new ArrayList<T>(list) : list);
    }

    /**
     * If the pass-in List is a Hibernate type, replace it with a java type. This is typically used
     * for ensuring that we don't serialize Hibernate types.
     * @param <T>
     * @param set
     * @return
     */
    public static <T> Set<T> safeSet(Set<T> set) {
        if (null == set) {
            return null;
        }

        return (set.getClass().getName().contains("hibernate") ? new LinkedHashSet<T>(set) : set);
    }

    /**
     * If the pass-in List is a Hibernate type, replace it with a java type. This is typically used
     * for ensuring that we don't serialize Hibernate types.
     * @param <T>
     * @param collection
     * @return
     */
    public static <T> Collection<T> safeCollection(Collection<T> collection) {
        if (null == collection) {
            return null;
        }

        return (collection.getClass().getName().contains("hibernate") ? new LinkedHashSet<T>(collection) : collection);
    }

    /**
     * If the pass-in Map is a Hibernate type, replace it with a java type. This is typically used
     * for ensuring that we don't serialize Hibernate types.
     * @param <K>
     * @param <V>
     * @param map
     * @return
     */
    public static <K, V> Map<K, V> safeMap(Map<K, V> map) {
        if (null == map) {
            return null;
        }

        return (map.getClass().getName().contains("hibernate") ? new LinkedHashMap<K, V>(map) : map);
    }

}
