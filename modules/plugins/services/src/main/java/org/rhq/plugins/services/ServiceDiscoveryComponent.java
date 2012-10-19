package org.rhq.plugins.services;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

public class ServiceDiscoveryComponent implements ResourceDiscoveryComponent<ServicesComponent> {
    public static final String SYS_V_SERVICE = "SysV";
    public static final String XINETD_SERVICE = "Xinetd";

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<ServicesComponent> context)
        throws InvalidPluginConfigurationException, Exception {
        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();
        ServicesComponent servicesComponent = context.getParentResourceComponent();
        List<String> services = servicesComponent.getSysVServices();
        for (String name : services) {
            Configuration pluginConfig = context.getDefaultPluginConfiguration();
            pluginConfig.put(new PropertySimple("name", name));
            pluginConfig.put(new PropertySimple("type", SYS_V_SERVICE));
            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(context.getResourceType(), name, name
                + " (Sys V)", null, "Service -  [" + name + "]", pluginConfig, null);
            details.add(detail);
        }
        services = servicesComponent.getXinetDServices();

        for (String name : services) {
            Configuration pluginConfig = context.getDefaultPluginConfiguration();
            pluginConfig.put(new PropertySimple("name", name));
            pluginConfig.put(new PropertySimple("type", XINETD_SERVICE));
            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(context.getResourceType(), name, name
                + " (XinetD)", null, "Service -  [" + name + "]", pluginConfig, null);
            details.add(detail);
        }
        return details;

    }

}
