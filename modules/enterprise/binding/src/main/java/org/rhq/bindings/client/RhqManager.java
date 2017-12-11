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

package org.rhq.bindings.client;

import org.rhq.enterprise.server.alert.AlertDefinitionManagerRemote;
import org.rhq.enterprise.server.alert.AlertManagerRemote;
import org.rhq.enterprise.server.auth.SubjectManagerRemote;
import org.rhq.enterprise.server.authz.RoleManagerRemote;
import org.rhq.enterprise.server.bundle.BundleManagerRemote;
import org.rhq.enterprise.server.cloud.StorageNodeManagerRemote;
import org.rhq.enterprise.server.configuration.ConfigurationManagerRemote;
import org.rhq.enterprise.server.content.ContentManagerRemote;
import org.rhq.enterprise.server.content.RepoManagerRemote;
import org.rhq.enterprise.server.core.AgentManagerRemote;
import org.rhq.enterprise.server.dashboard.DashboardManagerRemote;
import org.rhq.enterprise.server.discovery.DiscoveryBossRemote;
import org.rhq.enterprise.server.drift.DriftManagerRemote;
import org.rhq.enterprise.server.drift.DriftTemplateManagerRemote;
import org.rhq.enterprise.server.event.EventManagerRemote;
import org.rhq.enterprise.server.install.remote.RemoteInstallManagerRemote;
import org.rhq.enterprise.server.measurement.AvailabilityManagerRemote;
import org.rhq.enterprise.server.measurement.CallTimeDataManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementBaselineManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementProblemManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerRemote;
import org.rhq.enterprise.server.operation.OperationManagerRemote;
import org.rhq.enterprise.server.plugin.ServerPluginManagerRemote;
import org.rhq.enterprise.server.report.DataAccessManagerRemote;
import org.rhq.enterprise.server.resource.ResourceFactoryManagerRemote;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;
import org.rhq.enterprise.server.resource.ResourceTypeManagerRemote;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerRemote;
import org.rhq.enterprise.server.resource.group.definition.GroupDefinitionManagerRemote;
import org.rhq.enterprise.server.resource.metadata.PluginManagerRemote;
import org.rhq.enterprise.server.search.SavedSearchManagerRemote;
import org.rhq.enterprise.server.storage.StorageClusterSettingsManagerRemote;
import org.rhq.enterprise.server.storage.StorageNodeOperationsHandlerRemote;
import org.rhq.enterprise.server.support.SupportManagerRemote;
import org.rhq.enterprise.server.sync.SynchronizationManagerRemote;
import org.rhq.enterprise.server.system.SystemManagerRemote;
import org.rhq.enterprise.server.tagging.TagManagerRemote;

/**
 * An enumeration of all remote SLSBs of the RHQ server.
 *
 * @author Lukas Krejci
 * @author Greg Hinkle
 */
public enum RhqManager {
    /**
     * @since 4.12
     */
    AgentManager(AgentManagerRemote.class, "${AgentManager}"), //
    AlertManager(AlertManagerRemote.class, "${AlertManager}"), //
    AlertDefinitionManager(AlertDefinitionManagerRemote.class, "${AlertDefinitionManager}"), //
    AvailabilityManager(AvailabilityManagerRemote.class, "${AvailabilityManager}"), //
    BundleManager(BundleManagerRemote.class, "${BundleManager}"), //
    CallTimeDataManager(CallTimeDataManagerRemote.class, "${CallTimeDataManager}"), //
//    ClusterManager(ClusterManagerRemote.class, "${ClusterManager}"), //
    RepoManager(RepoManagerRemote.class, "${RepoManager}"), //
    ConfigurationManager(ConfigurationManagerRemote.class, "${ConfigurationManager}"), //
    ContentManager(ContentManagerRemote.class, "${ContentManager}"), //
    DashboardManager(DashboardManagerRemote.class, "${DashboardManager}"), //
    DataAccessManager(DataAccessManagerRemote.class, "${DataAccessManager}"), //
    DriftManager(DriftManagerRemote.class, "${DriftManager}"), //
    DriftTemplateManager(DriftTemplateManagerRemote.class, "${DriftTemplateManager}"), //
    DiscoveryBoss(DiscoveryBossRemote.class, "${DiscoveryBoss}"), //
    EventManager(EventManagerRemote.class, "${EventManager}"), //
    GroupDefinitionManager(GroupDefinitionManagerRemote.class, "${GroupDefinitionManager}"), //
    MeasurementBaselineManager(MeasurementBaselineManagerRemote.class, "${MeasurementBaselineManager}"), //
    MeasurementDataManager(MeasurementDataManagerRemote.class, "${MeasurementDataManager}"), //
    MeasurementDefinitionManager(MeasurementDefinitionManagerRemote.class, "${MeasurementDefinitionManager}"), //
    MeasurementScheduleManager(MeasurementScheduleManagerRemote.class, "${MeasurementScheduleManager}"), //
    MeasurementProblemManager(MeasurementProblemManagerRemote.class, "${MeasurementProblemManager}"), //
    OperationManager(OperationManagerRemote.class, "${OperationManager}"), //
    /**
     * @since 4.11
     */
    PluginManager(PluginManagerRemote.class, "${PluginManagerRemote}"), //
    ResourceManager(ResourceManagerRemote.class, "${ResourceManager}"), //
    ResourceFactoryManager(ResourceFactoryManagerRemote.class, "${ResourceFactoryManager}"), //
    ResourceGroupManager(ResourceGroupManagerRemote.class, "${ResourceGroupManager}"), //
    ResourceTypeManager(ResourceTypeManagerRemote.class, "${ResourceTypeManager}"), //
    RoleManager(RoleManagerRemote.class, "${RoleManager}"), //
    SavedSearchManager(SavedSearchManagerRemote.class, "${SavedSearchManager}"), //
    /**
     * @since 4.12
     */
    ServerPluginManager(ServerPluginManagerRemote.class, "${ServerPluginManager}"), //
    StorageNodeManager(StorageNodeManagerRemote.class, "${StorageNodeManager}"), //
    StorageNodeOperationsHandler(StorageNodeOperationsHandlerRemote.class, "${StorageNodeOperationsHandler}"),
    SubjectManager(SubjectManagerRemote.class, "${SubjectManager}"), //
    SupportManager(SupportManagerRemote.class, "${SupportManager}"), //
    SystemManager(SystemManagerRemote.class, "${SystemManager}"), //
    RemoteInstallManager(RemoteInstallManagerRemote.class, "${RemoteInstallManager}"), //
    TagManager(TagManagerRemote.class, "${TagManager}"), //
    SynchronizationManager(SynchronizationManagerRemote.class, "${SynchronizationManager}"),
    StorageClusterSettingsManager(StorageClusterSettingsManagerRemote.class, "${StorageClusterSettingsManager}");

    private Class<?> remote;
    private String localInterfaceClassName;
    private String beanName;
    private boolean enabled;


    private RhqManager(Class<?> remote, String enable) {
        this.remote = remote;
        this.beanName = this.name() + "Bean";
        localInterfaceClassName = getLocalInterfaceClassName(remote);

        //defaults and evaluates to TRUE unless the string contains "false". Done to defend against
        //possible errors in string replacement during rhq build.
        this.enabled = true;
        if ((enable != null) && (enable.trim().length() > 0)) {
            this.enabled = (enable.trim().equalsIgnoreCase("false")) ? Boolean.FALSE : Boolean.TRUE;
        }
    }

    public static RhqManager forInterface(Class<?> iface) {
        for (RhqManager m : values()) {
            if (m.remote().equals(iface)) {
                return m;
            }
        }

        return null;
    }

    public Class<?> remote() {
        return this.remote;
    }

    /**
     * @deprecated since 4.6.0, use the {@link #remote()} method instead
     * @return the class name of the remote interface
     */
    @Deprecated
    public String remoteName() {
        return this.remote.getName();
    }

    public String localInterfaceClassName() {
        return localInterfaceClassName;
    }

    public String beanName() {
        return this.beanName;
    }

    public boolean enabled() {
        return this.enabled;
    }

    private static String getLocalInterfaceClassName(Class<?> remoteIface) {
        String ifaceName = remoteIface.getName();
        if (!ifaceName.endsWith("Remote")) {
            throw new AssertionError("Inconsistent SLSB naming in RHQ! Remote interface '" + remoteIface.getName()
                + "' does not follow the established naming convention. This is a bug, please report it.");
        }

        return (ifaceName.substring(0, ifaceName.lastIndexOf("Remote")) + "Local");
    }
}
