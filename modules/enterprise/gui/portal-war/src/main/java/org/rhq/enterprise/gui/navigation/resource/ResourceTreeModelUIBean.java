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
package org.rhq.enterprise.gui.navigation.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.flyweight.AutoGroupCompositeFlyweight;
import org.rhq.core.domain.resource.flyweight.MembersAvailabilityHint;
import org.rhq.core.domain.resource.flyweight.MembersCategoryHint;
import org.rhq.core.domain.resource.flyweight.ResourceFlyweight;
import org.rhq.core.domain.resource.flyweight.ResourceTypeFlyweight;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.HibernatePerformanceMonitor;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Backing bean for the left navigation tree for resources
 *
 * @author Greg Hinkle
 */
public class ResourceTreeModelUIBean {
    private static final Log log = LogFactory.getLog(ResourceTreeModelUIBean.class);

    private List<ResourceTreeNode> roots = new ArrayList<ResourceTreeNode>();
    private ResourceTreeNode rootNode = null;

    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
    private AgentManagerLocal agentManager = LookupUtil.getAgentManager();

    private String nodeTitle;

    private void loadTree() {
        int searchId;
        Resource currentResource = EnterpriseFacesContextUtility.getResourceIfExists();
        if (currentResource == null) {
            searchId = Integer.parseInt(FacesContextUtility.getOptionalRequestParameter("parent"));
        } else {
            searchId = currentResource.getId();
        }

        Subject user = EnterpriseFacesContextUtility.getSubject();

        long start = System.currentTimeMillis();
        long monitorId = HibernatePerformanceMonitor.get().start();
        Resource rootResource = resourceManager.getRootResourceForResource(searchId);
        long end = System.currentTimeMillis();
        HibernatePerformanceMonitor.get().stop(monitorId, "ResourceTree root resource");
        log.debug("Found root resource in " + (end - start));

        Agent agent = agentManager.getAgentByResourceId(LookupUtil.getSubjectManager().getOverlord(), rootResource
            .getId());

        start = System.currentTimeMillis();
        monitorId = HibernatePerformanceMonitor.get().start();
        List<ResourceFlyweight> resources = resourceManager.findResourcesByAgent(user, agent.getId(), PageControl
            .getUnlimitedInstance());
        end = System.currentTimeMillis();
        HibernatePerformanceMonitor.get().stop(monitorId, "ResourceTree agent resource");
        log.debug("Loaded " + resources.size() + " raw resources in " + (end - start));

        start = System.currentTimeMillis();
        monitorId = HibernatePerformanceMonitor.get().start();
        rootNode = load(rootResource.getId(), resources);
        end = System.currentTimeMillis();
        HibernatePerformanceMonitor.get().stop(monitorId, "ResourceTree tree construction");
        log.debug("Constructed tree in " + (end - start));
    }

    public static ResourceTreeNode load(int rootId, List<ResourceFlyweight> resources) {
        ResourceFlyweight found = null;
        for (ResourceFlyweight res : resources) {
            if (res.getId() == rootId) {
                found = res;
            }
        }
        ResourceTreeNode root = new ResourceTreeNode(found);
        load(root);
        return root;
    }

    public static void load(ResourceTreeNode parentNode) {
        if (parentNode.getData() instanceof ResourceFlyweight) {
            ResourceFlyweight parentResource = (ResourceFlyweight) parentNode.getData();

            Map<Object, List<ResourceFlyweight>> children = new HashMap<Object, List<ResourceFlyweight>>();
            for (ResourceFlyweight res : parentResource.getChildResources()) {
                if (res.getResourceType().getSubCategory() != null) {
                    // These are children that have subcategories
                    // Split them by if they are a sub-sub category or just a category
                    String categoryKey = res.getResourceType().getSubCategory();
                    addToList(children, categoryKey, res);
                } else {
                    // These are children without categories of the parent resource
                    // - Add them into groupings by their resource type
                    addToList(children, res.getResourceType().getName(), res);
                }

            }

            Set<String> dupResourceTypeNames = getDuplicateResourceTypeNames(children);

            for (Map.Entry<Object, List<ResourceFlyweight>> entry : children.entrySet()) {
                Object key = entry.getKey();
                List<ResourceFlyweight> resources = entry.getValue();

                double avail = 0;
                for (ResourceFlyweight res : resources) {
                    avail += res.getCurrentAvailability().getAvailabilityType() == AvailabilityType.UP ? 1 : 0;
                }
                avail = avail / resources.size();

                Object nodeData = null;
                if (key instanceof String) {
                    nodeData = new AutoGroupCompositeFlyweight(avail, parentResource, (String) key, resources.size());
                } else if (key instanceof ResourceTypeFlyweight) {
                    ResourceTypeFlyweight typeKey = (ResourceTypeFlyweight) key;

                    if (typeKey.isSingleton()) {
                        nodeData = resources.get(0);
                    } else {
                        boolean isDupResourceTypeName = dupResourceTypeNames.contains(typeKey.getName());
                        nodeData = new AutoGroupCompositeFlyweight(avail, parentResource, typeKey, resources.size(),
                            isDupResourceTypeName);
                    }
                }
                ResourceTreeNode node = new ResourceTreeNode(nodeData, parentNode);
                load(node);

                if (!recursivelyLocked(node)) {
                    parentNode.getChildren().add(node);
                }
            }
        } else {
            // #####################################################################################

            AutoGroupCompositeFlyweight compositeParent = (AutoGroupCompositeFlyweight) parentNode.getData();

            Map<Object, List<ResourceFlyweight>> children = new HashMap<Object, List<ResourceFlyweight>>();
            log.debug("composite parent" + compositeParent);
            if (compositeParent != null) {

                MembersCategoryHint membersCategory = MembersCategoryHint.NONE;
                MembersAvailabilityHint membersAvailabilityHint = MembersAvailabilityHint.UP;

                for (ResourceFlyweight res : compositeParent.getParentResource().getChildResources()) {
                    boolean process = false;
                    if (compositeParent.getSubcategory() != null) {
                        // parent is a sub category
                        if (res.getResourceType().getSubCategory() != null
                        //BZ1069545 This functionality is no longer needed since portal-war is no longer needed.
                        //&& compositeParent.getSubcategory().equals(
                        //    res.getResourceType().getSubCategory().getParentSubCategory())
                            && compositeParent.getParentResource().equals(res.getParentResource())) {

                            // A subSubCategory in a subcategory
                            addToList(children, res.getResourceType().getSubCategory(), res);
                            process = true;
                        } else if (compositeParent.getSubcategory().equals(res.getResourceType().getSubCategory())
                            && compositeParent.getParentResource().equals(res.getParentResource())) {
                            // Direct entries in a subcategory... now group them by autogroup (type)
                            addToList(children, res.getResourceType(), res);
                            process = true;
                        }
                    } else if (compositeParent.getResourceType() != null) {
                        if (compositeParent.getResourceType().equals(res.getResourceType())
                            && compositeParent.getParentResource().getId() == res.getParentResource().getId()) {

                            addToList(children, res.getResourceType(), res);
                            process = true;
                        }
                    }

                    if (process) {
                        //amend the overall category of all the members of the auto group.
                        switch (membersCategory) {
                        case NONE: //this is the first child, so let's use its category as a starting point
                            membersCategory = MembersCategoryHint.fromResourceCategory(res.getResourceType()
                                .getCategory());
                            break;
                        case MIXED: //this is the "final" state. The children type is not going to change from this.
                            break;
                        default: //check if this child has the same category as its previous siblings.
                            if (MembersCategoryHint.fromResourceCategory(res.getResourceType().getCategory()) != membersCategory) {
                                membersCategory = MembersCategoryHint.MIXED;
                            }
                        }

                        //amend the availability hint of the autogroup. If all resources are up, the hint is UP, if some of the resources
                        //are down, the hint is DOWN, if some of the resources' avail state is unknown, the hint is UNKNOWN.
                        //The down state has the highest priority.
                        switch (membersAvailabilityHint) {
                        case UP:
                            membersAvailabilityHint = MembersAvailabilityHint.fromAvailabilityType(res
                                .getCurrentAvailability().getAvailabilityType());
                            break;
                        case UNKNOWN:
                            if (res.getCurrentAvailability().getAvailabilityType() == AvailabilityType.DOWN) {
                                membersAvailabilityHint = MembersAvailabilityHint.DOWN;
                            }
                            break;
                        case DOWN:
                            ; //a "terminal" state... if some resource is down, the overall state is going to be down as that is the most important information.
                        }
                    }
                }

                compositeParent.setMembersCategoryHint(membersCategory);
                compositeParent.setMembersAvailabilityHint(membersAvailabilityHint);
            }

            AutoGroupCompositeFlyweight compositeParentNode = (AutoGroupCompositeFlyweight) parentNode.getData();

            for (Map.Entry<Object, List<ResourceFlyweight>> entry : children.entrySet()) {
                Object key = entry.getKey();
                List<ResourceFlyweight> resources = entry.getValue();

                if (compositeParentNode.getSubcategory() != null) {
                    double avail = 0;
                    for (ResourceFlyweight res : resources) {
                        avail += res.getCurrentAvailability().getAvailabilityType() == AvailabilityType.UP ? 1 : 0;
                    }
                    avail = avail / resources.size();

                    Object nodeData = null;
                    if (key instanceof String) {
                        nodeData = new AutoGroupCompositeFlyweight(avail, compositeParent.getParentResource(),
                            (String) key, resources.size());
                    } else if (key instanceof ResourceTypeFlyweight) {
                        ResourceTypeFlyweight typeKey = (ResourceTypeFlyweight) key;
                        if (typeKey.isSingleton()) {
                            nodeData = resources.get(0);
                        } else {
                            nodeData = new AutoGroupCompositeFlyweight(avail, compositeParent.getParentResource(),
                                typeKey, resources.size(), false);
                        }
                    }
                    ResourceTreeNode node = new ResourceTreeNode(nodeData, parentNode);
                    load(node);

                    if (!recursivelyLocked(node)) {
                        parentNode.getChildren().add(node);
                    }
                } else {
                    for (ResourceFlyweight res : resources) {
                        ResourceTreeNode node = new ResourceTreeNode(res, parentNode);
                        load(node);
                        if (!recursivelyLocked(node)) {
                            parentNode.getChildren().add(node);
                        }
                    }
                }
            }
        }
    }

    public static boolean recursivelyLocked(ResourceTreeNode node) {
        if (node.getData() instanceof ResourceFlyweight && !((ResourceFlyweight) node.getData()).isLocked()) {
            return false;
        }

        boolean allLocked = true;
        for (ResourceTreeNode child : node.getChildren()) {
            if (!recursivelyLocked(child))
                allLocked = false;
        }
        return allLocked;
    }

    public ResourceTreeNode getTreeNode() {
        if (rootNode == null) {
            long start = System.currentTimeMillis();
            loadTree();
            log.debug("Loaded full tree in " + (System.currentTimeMillis() - start));
        }

        return rootNode;
    }

    public List<ResourceTreeNode> getRoots() {
        if (roots.isEmpty()) {
            roots.add(getTreeNode());
        }
        return roots;
    }

    public String getNodeTitle() {
        return nodeTitle;
    }

    public void setNodeTitle(String nodeTitle) {
        this.nodeTitle = nodeTitle;
    }

    private static Set<String> getDuplicateResourceTypeNames(Map<Object, List<ResourceFlyweight>> children) {
        Set<String> resourceTypeNames = new HashSet<String>();
        Set<String> dupResourceTypeNames = new HashSet<String>();
        for (Object rsc : children.keySet()) {
            if (rsc instanceof ResourceTypeFlyweight) {
                String resourceTypeName = ((ResourceTypeFlyweight) rsc).getName();
                if (resourceTypeNames.contains(resourceTypeName)) {
                    dupResourceTypeNames.add(resourceTypeName);
                }
                resourceTypeNames.add(resourceTypeName);
            }
        }
        return dupResourceTypeNames;
    }

    private static <K, V> void addToList(Map<K, List<V>> mapOfLists, K key, V value) {
        List<V> list = mapOfLists.get(key);

        if (list == null) {
            list = new ArrayList<V>();
            mapOfLists.put(key, list);
        }

        list.add(value);
    }
}
