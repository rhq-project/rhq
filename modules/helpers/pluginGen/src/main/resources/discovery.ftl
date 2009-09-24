<#--
/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
-->
<#-- @ftlvariable name="props" type="org.rhq.helpers.pluginGen.Props" -->
package ${props.pkg};

<#if  props.manualAddOfResourceType>
import java.util.Collections;
</#if>
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
<#if props.manualAddOfResourceType>
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
</#if>
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;


/**
 * Discovery class
 */
public class ${props.discoveryClass} implements ResourceDiscoveryComponent<#if props.parentType??><${props.parentType}></#if>
<#if props.manualAddOfResourceType>,ManualAddFacet</#if>
{

    private final Log log = LogFactory.getLog(this.getClass());

<#if props.manualAddOfResourceType>
    /**
     * This method is an empty dummy, as we do not support auto discovery
     */
<#else>
    /**
     * Run the auto-discovery
     */
</#if>
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<#if props.parentType??><${props.parentType}></#if> discoveryContext) throws Exception {
<#if  props.manualAddOfResourceType>
        return Collections.emptySet();
<#else>
        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();

        /**
         * TODO : do your discovery here
         * A discovered resource must have a unique key, that must
         * stay the same when the resource is discovered the next
         * time
         */
        DiscoveredResourceDetails detail = null; // new DiscoveredResourceDetails(
//            discoveryContext.getResourceType(), // ResourceType
//        );


        // Add to return values
        discoveredResources.add(detail);
        log.info("Discovered new ... TODO "); // TODO change

        return discoveredResources;

</#if>
        }

<#if props.manualAddOfResourceType>
      /**
       * Do the manual add of this one resource
       */
      public DiscoveredResourceDetails discoverResource(Configuration pluginConfiguration, ResourceDiscoveryContext<#if props.parentType??><${props.parentType}></#if> context) throws InvalidPluginConfigurationException {

            // TODO implement this
            DiscoveredResourceDetails detail = null; // new DiscoveredResourceDetails(
//                context.getResourceType(), // ResourceType
//            );

            return detail;
      }
</#if>
}