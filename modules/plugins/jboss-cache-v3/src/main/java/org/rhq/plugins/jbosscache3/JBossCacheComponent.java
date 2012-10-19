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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.jbossas5.ProfileServiceComponent;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnection;

/**
 * 
 * @author Filip Drabek
 * 
 */
public class JBossCacheComponent implements
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

	}

	public void stop() {
		return;
	}

	public AvailabilityType getAvailability() {
		return parentComp.getAvailability();
	}

	public ProfileServiceConnection getConnection() {
		return parentComp.getConnection();
	}

	public EmsConnection getEmsConnection() {
		return parentComp.getEmsConnection();
	}
}
