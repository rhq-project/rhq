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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.mc4j.ems.connection.bean.operation.EmsOperation;

import org.rhq.core.domain.configuration.Configuration;
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

    public void start(ResourceContext context) throws InvalidPluginConfigurationException, Exception {
        PropertySimple objectName = context.getPluginConfiguration().getSimple("objectName");
        baseObjectName = objectName.getStringValue();

        PropertySimple tcProp = context.getPluginConfiguration().getSimple("isTreeCache");
        isTreeCache = tcProp.getBooleanValue();

        parentServer = (JBossCacheSubsystemComponent) context.getParentResourceComponent();

        String query = baseObjectName + ",";
        if (isTreeCache)
            query += "tree";
        query += "cache-interceptor=%name%";
        ObjectNameQueryUtility util = new ObjectNameQueryUtility(query);
        query = util.getTranslatedQuery();
        interceptors = parentServer.getEmsConnection().queryBeans(query);

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
            boolean up = parentServer.getEmsConnection().getBean(baseObjectName).isRegistered();
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

        Configuration config = new Configuration();
        PropertySimple ps = new PropertySimple("Flavour", "cache");
        config.getProperties().add(ps);

        // TODO Auto-generated method stub
        return config;
    }

    /**
     * Write an updated version of the configuration to the resource
     */
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {

        Configuration newOne = report.getConfiguration();
        // TODO Auto-generated method stub

    }

}
