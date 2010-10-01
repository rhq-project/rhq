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
package org.rhq.enterprise.gui.coregui.server.gwt;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Set;

import org.rhq.core.domain.resource.Resource;

/**
 * @author Greg Hinkle
 */
public class ObjectFilter {

    public static <T extends Collection<?>> T filterFieldsInCollection(T collection, Set<String> goodFields) {
        long sizeOfBefore = sizeOf(collection);
        for (Object object : collection) {
            filterFields(object, goodFields);
        }
        System.out.println("Object filtered from size [" + sizeOfBefore + "] to [" + sizeOf(collection) + "]");

        return collection;
    }

    public static <T> T filterFields(T object, Set<String> goodFields) {
        try {
            Field[] fields = Resource.class.getDeclaredFields();
            for (Field f : fields) {
                if (!Modifier.isFinal(f.getModifiers())) {
                    if (!goodFields.contains(f.getName())) {
                        // Only clearing objects, no point in clearing primitives as it
                        // doesn't save any space on the stream
                        if (!f.getType().isPrimitive()) {
                            // System.out.println("clearing " + f.getName());
                            f.setAccessible(true);
                            f.set(object, null);
                        } else {
                            // System.out.println("Can't do " + f.getType());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return object;
    }

    private static int sizeOf(Object object) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(50000);
            ObjectOutputStream o = new ObjectOutputStream(baos);
            o.writeObject(object);
            o.flush();
            return baos.size();
        } catch (Exception e) {
        }
        return -1;
    }
}
