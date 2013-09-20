/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.client;

import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.bindings.client.AbstractRhqFacade;
import org.rhq.bindings.client.RhqFacade;
import org.rhq.bindings.client.RhqManager;
import org.rhq.bindings.util.InterfaceSimplifier;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class LocalClient extends AbstractRhqFacade {

    private static final Log LOG = LogFactory.getLog(LocalClient.class);

    private Subject subject;
    private Map<RhqManager, Object> managers;

    public LocalClient(Subject subject) {
        this.subject = subject;
    }

    @Override
    public Subject getSubject() {
        return subject;
    }

    @Override
    public Subject login(String user, String password) throws Exception {
        return subject;
    }

    @Override
    public void logout() {
    }

    @Override
    public boolean isLoggedIn() {
        return true;
    }

    @Override
    public Map<RhqManager, Object> getScriptingAPI() {
        if (managers == null) {

            managers = new HashMap<RhqManager, Object>();

            for (final RhqManager manager : RhqManager.values()) {
                if (manager.enabled()) {
                    try {
                        Object proxy = AccessController.doPrivileged(new PrivilegedAction<Object>() {
                            @Override
                            public Object run() {
                                return getScriptingProxy(getLocalSLSB(manager), manager);
                            }
                        });

                        managers.put(manager, proxy);
                    } catch (Throwable e) {
                        LOG.error("Failed to load manager " + manager + ".", e);
                        throw new IllegalStateException("Failed to load manager " + manager + ".", e);
                    }
                }
            }
        }

        return managers;
    }

    @Override
    public <T> T getProxy(final Class<T> remoteApiIface) {
        final RhqManager manager = RhqManager.forInterface(remoteApiIface);

        if (manager == null) {
            throw new IllegalArgumentException("Unknown remote interface " + remoteApiIface);
        }

        return AccessController.doPrivileged(new PrivilegedAction<T>() {
            @Override
            public T run() {
                Object localSLSB = getLocalSLSB(manager);

                Object proxy = Proxy.newProxyInstance(remoteApiIface.getClassLoader(),
                    new Class<?>[] { remoteApiIface }, new LocalClientProxy(localSLSB, LocalClient.this, manager));

                return remoteApiIface.cast(proxy);
            }
        });
    }

    private Object getScriptingProxy(Object slsb, RhqManager manager) {
        Class<?> iface = manager.remote();

        Class<?> simplified = null;

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(iface.getClassLoader());
            simplified = InterfaceSimplifier.simplify(iface);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }

        Object proxy = Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] { simplified },
            new LocalClientProxy(slsb, this, manager));

        return proxy;
    }

    private Object getLocalSLSB(RhqManager manager) {
        return LookupUtil.getEjb(manager.beanName(), manager.localInterfaceClassName());
    }
}
