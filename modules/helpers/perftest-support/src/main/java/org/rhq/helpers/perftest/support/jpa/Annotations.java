/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.helpers.perftest.support.jpa;

import java.util.HashMap;
import java.util.Map;

/**
 * A minimal extension of {@link HashMap} with {@link Class} keys and Object values.
 * Provides only a convenience {@link #get(Class)} method.
 * 
 * @author Lukas Krejci
 */
public class Annotations extends HashMap<Class<?>, Object> {

    private static final long serialVersionUID = 1L;

    public Annotations() {
        super();
    }

    public Annotations(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public Annotations(int initialCapacity) {
        super(initialCapacity);
    }

    public Annotations(Map<? extends Class<?>, ? extends Object> m) {
        super(m);
    }

    /**
     * Returns the annotation of given class (if present in this map) cast
     * to the class. This is a convenience method for the users of this class
     * so that they don't have to cast the annotation object themselves.
     * 
     * @param <T>
     * @param annotationClass
     * @return
     */
    public <T> T get(Class<T> annotationClass) {
        Object annotation = get((Object) annotationClass);
        return annotationClass.cast(annotation);
    }
}
