package org.rhq.plugins.hudson;

import org.json.JSONObject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Maven archetypes cannot create empty directories, so this class simply functions to get the
 * requested package structure created.
 */
public class HudsonDiscoveryComponent implements ResourceDiscoveryComponent {

    public Set discoverResources(ResourceDiscoveryContext resourceDiscoveryContext) throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> found = new HashSet<DiscoveredResourceDetails>();

        for (Configuration config : (List<Configuration>)resourceDiscoveryContext.getPluginConfigurations()) {

            String path = config.getSimple("urlBase").getStringValue();
            URL url = new URL(path);

            JSONObject server = HudsonJSONUtility.getData(path, 0);

            server.getString("description");


            DiscoveredResourceDetails hudson =
                new DiscoveredResourceDetails(
                        resourceDiscoveryContext.getResourceType(),
                        url.toString(),
                        url.getHost() + url.getPath(),
                        HudsonJSONUtility.getVersion(path),
                        "hudson server",
                        config,
                        null);
            
            found.add(hudson);
        }

        return found;
    }
}
