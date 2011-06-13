/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.plugins.apache.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class FakeInventory {
    
    HashMap<Class<?>, Set<Object>> database = new HashMap<Class<?>, Set<Object>>();
    
    private static final Comparator<Object> EQUALS_COMPARATOR = new Comparator<Object>() {
        @Override
        public int compare(Object a, Object b) {
            if (a == null ? b == null : a.equals(b)) {
                return 0;
            }
            return 1;
        }
    };
    
    public boolean insert(Object o) {
        Class<?> c = o.getClass();
        
        Set<Object> objects = database.get(c);
        
        if (objects == null) {
            objects = new HashSet<Object>();
        }
        
        return objects.add(o);
    }
    
    public void insertAll(Object... objects) {
        for(Object o : objects) {
            insert(o);
        }
    }
    
    public void insertAll(Collection<Object> objects) {
        for(Object o : objects) {
            insert(o);
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T> T get(T prototype) {
        return get(prototype, (Class<T>) prototype.getClass());
    }
    
    public <T> T get(T prototype, Class<T> clazz) {
        return get(prototype, clazz, EQUALS_COMPARATOR);
    }
    
    public <T> T get(T prototype, Class<T> clazz, Comparator<? super T> comparator) {
        Set<Object> objects = database.get(clazz);
        
        if (objects == null) {
            return null;
        }
        
        for (Object o : objects) {
            T cast = clazz.cast(o);
            if (comparator.compare(cast, prototype) == 0) {
                return cast;
            }
        }
        
        return null;
    }
}
