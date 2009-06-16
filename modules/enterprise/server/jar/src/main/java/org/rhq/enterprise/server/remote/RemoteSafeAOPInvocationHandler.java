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
package org.rhq.enterprise.server.remote;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.naming.InitialContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.invocation.NameBasedInvocation;

import org.rhq.enterprise.server.util.HibernateDetachUtility;

/**
 * This rather hackish endpoint is here to handle remote invocations over the standard
 * invocation handler.
 *
 * @author Greg Hinkle
 */
public class RemoteSafeAOPInvocationHandler implements ServerInvocationHandler {

    private static final Log log = LogFactory.getLog(RemoteSafeAOPInvocationHandler.class);

    public static final Map<String, Class> PRIMITIVE_CLASSES;

    static {
        PRIMITIVE_CLASSES = new HashMap<String, Class>();
        PRIMITIVE_CLASSES.put(Short.TYPE.getName(), Short.TYPE);
        PRIMITIVE_CLASSES.put(Integer.TYPE.getName(), Integer.TYPE);
        PRIMITIVE_CLASSES.put(Long.TYPE.getName(), Long.TYPE);
        PRIMITIVE_CLASSES.put(Float.TYPE.getName(), Float.TYPE);
        PRIMITIVE_CLASSES.put(Double.TYPE.getName(), Double.TYPE);
        PRIMITIVE_CLASSES.put(Boolean.TYPE.getName(), Boolean.TYPE);
        PRIMITIVE_CLASSES.put(Character.TYPE.getName(), Character.TYPE);
        PRIMITIVE_CLASSES.put(Byte.TYPE.getName(), Byte.TYPE);
    }

    public Object invoke(InvocationRequest invocationRequest) throws Throwable {

        Object result = null;
        long time = System.currentTimeMillis();

        try {
            InitialContext ic = new InitialContext();

            NameBasedInvocation nbi = ((NameBasedInvocation) invocationRequest.getParameter());
            String[] methodInfo = nbi.getMethodName().split(":");

            String jndiName = "rhq/" + methodInfo[0] + "/local";
            Object target = ic.lookup(jndiName);

            Class[] sig = new Class[nbi.getSignature().length];
            int i = 0;
            for (String cn : nbi.getSignature()) {
                sig[i] = getClass(nbi.getSignature()[i++]);
            }

            Method m = target.getClass().getMethod(methodInfo[1], sig);
            result = m.invoke(target, nbi.getParameters());

        } catch (InvocationTargetException e) {
            log.error("Failed to invoke remote request", e);
            return e.getTargetException();
        } catch (Exception e) {
            log.error("Failed to invoke remote request", e);
            return e;
        } finally {
            if (result != null) {
                try {
                    HibernateDetachUtility.nullOutUninitializedFields(result,
                        HibernateDetachUtility.SerializationType.SERIALIZATION);
                } catch (Exception e) {
                    log.error("Failed to null out uninitialized fields", e);
                    return e;
                }
                if (log.isDebugEnabled()) {
                    log.debug("Remote request execution time (ms): " + (System.currentTimeMillis() - time));
                }
            }
        }

        return result;
    }

    Class getClass(String name) throws ClassNotFoundException {
        // TODO GH: Doesn't support arrays
        if (PRIMITIVE_CLASSES.containsKey(name)) {
            return PRIMITIVE_CLASSES.get(name);
        } else {
            return Class.forName(name);
        }
    }

    public void addListener(InvokerCallbackHandler arg0) {
    }

    public void removeListener(InvokerCallbackHandler arg0) {
    }

    public void setInvoker(ServerInvoker arg0) {
    }

    public void setMBeanServer(MBeanServer arg0) {
    }
}
