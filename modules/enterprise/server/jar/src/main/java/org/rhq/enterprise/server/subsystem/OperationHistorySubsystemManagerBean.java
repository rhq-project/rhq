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
package org.rhq.enterprise.server.subsystem;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.composite.ResourceOperationHistoryComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;

/**
 * @author Joseph Marques
 */
@Stateless
public class OperationHistorySubsystemManagerBean implements OperationHistorySubsystemManagerLocal {

    //private final Log log = LogFactory.getLog(ConfigurationSubsystemManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    @SuppressWarnings("unchecked")
    public PageList<ResourceOperationHistoryComposite> getResourceOperationHistories(Subject subject,
        String resourceFilter, String parentFilter, Long startTime, Long endTime, OperationRequestStatus status,
        PageControl pc) {
        pc.initDefaultOrderingField("roh.id", PageOrdering.DESC);

        String queryName = null;
        if (authorizationManager.isInventoryManager(subject)) {
            queryName = ResourceOperationHistory.QUERY_FIND_ALL_ADMIN;
        } else {
            queryName = ResourceOperationHistory.QUERY_FIND_ALL;
        }

        Query queryCount = PersistenceUtility.createCountQuery(entityManager, queryName);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pc);

        if (authorizationManager.isInventoryManager(subject) == false) {
            queryCount.setParameter("subjectId", subject.getId());
            query.setParameter("subjectId", subject.getId());
        }

        resourceFilter = PersistenceUtility.formatSearchParameter(resourceFilter);
        parentFilter = PersistenceUtility.formatSearchParameter(parentFilter);

        queryCount.setParameter("resourceFilter", resourceFilter);
        query.setParameter("resourceFilter", resourceFilter);
        queryCount.setParameter("parentFilter", parentFilter);
        query.setParameter("parentFilter", parentFilter);
        queryCount.setParameter("startTime", startTime);
        query.setParameter("startTime", startTime);
        queryCount.setParameter("endTime", endTime);
        query.setParameter("endTime", endTime);
        queryCount.setParameter("status", status);
        query.setParameter("status", status);

        long totalCount = (Long) queryCount.getSingleResult();
        List<ResourceOperationHistoryComposite> results = query.getResultList();

        return new PageList<ResourceOperationHistoryComposite>(results, (int) totalCount, pc);
    }
}
