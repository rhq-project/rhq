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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.jbossas.util.DeploymentUtility;
import org.rhq.plugins.jbossas.util.XMLConfigurationEditor;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * Common base class for Messaging related stuff
 * @author Heiko W. Rupp
 */
public abstract class AbstractMessagingComponent extends MBeanResourceComponent<JBossASServerComponent> implements
    CreateChildResourceFacet {

    private static final Pattern PROPERTY_PATTERN = Pattern.compile("^(.*:.*):(.*)$");

    protected static final String PLUGIN_CONFIG_NAME_PROP = "name";
    private static final String MBEAN_NAME_PROP = "MBeanName";

    XMLConfigurationEditor xmlEditor;
    ResourceType resourceType;
    protected String name;

    protected Log LOG = null;

    public void start(ResourceContext<JBossASServerComponent> resourceContext, XMLConfigurationEditor editor) {
        super.start(resourceContext);
        this.resourceType = resourceContext.getResourceType();
        xmlEditor = editor;
        this.name = resourceContext.getPluginConfiguration().getSimpleValue(PLUGIN_CONFIG_NAME_PROP, null);
    }

    protected void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests, String pattern) {

        Set<MeasurementScheduleRequest> others = new HashSet<MeasurementScheduleRequest>();

        for (MeasurementScheduleRequest request : requests) {

            // Handle stuff for the generic Messaging MBeans ourselves. Pass the remainder
            // to our parent later.
            if (request.getName().startsWith(pattern)) {
                Matcher m = PROPERTY_PATTERN.matcher(request.getName());
                if (m.matches() && (m.group(1) != null)) {
                    EmsBean eBean = getEmsConnection().getBean(m.group(1));

                    List<String> attributes = new ArrayList<String>(1);
                    attributes.add(m.group(2));
                    eBean.refreshAttributes(attributes);
                    EmsAttribute emsAtt = eBean.getAttribute(m.group(2));
                    Object value = emsAtt.getValue();
                    if ((request.getDataType() == DataType.MEASUREMENT) && (value instanceof Number)) {
                        report.addData(new MeasurementDataNumeric(request, ((Number) value).doubleValue()));
                    } else if (request.getDataType() == DataType.TRAIT) {
                        String displayValue = null;
                        if ((value != null) && value.getClass().isArray()) {
                            displayValue = Arrays.deepToString((Object[]) value);
                        } else {
                            displayValue = String.valueOf(value);
                        }

                        report.addData(new MeasurementDataTrait(request, displayValue));
                    }
                }
            } else
                others.add(request);
        }

        super.getValues(report, others);
    }

    @Override
    public Configuration loadResourceConfiguration() {

        String resourceKey = getResourceContext().getResourceKey();

        JBossASServerComponent jasco = getOurJBossASComponent();
        File deploymentFile = jasco.getDeploymentFilePath(resourceKey);
        Configuration loadedConfiguration = xmlEditor.loadConfiguration(deploymentFile, this.name);
        if (loadedConfiguration==null)
            return null;
        String boundJNDIName = DeploymentUtility.getJndiNameBinding(getEmsBean());
        loadedConfiguration.put(new PropertySimple("JNDIBinding", boundJNDIName));
        return loadedConfiguration;
    }

    private JBossASServerComponent getOurJBossASComponent() {

        ResourceComponent parent = getResourceContext().getParentResourceComponent();
        JBossASServerComponent jasco;
        if (parent instanceof JMSComponent) {
            jasco = ((JMSComponent) parent).getResourceContext().getParentResourceComponent();
        } else if (parent instanceof JBossMessagingComponent) {
            jasco = ((JBossMessagingComponent) parent).getResourceContext().getParentResourceComponent();
        } else if (parent instanceof JBossASServerComponent) {
            jasco = (JBossASServerComponent) parent;
        } else
            jasco = new JBossASServerComponent(); // TODO fix me
        return jasco;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {

        JBossASServerComponent jasco = getOurJBossASComponent();
        File deploymentFile = jasco.getDeploymentFilePath(getResourceContext().getResourceKey());

        // Check to see if the user is changing the name of this JMS Topic/Queue
        String mBeanName = report.getConfiguration().getSimpleValue(MBEAN_NAME_PROP, null);
        boolean mBeanNameChanged = false;
        if (!mBeanName.equals(this.name)) {
            LOG
                .info("The MBEan Name for this Topic/Queue has been changed. This change will appear in the <mbean> tag for"
                    + "this Topic/Queue.");

            // User has changed the name, so update the plugin configuration.
            PropertySimple nameProp = getResourceContext().getPluginConfiguration().getSimple(PLUGIN_CONFIG_NAME_PROP);
            nameProp.setStringValue(mBeanName);
            mBeanNameChanged = true;
        }

        if ((deploymentFile == null) || !deploymentFile.exists()) {
            deploymentFile = new File(getResourceContext().getParentResourceComponent().getConfigurationPath(), name
                + ".xml");
        }

        xmlEditor.updateConfiguration(deploymentFile, this.name, report);
        if (mBeanNameChanged) {
            this.name = mBeanName;
        }
    }

    public void deleteResource() throws Exception {
        String resourceKey = getResourceContext().getResourceKey();
        JBossASServerComponent parent = getOurJBossASComponent();
        File deploymentFile = parent.getDeploymentFilePath(resourceKey);
        assert deploymentFile.exists() : "Deployment file " + deploymentFile + " doesn't exist for resource "
            + resourceKey;
        xmlEditor.deleteComponent(deploymentFile, this.name);
        parent.redeployFile(deploymentFile);
    }

    /**
     * Returns the canonical version of the passed ObjectName
     * @param objectName a valid {@link ObjectName}
     * @return an {@link ObjectName} in its canonical form
     * @throws MalformedObjectNameException if the passed {@link ObjectName} is invalid
     */
    public String getCanonicalName(String objectName) throws MalformedObjectNameException {
        ObjectName on = new ObjectName(objectName);
        return on.getCanonicalName();
    }

}
