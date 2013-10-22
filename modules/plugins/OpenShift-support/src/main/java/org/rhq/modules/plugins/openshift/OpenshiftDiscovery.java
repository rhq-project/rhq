package org.rhq.modules.plugins.openshift;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.modules.plugins.jbossas7.BaseServerComponent;
import org.rhq.modules.plugins.jbossas7.StandaloneASComponent;
import org.rhq.modules.plugins.jbossas7.StandaloneASDiscovery;

/**
 * Discovery class
 */
@SuppressWarnings("unused")
public class OpenshiftDiscovery implements ResourceDiscoveryComponent<StandaloneASComponent<?>>

{

    private final Log log = LogFactory.getLog(this.getClass());

    /**
     * Run the auto-discovery
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext discoveryContext) throws Exception {
        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();

        /**
         * TODO : do your discovery here
         * A discovered resource must have a unique key, that must
         * stay the same when the resource is discovered the next
         * time
         */

        ResourceComponent parent = discoveryContext.getParentResourceComponent();
        StandaloneASComponent parentComponent = (StandaloneASComponent) parent;
        String home = parentComponent.getServerPluginConfiguration().getHomeDir().getAbsolutePath();

        log.info("Home is " + home);

        //  only discover if the home path contains /var/lib/openshift
        if (home.contains("/var/lib/openshift")) {

            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                discoveryContext.getResourceType(), // ResourceType
                "openshift",
                "Openshift",
                "1.0",
                "OpenShift",
                discoveryContext.getDefaultPluginConfiguration(),
                null
            );


            // Add to return values
            discoveredResources.add(detail);
            log.info("Discovered new ... OpenShift at  " + home);
        }

        return discoveredResources;

        }

}