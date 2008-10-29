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
package org.rhq.plugins.jbosscache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.jbossas.JBossASServerComponent;
import org.rhq.plugins.jbossas.util.FileNameUtility;
import org.rhq.plugins.jmx.JMXComponent;

/**
 * Manage the Jboss cache instances found
 *
 * @author Heiko W. Rupp
 */
public class JBossCacheSubsystemComponent<T extends JMXComponent> implements ResourceComponent<T>,
    CreateChildResourceFacet {

    private final Log log = LogFactory.getLog(JBossCacheSubsystemComponent.class);

    ResourceContext<T> ctx;

    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    public void stop() {
        // TODO Auto-generated method stub

    }

    public CreateResourceReport createResource(CreateResourceReport report) {

        JBossASServerComponent parentResourceComponent = (JBossASServerComponent) ctx.getParentResourceComponent();
        Configuration config = report.getResourceConfiguration();
        String name = report.getUserSpecifiedResourceName();
        //       String name = config.getSimple("MBeanName").getStringValue();
        //       PropertySimple nameTemplateProp = report.getPluginConfiguration().getSimple("nameTemplate");
        //       String rName = nameTemplateProp.getStringValue();
        //noinspection ConstantConditions
        //       rName = rName.replace("{name}", name);

        // TODO check for duplcicate name/mbean

        //       PropertySimple pluginNameProperty = new PropertySimple("name", rName);
        //       ctx.getPluginConfiguration().put(pluginNameProperty);

        File deployDir = new File(parentResourceComponent.getConfigurationPath() + "/deploy");
        File deploymentFile = new File(deployDir, FileNameUtility.formatFileName(name) + "-cache-service.xml");

        FileOutputStream fos = null;
        String flavour = config.getSimple("Flavour").getStringValue();
        boolean isTc = false;
        if (flavour != null && flavour.startsWith("tree"))
            isTc = true;

        try {
            fos = new FileOutputStream(deploymentFile);
            fos.write("<server>\n".getBytes());
            fos.write(("  <mbean name=\"jboss.cache:name=" + name + "\"\n").getBytes());
            if (isTc)
                fos.write("      code=\"org.jboss.cache.TreeCache\">\n".getBytes());
            else
                fos.write("      code=\"org.jboss.cache.PojoCache\">\n".getBytes());
            fos.write("    <depends>jboss:service=TransactionManager</depends>".getBytes());

            for (String propName : config.getSimpleProperties().keySet()) {

                if (propName.equals("Flavour")) // Skip this, this is to set the mbean class
                    continue;

                String propVal = config.getSimple(propName).getStringValue();
                fos.write(("    <attribute name=\"" + propName + "\">").getBytes());
                if (propName.equals("ClusterConfig"))
                    fos.write("\n".getBytes());
                fos.write(propVal.getBytes());
                if (propName.equals("ClusterConfig"))
                    fos.write("\n".getBytes());
                fos.write(("</attribute>\n").getBytes());
            }
            fos.write("  </mbean>\n".getBytes());
            fos.write("</server>\n".getBytes());
            fos.flush();

        } catch (IOException ioe) {
            ioe.printStackTrace(); // TODO remove later
            report.setErrorMessage(ioe.getLocalizedMessage());
            report.setException(ioe);
            report.setStatus(CreateResourceStatus.FAILURE);
            return report;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                    log.error("Error closing output stream: ", e);
                }
            }
        }

        String objectName = "jboss.cache:name=" + name;
        if (isTc)
            objectName += ",treecache-interceptor=CacheMgmtInterceptor";
        else
            objectName += ",cache-interceptor=CacheMgmtInterceptor";

        try {
            ObjectName on = new ObjectName(objectName);
            objectName = on.getCanonicalName();
            report.setResourceKey(objectName);
        } catch (MalformedObjectNameException e) {
            log.warn("Invalid key [" + objectName + "]: " + e.getMessage());
            return report;
        }
        report.setResourceName(name); // TODO ok? or better objectName?

        //       try {
        //           parentResourceComponent.deployFile(deploymentFile);
        //       }
        //       catch (Exception e) {
        //           JBossASServerComponent.setErrorOnCreateResourceReport(report, e.getLocalizedMessage(), e);
        //           return report;
        //       }   

        report.setStatus(CreateResourceStatus.SUCCESS);
        return report;
    }

    public EmsConnection getEmsConnection() {
        return ctx.getParentResourceComponent().getEmsConnection();
    }

    public void start(ResourceContext<T> context) throws InvalidPluginConfigurationException, Exception {
        // TODO Auto-generated method stub
        ctx = context;
    }

}
