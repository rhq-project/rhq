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
public class LocalClient implements RhqFacade {

    private static final Log LOG = LogFactory.getLog(LocalClient.class);

    private Subject subject;
    private Map<String, Object> managers;

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
    public Map<String, Object> getScriptingAPI() {
        if (managers == null) {

            managers = new HashMap<String, Object>();

            for (final RhqManager manager : RhqManager.values()) {
                if (manager.enabled()) {
                    try {
                        Object proxy = AccessController.doPrivileged(new PrivilegedAction<Object>() {
                            @Override
                            public Object run() {
                                return getScriptingProxy(getLocalSLSB(manager), manager);
                            }
                        });

                        managers.put(manager.name(), proxy);
                    } catch (Throwable e) {
                        LOG.error("Failed to load manager " + manager + " due to missing class.", e);
                    }
                }
            }
        }

        return managers;
    }

    @Override
    public <T> T getProxy(Class<T> remoteApiIface) {
        RhqManager manager = RhqManager.forInterface(remoteApiIface);

        if (manager == null) {
            throw new IllegalArgumentException("Unknown remote interface " + remoteApiIface);
        }

        Object localSLSB = getLocalSLSB(manager);

        Object proxy = Proxy.newProxyInstance(remoteApiIface.getClassLoader(), new Class<?>[] { remoteApiIface },
            new LocalClientProxy(localSLSB, this, manager));

        return remoteApiIface.cast(proxy);
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
        switch (manager) {
        case AlertDefinitionManager:
            return LookupUtil.getAlertDefinitionManager();
        case AlertManager:
            return LookupUtil.getAlertManager();
        case AvailabilityManager:
            return LookupUtil.getAvailabilityManager();
        case BundleManager:
            return LookupUtil.getBundleManager();
        case CallTimeDataManager:
            return LookupUtil.getCallTimeDataManager();
        case ConfigurationManager:
            return LookupUtil.getConfigurationManager();
        case ContentManager:
            return LookupUtil.getContentManager();
        case DataAccessManager:
            return LookupUtil.getDataAccessManager();
        case DiscoveryBoss:
            return LookupUtil.getDiscoveryBoss();
        case DriftManager:
            return LookupUtil.getDriftManager();
        case DriftTemplateManager:
            return LookupUtil.getDriftTemplateManager();
        case EventManager:
            return LookupUtil.getEventManager();
        case MeasurementBaselineManager:
            return LookupUtil.getMeasurementBaselineManager();
        case MeasurementDataManager:
            return LookupUtil.getMeasurementDataManager();
        case MeasurementDefinitionManager:
            return LookupUtil.getMeasurementDefinitionManager();
        case MeasurementScheduleManager:
            return LookupUtil.getMeasurementScheduleManager();
        case OperationManager:
            return LookupUtil.getOperationManager();
        case RemoteInstallManager:
            return LookupUtil.getRemoteInstallManager();
        case RepoManager:
            return LookupUtil.getRepoManagerLocal();
        case ResourceFactoryManager:
            return LookupUtil.getResourceFactoryManager();
        case ResourceGroupManager:
            return LookupUtil.getResourceGroupManager();
        case ResourceManager:
            return LookupUtil.getResourceManager();
        case ResourceTypeManager:
            return LookupUtil.getResourceTypeManager();
        case RoleManager:
            return LookupUtil.getRoleManager();
        case SavedSearchManager:
            return LookupUtil.getSavedSearchManager();
        case SubjectManager:
            return LookupUtil.getSubjectManager();
        case SupportManager:
            return LookupUtil.getSupportManager();
        case SynchronizationManager:
            return LookupUtil.getSynchronizationManager();
        case SystemManager:
            return LookupUtil.getSystemManager();
        case TagManager:
            return LookupUtil.getTagManager();
        }

        throw new IllegalStateException("LocalClient does not handle the manager: " + manager
            + ". This is a bug, please report it.");
    }
}
