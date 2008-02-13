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
package org.rhq.enterprise.server.util;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.annotation.XmlTransient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.jboss.ws.core.EndpointInvocation;
import org.jboss.ws.core.server.ServiceEndpointInvoker;
import org.jboss.wsf.spi.invocation.InvocationContext;

/**
 * This invoker will take an uninitialized relationships or references from EJB3 pojo's and null them out so that we can
 * pass them as normal objects to the webservice binding layer. (Avoiding LazyInitializationExceptions)
 *
 * @author Greg Hinkle
 */
public class EJB3SafeEndpointInvoker extends ServiceEndpointInvoker {
    private static final Log LOG = LogFactory.getLog(EJB3SafeEndpointInvoker.class);

    public void invoke(InvocationContext invocationContext) throws Exception {
        super.invoke(invocationContext);
        EndpointInvocation inv = invocationContext.getAttachment(EndpointInvocation.class);
        Object value = inv.getReturnValue();
        if (value != null) {
            try {
                Method m = value.getClass().getMethod("getReturn");
                if (m != null) {
                    nullOutUninitializedFields(m.invoke(value));
                }

                inv.setReturnValue(value);
            } catch (NoSuchMethodException nsme) {
                // Expected for void return types
            }
        }
    }

    public static void nullOutUninitializedFields(Object value) throws Exception {
        long start = System.currentTimeMillis();
        nullOutUninitializedFields(value, new HashSet<Integer>(), 0);
        LOG.debug("Nulled in: " + (System.currentTimeMillis() - start));
    }

    private static void nullOutUninitializedFields(Object value, Set<Integer> nulledObjects, int depth)
        throws Exception {
        if (depth > 50) {
            //         LOG.warn("Getting different object hierarchies back from calls: " + value.getClass().getName());
            return;
        }

        if ((value == null) || nulledObjects.contains(System.identityHashCode(value))) {
            return;
        }

        nulledObjects.add(System.identityHashCode(value));

        if (value instanceof Collection) {
            // Null out any entries in initialized collections
            for (Object val : (Collection) value) {
                nullOutUninitializedFields(val, nulledObjects, depth + 1);
            }
        } else {
            // Null out any collections that aren't loaded
            BeanInfo bi = Introspector.getBeanInfo(value.getClass(), Object.class);

            PropertyDescriptor[] pds = bi.getPropertyDescriptors();
            for (PropertyDescriptor pd : pds) {
                Object propertyValue = null;
                try {
                    propertyValue = pd.getReadMethod().invoke(value);
                } catch (Exception lie) {
                    if (LOG.isDebugEnabled()) {
                        LOG
                            .debug("Couldn't load: " + pd.getName() + " off of " + value.getClass().getSimpleName(),
                                lie);
                    }
                }

                if (!Hibernate.isInitialized(propertyValue)) {
                    try {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Nulling out: " + pd.getName() + " off of " + value.getClass().getSimpleName());
                        }

                        Method writeMethod = pd.getWriteMethod();
                        if ((writeMethod != null) && (writeMethod.getAnnotation(XmlTransient.class) == null)) {
                            pd.getWriteMethod().invoke(value, new Object[] { null });
                        } else {
                            nullOutField(value, pd);
                        }
                    } catch (Exception lie) {
                        LOG.debug("Couldn't null out: " + pd.getName() + " off of " + value.getClass().getSimpleName()
                            + " trying field access", lie);
                        nullOutField(value, pd);
                    }
                } else {
                    if ((propertyValue instanceof Collection)
                        || ((propertyValue != null) && propertyValue.getClass().getName().startsWith(
                            "org.rhq.core.domain"))) {
                        nullOutUninitializedFields(propertyValue, nulledObjects, depth + 1);
                    }
                }
            }
        }
    }

    private static void nullOutField(Object value, PropertyDescriptor pd) {
        try {
            Field f = value.getClass().getDeclaredField(pd.getName());
            if (f != null) {
                // try to set the field this way
                f.setAccessible(true);
                f.set(value, null);
            }
        } catch (NoSuchFieldException e) {
            // ignore this
        } catch (IllegalAccessException e) {
            // ignore this
        }
    }
}