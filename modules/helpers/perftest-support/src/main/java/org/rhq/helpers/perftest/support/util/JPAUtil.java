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

package org.rhq.helpers.perftest.support.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;

/**
 *
 * @author Lukas Krejci
 */
public class JPAUtil {

    private JPAUtil() {
        
    }
    
    public static boolean isEntity(Class<?> clazz) {
        return clazz.getAnnotation(Entity.class) != null;
    }

    public static Annotations getJPAAnnotations(Class<?> clazz) {
        return extractJPAAnnotations(clazz.getAnnotations());
    }

    public static Annotations getJPAAnnotations(Field field) {
        return extractJPAAnnotations(field.getAnnotations());
    }
    
    public static Map<Field, Annotations> getJPAFields(Class<?> clazz) {
        HashMap<Field, Annotations> ret = new HashMap<Field, Annotations>();
        
        for (Field f : getAllFields(clazz)) {
            ret.put(f, extractJPAAnnotations(f.getAnnotations()));
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
