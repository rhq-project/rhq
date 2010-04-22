package org.rhq.NagiosMonitor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;


/**
 * Discovery class
 */
public class NagiosMonitorDiscovery implements ResourceDiscoveryComponent, ManualAddFacet
{
    private final Log log = LogFactory.getLog(this.getClass());

   /**
    * Support manually adding this resource type via Platform's inventory tab
    * @param configuration
    * @param resourceDiscoveryContext
    * @return
    * @throws org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException
    */
   public DiscoveredResourceDetails discoverResource(Configuration configuration,
                                                     ResourceDiscoveryContext resourceDiscoveryContext) throws InvalidPluginConfigurationException {

      String nagiosHost = configuration.getSimpleValue("nagiosHost",NagiosMonitorComponent.NAGIOSIP);
      String nagiosPort = configuration.getSimpleValue("nagiosPort",NagiosMonitorComponent.NAGIOSPORT);


      DiscoveredResourceDetails detail = new DiscoveredResourceDetails(

         resourceDiscoveryContext.getResourceType(),
          "nagios@"+nagiosHost+":"+nagiosPort,
          "Nagios@"+nagiosHost+":"+nagiosPort,
          null,
          "Nagios server @ " + nagiosHost + ":" + nagiosPort,
          configuration,
          null
      );

      return detail;
   }



    /**
     * Don't run the auto-discovery of this "NagiosMonitor" server type,
     * as we probably won't have one on each platform. Rather have the admin
     * explicitly add it to one platform.
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext discoveryContext) throws Exception {
    	Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();

       return discoveredResources;
    }

}