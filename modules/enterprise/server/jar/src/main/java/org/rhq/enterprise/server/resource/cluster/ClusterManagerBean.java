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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ClusterKey;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ClusterFlyweight;
import org.rhq.core.domain.resource.group.composite.ClusterKeyFlyweight;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.resource.group.ResourceGroupAlreadyExistsException;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;

/**
 * 
 * @author jay shaughnessy
 *
 */
@Stateless
public class ClusterManagerBean implements ClusterManagerLocal, ClusterManagerRemote {
    private final Log log = LogFactory.getLog(ClusterManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private ResourceGroupManagerLocal resourceGroupManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    public ResourceGroup createAutoClusterBackingGroup(Subject subject, ClusterKey clusterKey, boolean addResources) {
        ResourceGroup autoClusterBackingGroup = null;

        Query query = entityManager.createNamedQuery(ResourceGroup.QUERY_FIND_BY_CLUSTER_KEY);
        query.setParameter("clusterKey", clusterKey.toString());

        ResourceType resourceType = entityManager.find(ResourceType.class, ClusterKey.getResourceType(clusterKey));
        ResourceGroup resourceGroup = entityManager.find(ResourceGroup.class, clusterKey.getClusterGroupId());

        if (!authorizationManager.canViewGroup(subject, clusterKey.getClusterGroupId())) {
            throw new PermissionException("You do not have permission to view child cluster groups of the group ["
                + resourceGroup.getName() + "]");
        }

        List<Resource> resources = null;
        try {
            autoClusterBackingGroup = (ResourceGroup) query.getSingleResult();
        } catch (NoResultException nre) {
            try {
                resources = getAutoClusterResources(subject, clusterKey);
                String name = null;
                if (resources.isEmpty()) {
                    name = "Group of " + resourceType.getName();
                } else {
                    for (Resource res : resources) {
                        if (name == null) {
                            name = res.getName();
                        } else {
                            if (!name.equals(res.getName())) {
                                name = "Group of " + resourceType.getName();
                            }
                        }
                    }
                }

                // For AutoClusters the group name is the unique cluster key
                autoClusterBackingGroup = new ResourceGroup(name, resourceType);
                autoClusterBackingGroup.setClusterKey(clusterKey.toString());
                autoClusterBackingGroup.setClusterResourceGroup(resourceGroup);
                autoClusterBackingGroup.setVisible(false);

                // You are allowed to cause the creation of an auto cluster backing group as long as you can
                // view the parent group. (That check was done above)
                int id = resourceGroupManager
                    .createResourceGroup(subjectManager.getOverlord(), autoClusterBackingGroup).getId();
                autoClusterBackingGroup = entityManager.find(ResourceGroup.class, id);

            } catch (ResourceGroupAlreadyExistsException e) {
                // This should not happen since the group name is actually generated
                log.error("Unexpected Error, group exists " + e);
                return null;
            }
        }

        if (addResources) {
            if (resources == null) {
                resources = getAutoClusterResources(subject, clusterKey);
            }

            int i = 0;
            int[] resourceIds = new int[resources.size()];
            for (Resource res : resources) {
                resourceIds[i++] = res.getId();
            }

            try {
                // You are allowed to cause the creation of an auto cluster backing group as long as you can
                // view the parent group. (That check was done above)
                resourceGroupManager.setAssignedResources(subjectManager.getOverlord(),
                    autoClusterBackingGroup.getId(), resourceIds, false);
            } catch (Exception e) {
                log.error("Could not add resources to group:" + e);
            }
        }

        return autoClusterBackingGroup;
    }

    public ResourceGroup getAutoClusterBackingGroup(Subject subject, ClusterKey clusterKey) {
        ResourceGroup result = null;

        Query query = entityManager.createNamedQuery(ResourceGroup.QUERY_FIND_BY_CLUSTER_KEY);
        query.setParameter("clusterKey", clusterKey.toString());
        try {
            result = (ResourceGroup) query.getSingleResult();
        } catch (NoResultException e) {
            result = null;
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Resource> getAutoClusterResources(Subject subject, ClusterKey clusterKey) {
        // Build the query
        String queryString = getClusterKeyQuery(clusterKey);
        if (log.isDebugEnabled()) {
            log.debug("getAutoClusterResources() generated query: " + queryString);
        }
        Query query = entityManager.createQuery(queryString);
        List<Resource> rs = query.getResultList();

        return rs;
    }

    public ClusterFlyweight getClusterTree(Subject subject, int groupId) {
        Query query = entityManager
            .createQuery("SELECT r.id, r.resourceType.id, r.parentResource.id, r.resourceKey, r.name, "
                + "(SELECT count(r2) FROM Resource r2 join r2.explicitGroups g2 WHERE g2.id = :groupId and r2.id = r.id) "
                + "FROM Resource r join r.implicitGroups g " + "WHERE g.id = :groupId");

        query.setParameter("groupId", groupId);
        List<Object[]> rs = query.getResultList();

        Map<Integer, List<ClusterTreeQueryResults>> dataMap = new HashMap<Integer, List<ClusterTreeQueryResults>>();
        Set<Integer> explicitResources = new HashSet<Integer>();

        for (Object[] d : rs) {
            ClusterTreeQueryResults row = new ClusterTreeQueryResults(d);
            Integer parentId = row.parentId;
            List<ClusterTreeQueryResults> childList = dataMap.get(parentId);
            if (childList == null) {
                childList = new ArrayList<ClusterTreeQueryResults>();
                dataMap.put(parentId, childList);
            }
            childList.add(row);
            if (row.isExplicitResource) {
                explicitResources.add(row.resourceId);
            }
        }

        ClusterFlyweight topTreeNode = new ClusterFlyweight(groupId);

        // dataMap contains one key for every *parent resource* that is a parent to a group resource.
        // this could include platform resources if group members are top level servers.
        buildTree(groupId, topTreeNode, explicitResources, dataMap);

        return topTreeNode;
    }

    /**
     * Recursively builds tree nodes (where tree nodes are of type ClusterFlyweight).
     * The <code>parent</code> object will be modified - it is the tree that is being built.
     * The parent node reprents a single cluster node, where a cluster node is an aggregation
     * of N identical resources that belong to the group (where "identical" means the same
     * resource type and resource key).
     * 
     * @param groupId identifies the group whose tree is being built
     * @param parent the top "parent cluster node" of the tree - will get its children assigned to it via this method
     * @param parentIds the resource IDs for all identical parent resources that make up the one "parent cluster node"
     * @param data keyed on resource ID whose list is that of the resource's children. A few of these data
     *             won't necessarily be placed in the "parent" top node because these may not be part of the tree, but
     *             may be parents themselves of the top most resources (for example, if I have a group of JBossAS
     *             Server resources, some of these data will be the platform resources since they are parents to the
     *             JBossAS Server resources - those platforms will not be represented in the "parent" top tree node).
     *             Every parent resource, regardless of where they are in the cluster hierarchy, is represented by a key
     *             in this data map.
     */
    private void buildTree(int groupId, ClusterFlyweight parent, Set<Integer> parentIds,
        Map<Integer, List<ClusterTreeQueryResults>> data) {

        // this is the children cluster nodes for the parent cluster node we are building
        // notice the key to this map is a ClusterKeyFlyweight, which is a resourceType/resourceKey tuple
        Map<ClusterKeyFlyweight, ClusterFlyweight> children = new HashMap<ClusterKeyFlyweight, ClusterFlyweight>();

        // has the same as children above except its values aren't child nodes, but just the child nodes' resource IDs
        Map<ClusterKeyFlyweight, Set<Integer>> members = new HashMap<ClusterKeyFlyweight, Set<Integer>>();

        // loop through each identical parent resource to aggregate their children into a single cluster node
        for (Integer parentId : parentIds) {

            List<ClusterTreeQueryResults> directChildren = data.get(parentId);
            if (directChildren != null) {
                for (ClusterTreeQueryResults child : directChildren) {
                    ClusterKeyFlyweight childNodeKey = new ClusterKeyFlyweight(child.resourceTypeId, child.resourceKey);
                    ClusterFlyweight childNode = children.get(childNodeKey);
                    Set<Integer> memberList;
                    if (childNode == null) {
                        childNode = new ClusterFlyweight(childNodeKey);
                        children.put(childNodeKey, childNode);
                        memberList = new HashSet<Integer>();
                        members.put(childNodeKey, memberList);
                    } else {
                        memberList = members.get(childNodeKey);
                    }
                    childNode.addResource(child.resourceName);
                    memberList.add(child.resourceId);
                }
            }
        }

        // now that we have aggregated all the children for the individual yet identical parent resources, assign those
        // aggregated child nodes to the parent cluster node
        parent.setChildren(new ArrayList<ClusterFlyweight>(children.values()));

        for (ClusterFlyweight child : children.values()) {
            buildTree(groupId, child, members.get(child.getClusterKey()), data);
        }

        return;
    }

    private class ClusterTreeQueryResults {
        Integer resourceId;
        Integer resourceTypeId;
        Integer parentId;
        String resourceKey;
        String resourceName;
        boolean isExplicitResource;

        private ClusterTreeQueryResults(Object[] queryResults) {
            resourceId = (Integer) queryResults[0];
            resourceTypeId = (Integer) queryResults[1];
            parentId = (Integer) queryResults[2];
            resourceKey = (String) queryResults[3];
            resourceName = (String) queryResults[4];
            isExplicitResource = ((Number) queryResults[5]).intValue() > 0;
        }
    }

    private String getClusterKeyQuery(ClusterKey clusterKey) {
        if (null == clusterKey)
            return null;
        if (0 == clusterKey.getDepth())
            return null;

        StringBuilder query = new StringBuilder();

        buildQuery(query, clusterKey, clusterKey.getHierarchy());

        return query.toString();
    }

    /**
     * Builds a query like the following (this is a depth-2 example):
     * <pre>
     *  SELECT r2 FROM Resource r2 
     *  WHERE r2.resourceKey = :r2key AND r2.resourceType = :r2rt AND r2.parentResource IN (
     *   SELECT r1 FROM Resource r1 
     *   WHERE r1.resourceKey = :r1key AND r1.resourceType = :r1rt AND r1.parentResource IN ( 
     *     SELECT rgir FROM ResourceGroup rg JOIN rg.implicitResources rgir 
     *     WHERE rg = :rgId
     *  </pre>
     *  The parameters are actually filled in with the literal values.
     */
    private void buildQuery(StringBuilder query, ClusterKey clusterKey, List<ClusterKey.Node> nodes) {
        int size = nodes.size();
        ClusterKey.Node node = nodes.get(size - 1);
        String alias = "r" + size;

        // TODO: Change subquery syntax to be like the JOIN below.
        query.append(" SELECT " + alias + " FROM Resource " + alias + " WHERE ");
        query.append(alias + ".resourceKey = '" + node.getResourceKey() + "' AND ");
        query.append(alias + ".resourceType = " + node.getResourceTypeId() + " AND ");
        query.append(alias + ".parentResource IN ( ");

        // this is an authorization-related query, so use implicitResource (not explicitResources)
        if (1 == size) {
            query.append("SELECT rgir FROM ResourceGroup rg JOIN rg.implicitResources rgir WHERE rg = "
                + clusterKey.getClusterGroupId());
        } else {
            buildQuery(query, clusterKey, nodes.subList(0, size - 1));
        }

        query.append(")");
    }

}
