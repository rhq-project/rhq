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

import java.util.List;

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
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.resource.group.ResourceGroupAlreadyExistsException;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupUpdateException;

/**
 * 
 * @author jay shaughnessy
 *
 */
@Stateless
public class ClusterManagerBean implements ClusterManagerLocal {
    private final Log log = LogFactory.getLog(ClusterManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    ResourceGroupManagerLocal resourceGroupManager;

    public ResourceGroup createAutoClusterBackingGroup(Subject subject, ClusterKey clusterKey, boolean addResources) {
        ResourceGroup result = null;

        Query query = entityManager.createNamedQuery(ResourceGroup.QUERY_FIND_BY_CLUSTER_KEY);
        query.setParameter("clusterKey", clusterKey.toString());
        if (!query.getResultList().isEmpty()) {
            throw new IllegalArgumentException("Backing Group exists for clusterKey: " + clusterKey);
        }

        ResourceType resourceType = entityManager.find(ResourceType.class, ClusterKey.getResourceType(clusterKey));
        ResourceGroup resourceGroup = entityManager.find(ResourceGroup.class, clusterKey.getClusterGroupId());

        // For AutoClusters the group name is the unique cluster key
        result = new ResourceGroup(clusterKey.toString(), resourceType);
        result.setClusterResourceGroup(resourceGroup);
        result.setVisible(false);

        try {
            resourceGroupManager.createResourceGroup(subject, result);
        } catch (ResourceGroupAlreadyExistsException e) {
            // This should not happen since the group name is actually generated
            log.error("Unexpected Error, group exists: " + e);
            return null;
        }

        if (addResources) {
            List<Resource> resources = getAutoClusterResources(subject, clusterKey);
            Integer[] resourceIds = new Integer[resources.size()];
            for (int i = 0; i < resourceIds.length; ++i) {
                resourceIds[i] = resources.get(i).getId();
            }

            try {
                resourceGroupManager.addResourcesToGroup(subject, result.getId(), resourceIds);
            } catch (ResourceGroupUpdateException e) {
                log.error("Could not add resources to group:" + e);
            }
        }

        return result;
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

        if (1 == size) {
            query.append("SELECT rgir FROM ResourceGroup rg JOIN rg.implicitResources rgir WHERE rg = "
                + clusterKey.getClusterGroupId());
        } else {
            buildQuery(query, clusterKey, nodes.subList(0, size - 1));
        }

        query.append(")");
    }

}
