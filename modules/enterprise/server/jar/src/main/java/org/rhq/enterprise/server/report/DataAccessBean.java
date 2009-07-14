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
package org.rhq.enterprise.server.report;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.RequiredPermission;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;


/**
 * This service provides jpql querying access to the core. You can pass in arbitrary
 * jpql and get back the object bound forms of the results. This is intended only for
 * external reporting functionality and should not be used with the enterprise core
 * to implement querying.
 *
 * @author Greg Hinkle
 */
@Stateless
public class DataAccessBean implements DataAccessLocal, DataAccessRemote {


    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;


    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public List<Object[]> executeQuery(Subject subject, String query) {
        Query q = entityManager.createQuery(query);

        return q.getResultList();
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public List<Object[]> executeQuery(Subject subject, String query, PageControl pageControl) {
        Query q = buildQuery(query, pageControl);

        return q.getResultList();
    }

    private Query buildQuery(String queryString, PageControl pageControl) {
        boolean first = true;
        StringBuilder queryBuilder = new StringBuilder(queryString);
        for (OrderingField orderingField : pageControl.getOrderingFieldsAsArray()) {
            if (first) {
                // TODO GH: We could see if there already is an order by clause and contribute or override it
                queryBuilder.append(" ORDER BY ");
                first = false;
            } else {
                queryBuilder.append(", ");
            }

            queryBuilder.append(orderingField.getField()).append(" ").append(orderingField.getOrdering());
        }

        Query query = entityManager.createQuery(queryBuilder.toString());

        if (pageControl.getPageSize() > 0) {
            query.setFirstResult(pageControl.getStartRow());
            query.setMaxResults(pageControl.getPageSize());
        }

        return query;
    }

}

