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

import java.util.ArrayList;
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
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.resource.composite.ProblemResourceComposite;
import org.rhq.core.domain.resource.composite.RecentlyAddedResourceComposite;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.composite.ResourceLineageComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.IntExtractor;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.discovery.DiscoveryBossLocal;
import org.rhq.enterprise.server.measurement.MeasurementProblemManagerLocal;
import org.rhq.enterprise.server.resource.ResourceFactoryManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.disambiguation.DefaultDisambiguationUpdateStrategies;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class ResourceGWTServiceImpl extends AbstractGWTServiceImpl implements ResourceGWTService {

    private static final long serialVersionUID = 1L;

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
        "explicitGroups",
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
        com.allen_sauer.gwt.log.client.Log.info("Loading GWT RPC Services");
    }

    public ResourceGWTServiceImpl() {
    }

    public PageList<Resource> findResourcesByCriteria(ResourceCriteria criteria) {
        try {
            PageList<Resource> result = resourceManager.findResourcesByCriteria(getSessionSubject(), criteria);

            ObjectFilter.filterFieldsInCollection(result, importantFieldsSet);

            return SerialUtility.prepare(result, "ResourceService.findResourcesByCriteria");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public PageList<ResourceComposite> findResourceCompositesByCriteria(ResourceCriteria criteria) {
        try {
            PageList<ResourceComposite> result = resourceManager.findResourceCompositesByCriteria(getSessionSubject(),
                criteria);
            List<Resource> resources = new ArrayList<Resource>(result.size());

            if (resources.size() > 1) {
                ObjectFilter.filterFieldsInCollection(resources, importantFieldsSet);
            }

            return SerialUtility.prepare(result, "ResourceService.findResourceCompositesByCriteria");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    /** Locate ProblemResourcesComposites and generate the disambiguation reports for them.
     *  Criteria passed in not currently used.
     */
    public List<DisambiguationReport<ProblemResourceComposite>> findProblemResources(long ctime, int maxItems) {
        try {
            List<ProblemResourceComposite> located = new ArrayList<ProblemResourceComposite>();
            MeasurementProblemManagerLocal problemManager = LookupUtil.getMeasurementProblemManager();
            ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

            //retrieve list of discovered problem resources. Grab all, live scrolling data
            located = problemManager.findProblemResources(getSessionSubject(), ctime, new PageControl(0, maxItems));

            //translate the returned problem resources to disambiguated links
            List<DisambiguationReport<ProblemResourceComposite>> translated = resourceManager.disambiguate(located,
                RESOURCE_ID_EXTRACTOR, DefaultDisambiguationUpdateStrategies.getDefault());
            return translated;
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    private static final IntExtractor<ProblemResourceComposite> RESOURCE_ID_EXTRACTOR = new IntExtractor<ProblemResourceComposite>() {
        public int extract(ProblemResourceComposite object) {
            return object.getResourceId();
        }
    };

    public List<ResourceLineageComposite> getResourceLineageAndSiblings(int resourceId) {
        try {
            return SerialUtility.prepare(
                resourceManager.getResourceLineageAndSiblings(getSessionSubject(), resourceId),
                "ResourceService.getResourceLineageAndSiblings");

        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public Resource getPlatformForResource(int resourceId) {
        try {
            return SerialUtility.prepare(resourceManager.getRootResourceForResource(resourceId),
                "ResourceService.getPlatformForResource");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public List<RecentlyAddedResourceComposite> findRecentlyAddedResources(long ctime, int maxItems) {
        try {
            List<RecentlyAddedResourceComposite> platforms = resourceManager.findRecentlyAddedPlatforms(
                getSessionSubject(), ctime, maxItems);

            for (RecentlyAddedResourceComposite platform : platforms) {
                List<RecentlyAddedResourceComposite> servers = resourceManager.findRecentlyAddedServers(
                    getSessionSubject(), ctime, platform.getId());
                platform.setChildren(servers);
            }

            return SerialUtility.prepare(platforms, "ResourceService.findRecentlyAddedResources");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public List<Integer> uninventoryResources(int[] resourceIds) {
        try {
            return SerialUtility.prepare(resourceManager.uninventoryResources(getSessionSubject(), resourceIds),
                "ResourceService.uninventoryResources");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public void updateResource(Resource resource) {
        try {
            resourceManager.updateResource(getSessionSubject(), resource);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public void createResource(int parentResourceId, int newResourceTypeId, String newResourceName,
        Configuration newResourceConfiguration) {
        try {

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
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public Map<Resource, List<Resource>> getQueuedPlatformsAndServers(HashSet<InventoryStatus> statuses, PageControl pc) {
        try {
            return SerialUtility.prepare(discoveryBoss.getQueuedPlatformsAndServers(getSessionSubject(), EnumSet
                .copyOf(statuses), pc), "ResourceService.getQueuedPlatformsAndServers");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public void importResources(Integer[] resourceIds) {
        try {
            discoveryBoss.importResources(getSessionSubject(), resourceIds);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public void ignoreResources(Integer[] resourceIds) {
        try {
            discoveryBoss.ignoreResources(getSessionSubject(), resourceIds);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public void unignoreResources(Integer[] resourceIds) {
        try {
            discoveryBoss.unignoreResources(getSessionSubject(), resourceIds);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public List<ResourceError> findResourceErrors(int resourceId) {
        return SerialUtility.prepare(resourceManager.findResourceErrors(getSessionSubject(), resourceId),
            "ResourceService.getResourceErrors");
    }

    public Resource manuallyAddResource(int resourceTypeId, int parentResourceId, Configuration pluginConfiguration) {
        try {
            Resource result = discoveryBoss.manuallyAddResource(getSessionSubject(), resourceTypeId, parentResourceId,
                pluginConfiguration);
            return SerialUtility.prepare(result, "ResourceService.manuallyAddResource");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

}
