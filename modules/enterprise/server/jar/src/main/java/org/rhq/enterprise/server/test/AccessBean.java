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
/*
 * AccessBean.java
 *
 * Created on August 24, 2006, 11:30 PM
 *
 * To change this template, choose Tools | Template Manager and open the template in the editor.
 */

package org.rhq.enterprise.server.test;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.beans.PropertyEditorSupport;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import org.rhq.enterprise.server.RHQConstants;

/**
 * @author Greg Hinkle
 */
@Stateless
public class AccessBean implements AccessLocal {
    public static final int MAX_RESULTS = 2000;
    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager manager;

    /**
     * Creates a new instance of AccessBean
     */
    public AccessBean() {
    }

    static List<String> types;

    static {
        types = new ArrayList<String>();
        types.add("Resource");
        types.add("Agent");
        types.add("Role");
    }

    public List<String> getKnownTypes() {
        return types;
    }

    public List getAll(String type) {
        return manager.createQuery("from " + type + " d").setMaxResults(MAX_RESULTS).getResultList();
    }

    public EntityManager getManager() {
        return manager;
    }

    public List getAllDeep(String type) throws IntrospectionException, IllegalAccessException,
        InvocationTargetException {
        List l = getAll(type);
        if (l.size() == 0) {
            return l;
        }

        BeanInfo info = Introspector.getBeanInfo(l.get(0).getClass());
        PropertyDescriptor[] pds = info.getPropertyDescriptors();
        for (Object o : l) {
            for (PropertyDescriptor pd : pds) {
                if (Collection.class.isAssignableFrom(pd.getPropertyType())) {
                    Object v = pd.getReadMethod().invoke(o);
                    System.out.println(pd.getName() + ": " + v);
                }
            }
        }

        return l;
    }

    public List getAllFetching(String type, String... properties) throws IntrospectionException,
        IllegalAccessException, InvocationTargetException {
        List l = getAll(type);
        if (l.size() == 0) {
            return l;
        }

        BeanInfo info = Introspector.getBeanInfo(l.get(0).getClass());
        PropertyDescriptor[] pds = info.getPropertyDescriptors();
        Set<String> propsToLoad = new HashSet<String>(Arrays.asList(properties));
        for (Object o : l) {
            for (PropertyDescriptor pd : pds) {
                if (propsToLoad.contains(pd.getName())) {
                    Object v = pd.getReadMethod().invoke(o);
                    System.out.println(pd.getName() + ": " + v);
                }
            }
        }

        return l;
    }

    public Object findDeep(String typeName, Object key) throws IllegalAccessException, IntrospectionException,
        InvocationTargetException, ClassNotFoundException {
        Class type = Class.forName(typeName);
        BeanInfo info = Introspector.getBeanInfo(type);

        Object convertedKey = getConvertedKey(type, key);

        Object o = find(type, convertedKey);
        if (o == null) {
            return null;
        }

        PropertyDescriptor[] pds = info.getPropertyDescriptors();
        for (PropertyDescriptor pd : pds) {
            if (pd.getName().equals("id")) {
                pd.getReadMethod().invoke(o);
            }

            if (Collection.class.isAssignableFrom(pd.getPropertyType())) {
                Object v = pd.getReadMethod().invoke(o);
                System.out.println(pd.getName() + ": " + v);
            }
        }

        return o;
    }

    @SuppressWarnings("unchecked")
    public void delete(String entityName, String key) {
        try {
            Class type = Class.forName(entityName);
            Object objectKey = getConvertedKey(type, key);
            manager.remove(manager.find(type, objectKey));
        } catch (ClassNotFoundException e) {
            e.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public Object getConvertedKey(Class type, Object key) {
        Class keyType = getKeyType(type);
        System.out.println("Key type is: " + keyType);
        PropertyEditor ed = PropertyEditorManager.findEditor(keyType);
        if (ed != null) {
            ed.setAsText(String.valueOf(key));
            return ed.getValue();
        } else {
            return key;
        }
    }

    public Class getKeyType(Class type) {
        Field[] fields = type.getDeclaredFields();
        for (Field f : fields) {
            Id id = f.getAnnotation(javax.persistence.Id.class);
            if (id != null) {
                return f.getType();
            }
        }

        return Integer.class;
    }

    @SuppressWarnings("unchecked")
    public Object find(Class type, Object key) {
        return manager.find(type, key);
    }

    static {
        PropertyEditorManager.registerEditor(BigInteger.class, AccessBean.BigIntegerEditor.class);
    }

    public static class BigIntegerEditor extends PropertyEditorSupport {
        BigInteger val;

        public void setValue(Object value) {
            val = (BigInteger) value;
        }

        public Object getValue() {
            return val;
        }

        public String getAsText() {
            return val.toString();
        }

        public void setAsText(String text) throws IllegalArgumentException {
            val = new BigInteger(text);
        }
    }
}