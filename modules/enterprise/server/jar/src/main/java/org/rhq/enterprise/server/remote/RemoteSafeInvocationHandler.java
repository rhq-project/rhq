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
import java.util.concurrent.ConcurrentHashMap;

import javax.management.MBeanServer;
import javax.naming.InitialContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.invocation.NameBasedInvocation;

import org.rhq.core.domain.server.ExternalizableStrategy;
import org.rhq.core.util.exception.WrappedRemotingException;
import org.rhq.enterprise.server.safeinvoker.HibernateDetachUtility;

/**
 * Handle remote invocations.  Note that we perform only invocations defined in the remote interfaces.
 * Although, we execute only locals to bypass the serialization performed by a remote invocation. Even
 * though this handler is co-located, for remotes, remoting will serialize the return data immediately.
 * This is bad for us because since we return domain objects we ned to scrub the data, removing
 * hibernate proxies (see {@link HibernateDetachUtility}.
 *
 * @author Greg Hinkle
 * @autor Jay Shaughnessy
 */
public class RemoteSafeInvocationHandler implements ServerInvocationHandler {

    private static final Log log = LogFactory.getLog(RemoteSafeInvocationHandler.class);
    private static final Map<String, Class<?>> PRIMITIVE_CLASSES;
    private static final ConcurrentHashMap<Class<?>, String> LOCAL_JNDI_NAMES;
    private static final ConcurrentHashMap<Class<?>, String> REMOTE_JNDI_NAMES;

    static {
        PRIMITIVE_CLASSES = new HashMap<String, Class<?>>();
        PRIMITIVE_CLASSES.put(Short.TYPE.getName(), Short.TYPE);
        PRIMITIVE_CLASSES.put(Integer.TYPE.getName(), Integer.TYPE);
        PRIMITIVE_CLASSES.put(Long.TYPE.getName(), Long.TYPE);
        PRIMITIVE_CLASSES.put(Float.TYPE.getName(), Float.TYPE);
        PRIMITIVE_CLASSES.put(Double.TYPE.getName(), Double.TYPE);
        PRIMITIVE_CLASSES.put(Boolean.TYPE.getName(), Boolean.TYPE);
        PRIMITIVE_CLASSES.put(Character.TYPE.getName(), Character.TYPE);
        PRIMITIVE_CLASSES.put(Byte.TYPE.getName(), Byte.TYPE);

        LOCAL_JNDI_NAMES = new ConcurrentHashMap<Class<?>, String>();
        REMOTE_JNDI_NAMES = new ConcurrentHashMap<Class<?>, String>();
    }

    private RemoteSafeInvocationHandlerMetrics metrics = new RemoteSafeInvocationHandlerMetrics();

    public Object invoke(InvocationRequest invocationRequest) throws Throwable {

        if (invocationRequest == null) {
            throw new IllegalArgumentException("InvocationRequest was null.");
        }

        String methodName = null;
        boolean successful = false; // we will flip this to true when we know we were successful
        Object result = null;
        long time = System.currentTimeMillis();

        try {
            InitialContext ic = new InitialContext();

            NameBasedInvocation nbi = ((NameBasedInvocation) invocationRequest.getParameter());
            if (null == nbi) {
                throw new IllegalArgumentException("InvocationRequest did not supply method.");
            }

            methodName = nbi.getMethodName();
            String[] methodInfo = methodName.split(":");
            Class<?> remoteClass = getClass(methodInfo[0]);

            String[] signature = nbi.getSignature();
            int signatureLength = signature.length;
            Class<?>[] sig = new Class[signatureLength];
            for (int i = 0; i < signatureLength; i++) {
                sig[i] = getClass(signature[i]);
            }

            // make sure the remote method is defined to ensure remote clients don't access locals
            String jndiName = getRemoteJNDIName(remoteClass);
            Object target = ic.lookup(jndiName);
            Method m = target.getClass().getMethod(methodInfo[1], sig);

            // switch to the local
            jndiName = getLocalJNDIName(remoteClass);
            target = ic.lookup(jndiName);

            m = target.getClass().getMethod(methodInfo[1], sig);
            result = m.invoke(target, nbi.getParameters());
            successful = true;

        } catch (InvocationTargetException e) {
            log.error("Failed to invoke remote request", e);
            return new WrappedRemotingException(e.getTargetException());
        } catch (Exception e) {
            log.error("Failed to invoke remote request", e);
            return new WrappedRemotingException(e);
        } finally {
            if (result != null) {
                // set the strategy guiding how the return information is serialized
                ExternalizableStrategy.setStrategy(ExternalizableStrategy.Subsystem.REFLECTIVE_SERIALIZATION);

                // scrub the return data if Hibernate proxies
                try {
                    HibernateDetachUtility.nullOutUninitializedFields(result,
                        HibernateDetachUtility.SerializationType.SERIALIZATION);
                } catch (Exception e) {
                    log.error("Failed to null out uninitialized fields", e);
                    this.metrics.addData(methodName, System.currentTimeMillis() - time, false);

                    return new WrappedRemotingException(e);
                }
            }

            // want to calculate this after the hibernate util so we take that into account too
            long executionTime = System.currentTimeMillis() - time;
            this.metrics.addData(methodName, executionTime, successful);
            if (log.isDebugEnabled()) {
                log.debug("Remote request [" + methodName + "] execution time (ms): " + executionTime);
            }
        }

        return result;
    }

    private static <T> String getLocalJNDIName(Class<?> remoteClass) {
        String jndiName = LOCAL_JNDI_NAMES.get(remoteClass);
        if (jndiName == null) {
            jndiName = "java:global/rhq/rhq-server/" + remoteClass.getSimpleName().replaceFirst("Remote$", "Bean")
                + "!" + remoteClass.getName().replaceFirst("Remote$", "Local");
            LOCAL_JNDI_NAMES.put(remoteClass, jndiName);
        }
        return jndiName;
    }

    private static <T> String getRemoteJNDIName(Class<?> remoteClass) {
        String jndiName = REMOTE_JNDI_NAMES.get(remoteClass);
        if (jndiName == null) {
            jndiName = "java:global/rhq/rhq-server/" + remoteClass.getSimpleName().replaceFirst("Remote$", "Bean")
                + "!" + remoteClass.getName();
            REMOTE_JNDI_NAMES.put(remoteClass, jndiName);
        }
        return jndiName;
    }

    private static Class<?> getClass(String name) throws ClassNotFoundException {
        // TODO GH: Doesn't support arrays
        if (PRIMITIVE_CLASSES.containsKey(name)) {
            return PRIMITIVE_CLASSES.get(name);
        } else {
            return Class.forName(name);
        }
    }

    /**
     * Registers the MBean used to monitor the remote API processing.
     *
     * @param mbs the MBeanServer where the metrics MBean should be registered
     */
    public void registerMetricsMBean(MBeanServer mbs) {
        try {
            mbs.registerMBean(this.metrics, RemoteSafeInvocationHandlerMetricsMBean.OBJECTNAME_METRICS);
        } catch (Exception e) {
            log.warn("Failed to register the metrics object, will not be able to monitor remote API: " + e);
        }
    }

    /**
     * Unregisters the MBean that was used to monitor the remote API processing.
     *
     * @param mbs the MBeanServer where the metrics MBean is registered
     */
    public void unregisterMetricsMBean(MBeanServer mbs) {
        try {
            mbs.unregisterMBean(RemoteSafeInvocationHandlerMetricsMBean.OBJECTNAME_METRICS);
        } catch (Exception e) {
            log.warn("Failed to unregister the metrics object: " + e);
        }
    }

    public void addListener(InvokerCallbackHandler handler) {
    }

    public void removeListener(InvokerCallbackHandler handler) {
    }

    public void setInvoker(ServerInvoker invoker) {
    }

    public void setMBeanServer(MBeanServer mbs) {
    }
}
