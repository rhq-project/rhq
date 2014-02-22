package org.rhq.modules.plugins.openshift;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.modules.plugins.jbossas7.StandaloneASComponent;

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
         * Discover the resource by checking for existence of /var/lib/openshift directory.
         * A discovered resource must have a unique key, that must
         * stay the same when the resource is discovered the next
         * time
         */

        ResourceComponent parent = discoveryContext.getParentResourceComponent();
        StandaloneASComponent parentComponent = (StandaloneASComponent) parent;
        File homeDir = parentComponent.getServerPluginConfiguration().getHomeDir();
        if (homeDir==null || !homeDir.exists()) {
            return Collections.EMPTY_SET;
        }
        String home = homeDir.getAbsolutePath();

        log.debug("Home is " + home);

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