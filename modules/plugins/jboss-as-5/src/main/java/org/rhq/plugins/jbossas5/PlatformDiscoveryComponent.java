package org.rhq.plugins.jbossas5;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ManagedDeployment;
import org.jboss.profileservice.spi.NoSuchDeploymentException;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnection;
import org.rhq.plugins.jbossas5.util.ManagedComponentUtils;

public class PlatformDiscoveryComponent implements
		ResourceDiscoveryComponent<ApplicationServerComponent> {

	private final Log log = LogFactory.getLog(this.getClass());
	public static String PLATFORM_DEPLOYMENT_NAME = "deployment";
	public static String PLATFORM_COMPONENT_NAME = "componentName";

	public Set discoverResources(
			ResourceDiscoveryContext<ApplicationServerComponent> context)
			throws InvalidPluginConfigurationException, Exception {

		log.trace("Discovering " + context.getResourceType().getName()
				+ " Resources...");

		Set<DiscoveredResourceDetails> resources = new HashSet<DiscoveredResourceDetails>();
		ResourceType resourceType = context.getResourceType();

		Set<ManagedComponent> components = new HashSet<ManagedComponent>();

		Configuration config = context.getDefaultPluginConfiguration();

		String deployName = config.getSimple(
				PlatformDiscoveryComponent.PLATFORM_DEPLOYMENT_NAME)
				.getStringValue();

		String componentName = config.getSimple(
				PlatformDiscoveryComponent.PLATFORM_COMPONENT_NAME)
				.getStringValue();

		components.addAll(addManagedDeploymentComponent(deployName,
				componentName, context));

		for (ManagedComponent component : components) {

			ComponentType type = component.getType();
			String displayName;

			try {
				displayName = (String) ManagedComponentUtils
						.getSimplePropertyValue(component, "name");
			} catch (IllegalStateException il) {
				displayName = component.getName();
			}

			try {
				String resourceName = (String) component.getName();

				String resourceKey = component.getType() + "|"
						+ type.getSubtype() + "|" + resourceName;

				String version = "";
				Configuration pluginConfig = new Configuration();
				pluginConfig.put(new PropertySimple(PLATFORM_DEPLOYMENT_NAME,
						deployName));
				pluginConfig.put(new PropertySimple(PLATFORM_COMPONENT_NAME,
						resourceName));

				resources.add(new DiscoveredResourceDetails(context
						.getResourceType(), resourceKey, displayName, version,
						resourceType.getDescription(), pluginConfig, null));
			} catch (Exception e) {
				log.error("failed discovering the platform Mbeans", e);
			}
		}

		return resources;
	}

	private Set<ManagedComponent> addManagedDeploymentComponent(
			String deployName, String componentName,
			ResourceDiscoveryContext<ApplicationServerComponent> context)
			throws NoSuchDeploymentException {

		ProfileServiceConnection connection = context
				.getParentResourceComponent().getConnection();

		ManagementView managementView = connection.getManagementView();

		Set<ManagedComponent> components = new HashSet<ManagedComponent>();
		ManagedDeployment deploy = managementView.getDeployment(deployName);
		if (deploy != null) {
			if (componentName == null || componentName.equals("*"))
				components.addAll(deploy.getComponents().values());
			else {
				for (ManagedComponent comp : deploy.getComponents().values()) {
					if (comp.getName().equals(componentName))
						components.add(comp);
				}
			}
		}

		return components;
	}

}
