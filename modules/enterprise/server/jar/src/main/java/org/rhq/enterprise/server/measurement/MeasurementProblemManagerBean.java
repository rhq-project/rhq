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
package org.rhq.enterprise.server.measurement;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ProblemResourceComposite;
import org.rhq.core.domain.server.PersistenceUtility;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;

/**
 * A manager for working with problems such as out-of-bounds measurements.
 */
@Stateless
public class MeasurementProblemManagerBean implements MeasurementProblemManagerLocal, MeasurementProblemManagerRemote {
    @SuppressWarnings("unused")
    private final Log log = LogFactory.getLog(MeasurementProblemManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    @SuppressWarnings("unchecked")
    public PageList<ProblemResourceComposite> findProblemResources(Subject subject, long oldestDate, PageControl pc) {
        pc.initDefaultOrderingField("COUNT(DISTINCT alert.id)", PageOrdering.DESC);
        pc.addDefaultOrderingField("LENGTH(res.ancestry)");
        pc.addDefaultOrderingField("res.name");

        Query queryCount;
        Query query;
        if (authorizationManager.isInventoryManager(subject)) {
            queryCount = entityManager.createNamedQuery(Resource.QUERY_FIND_PROBLEM_RESOURCES_ALERT_COUNT_ADMIN);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Resource.QUERY_FIND_PROBLEM_RESOURCES_ALERT_ADMIN, pc);
        } else {
            queryCount = entityManager.createNamedQuery(Resource.QUERY_FIND_PROBLEM_RESOURCES_ALERT_COUNT);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Resource.QUERY_FIND_PROBLEM_RESOURCES_ALERT, pc);
            queryCount.setParameter("subject", subject);
            query.setParameter("subject", subject);
        }

        queryCount.setParameter("oldest", oldestDate);
        query.setParameter("oldest", oldestDate);

        long count = (Long) queryCount.getSingleResult();
        List<ProblemResourceComposite> results = query.getResultList();

        return new PageList<ProblemResourceComposite>(results, (int) count, pc);
    }

}