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

import javax.management.MalformedObjectNameException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.jbossas.util.DeploymentUtility;
import org.rhq.plugins.jbossas.util.FileNameUtility;
import org.rhq.plugins.jbossas.util.JBossMBeanUtility;
import org.rhq.plugins.jbossas.util.JMSConfigurationEditor;

/**
 * Functionality around the JBossMQ subsystem
 *
 * @author Mark Spritzler
 * @author Heiko W. Rupp
 */
// TODO: Rename this class JBossMQComponent
public class JMSComponent extends AbstractMessagingComponent implements ConfigurationFacet, DeleteResourceFacet {

    public static final String TOPIC_MBEAN_NAME = "jboss.mq.destination:service=Topic";
    public static final String QUEUE_MBEAN_NAME = "jboss.mq.destination:service=Queue";

    private static final Log LOG = LogFactory.getLog(JMSComponent.class);

    @Override
    public AvailabilityType getAvailability() {
        return (JBossMBeanUtility.isStarted(getEmsBean(), this.resourceContext)) ? AvailabilityType.UP
            : AvailabilityType.DOWN;
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {
        super.getValues(report, requests, "jboss.mq:service=");
    }

    @Override
    public void start(ResourceContext<JBossASServerComponent> context) {
        super.start(context, new JMSConfigurationEditor(context.getResourceType().getName()));
    }

    public CreateResourceReport createResource(CreateResourceReport report) {

        JBossASServerComponent parentResourceComponent = resourceContext.getParentResourceComponent();

        String resourceTypeName = report.getResourceType().getName();

        String objectNamePreString;
        boolean isTopic = false;
        if (resourceTypeName.contains("Topic")) {
            objectNamePreString = TOPIC_MBEAN_NAME;
            isTopic = true;
        } else {
            objectNamePreString = QUEUE_MBEAN_NAME;
        }

        Configuration config = report.getResourceConfiguration();

        String name = config.getSimple("MBeanName").getStringValue();
        PropertySimple nameTemplateProp = report.getPluginConfiguration().getSimple("nameTemplate");
        String rName = nameTemplateProp.getStringValue();
        rName = rName.replace("{name}", name);

        EmsConnection connection = parentResourceComponent.getEmsConnection();

        if (DeploymentUtility.isDuplicateJndiName(connection, objectNamePreString, name)) {
            report.setStatus(CreateResourceStatus.FAILURE);
            report.setErrorMessage("Duplicate JNDI Name, a resource with that name already exists");
            return report;
        }
        PropertySimple pluginNameProperty = new PropertySimple("name", rName);
        resourceContext.getPluginConfiguration().put(pluginNameProperty);

        File deployDir = new File(parentResourceComponent.getConfigurationPath() + "/deploy");
        File deploymentFile = new File(deployDir, FileNameUtility.formatFileName(name) + "-jms-service.xml");

        xmlEditor = new JMSConfigurationEditor(resourceTypeName);
        xmlEditor.updateConfiguration(deploymentFile, name, report);
        try {
            parentResourceComponent.deployFile(deploymentFile);
        }
        catch (Exception e) {
            JBossASServerComponent.setErrorOnCreateResourceReport(report, e.getLocalizedMessage(), e);
            return report;
        }

        // This key needs to be the same as the object name string used in discovery, defined in the plugin descriptor
        //   jboss.mq.destination:service=<Queue|Topic,name=%name%
        String serviceString = (isTopic ? "Topic" : "Queue");
        
        String objectName = "jboss.mq.destination:name=" + name + ",service=" + serviceString;
        try {
            // IMPORTANT: The object name must be canonicalized so it matches the resource key that
            //            MBeanResourceDiscoveryComponent uses, which is the canonical object name.
            objectName = getCanonicalName(objectName);
            report.setResourceKey(objectName);
        } catch (MalformedObjectNameException e) {
            log.warn("Invalid key [" + objectName + "]: " + e.getMessage());
            return report;
        }
        report.setResourceName(rName);
        
        try {
            Thread.sleep(5000L);
        } catch (InterruptedException e) {
            log.info("Sleep after datasource create interrupted", e);
        }

        return report;
    }
}