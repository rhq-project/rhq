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
package org.rhq.enterprise.server.test;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import javax.ejb.Local;

/**
 * This is the business interface for Access enterprise bean.
 *
 * @author Greg Hinkle
 */
@Local
public interface AccessLocal {
    java.util.List<java.lang.String> getKnownTypes();

    java.util.List getAll(String type);

    java.lang.Object find(Class type, Object key);

    java.util.List getAllFetching(String type, String... properties) throws IntrospectionException,
        IllegalAccessException, InvocationTargetException;

    java.util.List getAllDeep(String type) throws IntrospectionException, IllegalAccessException,
        InvocationTargetException;

    java.lang.Object findDeep(String typeName, Object key) throws IllegalAccessException, IntrospectionException,
        InvocationTargetException, ClassNotFoundException;

    void delete(String entityName, String key);
}