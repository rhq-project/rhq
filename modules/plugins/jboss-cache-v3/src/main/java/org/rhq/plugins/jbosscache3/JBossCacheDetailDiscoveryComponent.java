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
import java.util.Map;
import java.util.Set;

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
import org.rhq.plugins.jmx.ObjectNameQueryUtility;

/**
 * 
 * @author Filip Drabek
 * 
 */
public class JBossCacheDetailDiscoveryComponent implements
		ResourceDiscoveryComponent<ProfileServiceComponent> {

	private ProfileServiceComponent parentComponent;
	public static String CACHE_JMX_NAME = "jmx-resource";

	public Set<DiscoveredResourceDetails> discoverResources(
			ResourceDiscoveryContext<ProfileServiceComponent> context)
			throws InvalidPluginConfigurationException, Exception {

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

		ResourceType resourceType = context.getResourceType();

		String jmxName;

		for (Configuration config : configurations) {
			jmxName = config.getSimple(CACHE_JMX_NAME).getStringValue();

			ObjectNameQueryUtility queryUtility = new ObjectNameQueryUtility(
					beanName + "," + jmxName);

			List<EmsBean> cacheBeans = connection.queryBeans(queryUtility
					.getTranslatedQuery());

            for (EmsBean bean : cacheBeans) {
                Map<String, String> nameMap = bean.getBeanName()
                        .getKeyProperties();

                String name = null;

                if (nameMap.containsKey(CACHE_JMX_NAME))
                    name = nameMap.get(CACHE_JMX_NAME);

                if (name != null) {
                    Configuration conf = new Configuration();
                    conf.put(new PropertySimple(
                            JBossCacheDetailComponent.CACHE_DETAIL_BEAN_NAME,
                            bean.getBeanName()));
                    resources.add(new DiscoveredResourceDetails(resourceType,
                            bean.getBeanName().toString(), name, "",
                            "JBoss Cache", conf, null));
                }
            }
		}
		return resources;

	}
}