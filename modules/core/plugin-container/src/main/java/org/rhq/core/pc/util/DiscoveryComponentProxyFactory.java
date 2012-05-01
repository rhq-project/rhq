/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.inventory.TimeoutException;
import org.rhq.core.pc.plugin.BlacklistedException;
import org.rhq.core.pc.plugin.PluginComponentFactory;
import org.rhq.core.pluginapi.inventory.ClassLoaderFacet;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;

/**
 * Factory that can build discovery component proxies. These proxies wrap
 * timeouts around discovery component method invocations.
 *
 * Note that if a discovery component invocation times out, the resource type
 * will be blacklisted by this factory. Any further attempt to retrieve a proxy
 * for that resource type's discovery component will fail.
 *
 * @author John Mazzitelli
 * @author Ian Springer
 */
public class DiscoveryComponentProxyFactory {
    private final Log log = LogFactory.getLog(DiscoveryComponentProxyFactory.class);

    private static final String DAEMON_THREAD_POOL_NAME = "ResourceDiscoveryComponent.invoker.daemon";
    private ExecutorService daemonThreadPool = null;
    private final Set<ResourceType> blacklist = new HashSet<ResourceType>();

    /**
     * Same as {@link #getDiscoveryComponentProxy(org.rhq.core.domain.resource.ResourceType, org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent, long, org.rhq.core.pc.inventory.ResourceContainer)} except
     * this lets you provide the interface of the discovery component you want to talk to. For example,
     * use this to talk to the {@link ClassLoaderFacet} of a discovery component.
     */
    @SuppressWarnings("unchecked")
    public <T> T getDiscoveryComponentProxy(ResourceType type, ResourceDiscoveryComponent component, long timeout,
                                            Class<T> componentInterface, ResourceContainer parentResourceContainer)
            throws PluginContainerException, BlacklistedException {

        if (isResourceTypeBlacklisted(type)) {
            throw new BlacklistedException("Discovery component for resource type [" + type + "] has been blacklisted");
        }

        try {
            PluginComponentFactory pluginComponentFactory = PluginContainer.getInstance().getPluginComponentFactory();

            ClassLoader pluginClassLoader =
                    pluginComponentFactory.getDiscoveryComponentClassLoader(parentResourceContainer, type.getPlugin());

            // This is the handler that will actually invoke the method calls.
            ResourceDiscoveryComponentInvocationHandler handler = new ResourceDiscoveryComponentInvocationHandler(type,
                component, timeout, pluginClassLoader, componentInterface);

            // This is the proxy that will look like the discovery component object that the caller will use.
            T proxy = (T) Proxy.newProxyInstance(pluginClassLoader, new Class<?>[] { componentInterface }, handler);
            return proxy;

        } catch (Throwable t) {
            throw new PluginContainerException("Cannot get discovery component proxy for [" + component + "]", t);
        }
    }

    /**
     * Given a discovery component instance, this returns that component wrapped in a proxy that provides the ability
     * for invocations to that component to timeout after a certain time limit expires. This allows the plugin container
     * to make calls into the plugin discovery component and not deadlock if that plugin misbehaves and never returns
     * (or takes too long to return).
     *
     *
     * @param type the resource type that is to be discovered by the given discovery component
     * @param component the discovery component to be wrapped in a timer proxy
     * @param timeout the time, in milliseconds, that invocations can take to invoke discovery component methods
     *
     * @param parentResourceContainer
     * @return the discovery component wrapped in a proxy that should be used to make calls to the component
     *
     * @throws PluginContainerException if this method failed to create the proxy
     * @throws BlacklistedException if the resource type's discovery component has been blacklisted and
     *                              not allowed to be invoked anymore
     */
    @SuppressWarnings("unchecked")
    public ResourceDiscoveryComponent getDiscoveryComponentProxy(ResourceType type,
                                                                 ResourceDiscoveryComponent component, long timeout,
                                                                 ResourceContainer parentResourceContainer)
            throws PluginContainerException, BlacklistedException {
        return getDiscoveryComponentProxy(type, component, timeout, ResourceDiscoveryComponent.class,
                parentResourceContainer);
    }

    public void initialize() {
        LoggingThreadFactory daemonFactory = new LoggingThreadFactory(DAEMON_THREAD_POOL_NAME, true);
        daemonThreadPool = Executors.newCachedThreadPool(daemonFactory);
    }

    public void shutdown() {
        if (daemonThreadPool != null) {
            PluginContainer pluginContainer = PluginContainer.getInstance();
            pluginContainer.shutdownExecutorService(daemonThreadPool, true);
        }
        daemonThreadPool = null;
    }

    public HashSet<ResourceType> getResourceTypeBlacklist() {
        synchronized (this.blacklist) {
            return new HashSet<ResourceType>(this.blacklist); // return a copy, not the real set
        }
    }

    public void clearResourceTypeBlacklist() {
        synchronized (this.blacklist) {
            this.blacklist.clear();
        }
    }

    public boolean isResourceTypeBlacklisted(ResourceType type) {
        synchronized (this.blacklist) {
            return this.blacklist.contains(type);
        }
    }

    public void addResourceTypeToBlacklist(ResourceType type) {
        synchronized (this.blacklist) {
            this.blacklist.add(type);
        }
        log.warn("The discovery component for resource type [" + type + "] has been blacklisted");
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
        private final ResourceType resourceType;
        private final ClassLoader pluginClassLoader;
        private Class<?> componentInterface;
        
        public ResourceDiscoveryComponentInvocationHandler(ResourceType type, ResourceDiscoveryComponent component,
            long timeout, ClassLoader pluginClassLoader, Class<?> componentInterface) {
            if (timeout <= 0) {
                throw new IllegalArgumentException("timeout value is not positive.");
            }
            if (component == null) {
                throw new IllegalArgumentException("component is null");
            }

            this.resourceType = type;
            this.component = component;
            this.timeout = timeout;
            this.pluginClassLoader = pluginClassLoader;
            this.componentInterface = componentInterface;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (isResourceTypeBlacklisted(this.resourceType)) {
                throw new RuntimeException("Discovery component for resource type [" + this.resourceType
                    + "] has been blacklisted and can no longer be invoked.");
            }

            if (componentInterface.isAssignableFrom(method.getDeclaringClass())) {
                return invokeInNewThread(method, args);
            } else {
                // toString(), etc.
                return invokeInCurrentThread(method, args);
            }
        }

        private Object invokeInNewThread(Method method, Object[] args) throws Throwable {
            ExecutorService threadPool = getThreadPool();
            ComponentInvocationThread invocationThread = new ComponentInvocationThread(this.component, method, args,
                this.pluginClassLoader);
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
                addResourceTypeToBlacklist(this.resourceType);
                String msg = invokedMethodString(method, args, "timed out. Invocation thread will be interrupted.");
                Thread thread = invocationThread.getThread();
                Exception cause;
                if (thread != null) {
                    StackTraceElement[] stackTrace = thread.getStackTrace();
                    cause = new Exception(thread + " with id [" + thread.getId()
                            + "] is hung. This exception contains its stack trace.");
                    cause.setStackTrace(stackTrace);
                } else {
                    cause = null;
                }
                TimeoutException timeoutException = new TimeoutException(msg, cause);
                future.cancel(true);
                throw timeoutException;
            }
        }

        private Object invokeInCurrentThread(Method method, Object[] args) throws Throwable {
            // This method is triggered when the component calls itself - do not timeout.
            // We already have a timed call on the call stack - no need to do it again
            ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(this.pluginClassLoader);
                return method.invoke(this.component, args);
            } catch (InvocationTargetException ite) {
                throw (ite.getCause() != null) ? ite.getCause() : ite;
            } finally {
                Thread.currentThread().setContextClassLoader(originalContextClassLoader);
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
        private ClassLoader pluginClassLoader;
        private Thread thread;

        ComponentInvocationThread(ResourceDiscoveryComponent component, Method method, Object[] args,
            ClassLoader pluginClassLoader) {
            this.component = component;
            this.method = method;
            this.args = args;
            this.pluginClassLoader = pluginClassLoader;
        }

        public Object call() throws Exception {
            ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                this.thread = Thread.currentThread();
                this.thread.setContextClassLoader(this.pluginClassLoader);
                // This is the actual call into the discovery component.
                Object results = this.method.invoke(this.component, this.args);
                return results;
            } catch (InvocationTargetException ite) {
                Throwable cause = ite.getCause();
                throw new Exception("Discovery component invocation failed.", cause);
            } catch (Throwable t) {
                throw new Exception("Failed to invoke discovery component", t);
            } finally {
                this.thread.setContextClassLoader(originalContextClassLoader);
                this.thread = null;
            }
        }

        public Thread getThread() {
            return this.thread;
        }
    }
}
