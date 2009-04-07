/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.pc.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.pc.inventory.TimeoutException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;

/**
 * Factory that can build discovery component proxies. These proxies wrap
 * timeouts around discovery component method invocations.
 *
 * @author John Mazzitelli
 */
public class DiscoveryComponentProxyFactory {
    private final Log log = LogFactory.getLog(DiscoveryComponentProxyFactory.class);

    private static final String DAEMON_THREAD_POOL_NAME = "ResourceDiscoveryComponent.invoker.daemon";
    private ExecutorService daemonThreadPool = null;

    /**
     * Given a discovery component instance, this returns that component wrapped in a proxy that provides the ability
     * for invocations to that component to timeout after a certain time limit expires. This allows the plugin container
     * to make calls into the plugin discovery component and not deadlock if that plugin misbehaves and never returns
     * (or takes too long to return).
     *
     * @param component the discovery component to be wrapped in a timer proxy
     * @param timeout the time, in milliseconds, that invocations can take to invoke discovery component methods
     * 
     * @return the discovery component wrapped in a proxy that should be used to make calls to the component
     *
     * @throws PluginContainerException if this method failed to create the proxy
     */
    @SuppressWarnings("unchecked")
    public ResourceDiscoveryComponent getDiscoveryComponentProxy(ResourceDiscoveryComponent component, long timeout)
        throws PluginContainerException {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            // this is the handler that will actually invoke the method calls
            ResourceDiscoveryComponentInvocationHandler handler = new ResourceDiscoveryComponentInvocationHandler(
                component, timeout);

            // this is the proxy that will look like the discovery component object that the caller will use
            ResourceDiscoveryComponent proxy;
            proxy = (ResourceDiscoveryComponent) Proxy.newProxyInstance(classLoader,
                new Class<?>[] { ResourceDiscoveryComponent.class }, handler);

            return proxy;
        } catch (Throwable t) {
            throw new PluginContainerException("Cannot get discovery component proxy for [" + component + "]", t);
        }
    }

    public void initialize() {
        LoggingThreadFactory daemonFactory = new LoggingThreadFactory(DAEMON_THREAD_POOL_NAME, true);
        daemonThreadPool = Executors.newCachedThreadPool(daemonFactory);
    }

    public void shutdown() {
        if (daemonThreadPool != null) {
            daemonThreadPool.shutdownNow();
        }
        daemonThreadPool = null;
    }

    private ExecutorService getThreadPool() {
        return daemonThreadPool;
    }

    /**
     * This is a {@link ResourceDiscoveryComponent} proxy that invokes discovery component methods in pooled threads.
     * It can interrupt the invocation thread and throw a {@link TimeoutException} if its execution time exceeds a
     * specified timeout.
     */
    @SuppressWarnings("unchecked")
    private class ResourceDiscoveryComponentInvocationHandler implements InvocationHandler {
        private final ResourceDiscoveryComponent component;
        private final long timeout;

        public ResourceDiscoveryComponentInvocationHandler(ResourceDiscoveryComponent component, long timeout) {
            if (timeout <= 0) {
                throw new IllegalArgumentException("timeout value is not positive.");
            }
            if (component == null) {
                throw new IllegalArgumentException("component is null");
            }

            this.component = component;
            this.timeout = timeout;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass().equals(ResourceDiscoveryComponent.class)) {
                return invokeInNewThread(method, args);
            } else {
                // toString(), etc.
                return invokeInCurrentThread(method, args);
            }
        }

        private Object invokeInNewThread(Method method, Object[] args) throws Throwable {
            ExecutorService threadPool = getThreadPool();
            Callable invocationThread = new ComponentInvocationThread(this.component, method, args);
            Future<?> future = threadPool.submit(invocationThread);
            try {
                return future.get(this.timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                log.error("Thread [" + Thread.currentThread().getName() + "] was interrupted.");
                future.cancel(true); // this is a daemon thread, let's try to cancel it
                throw new RuntimeException(invokedMethodString(method, args, "was interrupted."), e);
            } catch (ExecutionException e) {
                if (log.isDebugEnabled()) {
                    log.debug(invokedMethodString(method, args, "failed."), e);
                }
                throw e.getCause();
            } catch (java.util.concurrent.TimeoutException e) {
                String msg = invokedMethodString(method, args, "timed out. Invocation thread will be interrupted");
                log.debug(msg);
                future.cancel(true);
                throw new TimeoutException(msg);
            }
        }

        private Object invokeInCurrentThread(Method method, Object[] args) throws Throwable {
            // this method is triggered when the component calls itself, do not timeout.
            // we already have a timed call in the call stack, no need to do it again
            try {
                return method.invoke(this.component, args);
            } catch (InvocationTargetException ite) {
                throw (ite.getCause() != null) ? ite.getCause() : ite;
            }
        }

        private String invokedMethodString(Method method, Object[] methodArgs, String extraMsg) {
            String name = this.component.getClass().getName() + '.' + method.getName() + "()";
            String args = ((methodArgs != null) ? Arrays.asList(methodArgs).toString() : "");
            return "Call to [" + name + "] with args [" + args + "] " + extraMsg;
        }
    }

    @SuppressWarnings("unchecked")
    private class ComponentInvocationThread implements Callable {
        private final ResourceDiscoveryComponent component;
        private final Method method;
        private final Object[] args;

        ComponentInvocationThread(ResourceDiscoveryComponent component, Method method, Object[] args) {
            this.component = component;
            this.method = method;
            this.args = args;
        }

        public Object call() throws Exception {
            try {
                // This is the actual call into the discovery component
                Object results = this.method.invoke(this.component, this.args);
                return results;
            } catch (InvocationTargetException ite) {
                Throwable cause = ite.getCause();
                throw new Exception("Discovery component invocation failed.", cause);
            } catch (Throwable t) {
                throw new Exception("Failed to invoke discovery component", t);
            }
        }
    }
}
