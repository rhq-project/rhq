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
import org.rhq.enterprise.server.sync.SynchronizationManagerRemote;
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

    /**
     * This map is constructed using all the elements in the {@link RhqManager} enum which are then proxied
     * using this instance.
     *
     * @return a map of all available proxied managers keyed by their names.
     */
    Map<RhqManager, Object> getScriptingAPI();

    /**
     * Unlike the {@link #getScriptingAPI()} method that returns objects with modified signatures
     * meant to be used by the scripting environment, this method provides the access to the "raw"
     * remote API interface implementation backed by this RHQ facade implementation.
     *
     * @param remoteApiIface one of the RHQ's remote API interfaces of which the proxied instance
     * should be returned
     * @return the proxy of the remote API interface backed by this facade
     */
    <T> T getProxy(Class<T> remoteApiIface);

    ///////////////////// deprecated methods added to re-introduce compatibility with RHQ 4.4.0

    /**
     * deprecated use {@code RhqFacade.getProxy(RhqManager.XXX.remote())} instead
     * @since 4.10
     */
    @Deprecated
    AlertDefinitionManagerRemote getAlertDefinitionManager();

    /**
     * deprecated use {@code RhqFacade.getProxy(RhqManager.XXX.remote())} instead
     * @since 4.10
     */
    @Deprecated
    AlertManagerRemote getAlertManager();

    /**
     * deprecated use {@code RhqFacade.getProxy(RhqManager.XXX.remote())} instead
     * @since 4.10
     */
    @Deprecated
    AvailabilityManagerRemote getAvailabilityManager();

    /**
     * deprecated use {@code RhqFacade.getProxy(RhqManager.XXX.remote())} instead
     * @since 4.10
     */
    @Deprecated
    BundleManagerRemote getBundleManager();

    /**
     * deprecated use {@code RhqFacade.getProxy(RhqManager.XXX.remote())} instead
     * @since 4.10
     */
    @Deprecated
    CallTimeDataManagerRemote getCallTimeDataManager();

    /**
     * deprecated use {@code RhqFacade.getProxy(RhqManager.XXX.remote())} instead
     * @since 4.10
     */
    @Deprecated
    ConfigurationManagerRemote getConfigurationManager();

    /**
     * deprecated use {@code RhqFacade.getProxy(RhqManager.XXX.remote())} instead
     * @since 4.10
     */
    @Deprecated
    ContentManagerRemote getContentManager();

    /**
     * deprecated use {@code RhqFacade.getProxy(RhqManager.XXX.remote())} instead
     * @since 4.10
     */
    @Deprecated
    DataAccessManagerRemote getDataAccessManager();

    /**
     * deprecated use {@code RhqFacade.getProxy(RhqManager.XXX.remote())} instead
     * @since 4.10
     */
    @Deprecated
    DiscoveryBossRemote getDiscoveryBoss();

    /**
     * deprecated use {@code RhqFacade.getProxy(RhqManager.XXX.remote())} instead
     * @since 4.10
     */
    @Deprecated
    DriftManagerRemote getDriftManager();

    /**
     * deprecated use {@code RhqFacade.getProxy(RhqManager.XXX.remote())} instead
     * @since 4.10
     */
    @Deprecated
    EventManagerRemote getEventManager();

    /**
     * Kept for backwards compatibility but otherwise unused.
     * In RHQ prior to 4.5.0, the values in the map, i.e. the manager objects themselves both implemented the various
     * {@code *Remote} interfaces and contained methods with the modified signatures with the {@link Subject} parameter
     * removed.
     * <p />
     * Since RHQ 4.5.0 the returned objects no longer contain the modified method. If you want to obtain objects with
     * such methods (intended for use in scripted environments), use {@link #getScriptingAPI()} method instead.
     * @since 4.10
     */
    @Deprecated
    Map<RhqManagers, Object> getManagers();

    /**
     * deprecated use {@code RhqFacade.getProxy(RhqManager.XXX.remote())} instead
     * @since 4.10
     */
    @Deprecated
    MeasurementBaselineManagerRemote getMeasurementBaselineManager();

    /**
     * deprecated use {@code RhqFacade.getProxy(RhqManager.XXX.remote())} instead
     * @since 4.10
     */
    @Deprecated
    MeasurementDataManagerRemote getMeasurementDataManager();

    /**
     * deprecated use {@code RhqFacade.getProxy(RhqManager.XXX.remote())} instead
     * @since 4.10
     */
    @Deprecated
    MeasurementDefinitionManagerRemote getMeasurementDefinitionManager();

    /**
     * deprecated use {@code RhqFacade.getProxy(RhqManager.XXX.remote())} instead
     * @since 4.10
     */
    @Deprecated
    MeasurementScheduleManagerRemote getMeasurementScheduleManager();

    /**
     * deprecated use {@code RhqFacade.getProxy(RhqManager.XXX.remote())} instead
     * @since 4.10
     */
    @Deprecated
    OperationManagerRemote getOperationManager();

    /**
     * deprecated use {@code RhqFacade.getProxy(RhqManager.XXX.remote())} instead
     * @since 4.10
     */
    @Deprecated
    RemoteInstallManagerRemote getRemoteInstallManager();

    /**
     * deprecated use {@code RhqFacade.getProxy(RhqManager.XXX.remote())} instead
     * @since 4.10
     */
    @Deprecated
    RepoManagerRemote getRepoManager();

    /**
     * deprecated use {@code RhqFacade.getProxy(RhqManager.XXX.remote())} instead
     * @since 4.10
     */
    @Deprecated
    ResourceFactoryManagerRemote getResourceFactoryManager();

    /**
     * deprecated use {@code RhqFacade.getProxy(RhqManager.XXX.remote())} instead
     * @since 4.10
     */
    @Deprecated
    ResourceGroupManagerRemote getResourceGroupManager();

    /**
     * deprecated use {@code RhqFacade.getProxy(RhqManager.XXX.remote())} instead
     * @since 4.10
     */
    @Deprecated
    ResourceManagerRemote getResourceManager();

    /**
     * deprecated use {@code RhqFacade.getProxy(RhqManager.XXX.remote())} instead
     * @since 4.10
     */
    @Deprecated
    ResourceTypeManagerRemote getResourceTypeManager();

    /**
     * deprecated use {@code RhqFacade.getProxy(RhqManager.XXX.remote())} instead
     * @since 4.10
     */
    @Deprecated
    RoleManagerRemote getRoleManager();

    /**
     * deprecated use {@code RhqFacade.getProxy(RhqManager.XXX.remote())} instead
     * @since 4.10
     */
    @Deprecated
    SavedSearchManagerRemote getSavedSearchManager();

    /**
     * deprecated use {@code RhqFacade.getProxy(RhqManager.XXX.remote())} instead
     * @since 4.10
     */
    @Deprecated
    SubjectManagerRemote getSubjectManager();

    /**
     * deprecated use {@code RhqFacade.getProxy(RhqManager.XXX.remote())} instead
     * @since 4.10
     */
    @Deprecated
    SupportManagerRemote getSupportManager();

    /**
     * deprecated use {@code RhqFacade.getProxy(RhqManager.XXX.remote())} instead
     * @since 4.10
     */
    @Deprecated
    SynchronizationManagerRemote getSynchronizationManager();

    /**
     * deprecated use {@code RhqFacade.getProxy(RhqManager.XXX.remote())} instead
     * @since 4.10
     */
    @Deprecated
    SystemManagerRemote getSystemManager();

    /**
     * deprecated use {@code RhqFacade.getProxy(RhqManager.XXX.remote())} instead
     * @since 4.10
     */
    @Deprecated
    TagManagerRemote getTagManager();
}
