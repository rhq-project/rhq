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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.bindings.client.RhqFacade;
import org.rhq.bindings.client.RhqManagers;
import org.rhq.bindings.util.InterfaceSimplifier;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerRemote;
import org.rhq.enterprise.server.alert.AlertManagerRemote;
import org.rhq.enterprise.server.auth.SubjectManagerRemote;
import org.rhq.enterprise.server.authz.RoleManagerRemote;
import org.rhq.enterprise.server.bundle.BundleManagerRemote;
import org.rhq.enterprise.server.configuration.ConfigurationManagerRemote;
import org.rhq.enterprise.server.content.ContentManagerRemote;
import org.rhq.enterprise.server.content.RepoManagerRemote;
import org.rhq.enterprise.server.discovery.DiscoveryBossRemote;
import org.rhq.enterprise.server.event.EventManagerRemote;
import org.rhq.enterprise.server.install.remote.RemoteInstallManagerRemote;
import org.rhq.enterprise.server.measurement.AvailabilityManagerRemote;
import org.rhq.enterprise.server.measurement.CallTimeDataManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementBaselineManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerRemote;
import org.rhq.enterprise.server.operation.OperationManagerRemote;
import org.rhq.enterprise.server.report.DataAccessManagerRemote;
import org.rhq.enterprise.server.resource.ResourceFactoryManagerRemote;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;
import org.rhq.enterprise.server.resource.ResourceTypeManagerRemote;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerRemote;
import org.rhq.enterprise.server.search.SavedSearchManagerRemote;
import org.rhq.enterprise.server.support.SupportManagerRemote;
import org.rhq.enterprise.server.system.SystemManagerRemote;
import org.rhq.enterprise.server.tagging.TagManagerRemote;
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

    public Subject getSubject() {
        return subject;
    }

    public Subject login(String user, String password) throws Exception {
        return subject;
    }

    public void logout() {
    }

    public boolean isLoggedIn() {
        return true;
    }

    public AlertManagerRemote getAlertManager() {
        return getProxy(LookupUtil.getAlertManager(), AlertManagerRemote.class);
    }

    public AlertDefinitionManagerRemote getAlertDefinitionManager() {
        return getProxy(LookupUtil.getAlertDefinitionManager(), AlertDefinitionManagerRemote.class);
    }

    public AvailabilityManagerRemote getAvailabilityManager() {
        return getProxy(LookupUtil.getAvailabilityManager(), AvailabilityManagerRemote.class);
    }

    public BundleManagerRemote getBundleManager() {
        return getProxy(LookupUtil.getBundleManager(), BundleManagerRemote.class);
    }

    public CallTimeDataManagerRemote getCallTimeDataManager() {
        return getProxy(LookupUtil.getCallTimeDataManager(), CallTimeDataManagerRemote.class);
    }

    public RepoManagerRemote getRepoManager() {
        return getProxy(LookupUtil.getRepoManagerLocal(), RepoManagerRemote.class);
    }

    public ConfigurationManagerRemote getConfigurationManager() {
        return getProxy(LookupUtil.getConfigurationManager(), ConfigurationManagerRemote.class);
    }

    public ContentManagerRemote getContentManager() {
        return getProxy(LookupUtil.getContentManager(), ContentManagerRemote.class);
    }

    public DataAccessManagerRemote getDataAccessManager() {
        return getProxy(LookupUtil.getDataAccessManager(), DataAccessManagerRemote.class);
    }

    public DiscoveryBossRemote getDiscoveryBoss() {
        return getProxy(LookupUtil.getDiscoveryBoss(), DiscoveryBossRemote.class);
    }

    public EventManagerRemote getEventManager() {
        return getProxy(LookupUtil.getEventManager(), EventManagerRemote.class);
    }

    public MeasurementBaselineManagerRemote getMeasurementBaselineManager() {
        return getProxy(LookupUtil.getMeasurementBaselineManager(), MeasurementBaselineManagerRemote.class);
    }

    public MeasurementDataManagerRemote getMeasurementDataManager() {
        return getProxy(LookupUtil.getMeasurementDataManager(), MeasurementDataManagerRemote.class);
    }

    public MeasurementDefinitionManagerRemote getMeasurementDefinitionManager() {
        return getProxy(LookupUtil.getMeasurementDefinitionManager(), MeasurementDefinitionManagerRemote.class);
    }

    public MeasurementScheduleManagerRemote getMeasurementScheduleManager() {
        return getProxy(LookupUtil.getMeasurementScheduleManager(), MeasurementScheduleManagerRemote.class);
    }

    public OperationManagerRemote getOperationManager() {
        return getProxy(LookupUtil.getOperationManager(), OperationManagerRemote.class);
    }

    public ResourceManagerRemote getResourceManager() {
        return getProxy(LookupUtil.getResourceManager(), ResourceManagerRemote.class);
    }

    public ResourceFactoryManagerRemote getResourceFactoryManager() {
        return getProxy(LookupUtil.getResourceFactoryManager(), ResourceFactoryManagerRemote.class);
    }

    public ResourceGroupManagerRemote getResourceGroupManager() {
        return getProxy(LookupUtil.getResourceGroupManager(), ResourceGroupManagerRemote.class);
    }

    public ResourceTypeManagerRemote getResourceTypeManager() {
        return getProxy(LookupUtil.getResourceTypeManager(), ResourceTypeManagerRemote.class);
    }

    public RoleManagerRemote getRoleManager() {
        return getProxy(LookupUtil.getRoleManager(), RoleManagerRemote.class);
    }

    public SavedSearchManagerRemote getSavedSearchManager() {
        return getProxy(LookupUtil.getSavedSearchManager(), SavedSearchManagerRemote.class);
    }

    public SubjectManagerRemote getSubjectManager() {
        return getProxy(LookupUtil.getSubjectManager(), SubjectManagerRemote.class);
    }

    public SupportManagerRemote getSupportManager() {
        return getProxy(LookupUtil.getSupportManager(), SupportManagerRemote.class);
    }

    public SystemManagerRemote getSystemManager() {
        return getProxy(LookupUtil.getSystemManager(), SystemManagerRemote.class);
    }

    public RemoteInstallManagerRemote getRemoteInstallManager() {
        return getProxy(LookupUtil.getRemoteInstallManager(), RemoteInstallManagerRemote.class);
    }

    public TagManagerRemote getTagManager() {
        return getProxy(LookupUtil.getTagManager(), TagManagerRemote.class);
    }

    public Map<String, Object> getManagers() {
        if (managers == null) {

            managers = new HashMap<String, Object>();

            for (RhqManagers manager : RhqManagers.values()) {
                try {
                    Method m = getClass().getMethod("get" + manager.name());
                    managers.put(manager.name(), m.invoke(this));
                } catch (Throwable e) {
                    LOG.error("Failed to load manager " + manager + " due to missing class.", e);
                }
            }
        }

        return managers;
    }

    private <T> T getProxy(Object slsb, Class<T> iface) {
        RhqManagers manager = RhqManagers.forInterface(iface);

        Class<?> simplified = InterfaceSimplifier.simplify(iface);
        
        Object proxy = Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] { simplified }, new LocalClientProxy(slsb, this, manager));
        
        return iface.cast(proxy);
    }
}
