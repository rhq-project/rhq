package org.rhq.modules.plugins.Script2;

import java.util.Collections;
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
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;


/**
 * Discovery class
 */
public class ScriptDiscovery implements ResourceDiscoveryComponent
,ManualAddFacet
{

    private final Log log = LogFactory.getLog(this.getClass());

    /**
     * This method is an empty dummy, as you have selected manual addition
     * in the plugin generator.
     * If you want to have auto discovery too, remove the "return emptySet"
     * and implement the auto discovery logic.
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext discoveryContext) throws Exception {
        return Collections.emptySet();
        }

      /**
       * Do the manual add of this one resource
       */
      public DiscoveredResourceDetails discoverResource(Configuration pluginConfiguration, ResourceDiscoveryContext context) throws InvalidPluginConfigurationException {

            // TODO implement this
            DiscoveredResourceDetails detail = null; // new DiscoveredResourceDetails(
//                context.getResourceType(), // ResourceType
//            );

            return detail;
      }
}