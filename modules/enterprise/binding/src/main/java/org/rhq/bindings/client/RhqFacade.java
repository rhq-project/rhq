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

import java.util.Map;

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

/**
 * This is an interface through which the script can communicate with RHQ server.
 *
 * @author Lukas Krejci
 */
public interface RhqFacade {

    /**
     * @return the user the facade is authenticated as
     */
    Subject getSubject();
    
    Subject login(String user, String password) throws Exception;
    
    void logout();
    
    boolean isLoggedIn();
    
    AlertManagerRemote getAlertManager();

    AlertDefinitionManagerRemote getAlertDefinitionManager();

    AvailabilityManagerRemote getAvailabilityManager();

    BundleManagerRemote getBundleManager();

    CallTimeDataManagerRemote getCallTimeDataManager();

    RepoManagerRemote getRepoManager();

    ConfigurationManagerRemote getConfigurationManager();

    ContentManagerRemote getContentManager();

    DataAccessManagerRemote getDataAccessManager();

    DiscoveryBossRemote getDiscoveryBoss();

    EventManagerRemote getEventManager();

    MeasurementBaselineManagerRemote getMeasurementBaselineManager();

    MeasurementDataManagerRemote getMeasurementDataManager();

    MeasurementDefinitionManagerRemote getMeasurementDefinitionManager();

    MeasurementScheduleManagerRemote getMeasurementScheduleManager();

    OperationManagerRemote getOperationManager();

    ResourceManagerRemote getResourceManager();

    ResourceFactoryManagerRemote getResourceFactoryManager();

    ResourceGroupManagerRemote getResourceGroupManager();

    ResourceTypeManagerRemote getResourceTypeManager();

    RoleManagerRemote getRoleManager();

    SavedSearchManagerRemote getSavedSearchManager();

    SubjectManagerRemote getSubjectManager();

    SupportManagerRemote getSupportManager();

    SystemManagerRemote getSystemManager();

    RemoteInstallManagerRemote getRemoteInstallManager();

    TagManagerRemote getTagManager();

    /**
     * This map is constructed using all the elements in the {@link RhqManagers} enum which are then proxied
     * using this instance.
     * 
     * @return a map of all available proxied managers keyed by their names.
     */
    Map<String, Object> getManagers();
}
