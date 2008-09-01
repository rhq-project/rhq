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
package org.rhq.enterprise.server.cluster;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.cluster.AffinityGroup;
import org.rhq.core.domain.cluster.Server;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.RequiredPermission;

/**
 * @author Joseph Marques
 */
@Stateless
public class AffinityGroupManagerBean implements AffinityGroupManagerLocal {

    @SuppressWarnings("unused")
    private final Log log = LogFactory.getLog(AffinityGroupManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public AffinityGroup getById(Subject subject, int affinityGroupId) {
        AffinityGroup affinityGroup = entityManager.find(AffinityGroup.class, affinityGroupId);
        return affinityGroup;
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<Agent> getAgentMembers(Subject subject, int affinityGroupId, PageControl pageControl) {
        pageControl.initDefaultOrderingField("a.name");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, Agent.QUERY_FIND_BY_AFFINITY_GROUP,
            pageControl);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager, Agent.QUERY_FIND_BY_AFFINITY_GROUP);

        query.setParameter("affinityGroupId", affinityGroupId);
        countQuery.setParameter("affinityGroupId", affinityGroupId);

        long count = (Long) countQuery.getSingleResult();
        List<Agent> results = query.getResultList();

        return new PageList<Agent>(results, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<Server> getServerMembers(Subject subject, int affinityGroupId, PageControl pageControl) {
        pageControl.initDefaultOrderingField("s.name");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, Server.QUERY_FIND_BY_AFFINITY_GROUP,
            pageControl);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager, Server.QUERY_FIND_BY_AFFINITY_GROUP);

        query.setParameter("affinityGroupId", affinityGroupId);
        countQuery.setParameter("affinityGroupId", affinityGroupId);

        long count = (Long) countQuery.getSingleResult();
        List<Server> results = query.getResultList();

        return new PageList<Server>(results, (int) count, pageControl);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public AffinityGroup update(Subject subject, AffinityGroup affinityGroup) {
        return entityManager.merge(affinityGroup);
    }

}
