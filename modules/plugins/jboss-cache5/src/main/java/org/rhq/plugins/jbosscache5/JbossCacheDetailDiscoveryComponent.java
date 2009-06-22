package org.rhq.plugins.jbosscache5;

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

public class JBossCacheDetailDiscoveryComponent implements
		ResourceDiscoveryComponent<ProfileServiceComponent> {

	private ProfileServiceComponent parentComponent;
	public static String CACHE_JMX_NAME = "jmx-resource";

	public Set<DiscoveredResourceDetails> discoverResources(
			ResourceDiscoveryContext<ProfileServiceComponent> context)
			throws InvalidPluginConfigurationException, Exception {

		parentComponent = context.getParentResourceComponent();

		Configuration ccon = context.getParentResourceContext()
				.getPluginConfiguration();

		String configName = ccon.getSimple(
				JbossCacheComponent.CACHE_CONFIG_NAME).getStringValue();
		String domain = ccon.getSimple(JbossCacheComponent.CACHE_DOMAIN_NAME)
				.getStringValue();
		String service = ccon.getSimple(JbossCacheComponent.CACHE_SERVICE_NAME)
				.getStringValue();
		String jmxName;

		Configuration defaultConfig = context.getDefaultPluginConfiguration();

		List<Configuration> configurations = context.getPluginConfigurations();

		if (configurations.isEmpty())
			configurations.add(defaultConfig);

		EmsConnection connection = parentComponent.getEmsConnection();
		Set<DiscoveredResourceDetails> resources = new HashSet<DiscoveredResourceDetails>();

		ResourceType resourceType = context.getResourceType();

		for (Configuration config : configurations) {
			jmxName = config.getSimple(CACHE_JMX_NAME).getStringValue();

			ObjectNameQueryUtility queryUtility = new ObjectNameQueryUtility(
					domain + ":" + configName + "," + service + "," + jmxName);

			List<EmsBean> cacheBeans = connection.queryBeans(queryUtility
					.getTranslatedQuery());

			for (int i = 0; i < cacheBeans.size(); i++) {
				EmsBean bean = cacheBeans.get(i);

				Map<String, String> nameMap = bean.getBeanName()
						.getKeyProperties();

				String name = null;

				if (nameMap.containsKey(CACHE_JMX_NAME))
					name = nameMap.get(CACHE_JMX_NAME);

				if (name != null) {
					Configuration conf = new Configuration();
					conf.put(new PropertySimple(
							JbossCacheDetailComponent.CACHE_DETAIL_BEAN_NAME,
							bean.getBeanName()));
					resources.add(new DiscoveredResourceDetails(resourceType,
							bean.getBeanName().toString(), name, "",
							"Jboss Cache", conf, null));
				}
			}
		}
		return resources;

	}
}