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
package org.rhq.enterprise.server.alert;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.composite.AbstractAlertConditionCategoryComposite;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;

/**
 * @author Joseph Marques
 */

@Stateless
public class AlertConditionManagerBean implements AlertConditionManagerLocal {

    private static final Log LOG = LogFactory.getLog(AlertConditionManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    public AlertCondition getAlertConditionById(int alertConditionId) {
        return entityManager.find(AlertCondition.class, alertConditionId);
    }

    @SuppressWarnings("unchecked")
    public PageList<AlertDefinition> getAllAlertDefinitionsWithConditions(Subject user, PageControl pageControl) {
        if (authorizationManager.isOverlord(user) == false) {
            throw new PermissionException("User [" + user.getName() + "] does not have permission to call "
                + "getAllAlertDefinitionsWithConditions; only the overlord has that right");
        }

        Query queryCount = PersistenceUtility.createCountQuery(entityManager,
            AlertDefinition.QUERY_FIND_ALL_WITH_CONDITIONS);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            AlertDefinition.QUERY_FIND_ALL_WITH_CONDITIONS, pageControl);

        long totalCount = (Long) queryCount.getSingleResult();
        List<AlertDefinition> list = query.getResultList();

        return new PageList<AlertDefinition>(list, (int) totalCount, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<? extends AbstractAlertConditionCategoryComposite> getAlertConditionComposites(Subject user,
        AlertConditionCategory category, PageControl pageControl) {
        if (authorizationManager.isOverlord(user) == false) {
            throw new PermissionException("User [" + user.getName() + "] does not have permission to call "
                + "getAlertConditionComposites; only the overlord has that right");
        }

        String queryName = null;

        if (category == AlertConditionCategory.BASELINE) {
            queryName = AlertCondition.QUERY_BY_CATEGORY_BASELINE;

        } else if (category == AlertConditionCategory.CHANGE) {
            queryName = AlertCondition.QUERY_BY_CATEGORY_CHANGE;

        } else if (category == AlertConditionCategory.TRAIT) {
            queryName = AlertCondition.QUERY_BY_CATEGORY_TRAIT;

        } else if (category == AlertConditionCategory.AVAILABILITY) {
            queryName = AlertCondition.QUERY_BY_CATEGORY_AVAILABILITY;

        } else if (category == AlertConditionCategory.CONTROL) {
            queryName = AlertCondition.QUERY_BY_CATEGORY_CONTROL;

        } else if (category == AlertConditionCategory.THRESHOLD) {
            queryName = AlertCondition.QUERY_BY_CATEGORY_THRESHOLD;

        } else if (category == AlertConditionCategory.EVENT) {
            queryName = AlertCondition.QUERY_BY_CATEGORY_EVENT;

        } else {
            throw new IllegalArgumentException("getAlertConditionComposites does not support category '" + category
                + "'");
        }

        Query query = entityManager.createNamedQuery(queryName);
        Query queryCount = entityManager.createNamedQuery(AlertCondition.QUERY_BY_CATEGORY_COUNT_PARAMETERIZED);
        queryCount.setParameter("category", category);

        long totalCount = (Long) queryCount.getSingleResult();
        List<? extends AbstractAlertConditionCategoryComposite> list = query.getResultList();
        LOG.info("Found " + totalCount + " elements of type '" + category + "', list was size " + list.size());

        return new PageList<AbstractAlertConditionCategoryComposite>(list, (int) totalCount, pageControl);
    }

}