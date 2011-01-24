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
package org.rhq.enterprise.server.safeinvoker;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.ws.core.EndpointInvocation;
import org.jboss.ws.core.server.ServiceEndpointInvoker;
import org.jboss.wsf.spi.invocation.InvocationContext;

import org.rhq.enterprise.server.safeinvoker.HibernateDetachUtility.SerializationType;

/**
 * This invoker will take an uninitialized relationships or references from EJB3 pojo's and null them out so that we can
 * pass them as normal objects to the webservice binding layer. (Avoiding LazyInitializationExceptions)
 *
 * @author Greg Hinkle
 */
public class EJB3SafeEndpointInvoker extends ServiceEndpointInvoker {
    private static final Log LOG = LogFactory.getLog(EJB3SafeEndpointInvoker.class);

    @Override
    public void invoke(InvocationContext invocationContext) throws Exception {
        super.invoke(invocationContext);
        EndpointInvocation inv = invocationContext.getAttachment(EndpointInvocation.class);
        Object value = inv.getReturnValue();
        if (value != null) {
            try {
                Method m = value.getClass().getMethod("getReturn");
                if (m != null) {
                    HibernateDetachUtility.nullOutUninitializedFields(value,
                        HibernateDetachUtility.SerializationType.JAXB);
                }

                inv.setReturnValue(value);
            } catch (NoSuchMethodException nsme) {
                // Expected for void return types
            }
        }
    }
}