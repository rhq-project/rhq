package org.rhq.plugins.irc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

import java.util.HashSet;
import java.util.Set;
import java.util.List;


/**
 * Discovery class
 * @author Greg Hinkle
 */
public class IRCChannelDiscoveryComponent implements ResourceDiscoveryComponent<IRCServerComponent> {

   private final Log log = LogFactory.getLog(this.getClass());

    public static final String CONFIG_CHANNEL = "channelName";

    /**
     * Run the discovery
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext discoveryContext) throws Exception {

        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();

        List<Configuration> contextPluginConfigurations = discoveryContext.getPluginConfigurations();
        for (Configuration config : contextPluginConfigurations) {

            String channel = config.getSimple(CONFIG_CHANNEL).getStringValue();

            DiscoveredResourceDetails details =
                    new DiscoveredResourceDetails(
                            discoveryContext.getResourceType(),
                            channel,
                            "Channel " + channel,
                            null, null,
                            config,
                            null
                    );

            discoveredResources.add(details);
        }

        return discoveredResources;

    }
}