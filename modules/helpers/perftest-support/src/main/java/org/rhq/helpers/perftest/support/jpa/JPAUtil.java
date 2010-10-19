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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Embedded;
import javax.persistence.Entity;

/**
 * A set of utility methods to work with JPA annotated classes.
 * 
 * @author Lukas Krejci
 */
public class JPAUtil {

    private JPAUtil() {

    }

    /**
     * Returns true if the class is annotated with {@link Entity} annotation.
     * @param clazz
     * @return true if class is a JPA entity, false otherwise
     */
    public static boolean isEntity(Class<?> clazz) {
        return clazz.getAnnotation(Entity.class) != null;
    }

    /**
     * Returns all the JPA annotations declared on the class.
     * 
     * @param clazz
     * @return
     */
    public static Annotations getJPAAnnotations(Class<?> clazz) {
        return extractJPAAnnotations(clazz.getAnnotations());
    }

    /**
     * Returns all the JPA annotations declared on the field.
     * 
     * @param field
     * @return
     */
    public static Annotations getJPAAnnotations(Field field) {
        return extractJPAAnnotations(field.getAnnotations());
    }

    /**
     * Returns all the JPA annotated fields on given class along with their annotations.
     * Note that this also returns all the JPA annotated fields declared in the super classes of the provided class.
     * 
     * @param clazz
     * @return
     */
    public static Map<Field, Annotations> getJPAFields(Class<?> clazz) {
        HashMap<Field, Annotations> ret = new HashMap<Field, Annotations>();

        for (Field f : getAllFields(clazz)) {
            if (f.getAnnotation(Embedded.class) == null) {
                ret.put(f, extractJPAAnnotations(f.getAnnotations()));
            } else {
                ret.putAll(getJPAFields(f.getType()));
            }
        }

        return ret;
    }

    /**
     * Returns all the fields of given class that have the desired annotation defined.
     * This returns also the fields declared in the class' super classes.
     * 
     * @param clazz
     * @param desiredAnnotation
     * @return
     */
    public static Set<Field> getJPAFields(Class<?> clazz, Class<? extends Annotation> desiredAnnotation) {
        HashSet<Field> ret = new HashSet<Field>();

        for (Field f : getAllFields(clazz)) {
            if (f.getAnnotation(desiredAnnotation) != null) {
                ret.add(f);
            } else if (f.getAnnotation(Embedded.class) != null) {
                ret.addAll(getJPAFields(f.getType(), desiredAnnotation));
            }
        }

        return ret;
    }

    private static Annotations extractJPAAnnotations(Annotation[] annotations) {
        Annotations ret = new Annotations();

        for (Annotation annon : annotations) {
            if (annon.annotationType().getPackage().getName().startsWith("javax.persistence")) {
                ret.put(annon.annotationType(), annon);
            }
        }

        return ret;
    }

    public static Field getField(Class<?> clazz, String name) {
        while (clazz != null) {
            Field f = null;
            try {
                f = clazz.getDeclaredField(name);
            } catch (SecurityException e) {
                return null;
            } catch (NoSuchFieldException e) {
                //let's continue below
            }

            if (f != null) {
                return f;
            } else {
                clazz = clazz.getSuperclass();
            }
        }

        return null;
    }

    private static List<Field> getAllFields(Class<?> clazz) {
        ArrayList<Field> fields = new ArrayList<Field>();

        fields.addAll(Arrays.asList(clazz.getDeclaredFields()));

        Class<?> superClass = clazz.getSuperclass();

        if (superClass != null) {
            fields.addAll(getAllFields(clazz.getSuperclass()));
        }

        return fields;
    }
}
