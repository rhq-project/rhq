/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
import org.rhq.plugins.jbossas5.util.DeploymentUtils;

/**
 * ResourceComponent for managing ManagedDeployments (EARs, WARs, etc.).
 *
 * @author Mark Spritzler
 * @author Ian Springer
 */
public abstract class AbstractManagedDeploymentComponent extends AbstractManagedComponent implements MeasurementFacet,
    OperationFacet, ProgressListener {
    public static final String DEPLOYMENT_NAME_PROPERTY = "deploymentName";
    public static final String DEPLOYMENT_TYPE_NAME_PROPERTY = "deploymentTypeName";
    public static final String EXTENSION_PROPERTY = "extension";

    private static final boolean IS_WINDOWS = (File.separatorChar == '\\');

    private final Log log = LogFactory.getLog(this.getClass());

    /**
     * The name of the ManagedDeployment (e.g.: vfszip:/C:/opt/jboss-5.0.0.GA/server/default/deploy/foo.war).
     */
    protected String deploymentName;

    /**
     * The type of the ManagedDeployment.
     */
    protected KnownDeploymentTypes deploymentType;

    /**
     * The absolute path of the deployment file (e.g.: C:/opt/jboss-5.0.0.GA/server/default/deploy/foo.war).
     */
    protected File deploymentFile;

    // ----------- ResourceComponent Implementation ------------

    public void start(ResourceContext<ProfileServiceComponent> resourceContext) throws Exception {
        super.start(resourceContext);
        Configuration pluginConfig = getResourceContext().getPluginConfiguration();
        this.deploymentName = pluginConfig.getSimple(DEPLOYMENT_NAME_PROPERTY).getStringValue();
        this.deploymentFile = getDeploymentFile();
        String deploymentTypeName = pluginConfig.getSimple(DEPLOYMENT_TYPE_NAME_PROPERTY).getStringValue();
        this.deploymentType = KnownDeploymentTypes.valueOf(deploymentTypeName);
        log.trace("Started ResourceComponent for " + getResourceDescription() + ", managing " + this.deploymentType
            + " deployment '" + this.deploymentName + "' with path '" + this.deploymentFile + "'.");

        try {
            getManagedDeployment();
        } catch (Exception e) {
            log
                .warn("The underlying file ["
                    + this.deploymentFile
                    + "] no longer exists. It may have been deleted from the filesystem external to Jopr. If you wish to remove this Resource from inventory, you may add &debug=true to the URL for the Browse Resources > Services page and then click the UNINVENTORY button next to this Resource");
        }
    }

    public AvailabilityType getAvailability() {
        DeploymentState deploymentState = null;
        try {
            deploymentState = getManagedDeployment().getDeploymentState();
        } catch (NoSuchDeploymentException e) {
            log.warn(this.deploymentType + " deployment '" + this.deploymentName + "' not found. Cause: "
                + e.getLocalizedMessage());
            return AvailabilityType.DOWN;
        } catch (Throwable t) {
            log.debug("Could not get deployment state for " + this.deploymentType + " deployment '" 
                    + this.deploymentName + "', cause: ", t);
            return AvailabilityType.DOWN;
        }

        if (deploymentState == DeploymentState.STARTED) {
            return AvailabilityType.UP;
        } else {
            log.debug(this.deploymentType + " deployment '" + this.deploymentName + 
                    "' was not running, state was: " + deploymentState); 
            return AvailabilityType.DOWN;
        }
    }

    // ------------ MeasurementFacet Implementation ------------

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) throws Exception {
        if (this.deploymentType == KnownDeploymentTypes.JavaEEWebApplication) {
            WarMeasurementFacetDelegate warMeasurementFacetDelegate = new WarMeasurementFacetDelegate(this);
            warMeasurementFacetDelegate.getValues(report, requests);
        }
    }

    // ------------ OperationFacet Implementation ------------

    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        DeploymentManager deploymentManager = getConnection().getDeploymentManager();
        DeploymentProgress progress;
        if (name.equals("start")) {
            //FIXME: This is a workaround until JOPR-309 will be fixed.
            if (getAvailability() != AvailabilityType.UP) {
                progress = deploymentManager.start(this.deploymentName);
            } else {
                log.warn("Operation '" + name + "' on " + getResourceDescription()
                    + " failed because the Resource is already started.");
                OperationResult result = new OperationResult();
                result.setErrorMessage(this.deploymentFile.getName() + " is already started.");
                return result;
            }
        } else if (name.equals("stop")) {
            progress = deploymentManager.stop(this.deploymentName);
        } else if (name.equals("restart")) {
            progress = deploymentManager.stop(this.deploymentName);
            DeploymentStatus stopStatus = DeploymentUtils.run(progress);
            // Still try to start, even if stop fails (maybe the app wasn't running to begin with).
            progress = deploymentManager.start(this.deploymentName);
        } else {
            throw new UnsupportedOperationException(name);
        }
        DeploymentStatus status = DeploymentUtils.run(progress);
        log.debug("Operation '" + name + "' on " + getResourceDescription() + " returned status [" + status + "].");
        if (status.isFailed()) {
            throw status.getFailure();
        }
        return new OperationResult();
    }

    // ------------ ProgressListener implementation -------------

    public void progressEvent(ProgressEvent event) {
        log.debug(event);
    }

    // -------------------------------------------------------------

    public String getDeploymentName() {
        return deploymentName;
    }

    public KnownDeploymentTypes getDeploymentType() {
        return deploymentType;
    }

    protected ManagedDeployment getManagedDeployment() throws NoSuchDeploymentException {
        ManagementView managementView = getConnection().getManagementView();
        managementView.load();
        return managementView.getDeployment(this.deploymentName);
    }

    private File getDeploymentFile() {
        // e.g.: vfszip:/C:/opt/jboss-5.0.0.GA/server/default/deploy/foo.war
        URI vfsURI = URI.create(this.deploymentName);
        // e.g.: foo.war
        String path = vfsURI.getPath();
        // Under Windows, the deployment name URL will look like:
        // vfszip:/C:/opt/jboss-5.1.0.CR1/server/default/deploy/foo.ear/
        // and the path portion will look like: /C:/opt/jboss-5.1.0.CR1/server/default/deploy/foo.ear/
        // Java considers the path with the leading slash to be valid and equivalent to the same path with the
        // leading slash removed, but the leading slash is unnecessary and ugly, so excise it.
        if (IS_WINDOWS && path.charAt(0) == '/')
            path = path.substring(1);
        return new File(path);
    }
}
