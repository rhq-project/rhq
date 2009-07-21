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

package org.rhq.plugins.jbosscache3;

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
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jbossas5.ProfileServiceComponent;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnection;

/**
 * 
 * @author Filip Drabek
 * 
 */
public class JBossCacheComponent implements MeasurementFacet, OperationFacet,
		ProfileServiceComponent<ProfileServiceComponent> {

	private final Log log = LogFactory.getLog(this.getClass());

	public static String CACHE_SEARCH_STRING = "searchString";

	private ProfileServiceComponent parentComp;

	private String beanName;

	public void start(ResourceContext<ProfileServiceComponent> context)
			throws InvalidPluginConfigurationException, Exception {

		parentComp = context.getParentResourceComponent();

		Configuration configuration = context.getPluginConfiguration();

		if (configuration.get(JBossCacheComponent.CACHE_SEARCH_STRING) != null)
			beanName = configuration.getSimple(
					JBossCacheComponent.CACHE_SEARCH_STRING).getStringValue();
		else
			throw new InvalidPluginConfigurationException(
					"Invalid plugin configuration in JBossCache component.");

		EmsConnection connection = getEmsConnection();

		EmsBean emsBean = connection.getBean(beanName);
		if (emsBean == null) {
			connection.refresh();
			emsBean = connection.getBean(beanName);
			if (emsBean == null) {
				log.error("EmsBean " + beanName + " was not found.");
				throw new InvalidPluginConfigurationException(
						"Invalid plugin configuration in JBossCache component.");
			}
		}

		log.debug("JBoss Cache " + beanName + " was loaded.");
	}

	public void stop() {
		return;
	}

	public AvailabilityType getAvailability() {
		try {
			EmsConnection connection = parentComp.getEmsConnection();

			if (connection == null)
				return AvailabilityType.DOWN;

			EmsBean emsBean = connection.getBean(beanName);
			boolean up = emsBean.isRegistered();

			if (up == false) {
				// in some buggy situations, a remote server might tell us an
				// MBean isn't registered but it really is.
				// see JBPAPP-2031 for more
				String emsBeanName = emsBean.getBeanName().getCanonicalName();
				int size = emsBean.getConnectionProvider()
						.getExistingConnection().queryBeans(emsBeanName).size();
				up = (size == 1);
			}

			return up ? AvailabilityType.UP : AvailabilityType.DOWN;
		} catch (Exception e) {
			if (log.isDebugEnabled())
				log.debug("Can not determine availability for " + beanName
						+ ": " + e);
			return AvailabilityType.DOWN;
		}
	}

	public ProfileServiceConnection getConnection() {
		return parentComp.getConnection();
	}

	public EmsConnection getEmsConnection() {
		return parentComp.getEmsConnection();
	}

	public OperationResult invokeOperation(String name, Configuration parameters)
			throws InterruptedException, Exception {

		OperationResult result = null;

		try {
			EmsBean detailComponent = getEmsConnection().getBean(beanName);

			EmsOperation operation = detailComponent.getOperation(name);

			Object obj = operation.invoke(new Object[] {});

			if (obj != null) {
				result = new OperationResult();
				result.getComplexResults().put(
						new PropertySimple("output", String.valueOf(obj)));
			}
		} catch (Exception e) {
			log.error("Failure to invoke operation " + name + " on bean "
					+ beanName, e);
		}
		return result;
	}

	public void getValues(MeasurementReport report,
			Set<MeasurementScheduleRequest> metrics) throws Exception {

		EmsBean cacheBean = getEmsConnection().getBean(beanName);

		for (MeasurementScheduleRequest request : metrics) {
			String metricName = request.getName();
			try {

				EmsAttribute atribute = cacheBean.getAttribute(metricName);

				Object value = atribute.getValue();

				if (value != null) {
					if (request.getDataType() == DataType.MEASUREMENT) {
						Double number = ((Number) value).doubleValue();
						report.addData(new MeasurementDataNumeric(request,
								number));
					} else if (request.getDataType() == DataType.TRAIT)
						report.addData(new MeasurementDataTrait(request, value
								.toString()));

				}
			} catch (Exception e) {
				log.error(" Failure to collect measurement data for metric "
						+ metricName + " from bean "
						+ cacheBean.getBeanName().toString(), e);
			}
		}
	}
}
