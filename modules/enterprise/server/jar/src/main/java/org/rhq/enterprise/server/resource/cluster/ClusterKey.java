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
package org.rhq.enterprise.server.resource.cluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;

/**
 * A ClusterKey represents an AutoCluster of resources in a Cluster Hierarchy.  Given the key it is 
 * possible to determine specific membership of the AutoCluster at any time.  It represents the Cluster
 * Root (a Compatible Group) and the hierarchy of resource cluster nodes.  Each level of the hierarchy
 * represents a resource cluster node defined by a Plugin|ResourceType|ResourceKey tuple.<br/>
 * <br/>
 * The ClusterKey has the following form:
 * <pre> 
 *  Expressed iteratively:
 *  
 *  CompatibleGroupId::ResourceTypeId1:ResourceKey1::ResourceTypeId2:ResourceKey2:: ... ::ResourceTypeIdN:ResourceKeyN
 *  
 *  Expressed Recursively:
 *  
 *  Depth-1 AutoCluster  CompatibleGroupId::ResourceTypeId1:ResourceKey1
 *  Depth-N AutoCluster  <ParentClusterKey>::ResourceTypeIdN:ResourceKeyN
 * </pre> 
 * @author jay shaughnessy
 *
 */
public class ClusterKey {
    static final String DELIM = ":";
    static final String DELIM_NODE = "::";

    // Id of the compatible resource group containing the root set of clustered resources.  
    private int clusterGroupId = 0;
    private List<ClusterKey.Node> hierarchy;
    private String key = null;
    private String namedKey = null;

    /** Construct ClusterKey with to-be-defined Hierarcrhy */
    private ClusterKey(int clusterResourceGroupId) {
        super();
        this.clusterGroupId = clusterResourceGroupId;
        this.hierarchy = new ArrayList<ClusterKey.Node>();
    }

    /** Construct ClusterKey for a top level AutoCluster */
    public ClusterKey(int clusterResourceGroupId, int resourceTypeId, String resourceKey) {
        super();
        this.clusterGroupId = clusterResourceGroupId;
        this.hierarchy = new ArrayList<ClusterKey.Node>();
        this.hierarchy.add(new ClusterKey.Node(resourceTypeId, resourceKey));
    }

    /** Construct a new ClusterKey for a child AutoCluster of the provided parentKey*/
    public ClusterKey(ClusterKey parentKey, int childResourceTypeId, String childResourceKey) {
        super();
        List<ClusterKey.Node> rootClusterNodes = parentKey.getHierarchy();

        this.clusterGroupId = parentKey.getClusterGroupId();
        this.hierarchy = new ArrayList<ClusterKey.Node>(rootClusterNodes.size() + 1);
        Collections.copy(this.hierarchy, rootClusterNodes);
        this.hierarchy.add(new ClusterKey.Node(childResourceTypeId, childResourceKey));
    }

    public int getClusterGroupId() {
        return clusterGroupId;
    }

    public List<ClusterKey.Node> getHierarchy() {
        return hierarchy;
    }

    /**
     * Increase depth of hierarchy with new child node.
     * @param childResourceTypeId
     * @param childResourceKey
     * @return The updated hierarchy
     */
    public List<ClusterKey.Node> addChildToHierarchy(int childResourceTypeId, String childResourceKey) {

        this.hierarchy.add(new ClusterKey.Node(childResourceTypeId, childResourceKey));

        return hierarchy;
    }

    /** 
     * @return the depth of the AutoCluster hierarchy. Just another way of getting the hierarchy size.
     */
    public int getDepth() {
        return hierarchy.size();
    }

    /** 
     * @param newDepth > 0, keep only the hierarchy up to an including the specified depth.
     */
    public void setDepth(int newDepth) {
        while ((newDepth > 0) && (this.hierarchy.size() > newDepth)) {
            this.hierarchy.remove(this.hierarchy.size() - 1);
        }
    }

    public String getKey() {
        if (null == key) {
            StringBuilder b = new StringBuilder();
            b.append(clusterGroupId);

            for (ClusterKey.Node node : hierarchy) {
                b.append(DELIM_NODE);
                b.append(node.toString());
            }
            key = b.toString();
        }

        return key;
    }

    /**
     * format: see class doc, used delimiters ClusterKey.DELIM_NODE and ClusterKey.DELIM
     */
    @Override
    public String toString() {
        return getKey();
    }

    /**
     * format: see class doc, used delimiters ClusterKey.DELIM_NODE and ClusterKey.DELIM. 
     * ResourceTypeId replaced with Plugin:ResourceTypeName tuple.
     */
    public String toNamedString(EntityManager em) {
        if (null == namedKey) {
            ResourceGroup clusterResourceGroup = em.find(ResourceGroup.class, this.clusterGroupId);

            StringBuilder b = new StringBuilder();
            b.append((null == clusterResourceGroup) ? this.clusterGroupId : clusterResourceGroup.getName());

            for (ClusterKey.Node node : hierarchy) {
                b.append(DELIM_NODE);
                b.append(node.toNamedString(em));
            }
            namedKey = b.toString();
        }

        return namedKey;
    }

    public static ClusterKey valueOf(String clusterKey) {
        ClusterKey result = null;

        try {
            String[] nodes = clusterKey.split(DELIM_NODE);
            int groupId = Integer.valueOf(nodes[0]);

            result = new ClusterKey(groupId);

            for (int i = 1; i < nodes.length; ++i) {
                String[] nodeInfo = nodes[i].split(DELIM);

                if ((nodeInfo.length != 2) || "".equals(nodeInfo[0].trim()) || "".equals(nodeInfo[1].trim())) {
                    throw new IllegalArgumentException("Invalid cluster key node: " + nodeInfo);
                }

                result.addChildToHierarchy(Integer.valueOf(nodeInfo[0]), nodeInfo[1]);
            }
        } catch (Exception e) {
            LogFactory.getLog(ClusterKey.class).error("Invalid clusterKey |" + clusterKey + "| encountered: " + e);
            result = null;
        }

        return result;
    }

    /** 
     * @param clusterKey
     * @return The ResourceType id of the resource group this clusterKey defines.
     */
    public static int getResourceType(ClusterKey clusterKey) {
        List<ClusterKey.Node> nodes = clusterKey.getHierarchy();

        return nodes.get(nodes.size() - 1).getResourceTypeId();
    }

    /**
     * Immutable class representing a node in an AutoCluster hierarchy. The node describes a
     * Set of like resources (same type and same resource key). By itself the node lacks any context
     * and so typically this class is for use within a ClusterKey, which qualifies the node with
     * the root group and constraining node ancestry.. */
    public static class Node {
        int resourceTypeId;
        String resourceKey;

        public Node(int resourceTypeId, String resourceKey) {
            super();
            this.resourceTypeId = resourceTypeId;
            this.resourceKey = resourceKey;
        }

        public int getResourceTypeId() {
            return resourceTypeId;
        }

        public String getResourceKey() {
            return resourceKey;
        }

        /**
         * format: resourceTypeId:resourceKey (actual delimiter is ClusterKey.DELIM)
         */
        @Override
        public String toString() {
            return resourceTypeId + DELIM + resourceKey;
        }

        /*
         * format: resourceTypePlugin:resourceTypeName:resourceKey (actual delimiter is ClusterKey.DELIM)
         */
        public String toNamedString(EntityManager em) {
            ResourceType resourceType = em.find(ResourceType.class, this.resourceTypeId);
            String resourceTypePart = null;

            if (null == resourceType) {
                resourceTypePart = this.resourceTypeId + DELIM + this.resourceTypeId;
            } else
                resourceTypePart = resourceType.getPlugin() + DELIM + resourceType.getName();

            return resourceTypePart + DELIM + resourceKey;
        }
    }
}
