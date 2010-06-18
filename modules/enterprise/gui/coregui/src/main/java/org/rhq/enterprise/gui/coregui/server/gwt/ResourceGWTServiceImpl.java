/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.server.gwt;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.RecentlyAddedResourceComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.discovery.DiscoveryBossLocal;
import org.rhq.enterprise.server.resource.ResourceFactoryManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class ResourceGWTServiceImpl extends AbstractGWTServiceImpl implements ResourceGWTService {

    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
    private ResourceFactoryManagerLocal resourceFactoryManager = LookupUtil.getResourceFactoryManager();
    private DiscoveryBossLocal discoveryBoss = LookupUtil.getDiscoveryBoss();

    private static String[] importantFields = { "serialVersionUID",
    //                    "ROOT                            \n" +
        //                    "ROOT_ID                         \n" +
        "id",

        //                    "uuid                            \n" +
        //                    "resourceKey                     \n" +
        "name",

        //                    "connected                       \n" +
        //                    "version                         \n" +
        "description",

        //                    "ctime                           \n" +
        //                    "mtime                           \n" +
        //                    "itime                           \n" +
        //                    "modifiedBy                      \n" +
        //                    "location                        \n" +
        "resourceType", "childResources", "parentResource",
        //                    "resourceConfiguration           \n" +
        //                    "pluginConfiguration             \n" +
        //                    "agent                           \n" +
        //                    "alertDefinitions                \n" +
        //                    "resourceConfigurationUpdates    \n" +
        //                    "pluginConfigurationUpdates      \n" +
        //                    "implicitGroups                  \n" +
        //                    "explicitGroups                  \n" +
        //                    "contentServiceRequests          \n" +
        //                    "createChildResourceRequests     \n" +
        //                    "deleteResourceRequests          \n" +
        //                    "operationHistories              \n" +
        //                    "installedPackages               \n" +
        //                    "installedPackageHistory         \n" +
        //                    "resourceRepos                   \n" +
        "schedules",
        //                    "availability                    \n" +
        "currentAvailability",
        //                    "resourceErrors                  \n" +
        //                    "eventSources                    \n" +
        //                    "productVersion                  "}
        "tags" };

    private static Set<String> importantFieldsSet = new HashSet<String>(Arrays.asList(importantFields));

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        System.out.println("Loading GWT RPC Services");
    }

    public ResourceGWTServiceImpl() {
    }

    public PageList<Resource> findResourcesByCriteria(ResourceCriteria criteria) {
        try {
            PageList<Resource> result = resourceManager.findResourcesByCriteria(getSessionSubject(), criteria);
            for (Resource resource : result) {
                resource.setAgent(null);
            }

            ObjectFilter.filterFieldsInCollection(result, importantFieldsSet);

            return SerialUtility.prepare(result, "ResourceService.findResourceByCriteria");
        } catch (Exception e) {
            e.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
            throw new RuntimeException(e);
        }
    }

    public List<Resource> getResourceLineage(int resourceId) {
        return SerialUtility.prepare(resourceManager.getResourceLineage(resourceId),
            "ResourceService.getResourceLineage");
    }

    public List<Resource> getResourceLineageAndSiblings(int resourceId) {
        return SerialUtility.prepare(resourceManager.getResourceLineageAndSiblings(resourceId),
            "ResourceService.getResourceLineage");
    }

    public Resource getPlatformForResource(int resourceId) {
        return SerialUtility.prepare(resourceManager.getRootResourceForResource(resourceId),
            "ResourceService.getPlatformForResource");
    }

    public List<RecentlyAddedResourceComposite> findRecentlyAddedResources(long ctime, int maxItems) {
        List<RecentlyAddedResourceComposite> platforms = resourceManager.findRecentlyAddedPlatforms(
            getSessionSubject(), ctime, maxItems);

        for (RecentlyAddedResourceComposite platform : platforms) {
            List<RecentlyAddedResourceComposite> servers = resourceManager.findRecentlyAddedServers(
                getSessionSubject(), ctime, platform.getId());
            platform.setChildren(servers);
        }

        return platforms;
    }

    public List<Integer> uninventoryResources(int[] resourceIds) {
        return SerialUtility.prepare(resourceManager.uninventoryResources(getSessionSubject(), resourceIds),
            "ResourceService.uninventoryResources");
    }

    public void updateResource(Resource resource) {
        resourceManager.updateResource(getSessionSubject(), resource);
    }

    public void createResource(int parentResourceId, int newResourceTypeId, String newResourceName,
        Configuration newResourceConfiguration) {

        ConfigurationDefinition pluginConfigDefinition = LookupUtil.getConfigurationManager()
            .getPluginConfigurationDefinitionForResourceType(getSessionSubject(), newResourceTypeId);
        Configuration pluginConfig = null;
        if (pluginConfigDefinition != null) {
            ConfigurationTemplate pluginConfigTemplate = pluginConfigDefinition.getDefaultTemplate();
            pluginConfig = (pluginConfigTemplate != null) ? pluginConfigTemplate.createConfiguration()
                : new Configuration();

            // TODO GH: Is this still necessary now that we don't blow up on non-normalized configs
            // ConfigurationUtility.normalizeConfiguration(pluginConfig, pluginConfigDefinition);
        }

        resourceFactoryManager.createResource(getSessionSubject(), parentResourceId, newResourceTypeId,
            newResourceName, pluginConfig, newResourceConfiguration);
    }

    public Map<Resource, List<Resource>> getQueuedPlatformsAndServers(HashSet<InventoryStatus> statuses, PageControl pc) {
        return SerialUtility.prepare(discoveryBoss.getQueuedPlatformsAndServers(getSessionSubject(), EnumSet
            .copyOf(statuses), pc), "ResoruceService.getQueuedPlatformsAndServers");
    }

    public void importResources(Integer[] resourceIds) {
        discoveryBoss.importResources(getSessionSubject(), resourceIds);
    }

    public void ignoreResources(Integer[] resourceIds) {
        discoveryBoss.ignoreResources(getSessionSubject(), resourceIds);
    }

    public void unignoreResources(Integer[] resourceIds) {
        discoveryBoss.unignoreResources(getSessionSubject(), resourceIds);
    }

}