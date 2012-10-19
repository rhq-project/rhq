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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.mc4j.ems.connection.bean.operation.EmsOperation;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jbossas.util.DeploymentUtility;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.ObjectNameQueryUtility;

/**
 * Get statistic for JBossCache instances
 * @author Heiko W. Rupp
 *
 */
public class JBossCacheComponent implements ResourceComponent<JMXComponent>, MeasurementFacet, OperationFacet,
    ConfigurationFacet {

    private final static Log log = LogFactory.getLog(JBossCacheComponent.class);
    private String baseObjectName;
    JBossCacheSubsystemComponent parentServer;
    boolean isTreeCache = false;
    List<EmsBean> interceptors = new ArrayList<EmsBean>();
    ResourceContext context;

    public void start(ResourceContext context) throws Exception {

        PropertySimple objectName = context.getPluginConfiguration().getSimple("objectName");
        baseObjectName = objectName.getStringValue();
        this.context = context;

        PropertySimple tcProp = context.getPluginConfiguration().getSimple("isTreeCache");
        if (tcProp==null || tcProp.getBooleanValue()==null)
            throw new InvalidPluginConfigurationException("Cache flavour not provided");
        else
            isTreeCache = tcProp.getBooleanValue();

        parentServer = (JBossCacheSubsystemComponent) context.getParentResourceComponent();

        String query = baseObjectName + ",";
        if (isTreeCache)
            query += "tree";
        query += "cache-interceptor=%name%";
        ObjectNameQueryUtility util = new ObjectNameQueryUtility(query);
        query = util.getTranslatedQuery();
        EmsConnection connection = parentServer.getEmsConnection();
        if (connection != null)
            interceptors = connection.queryBeans(query);

    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {

        for (MeasurementScheduleRequest metric : requests) {
            String name = metric.getName();
            if (log.isDebugEnabled())
                log.debug("Trying to get metric " + name);

            int pos = name.indexOf(":");
            if (pos > -1) {
                String bean;
                bean = baseObjectName;
                if (pos > 0) {
                    bean += ",";
                    if (isTreeCache)
                        bean += "tree";
                    bean += "cache-interceptor=";
                    bean += name.substring(0, pos);
                }
                String attr = name.substring(pos + 1);

                EmsConnection conn = parentServer.getEmsConnection();
                EmsBean eBean = conn.getBean(bean);
                if (eBean != null) {
                    List<String> attrs = new ArrayList<String>();
                    eBean.refreshAttributes(attrs); // only refresh selecte attrs, as there might be non-serializable ones
                    EmsAttribute eAttr = eBean.getAttribute(attr);
                    if (metric.getDataType() == DataType.MEASUREMENT) {
                        Double val = ((Number) (eAttr.getValue())).doubleValue();

                        MeasurementDataNumeric ret = new MeasurementDataNumeric(metric, val);
                        report.addData(ret);
                    } else if (metric.getDataType() == DataType.TRAIT) {
                        MeasurementDataTrait ret = new MeasurementDataTrait(metric, String.valueOf(eAttr.getValue()));
                        report.addData(ret);
                    } else
                        log.warn("Unknown data type " + metric);
                } else if (log.isDebugEnabled())
                    log.debug("Bean " + bean + " not found ");

            } else
                log.warn("illegal metric " + metric + " skipping ..");
        }
    }

    public AvailabilityType getAvailability() {

        try {
            EmsConnection connection = parentServer.getEmsConnection();
            if (connection == null)
                return AvailabilityType.DOWN;

            boolean up = connection.getBean(baseObjectName).isRegistered();
            return up ? AvailabilityType.UP : AvailabilityType.DOWN;
        } catch (Exception e) {
            if (log.isDebugEnabled())
                log.debug("Can not determine availability for " + baseObjectName + ": " + e.getMessage());
            return AvailabilityType.DOWN;
        }
    }

    public void stop() {
        // TODO Auto-generated method stub

    }

    public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException,
        Exception {

        OperationResult result = null;
        if ("resetStatistics".equals(name)) {
            for (EmsBean bean : interceptors) {
                EmsOperation ops = bean.getOperation("resetStatistics");
                if (ops != null) // base bean has no resetStatistics
                    ops.invoke(new Object[] {});
            }
            result = null; // no result

        } else if ("listAssociatedMBeans".equals(name)) {
            StringBuilder sb = new StringBuilder();
            for (EmsBean bean : interceptors) {
                sb.append(bean.getBeanName().getCanonicalName());
                sb.append(" ");
            }
            result = new OperationResult(sb.toString());
        }

        return result;
    }

    /**
     * Load the configuration from the actual resource
     */
    public Configuration loadResourceConfiguration() throws Exception {

        File file = DeploymentUtility.getDescriptorFile(parentServer.getEmsConnection(), context.getResourceKey());
        if (file==null) {
            log.warn("Can not find the deployment descriptor for this cache ");
            return null;
        }
        try {
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(file);

            // Get the root element
            Element root = doc.getRootElement();

            // First look for the right mbean of *our* cache - the file may contain more than one
            Configuration config = new Configuration();
            for (Object mbeanObj : root.getChildren("mbean")) {
                if (mbeanObj instanceof Element) {
                    Element mbean = (Element) mbeanObj;
                    // normalize the content of 'name'
                    String nameAttrib = mbean.getAttributeValue("name");
                    try {
                        ObjectName on = new ObjectName(nameAttrib);
                        nameAttrib = on.getCanonicalName();
                    } catch (MalformedObjectNameException e) {
                        log.warn("Can't canonicalize " + nameAttrib);
                    }
                    if (nameAttrib.equals(context.getResourceKey())) {
                        // found ours, let the fun begin
                        fillAttributesInConfig(mbean, config);
                        Attribute code = mbean.getAttribute("code");
                        PropertySimple flavour = new PropertySimple();
                        flavour.setName("Flavour");
                        if (code.getValue().contains("Tree")) {
                            flavour.setStringValue("treecache");
                        } else
                            flavour.setStringValue("cache");
                        config.put(flavour);
                    }
                }
            }

            return config;
        } catch (IOException e) {
            log.error("IO error occurred while reading file: " + file, e);
        } catch (JDOMException e) {
            log.error("Parsing error occurred while reading file: " + file, e);
        }

        return null;
    }

    EmsConnection getEmsConnection() {
        return parentServer.getEmsConnection();
    }

    /**
     * Fill all the &lt;attribute name="XXX"&gt; elements found under mbean into the passed config.
     * @param mbean The &gt;mbean&lt; element that builds the root of the JBossCache config
     * @param config The configuration object to fill stuff in
     */
    private void fillAttributesInConfig(Element mbean, Configuration config) {

        List children = mbean.getChildren("attribute");
        for (Object childObj : children) {
            if (childObj instanceof Element) {
                Element child = (Element) childObj;
                String name = child.getAttributeValue("name");
                String value = child.getText();
                PropertySimple ps = new PropertySimple(name, value);
                config.put(ps);
            }
        }
    }

    /**
     * Write an updated version of the configuration to the resource
     */
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {

        Configuration newOne = report.getConfiguration();
        String mbeanName = context.getResourceKey();
        File file = DeploymentUtility.getDescriptorFile(parentServer.getEmsConnection(), mbeanName);

        if (file == null) {
            report.setStatus(ConfigurationUpdateStatus.FAILURE);
            report.setErrorMessage("Failed to determine the deployment descriptor file for mbean '" + mbeanName + "'.");
            return;
        }
        
        CacheConfigurationHelper helper = new CacheConfigurationHelper();
        try {
            helper.writeConfig(file, newOne, mbeanName, true);
            report.setStatus(ConfigurationUpdateStatus.SUCCESS);
        } catch (Exception e) {
            log.error(e); // TODO do more?
            report.setStatus(ConfigurationUpdateStatus.FAILURE);
            report.setErrorMessageFromThrowable(e);
        }
    }

}
