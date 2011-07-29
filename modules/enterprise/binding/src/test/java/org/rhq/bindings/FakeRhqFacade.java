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

package org.rhq.bindings;

import java.util.Collections;
import java.util.Map;

import org.rhq.bindings.client.RhqFacade;
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
import org.rhq.enterprise.server.drift.DriftManagerRemote;
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

public class FakeRhqFacade implements RhqFacade {

    public Subject getSubject() {
        return null;
    }

    public Subject login(String user, String password) throws Exception {
        return null;
    }

    public void logout() {

    }

    public boolean isLoggedIn() {
        return false;
    }

    public AlertManagerRemote getAlertManager() {
        return null;
    }

    public AlertDefinitionManagerRemote getAlertDefinitionManager() {
        return null;
    }

    public AvailabilityManagerRemote getAvailabilityManager() {
        return null;
    }

    public BundleManagerRemote getBundleManager() {
        return null;
    }

    public CallTimeDataManagerRemote getCallTimeDataManager() {
        return null;
    }

    public RepoManagerRemote getRepoManager() {
        return null;
    }

    public ConfigurationManagerRemote getConfigurationManager() {
        return null;
    }

    public ContentManagerRemote getContentManager() {
        return null;
    }

    public DataAccessManagerRemote getDataAccessManager() {
        return null;
    }

    public DiscoveryBossRemote getDiscoveryBoss() {
        return null;
    }

    public EventManagerRemote getEventManager() {
        return null;
    }

    public MeasurementBaselineManagerRemote getMeasurementBaselineManager() {
        return null;
    }

    public MeasurementDataManagerRemote getMeasurementDataManager() {
        return null;
    }

    public MeasurementDefinitionManagerRemote getMeasurementDefinitionManager() {
        return null;
    }

    public MeasurementScheduleManagerRemote getMeasurementScheduleManager() {
        return null;
    }

    public OperationManagerRemote getOperationManager() {
        return null;
    }

    public ResourceManagerRemote getResourceManager() {
        return null;
    }

    public ResourceFactoryManagerRemote getResourceFactoryManager() {
        return null;
    }

    public ResourceGroupManagerRemote getResourceGroupManager() {
        return null;
    }

    public ResourceTypeManagerRemote getResourceTypeManager() {
        return null;
    }

    public RoleManagerRemote getRoleManager() {
        return null;
    }

    public SavedSearchManagerRemote getSavedSearchManager() {
        return null;
    }

    public SubjectManagerRemote getSubjectManager() {
        return null;
    }

    public SupportManagerRemote getSupportManager() {
        return null;
    }

    public SystemManagerRemote getSystemManager() {
        return null;
    }

    public RemoteInstallManagerRemote getRemoteInstallManager() {
        return null;
    }

    public TagManagerRemote getTagManager() {
        return null;
    }

    @Override
    public DriftManagerRemote getDriftManager() {
        return null;
    }

    public Map<String, Object> getManagers() {
        return Collections.emptyMap();
    }

}
