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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.composite.AlertDefinitionComposite;
import org.rhq.core.domain.alert.composite.AlertHistoryComposite;
import org.rhq.core.domain.alert.notification.AlertNotificationLog;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;

/**
 * @author Joseph Marques
 */
@Stateless
public class AlertSubsystemManagerBean implements AlertSubsystemManagerLocal {

    private final Log log = LogFactory.getLog(AlertSubsystemManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    @EJB
    private AlertManagerLocal alertManager;

    @SuppressWarnings("unchecked")
    public PageList<AlertHistoryComposite> getAlertHistories(Subject subject, String resourceFilter,
        String parentFilter, Long startTime, Long endTime, AlertConditionCategory category, PageControl pc) {
        pc.initDefaultOrderingField("a.id", PageOrdering.DESC);

        String queryName = null;
        if (authorizationManager.isInventoryManager(subject)) {
            queryName = Alert.QUERY_FIND_ALL_COMPOSITES_ADMIN;
        } else {
            queryName = Alert.QUERY_FIND_ALL_COMPOSITES;
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
        queryCount.setParameter("category", category);
        query.setParameter("category", category);

        long totalCount = (Long) queryCount.getSingleResult();
        List<AlertHistoryComposite> results = query.getResultList();

        for (AlertHistoryComposite composite : results) {
            fetchCollectionFields(composite.getAlert());
        }

        return new PageList<AlertHistoryComposite>(results, (int) totalCount, pc);
    }

    @SuppressWarnings("unchecked")
    public PageList<AlertDefinitionComposite> getAlertDefinitions(Subject subject, String resourceFilter,
        String parentFilter, Long startTime, Long endTime, AlertConditionCategory category, PageControl pc) {
        pc.initDefaultOrderingField("ad.id", PageOrdering.DESC);

        String queryName = null;
        if (authorizationManager.isInventoryManager(subject)) {
            queryName = AlertDefinition.QUERY_FIND_ALL_COMPOSITES_ADMIN;
        } else {
            queryName = AlertDefinition.QUERY_FIND_ALL_COMPOSITES;
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
        queryCount.setParameter("category", category);
        query.setParameter("category", category);

        long totalCount = (Long) queryCount.getSingleResult();
        List<AlertDefinitionComposite> results = query.getResultList();

        for (AlertDefinitionComposite composite : results) {
            fetchCollectionFields(composite.getAlertDefinition());
        }

        return new PageList<AlertDefinitionComposite>(results, (int) totalCount, pc);
    }

    private void fetchCollectionFields(Alert alert) {
        for (AlertConditionLog log : alert.getConditionLogs()) {
            fetchCollectionFields(log.getCondition());
        }
    }

    private void fetchCollectionFields(AlertDefinition alertDefinition) {
        for (AlertCondition condition : alertDefinition.getConditions()) {
            fetchCollectionFields(condition);
        }
    }

    private void fetchCollectionFields(AlertCondition alertCondition) {
        if (alertCondition != null) {
            alertCondition.getName(); // eagerly load non-null alert condition logs
            if (alertCondition.getMeasurementDefinition() != null) {
                // this ManyToOne is not lazy by default, so eager load MeasurementDefinition for condition
                alertCondition.getMeasurementDefinition().getName();
            }
        }
    }

    public int deleteAlertHistories(Subject subject, Integer[] historyIds) {
        int removed = 0;
        for (Integer id : historyIds) {
            try {
                Alert alert = entityManager.find(Alert.class, id);
                if (alert != null) {
                    AlertNotificationLog anl = alert.getAlertNotificationLog();
                    entityManager.remove(anl);
                    entityManager.remove(alert); // condition logs will be removed with entity cascading
                }
                removed++;
            } catch (Throwable t) {
                if (log.isDebugEnabled()) {
                    log.error("Could not remove alert[id=" + id + "] from subsystme view: " + t.getMessage(), t);
                } else {
                    log.error("Could not remove alert[id=" + id + "] from subsystme view: " + t.getMessage());
                }
            }
        }
        return removed;
    }

    public int purgeAllAlertHistories(Subject subject) {
        return alertManager.deleteAlerts(0, System.currentTimeMillis());
    }

}
