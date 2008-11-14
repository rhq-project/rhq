 /*
  * Jopr Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.plugins.jbossas5.factory.ProfileServiceFactory;
import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.deployers.spi.management.deploy.ProgressListener;
import org.jboss.deployers.spi.management.deploy.ProgressEvent;
import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.deployers.spi.management.deploy.DeploymentProgress;
import org.jboss.managed.api.ManagedDeployment;

/**
 * Component class for deployable resources like ear/war/jar/sar.
 *
 * @author Mark Spritzler
 * @author Ian Springer
 */
public class DeploymentResourceComponent
        extends ContentDeploymentComponent
        implements ResourceComponent, MeasurementFacet, DeleteResourceFacet, ProgressListener {
    static final String DEPLOYMENT_NAME_PROPERTY = "deploymentName";

    private static final String CUSTOM_PATH_TRAIT = "custom.path";
    private static final String CUSTOM_EXPLODED_TRAIT = "custom.exploded";

    private final Log log = LogFactory.getLog(this.getClass());

    private ResourceContext resourceContext;
    private File deploymentFile;

    // ----------- ResourceComponent implementation ------------
    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;
        this.deploymentFile = getDeploymentFile(); 
    }

    public void stop() {
        this.resourceContext = null;
    }

    public AvailabilityType getAvailability() {
        // TODO (ips, 11/10/08): Verify this is the correct way to check availablity.
        try {
            return (getManagedDeployment() != null) ? AvailabilityType.UP : AvailabilityType.DOWN;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ------------ MeasurementFacet Implementation ------------
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {
        for (MeasurementScheduleRequest request : requests) {
             String metricName = request.getName();
             if (metricName.equals(CUSTOM_PATH_TRAIT)) {
                 MeasurementDataTrait trait = new MeasurementDataTrait(request, this.deploymentFile.getPath());
                 report.addData(trait);
             }
             else if (metricName.equals(CUSTOM_EXPLODED_TRAIT)) {
                 boolean exploded = this.deploymentFile.isDirectory();
                 MeasurementDataTrait trait = new MeasurementDataTrait(request, (exploded) ? "yes" : "no");
                 report.addData(trait);
             }
        }
    }

    // ------------ DeleteResourceFacet implementation -------------
    public void deleteResource() throws Exception {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String deploymentName = pluginConfig.getSimple(DEPLOYMENT_NAME_PROPERTY).getStringValue();
        log.debug("Undeploying deployment [" + deploymentName + "]...");
        DeploymentManager deploymentManager = ProfileServiceFactory.getDeploymentManager();
        DeploymentProgress deploymentProgress = deploymentManager.undeploy(
                ManagedDeployment.DeploymentPhase.APPLICATION, deploymentName);
        deploymentProgress.run();

        //if (!this.deploymentFile.exists())
        //    throw new Exception("Cannot find deployment file [" + this.deploymentFile + "] to delete.");
        //log.debug("Deleting deployment file [" + this.deploymentFile + "]...");
        //FileUtils.purge(this.deploymentFile, true);
    }

    private File getDeploymentFile() throws MalformedURLException {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        // e.g.: vfszip:/C:/opt/jboss-5.0.0.GA/server/default/deploy/foo.war
        String deploymentName = pluginConfig.getSimple(DEPLOYMENT_NAME_PROPERTY).getStringValue();
        URL vfsURL = new URL(deploymentName);
        String path = vfsURL.getPath();
        while (path.charAt(0) == '/')
            path = path.substring(1);
        File file = new File(path);
        return file;
    }

    // ------------ ProgressListener implementation -------------
    public void progressEvent(ProgressEvent event) {
        log.debug(event);
    }
    
    // -------------------------------------------------------------
    private ManagedDeployment getManagedDeployment() throws Exception {
        String resourceKey = this.resourceContext.getResourceKey();
        ManagementView managementView = ProfileServiceFactory.getCurrentProfileView();
        return managementView.getDeployment(resourceKey, ManagedDeployment.DeploymentPhase.APPLICATION);
    }
}
