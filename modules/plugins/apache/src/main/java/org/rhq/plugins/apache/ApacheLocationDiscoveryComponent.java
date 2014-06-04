package org.rhq.plugins.apache;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.util.StringUtil;
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;
import org.rhq.plugins.apache.util.AugeasNodeValueUtil;

/**
 * Location component for Apache discovery directives.
 * 
 * @author Jeremie Lagarde
 */
public class ApacheLocationDiscoveryComponent implements ResourceDiscoveryComponent<ApacheVirtualHostServiceComponent> {

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent#discoverResources(org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext)
     */
    public static final String LOCATION_DIRECTIVE = "<Location";
    private static final String HANDLER_DIRECTIVE = "SetHandler";

    public Set<DiscoveredResourceDetails> discoverResources(
        ResourceDiscoveryContext<ApacheVirtualHostServiceComponent> context)
        throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> discoveredResources = new LinkedHashSet<DiscoveredResourceDetails>();
        ApacheDirective vhost = context.getParentResourceComponent().getDirective(true);
        ApacheDirectiveTree tree = new ApacheDirectiveTree();
        tree.setRootNode(vhost);
        final List<ApacheDirective> allDirectories = tree.search(LOCATION_DIRECTIVE);
        ResourceType resourceType = context.getResourceType();
        for (ApacheDirective apacheDirective : allDirectories) {
            String locationParam;
            boolean isRegexp;
            List<String> params = apacheDirective.getValues();
            if (params.size() > 1 && StringUtil.isNotBlank(params.get(1))) {
                locationParam = params.get(1);
                isRegexp = true;
            } else {
                locationParam = params.get(0);
                isRegexp = false;
            }

            Configuration pluginConfiguration = context.getDefaultPluginConfiguration();
            pluginConfiguration.put(new PropertySimple(ApacheLocationComponent.REGEXP_PROP, isRegexp));
            
            List<ApacheDirective> handlers = apacheDirective.getChildByName(HANDLER_DIRECTIVE);
            if(handlers.size() == 1) {
                pluginConfiguration.put(new PropertySimple(ApacheLocationComponent.HANDLER_PROP, handlers.get(0).getValues().get(0)));
            }

            String resourceName = AugeasNodeValueUtil.unescape(locationParam);
            if(!isRegexp && context.getParentResourceComponent().getURL() !=null) {
                pluginConfiguration.put(new PropertySimple(ApacheLocationComponent.URL_PROP, context.getParentResourceComponent().getURL() + resourceName));
            }

            int index = 1;
            for (DiscoveredResourceDetails detail : discoveredResources) {
                if (detail.getResourceName().endsWith(resourceName))
                    index++;
            }
            StringBuilder resourceKey = new StringBuilder();
            resourceKey.append(apacheDirective.getName()).append("|").append(locationParam).append("|").append(index)
                .append(";");
            discoveredResources.add(new DiscoveredResourceDetails(resourceType, resourceKey.toString(), resourceName,
                null, null, pluginConfiguration, null));

        }
        return discoveredResources;
    }
}