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

/**
 * An enumeration of all remote SLSBs of the RHQ server.
 *
 * @author Lukas Krejci
 * @author Greg Hinkle
 */
public enum RhqManagers {
    AlertManager(AlertManagerRemote.class), //
    AlertDefinitionManager(AlertDefinitionManagerRemote.class), //
    AvailabilityManager(AvailabilityManagerRemote.class), //
    BundleManager(BundleManagerRemote.class), //
    CallTimeDataManager(CallTimeDataManagerRemote.class), //
    RepoManager(RepoManagerRemote.class), //
    ConfigurationManager(ConfigurationManagerRemote.class), //
    ContentManager(ContentManagerRemote.class), //
    DataAccessManager(DataAccessManagerRemote.class), //
    DiscoveryBoss(DiscoveryBossRemote.class), //
    EventManager(EventManagerRemote.class), //
    MeasurementBaselineManager(MeasurementBaselineManagerRemote.class), //
    MeasurementDataManager(MeasurementDataManagerRemote.class), //
    MeasurementDefinitionManager(MeasurementDefinitionManagerRemote.class), //
    MeasurementScheduleManager(MeasurementScheduleManagerRemote.class), //
    OperationManager(OperationManagerRemote.class), //
    ResourceManager(ResourceManagerRemote.class), //
    ResourceFactoryManager(ResourceFactoryManagerRemote.class), //
    ResourceGroupManager(ResourceGroupManagerRemote.class), //
    ResourceTypeManager(ResourceTypeManagerRemote.class), //
    RoleManager(RoleManagerRemote.class), //
    SavedSearchManager(SavedSearchManagerRemote.class), //
    SubjectManager(SubjectManagerRemote.class), //
    SupportManager(SupportManagerRemote.class), //
    SystemManager(SystemManagerRemote.class), //
    RemoteInstallManager(RemoteInstallManagerRemote.class), //
    TagManager(TagManagerRemote.class);

    private Class<?> remote;
    private String remoteName;
    private String beanName;

    private RhqManagers(Class<?> remote) {
        this.remote = remote;
        this.beanName = this.name() + "Bean";
        this.remoteName = this.name() + "Remote";
    }

    public static RhqManagers forInterface(Class<?> iface) {
        for (RhqManagers m : values()) {
            if (m.remote().equals(iface)) {
                return m;
            }
        }
        
        return null;
    }
    
    public Class<?> remote() {
        return this.remote;
    }

    public String beanName() {
        return this.beanName;
    }

    public String remoteName() {
        return this.remoteName;
    }
}