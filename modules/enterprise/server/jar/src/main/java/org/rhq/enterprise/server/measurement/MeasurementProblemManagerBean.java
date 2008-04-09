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

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.annotation.ejb.TransactionTimeout;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.oob.MeasurementOutOfBounds;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ProblemResourceComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.core.util.StopWatch;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;

/**
 * A manager for working with problems such as out-of-bounds measurements.
 */
@Stateless
public class MeasurementProblemManagerBean implements MeasurementProblemManagerLocal {
    private final Log log = LogFactory.getLog(MeasurementProblemManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    @SuppressWarnings("unchecked")
    public PageList<ProblemResourceComposite> findProblemResources(Subject subject, long oldestDate,
        PageControl pageControl) {
        pageControl.initDefaultOrderingField("res.name");

        Query queryCount;
        Query query;
        if (authorizationManager.isInventoryManager(subject)) {
            queryCount = entityManager.createNamedQuery(Resource.QUERY_FIND_PROBLEM_RESOURCES_COUNT_ADMIN);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Resource.QUERY_FIND_PROBLEM_RESOURCES_ADMIN, pageControl);
        } else {
            queryCount = entityManager.createNamedQuery(Resource.QUERY_FIND_PROBLEM_RESOURCES_COUNT);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager, Resource.QUERY_FIND_PROBLEM_RESOURCES,
                pageControl);
            queryCount.setParameter("subject", subject);
            query.setParameter("subject", subject);
        }

        queryCount.setParameter("oldest", oldestDate);
        query.setParameter("oldest", oldestDate);

        long count = (Long) queryCount.getSingleResult();
        List<ProblemResourceComposite> results = query.getResultList();

        return new PageList<ProblemResourceComposite>(results, (int) count, pageControl);
    }

    public void addMeasurementOutOfBounds(MeasurementOutOfBounds oob) {
        entityManager.persist(oob);
    }

    public void deleteMeasurementOutOfBounds(MeasurementOutOfBounds oob) {
        oob = entityManager.find(MeasurementOutOfBounds.class, oob.getId());
        if (oob != null) {
            entityManager.remove(oob);
        }
    }

    public MeasurementOutOfBounds loadMeasurementOutOfBounds(int oobId) {
        Query q = entityManager.createNamedQuery(MeasurementOutOfBounds.QUERY_FIND_BY_ID_AND_LOAD);
        q.setParameter("id", oobId);
        return (MeasurementOutOfBounds) q.getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public PageList<MeasurementOutOfBounds> findAllMeasurementOutOfBounds(Subject subject, long oldestDate,
        PageControl pageControl) {
        pageControl.initDefaultOrderingField("oob.occurred", PageOrdering.DESC);

        Query queryCount;
        Query query;
        if (authorizationManager.isInventoryManager(subject)) {
            queryCount = PersistenceUtility
                .createCountQuery(entityManager, MeasurementOutOfBounds.QUERY_FIND_ALL_ADMIN);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                MeasurementOutOfBounds.QUERY_FIND_ALL_ADMIN, pageControl);
        } else {
            queryCount = PersistenceUtility.createCountQuery(entityManager, MeasurementOutOfBounds.QUERY_FIND_ALL);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager, MeasurementOutOfBounds.QUERY_FIND_ALL,
                pageControl);
            queryCount.setParameter("subject", subject);
            query.setParameter("subject", subject);
        }

        queryCount.setParameter("oldest", oldestDate);
        query.setParameter("oldest", oldestDate);

        long count = (Long) queryCount.getSingleResult();
        List<MeasurementOutOfBounds> results = query.getResultList();

        return new PageList<MeasurementOutOfBounds>(results, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<MeasurementOutOfBounds> findResourceMeasurementOutOfBounds(Subject subject, long oldestDate,
        int resourceId, PageControl pageControl) {
        pageControl.initDefaultOrderingField("oob.occurred", PageOrdering.DESC);

        Query queryCount;
        Query query;
        if (authorizationManager.isInventoryManager(subject)) {
            queryCount = PersistenceUtility.createCountQuery(entityManager,
                MeasurementOutOfBounds.QUERY_FIND_FOR_RESOURCE_ADMIN);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                MeasurementOutOfBounds.QUERY_FIND_FOR_RESOURCE_ADMIN, pageControl);
        } else {
            queryCount = PersistenceUtility.createCountQuery(entityManager,
                MeasurementOutOfBounds.QUERY_FIND_FOR_RESOURCE);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                MeasurementOutOfBounds.QUERY_FIND_FOR_RESOURCE, pageControl);
            queryCount.setParameter("subject", subject);
            query.setParameter("subject", subject);
        }

        queryCount.setParameter("oldest", oldestDate);
        query.setParameter("oldest", oldestDate);
        queryCount.setParameter("resourceId", resourceId);
        query.setParameter("resourceId", resourceId);

        long count = (Long) queryCount.getSingleResult();
        List<MeasurementOutOfBounds> results = query.getResultList();

        return new PageList<MeasurementOutOfBounds>(results, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<MeasurementOutOfBounds> findScheduleMeasurementOutOfBounds(Subject subject, long oldestDate,
        int scheduleId, PageControl pageControl) {
        pageControl.initDefaultOrderingField("oob.occurred", PageOrdering.DESC);

        Query queryCount;
        Query query;
        if (authorizationManager.isInventoryManager(subject)) {
            queryCount = PersistenceUtility.createCountQuery(entityManager,
                MeasurementOutOfBounds.QUERY_FIND_FOR_SCHEDULE_ADMIN);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                MeasurementOutOfBounds.QUERY_FIND_FOR_SCHEDULE_ADMIN, pageControl);
        } else {
            queryCount = PersistenceUtility.createCountQuery(entityManager,
                MeasurementOutOfBounds.QUERY_FIND_FOR_SCHEDULE);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                MeasurementOutOfBounds.QUERY_FIND_FOR_SCHEDULE, pageControl);
            queryCount.setParameter("subject", subject);
            query.setParameter("subject", subject);
        }

        queryCount.setParameter("begin", oldestDate);
        query.setParameter("begin", oldestDate);

        long now = System.currentTimeMillis();
        queryCount.setParameter("end", now);
        query.setParameter("end", now);
        queryCount.setParameter("scheduleId", scheduleId);
        query.setParameter("scheduleId", scheduleId);

        long count = (Long) queryCount.getSingleResult();
        List<MeasurementOutOfBounds> results = query.getResultList();

        return new PageList<MeasurementOutOfBounds>(results, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    public int findScheduleMeasurementOutOfBoundsCount(long begin, long end, int scheduleId) {
        Query queryCount = PersistenceUtility.createCountQuery(entityManager,
            MeasurementOutOfBounds.QUERY_FIND_FOR_SCHEDULE_ADMIN);

        queryCount.setParameter("begin", begin);
        queryCount.setParameter("end", end);
        queryCount.setParameter("scheduleId", scheduleId);

        long count = (Long) queryCount.getSingleResult();

        return (int) count;
    }

    /*
     * Get the oob counts for the passed schedules at once and stuff them in a map<scheduleId,oobCount> @see
     * org.rhq.enterprise.server.measurement.MeasurementProblemManagerLocal#getMeasurementSchedulesOOBCount(long, long,
     * java.util.List)
     */
    @SuppressWarnings("unchecked")
    public Map<Integer, Integer> getMeasurementSchedulesOOBCount(long begin, long end, List<Integer> scheduleIds) {
        if ((scheduleIds == null) || (scheduleIds.size() == 0) || (end < begin)) {
            return new HashMap<Integer, Integer>();
        }

        final int BATCH_SIZE = 1000;

        int numSched = scheduleIds.size();
        int rounds = (numSched / BATCH_SIZE) + 1;

        Map<Integer, Integer> resMap = new HashMap<Integer, Integer>();

        // iterate over the passed schedules ids when we have more than 1000 of them, as some
        // databases bail out with more than 1000 resources in IN () clauses.
        for (int round = 0; round < rounds; round++) {
            int fromIndex = round * BATCH_SIZE;
            int toIndex = fromIndex + BATCH_SIZE;
            if (toIndex > numSched) // don't run over the end of the list
                toIndex = numSched;
            if (fromIndex == toIndex)
                continue;

            List<Integer> scheds = scheduleIds.subList(fromIndex, toIndex);

            Query q = entityManager.createNamedQuery(MeasurementOutOfBounds.QUERY_COUNT_FOR_SCHEDULE_IDS_ADMIN);
            q.setParameter("begin", begin);
            q.setParameter("end", end);
            q.setParameter("scheduleIds", scheds);

            List<Object[]> ret = q.getResultList();
            if (ret.size() > 0) {
                for (Object[] obj : ret) {
                    Integer scheduleId = (Integer) obj[0];
                    Long tmp = (Long) obj[1];
                    int oobCount = tmp.intValue();
                    resMap.put(scheduleId, oobCount);
                }
            }
        }

        // Now fill in those schedules without return value to have an oobCount of 0
        for (int scheduleId : scheduleIds) {
            if (!resMap.containsKey(scheduleId)) {
                resMap.put(scheduleId, 0);
            }
        }

        return resMap;
    }

    public int findMeasurementOutOfBoundCountForDefinitionAndResources(long begin, long end, int definitionId,
        Collection<Resource> resources) {
        if ((definitionId == 0) || (resources == null) || (resources.size() == 0)) {
            return 0;
        }

        Query q = PersistenceUtility.createCountQuery(entityManager,
            MeasurementOutOfBounds.QUERY_FIND_FOR_DEFINITION_AND_RESOURCEIDS_ADMIN);
        q.setParameter("begin", begin);
        q.setParameter("end", end);
        q.setParameter("definitionId", definitionId);
        q.setParameter("resources", resources);

        long count = (Long) q.getSingleResult();

        return (int) count;
    }

    /**
     * Purge OOB data older than a given time.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @TransactionTimeout(30 * 60 * 1000)
    public int purgeMeasurementOOBs(Date purgeAfter) {
        log.debug("Purging OOB data older than " + purgeAfter);
        StopWatch watch = new StopWatch();
        Query q = entityManager.createNamedQuery(MeasurementOutOfBounds.QUERY_DELETE_BY_TIME);
        q.setParameter("oldest", purgeAfter.getTime());
        int rows = q.executeUpdate();
        log.info("Done purging [" + rows + "] OOBs older than " + purgeAfter + " in (" + ((watch.getElapsed()) / 1000L)
            + " seconds)");
        return rows;
    }

}