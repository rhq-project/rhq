 /*
  * Jopr Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.plugins.jbossas5;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jbossas5.factory.ProfileServiceFactory;
//import org.jboss.profileservice.spi.NoSuchProfileException;
//import org.jboss.profileservice.spi.Profile;
//import org.jboss.profileservice.spi.ProfileService;

import java.util.HashSet;
import java.util.Set;

/**
 * Discovery component for the JBoss Server and Profile Service
 *
 * @author Mark Spritzler
 */
public class ProfileJBossServerDiscoveryComponent
        implements ResourceDiscoveryComponent
{
    private final Log LOG = LogFactory.getLog(ProfileJBossServerDiscoveryComponent.class);

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context)
    {
        LOG.info("Discovering resources of Server Type: " + context.getResourceType());

        Set<DiscoveredResourceDetails> servers = new HashSet<DiscoveredResourceDetails>();
        // Not just the one that it finds when in the embedded console
        //ProfileService profileService = ProfileServiceFactory.getProfileService();

        /*try
     {
        Profile activeProfile = profileService.getActiveProfile();
        Collection<ProfileKey> profileKeys = profileService.getProfileKeys();
        for (ProfileKey key: profileKeys)
        {

           Profile currentProfile = profileService.getProfile(key);
           if (currentProfile.equals(activeProfile))
           {
              resourceKey = "Jboss AS 5:" + key.getName();
           }

        }*/
        String resourceKey = "JBoss App Server:default";

        ProfileServiceFactory.refreshCurrentProfileView();

        DiscoveredResourceDetails server =
                new DiscoveredResourceDetails(
                        context.getResourceType(),
                        resourceKey,
                        resourceKey,
                        "5.0 CR1", //activeProfile.getVersion(),
                        "JBoss App Server", null, null);
        servers.add(server);
/*
      } catch (NoSuchProfileException e)
      {
         LOG.error("No Active Profile found", e);
      }
*/

        // Implementation to find all profiles
        /*
        Collection<ProfileKey> profileKeys = profileService.getProfileKeys();


        for (ProfileKey key: profileKeys)
        {

           String resourceKey = "Jboss AS 5:" + key.getName();
           // @TODO when the Profile Service gives more AS info, replace hardcoded strings
           DiscoveredResourceDetails server =
                 new DiscoveredResourceDetails(
                       context.getResourceType(),
                       resourceKey,
                       resourceKey,
                       "5.0",
                        "JBoss App Server");
           servers.add( server );
        }
        */
        LOG.info("Discovered " + servers.size() + " servers");

        return servers;
    }
}
