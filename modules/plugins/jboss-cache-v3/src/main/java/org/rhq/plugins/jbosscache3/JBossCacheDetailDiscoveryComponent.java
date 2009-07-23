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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jbossas5.ProfileServiceComponent;

/**
 * 
 * @author Filip Drabek
 * 
 */
public class JBossCacheDetailDiscoveryComponent implements
		ResourceDiscoveryComponent<ProfileServiceComponent> {

	private ProfileServiceComponent parentComponent;
	public static String CACHE_JMX_NAME = "jmx-resource";
	private final Log log = LogFactory.getLog(this.getClass());

	public Set<DiscoveredResourceDetails> discoverResources(
			ResourceDiscoveryContext<ProfileServiceComponent> context)
			throws InvalidPluginConfigurationException, Exception {

		ResourceType resourceType = context.getResourceType();

		log.trace("Discovering " + resourceType.getName() + " Resources...");

		parentComponent = context.getParentResourceComponent();

		Configuration parentConfiguration = context.getParentResourceContext()
				.getPluginConfiguration();

		String beanName;

		if (parentConfiguration.get(JBossCacheComponent.CACHE_SEARCH_STRING) != null)
			beanName = parentConfiguration.getSimple(
					JBossCacheComponent.CACHE_SEARCH_STRING).getStringValue();
		else
			throw new InvalidPluginConfigurationException(
					"Invalid plugin configuration in JBossCache component.");

		Configuration defaultConfig = context.getDefaultPluginConfiguration();

		List<Configuration> configurations = context.getPluginConfigurations();

		if (configurations.isEmpty())
			configurations.add(defaultConfig);

		EmsConnection connection = parentComponent.getEmsConnection();
		Set<DiscoveredResourceDetails> resources = new HashSet<DiscoveredResourceDetails>();

		String jmxName;

		for (Configuration config : configurations) {
			jmxName = config.getSimple(CACHE_JMX_NAME).getStringValue();

			beanName = (beanName + (jmxName.equals("") ? "" : "," + jmxName));

			EmsBean emsBean = connection.getBean(beanName);
			if (emsBean == null) {
				connection.refresh();
				emsBean = connection.getBean(beanName);

			}

			if (emsBean != null) {
				Configuration conf = new Configuration();
				conf.put(new PropertySimple(
						JBossCacheDetailComponent.CACHE_DETAIL_BEAN_NAME,
						beanName));
				resources.add(new DiscoveredResourceDetails(resourceType,
						beanName, resourceType.getName(), "", "JBoss Cache",
						conf, null));

			}
		}

		log.trace("Discovered " + resources.size() + " "
				+ resourceType.getName() + " Resources.");
		return resources;

	}
}