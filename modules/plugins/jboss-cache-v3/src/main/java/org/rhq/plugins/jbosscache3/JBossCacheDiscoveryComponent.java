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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.ObjectName;

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
public class JBossCacheDiscoveryComponent implements
		ResourceDiscoveryComponent<ProfileServiceComponent> {
	private String REGEX = "(,|:)jmx-resource=[^,]*(,|\\z)";
	private static String[] RESOURCE_NAME_KEY_PROPS = { "cluster", "config" };
    private static final String DEFAULT_RESOURCE_DESCRIPTION = "JBoss Cache";

    public Set<DiscoveredResourceDetails> discoverResources(
			ResourceDiscoveryContext<ProfileServiceComponent> context)
			throws InvalidPluginConfigurationException, Exception {

		ProfileServiceComponent parentComponent = context
				.getParentResourceComponent();

		Configuration defaultPluginConfig = context.getDefaultPluginConfiguration();

		EmsConnection connection = parentComponent.getEmsConnection();

		Set<DiscoveredResourceDetails> resources = new HashSet<DiscoveredResourceDetails>();

		Pattern pattern = Pattern.compile(REGEX);

        ObjectNameQueryUtility queryUtility = new ObjectNameQueryUtility(
                defaultPluginConfig.getSimple(JBossCacheComponent.CACHE_SEARCH_STRING).getStringValue());

        List<EmsBean> cacheBeans = connection.queryBeans(queryUtility
                .getTranslatedQuery());

        HashSet<String> beanNames = new HashSet<String>();

        for (EmsBean bean : cacheBeans) {
            String beanName = bean.getBeanName().toString();

            Matcher m = pattern.matcher(beanName);
            if (m.find()) {
                beanName = m.replaceFirst(m.group(2).equals(",") ? m
                        .group(1) : "");

                if (!beanNames.contains(beanName)) {
                    beanNames.add(beanName);
                }
            }
        }

        for (String beanName : beanNames) {
            Configuration pluginConfig = context.getDefaultPluginConfiguration();

            pluginConfig.put(new PropertySimple(
                    JBossCacheComponent.CACHE_SEARCH_STRING, beanName));

            String resourceName = beanName;
            ObjectName objName = new ObjectName(beanName);
            for (String objectName : RESOURCE_NAME_KEY_PROPS) {
                if (objName.getKeyProperty(objectName) != null) {
                    resourceName = objName.getKeyProperty(objectName);
                }
            }

            ResourceType resourceType = context.getResourceType();
            resources.add(new DiscoveredResourceDetails(resourceType, beanName,
                    resourceName, null, DEFAULT_RESOURCE_DESCRIPTION, pluginConfig, null));
        }

		return resources;
	}
}
