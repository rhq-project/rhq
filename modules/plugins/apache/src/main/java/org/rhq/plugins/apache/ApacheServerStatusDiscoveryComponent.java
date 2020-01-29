package org.rhq.plugins.apache;

import java.util.LinkedHashSet;
import java.util.Set;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.util.StringUtil;

/**
 * Apache Server Status component for Apache server-status handler.
 * 
 * @author Jeremie Lagarde
 */
public class ApacheServerStatusDiscoveryComponent implements ResourceDiscoveryComponent<ApacheLocationComponent> {

    public static final String STATUS_HANDLER = "server-status";

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent#discoverResources(org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext)
     */
    public Set<DiscoveredResourceDetails> discoverResources(
        ResourceDiscoveryContext<ApacheLocationComponent> context)
        throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> discoveredResources = new LinkedHashSet<DiscoveredResourceDetails>();
        ApacheLocationComponent location = context.getParentResourceComponent();
        String handler = ((PropertySimple)location.resourceContext.getPluginConfiguration().get(ApacheLocationComponent.HANDLER_PROP)).getStringValue();
        if(StringUtil.isNotBlank(handler) && STATUS_HANDLER.equals(handler)) { 
            discoveredResources.add(new DiscoveredResourceDetails(context.getResourceType(), STATUS_HANDLER, STATUS_HANDLER, null, null, null, null));
        }
        return discoveredResources;
    }
}