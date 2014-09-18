/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.plugins.jbossas5;

import static org.rhq.plugins.jbossas5.AbstractManagedDeploymentDiscoveryComponent.getDeploymentNamesForType;
import static org.rhq.plugins.jbossas5.AbstractManagedDeploymentDiscoveryComponent.getManagementView;

import java.io.File;
import java.net.URI;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.deployers.spi.management.KnownDeploymentTypes;
import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.deployers.spi.management.deploy.DeploymentProgress;
import org.jboss.deployers.spi.management.deploy.DeploymentStatus;
import org.jboss.deployers.spi.management.deploy.ProgressEvent;
import org.jboss.deployers.spi.management.deploy.ProgressListener;
import org.jboss.managed.api.DeploymentState;
import org.jboss.managed.api.ManagedDeployment;
import org.jboss.profileservice.spi.NoSuchDeploymentException;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnection;
import org.rhq.plugins.jbossas5.util.DeploymentUtils;

/**
 * ResourceComponent for managing ManagedDeployments (EARs, WARs, etc.).
 *
 * @author Mark Spritzler
 * @author Ian Springer
 */
public abstract class AbstractManagedDeploymentComponent extends AbstractManagedComponent implements MeasurementFacet,
    OperationFacet, ProgressListener {

    private static final Log LOG = LogFactory.getLog(AbstractManagedDeploymentComponent.class);

    /**
     * @deprecated as of 4.13. No longer used
     */
    @Deprecated
    public static final String DEPLOYMENT_NAME_PROPERTY = "deploymentName";
    public static final String DEPLOYMENT_KEY_PROPERTY = "deploymentKey";
    public static final String DEPLOYMENT_TYPE_NAME_PROPERTY = "deploymentTypeName";
    public static final String EXTENSION_PROPERTY = "extension";

    private static final boolean IS_WINDOWS = (File.separatorChar == '\\');

    /**
     * The name of the ManagedDeployment (e.g.: vfszip:/C:/opt/jboss-6.0.0.Final/server/default/deploy/foo.war).
     * @deprecated as of 4.13. Use {@link #getDeploymentName()} instead.
     */
    @Deprecated
    protected volatile String deploymentName;

    /**
     * The type of the ManagedDeployment (e.g. war).
     */
    protected KnownDeploymentTypes deploymentType;

    /**
     * The absolute path of the deployment file (e.g.: C:/opt/jboss-6.0.0.Final/server/default/deploy/foo.war).
     */
    protected File deploymentFile;

    private String deploymentKey;

    // ----------- ResourceComponent Implementation ------------

    @Override
    public void start(ResourceContext<ProfileServiceComponent<?>> resourceContext) throws Exception {
        super.start(resourceContext);

        Configuration pluginConfig = getResourceContext().getPluginConfiguration();
        deploymentKey = pluginConfig.getSimpleValue(DEPLOYMENT_KEY_PROPERTY);
        deploymentFile = getDeploymentFile(deploymentKey);
        deploymentName = lookupDeploymentName();
        String deploymentTypeName = pluginConfig.getSimple(DEPLOYMENT_TYPE_NAME_PROPERTY).getStringValue();
        deploymentType = KnownDeploymentTypes.valueOf(deploymentTypeName);
        try {
            getManagedDeployment();
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.warn("Could not start deployment for [" + deploymentKey
                    + "] no longer exists. It may have been deleted from the filesystem external to RHQ.");
            }
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("Started ResourceComponent for " + getResourceDescription() + ", managing " + deploymentType
                + " deployment '" + deploymentKey + "' with path '" + deploymentFile + "'.");
        }
    }

    @Override
    public void stop() {
        super.stop();
        deploymentName = null;
        deploymentType = null;
        deploymentFile = null;
        deploymentKey = null;
    }

    private synchronized String lookupDeploymentName() {
        ManagementView managementView = getManagementView(getConnection());
        if (managementView == null) {
            return null;
        }
        Set<String> deploymentNames = getDeploymentNamesForType(managementView, getResourceContext().getResourceType());
        for (String deploymentName : deploymentNames) {
            if (deploymentKey.equals(URI.create(deploymentName).getPath())) {
                return deploymentName;
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Did not find deployment with key [" + deploymentKey + "]");
        }
        return null;
    }

    @Override
    public AvailabilityType getAvailability() {
        DeploymentState deploymentState;
        try {
            deploymentState = getManagedDeployment(false).getDeploymentState();
        } catch (NoSuchDeploymentException e) {
            LOG.warn(deploymentType + " deployment '" + deploymentKey + "' not found. Cause: "
                + e.getLocalizedMessage());
            deploymentName = null;
            return AvailabilityType.DOWN;
        } catch (Throwable t) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Could not get deployment state for " + deploymentType + " deployment '" + deploymentKey
                    + "', cause: ", t);
            }
            deploymentName = null;
            return AvailabilityType.DOWN;
        }

        if (deploymentState == DeploymentState.STARTED) {
            return AvailabilityType.UP;
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug(deploymentType + " deployment '" + deploymentKey + "' was not running, state was: "
                    + deploymentState);
            }
            return AvailabilityType.DOWN;
        }
    }

    // ------------ MeasurementFacet Implementation ------------

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) throws Exception {
        if (deploymentType == KnownDeploymentTypes.JavaEEWebApplication) {
            WarMeasurementFacetDelegate warMeasurementFacetDelegate = new WarMeasurementFacetDelegate(this);
            warMeasurementFacetDelegate.getValues(report, requests);
        }
    }

    // ------------ OperationFacet Implementation ------------

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        ProfileServiceConnection connection = getConnection();
        if (connection == null) {
            OperationResult result = new OperationResult();
            result.setErrorMessage("No profile service connection available");
            return result;
        }

        String deploymentName = getDeploymentName();

        if (deploymentName == null) {
            OperationResult result = new OperationResult();
            result.setErrorMessage("Did not find deployment with key [" + deploymentKey + "]");
            return result;
        }

        DeploymentManager deploymentManager = connection.getDeploymentManager();
        DeploymentProgress progress;
        if (name.equals("start")) {
            //FIXME: This is a workaround until JOPR-309 will be fixed.
            if (getAvailability() != AvailabilityType.UP) {
                progress = deploymentManager.start(deploymentName);
            } else {
                LOG.warn("Operation '" + name + "' on " + getResourceDescription()
                    + " failed because the Resource is already started.");
                OperationResult result = new OperationResult();
                result.setErrorMessage(deploymentFile.getName() + " is already started.");
                return result;
            }
        } else if (name.equals("stop")) {
            progress = deploymentManager.stop(deploymentName);
        } else if (name.equals("restart")) {
            progress = deploymentManager.stop(deploymentName);
            DeploymentUtils.run(progress);
            // Still try to start, even if stop fails (maybe the app wasn't running to begin with).
            progress = deploymentManager.start(deploymentName);
        } else {
            throw new UnsupportedOperationException(name);
        }
        DeploymentStatus status = DeploymentUtils.run(progress);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Operation '" + name + "' on " + getResourceDescription() + " returned status [" + status + "].");
        }
        if (status.isFailed()) {
            throw status.getFailure();
        }
        return new OperationResult();
    }

    // ------------ ProgressListener implementation -------------

    @Override
    public void progressEvent(ProgressEvent event) {
        LOG.debug(event);
    }

    // -------------------------------------------------------------

    public String getDeploymentName() {
        if (deploymentName == null) {
            deploymentName = lookupDeploymentName();
        }
        return deploymentName;
    }

    public KnownDeploymentTypes getDeploymentType() {
        return deploymentType;
    }

    public final String getDeploymentKey() {
        return deploymentKey;
    }

    protected ManagedDeployment getManagedDeployment() throws NoSuchDeploymentException {
        return getManagedDeployment(true);
    }

    protected ManagedDeployment getManagedDeployment(boolean forceLoad) throws NoSuchDeploymentException {
        ManagementView managementView = getConnection().getManagementView();
        if (forceLoad) {
            managementView.load();
        }
        String deploymentName = getDeploymentName();
        if (deploymentName == null) {
            throw new NoSuchDeploymentException("Did not find deployment with key [" + deploymentKey + "]");
        }
        return managementView.getDeployment(deploymentName);
    }

    private File getDeploymentFile(String deploymentKey) {
        // Under Windows, the deploymentKey will look like:
        // /C:/opt/jboss-6.0.0.Final/server/default/deploy/foo.ear/
        // Java considers the path with the leading slash to be valid and equivalent to the same path with the
        // leading slash removed, but the leading slash is unnecessary and ugly, so excise it.
        if (IS_WINDOWS && deploymentKey.charAt(0) == '/') {
            deploymentKey = deploymentKey.substring(1);
        }
        return new File(deploymentKey);
    }
}
