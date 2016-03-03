package org.rhq.modules.plugins.wildfly10;

import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * Discovery component for special treatment of JVMs, which live below server=server-x for managed servers
 * @author Heiko W. Rupp
 */
public class JVMDiscoveryComponent extends SubsystemDiscovery {

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext<BaseComponent<?>> context) throws Exception {

        ResourceContext parentContext = context.getParentResourceContext();


        if (!parentContext.getResourceType().getName().equals("Managed Server"))
            return super.discoverResources(context);

        PropertySimple pathProp = parentContext.getPluginConfiguration().getSimple("path");
        String path = pathProp.getStringValue();
        path = path.replaceAll("server-config=","server=");
        path = path + ",core-service=platform-mbean";

        Configuration config = new Configuration();
        PropertySimple ps = new PropertySimple("path",path);
        config.getProperties().add(ps);

        DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                context.getResourceType(),
                path,
                path, // dname, todo
                null,
                context.getResourceType().getDescription(),
                config,
                null
        );

        Set<DiscoveredResourceDetails> discoveredResourceDetails = new HashSet<DiscoveredResourceDetails>();
        discoveredResourceDetails.add(detail);
        return discoveredResourceDetails;
    }
}
