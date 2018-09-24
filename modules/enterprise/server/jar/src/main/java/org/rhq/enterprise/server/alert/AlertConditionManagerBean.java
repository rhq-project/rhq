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

import java.util.Iterator;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.composite.AbstractAlertConditionCategoryComposite;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.server.PersistenceUtility;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.rest.StuffNotFoundException;

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
    
    @EJB
    private AlertDefinitionManagerLocal alertDefinitionManager;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Override
    public Integer getAlertDefinitionByConditionIdNewTx(int alertConditionId) {
        try {
            Query query = entityManager.createNamedQuery(AlertDefinition.QUERY_FIND_DEFINITION_ID_BY_CONDITION_ID);
            query.setParameter("alertConditionId", alertConditionId);
            Integer alertDefinitionId = (Integer) query.getSingleResult();
            return alertDefinitionId;
        } catch (NoResultException nre) {
            return null; // we always want this method to return
        }
    }

    public AlertCondition getAlertConditionById(int alertConditionId) {
        return entityManager.find(AlertCondition.class, alertConditionId);
    }

    @SuppressWarnings("unchecked")
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Override
    public PageList<? extends AbstractAlertConditionCategoryComposite> getAlertConditionComposites(Subject user,
        Integer agentId, AlertConditionCategory category, PageControl pageControl) {
        if (authorizationManager.isOverlord(user) == false) {
            throw new PermissionException("User [" + user.getName() + "] does not have permission to call "
                + "getAlertConditionComposites; only the overlord has that right");
        }

        String queryName = null;
        String countQueryName = AlertCondition.QUERY_BY_CATEGORY_COUNT_PARAMETERIZED; // default

        if (category == AlertConditionCategory.BASELINE) {
            queryName = AlertCondition.QUERY_BY_CATEGORY_BASELINE;
            countQueryName = AlertCondition.QUERY_BY_CATEGORY_COUNT_BASELINE; // no countQuery parameters needed

        } else if (category == AlertConditionCategory.CHANGE) {
            queryName = AlertCondition.QUERY_BY_CATEGORY_CHANGE;

        } else if (category == AlertConditionCategory.TRAIT) {
            queryName = AlertCondition.QUERY_BY_CATEGORY_TRAIT;

        } else if (category == AlertConditionCategory.AVAILABILITY) {
            queryName = AlertCondition.QUERY_BY_CATEGORY_AVAILABILITY;

        } else if (category == AlertConditionCategory.AVAIL_DURATION) {
            // The duration category can use the same availability composite query, just needs a parameter set
            queryName = AlertCondition.QUERY_BY_CATEGORY_AVAILABILITY;

        } else if (category == AlertConditionCategory.CONTROL) {
            queryName = AlertCondition.QUERY_BY_CATEGORY_CONTROL;

        } else if (category == AlertConditionCategory.THRESHOLD) {
            queryName = AlertCondition.QUERY_BY_CATEGORY_THRESHOLD;

        } else if (category == AlertConditionCategory.EVENT) {
            queryName = AlertCondition.QUERY_BY_CATEGORY_EVENT;

        } else if (category == AlertConditionCategory.RESOURCE_CONFIG) {
            queryName = AlertCondition.QUERY_BY_CATEGORY_RESOURCE_CONFIG;

        } else if (category == AlertConditionCategory.DRIFT) {
            queryName = AlertCondition.QUERY_BY_CATEGORY_DRIFT;

        } else if (category == AlertConditionCategory.RANGE) {
            queryName = AlertCondition.QUERY_BY_CATEGORY_RANGE;

        } else {
            throw new IllegalArgumentException("getAlertConditionComposites does not support category '" + category
                + "'");
        }

        Query query = entityManager.createNamedQuery(queryName);
        PersistenceUtility.setDataPage(query, pageControl);
        Query queryCount = entityManager.createNamedQuery(countQueryName);

        // The following query is used in two places, and needs the category set to get different results
        if (queryName == AlertCondition.QUERY_BY_CATEGORY_AVAILABILITY) {
            query.setParameter("category", category);
        }

        // the default parameterized count query needs a parameter
        if (countQueryName == AlertCondition.QUERY_BY_CATEGORY_COUNT_PARAMETERIZED) {
            queryCount.setParameter("category", category);
        }
        query.setParameter("agentId", agentId);
        queryCount.setParameter("agentId", agentId);

        long totalCount = (Long) queryCount.getSingleResult();
        List<? extends AbstractAlertConditionCategoryComposite> list = query.getResultList();
        LOG.debug("Found " + totalCount + " elements of type '" + category + "', list was size " + list.size());

        return new PageList<AbstractAlertConditionCategoryComposite>(list, (int) totalCount, pageControl);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Override
    public InventoryStatus getResourceStatusByConditionIdNewTx(int alertConditionId) {
        try {
            Query query = entityManager.createNamedQuery(AlertCondition.QUERY_FIND_RESOURCE_STATUS_BY_CONDITION_ID);
            query.setParameter("alertConditionId", alertConditionId);
            InventoryStatus status = (InventoryStatus) query.getSingleResult();

            // a resource was marked for asynchronous uninventory, but not actually deleted yet
            return status;
        } catch (NoResultException nre) {
            // the resource was already deleted asynchronously, tell the caller as much
            return InventoryStatus.UNINVENTORIED;
        }
    }

    private int unlinkDeletedAlertConditions() {
        Query unlinkQuery = entityManager.createNamedQuery(AlertCondition.QUERY_UPDATE_ORPHANED_DEFINITIONS);
        return unlinkQuery.executeUpdate();
    }

    @Override
    public int purgeOrphanedAlertConditions() {
        unlinkDeletedAlertConditions(); // Done here to avoid changes to the Remote API
        Query purgeQuery = entityManager.createNamedQuery(AlertCondition.QUERY_DELETE_ORPHANED);
        return purgeQuery.executeUpdate();
    }

}