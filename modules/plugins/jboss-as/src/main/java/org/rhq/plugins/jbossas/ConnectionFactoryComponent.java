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
package org.rhq.plugins.jbossas;

import org.mc4j.ems.connection.bean.EmsBean;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.jbossas.util.ConnectionFactoryConfigurationEditor;
import org.rhq.plugins.jbossas.util.JBossMBeanUtility;
import org.rhq.plugins.jmx.MBeanResourceComponent;

import java.io.File;
import java.util.Set;

/**
 * @author Mark Spritzler
 */
public class ConnectionFactoryComponent extends MBeanResourceComponent<JBossASServerComponent> implements ConfigurationFacet,
    DeleteResourceFacet {
    private static final String CONNECTION_POOL_OBJECT_NAME = "jboss.jca:name=%NAME%,service=ManagedConnectionPool";

    private EmsBean connectionPoolBean;
    private String name;

    @Override
    public AvailabilityType getAvailability() {
        return (JBossMBeanUtility.isStarted(getEmsBean(), this.resourceContext)) ? AvailabilityType.UP
            : AvailabilityType.DOWN;
    }

    @Override
    public void start(ResourceContext<JBossASServerComponent> resourceContext) {
        super.start(resourceContext);

        this.name = this.resourceContext.getPluginConfiguration().getSimpleValue("name", null);

        String connectionPoolBeanName = CONNECTION_POOL_OBJECT_NAME.replace("%NAME%", this.name);
        this.connectionPoolBean = getEmsConnection().getBean(connectionPoolBeanName);
    }

    @Override
    public Configuration loadResourceConfiguration() {
        File deploymentFile = this.resourceContext.getParentResourceComponent().getDeploymentFilePath(
            this.resourceContext.getResourceKey());

        assert deploymentFile.exists() : "Deployment file " + deploymentFile + " doesn't exist for resource "
            + resourceContext.getResourceKey();

        return ConnectionFactoryConfigurationEditor.loadConnectionFactory(deploymentFile, this.name);
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        JBossASServerComponent parentComponent = this.resourceContext.getParentResourceComponent();

        File deploymentFile = parentComponent.getDeploymentFilePath(this.resourceContext.getResourceKey());

        if (deploymentFile == null) {
            report.setErrorMessage("Parent resource is currently down. Unable to complete update of connection factory "
                + this.name);
            report.setStatus(ConfigurationUpdateStatus.FAILURE);
        } else {
            if (!deploymentFile.exists()) {
                deploymentFile = new File(parentComponent.getConfigurationPath(), this.name + "-ds.xml");
            }

            ConnectionFactoryConfigurationEditor.updateConnectionFactory(deploymentFile, this.name, report);
        }
    }

    public void deleteResource() throws Exception {
        JBossASServerComponent jbossASComponent = this.resourceContext.getParentResourceComponent();
        File deploymentFile = jbossASComponent.getDeploymentFilePath(this.resourceContext.getResourceKey());
        assert deploymentFile.exists() : "Deployment file " + deploymentFile + " doesn't exist for resource "
            + resourceContext.getResourceKey();
        ConnectionFactoryConfigurationEditor.deleteConnectionFactory(deploymentFile, this.name);
        jbossASComponent.redeployFile(deploymentFile);
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {
        super.getValues(report, requests, connectionPoolBean);
    }
}