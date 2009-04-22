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
package org.rhq.enterprise.gui.navigation.group;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourceWithAvailability;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.AutoGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.navigation.resource.ResourceTreeModelUIBean;
import org.rhq.enterprise.gui.navigation.resource.ResourceTreeNode;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.cluster.ClusterKey;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.HibernatePerformanceMonitor;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class ResourceGroupTreeModelUIBean {

    private final Log log = LogFactory.getLog(ResourceGroupTreeModelUIBean.class);

    private ResourceGroupTreeNode rootNode = null;
    private List<ResourceGroupTreeNode> children = new ArrayList<ResourceGroupTreeNode>();

    private ResourceGroupManagerLocal groupManager = LookupUtil.getResourceGroupManager();
    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

    private String nodeTitle;

    private void loadTree() {

        Integer parentGroupId = FacesContextUtility.getOptionalRequestParameter("parentGroupId", Integer.class);
        ResourceGroup parentGroup;
        if (parentGroupId != null) {
            parentGroup = groupManager.getResourceGroupById(EnterpriseFacesContextUtility.getSubject(), parentGroupId,
                GroupCategory.COMPATIBLE);
        } else {
            parentGroup = EnterpriseFacesContextUtility.getResourceGroup();
            if (parentGroup.getClusterResourceGroup() != null) {
                parentGroup = parentGroup.getClusterResourceGroup();
            }
        }

        rootNode = new ResourceGroupTreeNode(parentGroup);

        long start = System.currentTimeMillis();
        long monitorId = HibernatePerformanceMonitor.get().start();
        List<Resource> resources = resourceManager.getResourcesByCompatibleGroup(EnterpriseFacesContextUtility
            .getSubject(), parentGroup.getId(), PageControl.getUnlimitedInstance());
        long end = System.currentTimeMillis();
        HibernatePerformanceMonitor.get().stop(monitorId, "ResourceGroupTree group resources");
        log.debug("Loaded  " + resources.size() + " resources by group in " + (end - start));

        start = System.currentTimeMillis();
        monitorId = HibernatePerformanceMonitor.get().start();
        List<Integer> members = resourceManager.getExplicitResourceIdsByResourceGroup(parentGroup.getId());
        end = System.currentTimeMillis();
        HibernatePerformanceMonitor.get().stop(monitorId, "ResourceGroupTree group members");
        log.debug("Loaded  " + members.size() + " memebers by group in " + (end - start));

        start = System.currentTimeMillis();
        monitorId = HibernatePerformanceMonitor.get().start();
        rootNode = load(parentGroup, resources, members);
        end = System.currentTimeMillis();
        HibernatePerformanceMonitor.get().stop(monitorId, "ResourceGroupTree tree construction");
        log.debug("Constructed tree in " + (end - start));
    }

    private ResourceGroupTreeNode load(ResourceGroup group, List<Resource> resources, List<Integer> members) {

        Set<ResourceTreeNode> memberNodes = new HashSet<ResourceTreeNode>();

        for (Integer member : members) {
            memberNodes.add(ResourceTreeModelUIBean.load(member.intValue(), resources, true));
        }

        ResourceGroupTreeNode root = new ResourceGroupTreeNode(group);
        root.setClusterKey(new ClusterKey(group.getId()));
        root.addMembers(memberNodes);
        load(root, memberNodes);
        return root;
    }

    private void load(ResourceGroupTreeNode parentNode, Set<ResourceTreeNode> resources) {

        Map<Object, ResourceGroupTreeNode> children = new HashMap<Object, ResourceGroupTreeNode>();

        for (ResourceTreeNode childNode : parentNode.getMembers()) {
            for (ResourceTreeNode node : childNode.getChildren()) {
                Object level = node.getData();

                if (level instanceof AutoGroupComposite) {
                    AutoGroupComposite agc = (AutoGroupComposite) level;
                    Object key = agc.getResourceType() != null ? agc.getResourceType() : agc.getSubcategory();

                    ResourceGroupTreeNode childGroupNode = children.get(key);
                    if (childGroupNode == null) {
                        childGroupNode = new ResourceGroupTreeNode(level);
                        children.put(key, childGroupNode);
                    }
                    childGroupNode.addMember(node);
                    childGroupNode.setClusterKey(parentNode.getClusterKey());

                } else if (level instanceof ResourceWithAvailability) {
                } else if (level instanceof Resource) {
                    Resource res = (Resource) level;
                    ClusterKey parentKey = parentNode.getClusterKey();
                    ClusterKey key = null;
                    if (parentKey == null) {
                        key = new ClusterKey(((ResourceGroup) parentNode.getData()).getId(), res.getResourceType()
                            .getId(), res.getResourceKey());
                    } else {
                        key = new ClusterKey(parentKey, res.getResourceType().getId(), res.getResourceKey());
                    }
                    ResourceGroupTreeNode childGroupNode = children.get(key);

                    if (childGroupNode == null) {
                        childGroupNode = new ResourceGroupTreeNode(key);
                        childGroupNode.setClusterKey(key);
                        children.put(key, childGroupNode);
                    }

                    childGroupNode.addMember(node);

                }
            }
        }

        parentNode.addChildren(children.values());

        for (ResourceGroupTreeNode child : children.values()) {
            Set<ResourceTreeNode> childChildren = new HashSet<ResourceTreeNode>();
            for (ResourceTreeNode childChild : child.getMembers()) {
                childChildren.addAll(childChild.getChildren());
            }
            if (childChildren.size() > 0)
                load(child, childChildren);
        }
    }

    public List<ResourceGroupTreeNode> getRoots() {
        if (rootNode == null) {
            long start = System.currentTimeMillis();
            loadTree();
            log.debug("Loaded full tree in " + (System.currentTimeMillis() - start));
        }
        List<ResourceGroupTreeNode> roots = new ArrayList<ResourceGroupTreeNode>();
        roots.add(this.rootNode);
        return roots;
    }

    public List<ResourceGroupTreeNode> getChildren() {
        return children;
    }

    public String getNodeTitle() {
        return nodeTitle;
    }

    public void setNodeTitle(String nodeTitle) {
        this.nodeTitle = nodeTitle;
    }
}