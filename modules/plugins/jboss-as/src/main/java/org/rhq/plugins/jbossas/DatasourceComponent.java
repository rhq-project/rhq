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

import java.io.File;
import java.util.Set;

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
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jbossas.util.DatasourceConfigurationEditor;
import org.rhq.plugins.jbossas.util.JBossMBeanUtility;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * @author Greg Hinkle
 */
public class DatasourceComponent extends MBeanResourceComponent<JBossASServerComponent> implements ConfigurationFacet,
    DeleteResourceFacet, OperationFacet
{
    private static final String CONNECTION_POOL_OBJECT_NAME = "jboss.jca:name=%NAME%,service=ManagedConnectionPool";

    private String name;
    private String connectionPoolBeanName;
    private EmsBean connectionPoolBean;

    @Override
    public AvailabilityType getAvailability()
    {
        return (JBossMBeanUtility.isStarted(getEmsBean(), getResourceContext())) ? AvailabilityType.UP
            : AvailabilityType.DOWN;
    }

    @Override
    public void start(ResourceContext<JBossASServerComponent> resourceContext)
    {
        super.start(resourceContext);
        this.name = getResourceContext().getPluginConfiguration().getSimpleValue("name", null);
        this.connectionPoolBeanName = CONNECTION_POOL_OBJECT_NAME.replace("%NAME%", this.name);
        this.connectionPoolBean = getConnectionPoolBean();
    }

    @Override
    public Configuration loadResourceConfiguration()
    {
        File deploymentFile = getResourceContext().getParentResourceComponent().getDeploymentFilePath(
            getResourceContext().getResourceKey());

        if (!deploymentFile.exists()) {
            log.warn( "Deployment file " + deploymentFile + " doesn't exist for Datasource ["
            + getResourceContext().getResourceKey() + "].");
            return null;
        }

        return DatasourceConfigurationEditor.loadDatasource(deploymentFile, this.name);
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report)
    {
        JBossASServerComponent parentComponent = getResourceContext().getParentResourceComponent();

        File deploymentFile = parentComponent.getDeploymentFilePath(getResourceContext().getResourceKey());

        if (deploymentFile == null) {
            report.setErrorMessage("Parent Resource is currently down - unable to complete update of Datasource ["
                + this.name + "].");
            report.setStatus(ConfigurationUpdateStatus.FAILURE);
        } else {
            if (!deploymentFile.exists()) {
                deploymentFile = new File(parentComponent.getConfigurationPath(), this.name + "-ds.xml");
            }

            DatasourceConfigurationEditor.updateDatasource(deploymentFile, this.name, report);
        }
    }

    public void deleteResource() throws Exception {
        JBossASServerComponent jbossASComponent = getResourceContext().getParentResourceComponent();
        File deploymentFile = jbossASComponent.getDeploymentFilePath(getResourceContext().getResourceKey());
        assert deploymentFile.exists() : "Deployment file " + deploymentFile + " doesn't exist for Datasource ["
            + getResourceContext().getResourceKey() + "].";
        DatasourceConfigurationEditor.deleteDataSource(deploymentFile, this.name);
        jbossASComponent.redeployFile(deploymentFile);
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests)
    {
        super.getValues(report, requests, getConnectionPoolBean());
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception
    {
        return super.invokeOperation(name, parameters, getConnectionPoolBean());
    }

    private EmsBean getConnectionPoolBean() {
        if (this.connectionPoolBean == null)
            this.connectionPoolBean = loadBean(this.connectionPoolBeanName);
        return this.connectionPoolBean;
    }
}