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
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Map;
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
import org.jboss.managed.api.ManagedProperty;
import org.jboss.profileservice.spi.NoSuchDeploymentException;
import org.mc4j.ems.connection.EmsConnection;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jbossas5.util.DeploymentUtils;
import org.rhq.plugins.jbossas5.helper.JmxConnectionHelper;

/**
 * ResourceComponent for managing ManagedDeployments (EARs, WARs, etc.).
 *
 * @author Mark Spritzler
 * @author Ian Springer
 */
public abstract class AbstractManagedDeploymentComponent
        extends AbstractManagedComponent
        implements MeasurementFacet, OperationFacet, ProgressListener
{
    public static final String DEPLOYMENT_NAME_PROPERTY = "deploymentName";
    public static final String DEPLOYMENT_TYPE_NAME_PROPERTY = "deploymentTypeName";
    public static final String EXTENSION_PROPERTY = "extension";

    private static final boolean IS_WINDOWS = (File.separatorChar == '\\');

    private final Log log = LogFactory.getLog(this.getClass());


    /**
     * The name of the ManagedDeloyment (e.g.: vfszip:/C:/opt/jboss-5.0.0.GA/server/default/deploy/foo.war).
     */
    protected String deploymentName;

    /**
     * The type of the ManagedDeloyment.
     */
    protected KnownDeploymentTypes deploymentType;

    /**
     * The absolute path of the deployment file (e.g.: C:/opt/jboss-5.0.0.GA/server/default/deploy/foo.war).
     */
    protected File deploymentFile;

    // ----------- ResourceComponent Implementation ------------

    public void start(ResourceContext<ProfileServiceComponent> resourceContext) throws Exception
    {
        super.start(resourceContext);
        Configuration pluginConfig = getResourceContext().getPluginConfiguration();
        this.deploymentName = pluginConfig.getSimple(DEPLOYMENT_NAME_PROPERTY).getStringValue();
        this.deploymentFile = getDeploymentFile();
        String deploymentTypeName = pluginConfig.getSimple(DEPLOYMENT_TYPE_NAME_PROPERTY).getStringValue();
        this.deploymentType = KnownDeploymentTypes.valueOf(deploymentTypeName);
        log.trace("Started ResourceComponent for " + getResourceDescription() + ", managing " + this.deploymentType
                + " deployment '" + this.deploymentName + "' with path '" + this.deploymentFile + "'.");
    }

    public AvailabilityType getAvailability()
    {
        try
        {
            return (getManagedDeployment().getDeploymentState() == DeploymentState.STARTED) ? AvailabilityType.UP :
                    AvailabilityType.DOWN;
        }
        catch (NoSuchDeploymentException e)
        {
            log.warn(this.deploymentType + " deployment '" + this.deploymentName + "' not found. Cause: "
                    + e.getLocalizedMessage());
            return AvailabilityType.DOWN;
        }
    }

    // ------------ MeasurementFacet Implementation ------------

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests)
            throws Exception
    {
        if (this.deploymentType == KnownDeploymentTypes.JavaEEWebApplication)
        {
            WarMeasurementFacetDelegate warMeasurementFacetDelegate = new WarMeasurementFacetDelegate(this);
            warMeasurementFacetDelegate.getValues(report, requests);
        }
    }

    // TODO (ips): This method could be useful to other plugins, so move it to MeasurementReport.
    private static boolean isRequestInReport(MeasurementReport report, MeasurementScheduleRequest request)
    {
        if (request.getDataType() == DataType.MEASUREMENT)
        {
            for (MeasurementDataNumeric numericDatum : report.getNumericData())
            {
                if (numericDatum.getName().equals(request.getName()))
                    return true;
            }
            for (MeasurementDataTrait traitDatum : report.getTraitData())
            {
                if (traitDatum.getName().equals(request.getName()))
                    return true;
            }
            // TODO
            /*for (CallTimeData callTimeDatum : report.getCallTimeData()) {
                if (callTimeDatum.getName().equals(request.getName()))
                    return true;
            }*/
        }
        return false;
    }

    // ------------ OperationFacet Implementation ------------

    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception
    {
        DeploymentManager deploymentManager = getConnection().getDeploymentManager();
        DeploymentProgress progress;
        if (name.equals("start"))
        {
            progress = deploymentManager.start(this.deploymentName);
        }
        else if (name.equals("stop"))
        {
            progress = deploymentManager.stop(this.deploymentName);
        }
        else if (name.equals("restart"))
        {
            progress = deploymentManager.stop(this.deploymentName);
            DeploymentStatus stopStatus = DeploymentUtils.run(progress);
            // Still try to start, even if stop fails (maybe the app wasn't running to begin with).
            progress = deploymentManager.start(this.deploymentName);
        }
        else
        {
            throw new UnsupportedOperationException(name);
        }
        DeploymentStatus status = DeploymentUtils.run(progress);
        log.debug("Operation '" + name + "' on " + getResourceDescription() + " returned status [" + status
                + "].");
        if (status.isFailed()) {
            throw status.getFailure();
        }
        return new OperationResult();
    }

    // ------------ ProgressListener implementation -------------

    public void progressEvent(ProgressEvent event)
    {
        log.debug(event);
    }

    // ------------ AbstractManagedComponent implementation -------------

    protected Map<String, ManagedProperty> getManagedProperties() throws Exception
    {
        return getManagedDeployment().getProperties();
    }

    protected Log getLog()
    {
        return this.log;
    }

    protected void updateComponent() throws Exception
    {
        ManagementView managementView = getConnection().getManagementView();
        managementView.process();
    }

    // -------------------------------------------------------------

    protected ManagedDeployment getManagedDeployment() throws NoSuchDeploymentException
    {
        ManagementView managementView = getConnection().getManagementView();
        managementView.load();
        return managementView.getDeployment(this.deploymentName);
    }

    private File getDeploymentFile() throws MalformedURLException
    {
        // e.g.: vfszip:/C:/opt/jboss-5.0.0.GA/server/default/deploy/foo.war
        URI vfsURI = URI.create(this.deploymentName);
        // e.g.: foo.war
        String path = vfsURI.getPath();
        // Under Windows, the deployment name URL will look like:
        // vfszip:/C:/opt/jboss-5.1.0.CR1/server/default/deploy/eardeployment.ear/
        // so we need to trim the leading slash off the path portion. Java considers the version with the leading slash
        // to be a valid and equivalent path, but the leading slash is unnecessary and ugly, so we shall axe it.
        if (IS_WINDOWS && path.charAt(0) == '/')
            path = path.substring(1);
        return new File(path);
    }
}
