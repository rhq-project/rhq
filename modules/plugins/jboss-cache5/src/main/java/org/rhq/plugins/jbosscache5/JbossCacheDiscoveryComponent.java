package org.rhq.plugins.jbosscache5;

import java.util.ArrayList;
import java.util.HashMap;
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
import org.rhq.plugins.jbosscache5.JbossCacheComponent;
import org.rhq.plugins.jmx.ObjectNameQueryUtility;

public class JBossCacheDiscoveryComponent implements
		ResourceDiscoveryComponent<ProfileServiceComponent> {

	private ResourceDiscoveryContext<ProfileServiceComponent> context;
	private Configuration defaultConfig;
	private String service;
	private String configName;
	private String jmxName;
	private String domain;

	public Set<DiscoveredResourceDetails> discoverResources(
			ResourceDiscoveryContext<ProfileServiceComponent> context)
			throws InvalidPluginConfigurationException, Exception {

		ProfileServiceComponent parentComponent = context
				.getParentResourceComponent();

		this.context = context;
		this.defaultConfig = context.getDefaultPluginConfiguration();

		List<Configuration> configurations = context.getPluginConfigurations();

		EmsConnection connection = parentComponent.getEmsConnection();

		Set<DiscoveredResourceDetails> resources = new HashSet<DiscoveredResourceDetails>();

		if (configurations.isEmpty())
			configurations.add(defaultConfig);

		for (Configuration config : configurations) {

			ObjectNameQueryUtility queryUtility = new ObjectNameQueryUtility(
					buildSearchString(config));
			List<EmsBean> cacheBeans = connection.queryBeans(queryUtility
					.getTranslatedQuery());

			HashMap<String, List<EmsBean>> map = new HashMap<String, List<EmsBean>>();

			for (int i = 0; i < cacheBeans.size(); i++) {
				EmsBean bean = cacheBeans.get(i);
				Map<String, String> nameMap = bean.getBeanName()
						.getKeyProperties();

				if (nameMap.containsKey(JbossCacheComponent.CACHE_CONFIG_NAME)) {
					String configName = nameMap
							.get(JbossCacheComponent.CACHE_CONFIG_NAME);

					if (!map.containsKey(configName))
						map.put(configName, new ArrayList<EmsBean>());

					map.get(configName).add(bean);
				}
			}

			for (String key : map.keySet()) {
				Configuration conf = new Configuration();

				conf.put(new PropertySimple(
						JbossCacheComponent.CACHE_DOMAIN_NAME, domain));
				conf.put(new PropertySimple(
						JbossCacheComponent.CACHE_SERVICE_NAME, service));
				conf
						.put(new PropertySimple(
								JbossCacheComponent.CACHE_CONFIG_NAME,
								"config=" + key));

				ResourceType resourceType = context.getResourceType();
				resources.add(new DiscoveredResourceDetails(resourceType, key,
						key, "", "Jboss Cache", conf, null));

			}
		}
		return resources;
	}

	private String buildSearchString(Configuration config) {

		service = getValue(config, JbossCacheComponent.CACHE_SERVICE_NAME);
		configName = getValue(config, JbossCacheComponent.CACHE_CONFIG_NAME);
		jmxName = getValue(config, JbossCacheComponent.CACHE_JMX_NAME);
		domain = getValue(config, JbossCacheComponent.CACHE_DOMAIN_NAME);

		return domain + ":" + configName + "," + service + "," + jmxName;

	}

	private String getValue(Configuration config, String name) {

		if (config.get(name) != null)
			return config.getSimple(name).getStringValue();
		else
			return defaultConfig.getSimple(name).getStringValue();
	}
}
