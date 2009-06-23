package org.rhq.plugins.jbosscache5;

import java.util.HashSet;
import java.util.List;
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

public class JBossCacheDiscoveryComponent implements
		ResourceDiscoveryComponent<ProfileServiceComponent> {

	private ResourceDiscoveryContext<ProfileServiceComponent> context;
	private Configuration defaultConfig;

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
					getValue(config, JbossCacheComponent.CACHE_SEARCH_STRING));

			List<EmsBean> cacheBeans = connection.queryBeans(queryUtility
					.getTranslatedQuery());

			HashSet<String> names = new HashSet<String>();

			for (int i = 0; i < cacheBeans.size(); i++) {
				EmsBean bean = cacheBeans.get(i);

				String beanName = bean.getBeanName().toString();
				beanName = beanName.substring(0, beanName
						.indexOf(",jmx-resource"));

				if (!names.contains(beanName)) {
					names.add(beanName);
				}
			}

			for (String key : names) {
				Configuration conf = new Configuration();

				conf.put(new PropertySimple(
						JbossCacheComponent.CACHE_SEARCH_STRING, key));

				ResourceType resourceType = context.getResourceType();
				resources.add(new DiscoveredResourceDetails(resourceType, key,
						key, "", "Jboss Cache", conf, null));

			}
		}
		return resources;
	}

	private String getValue(Configuration config, String name) {

		if (config.get(name) != null)
			return config.getSimple(name).getStringValue();
		else
			return defaultConfig.getSimple(name).getStringValue();
	}

}
