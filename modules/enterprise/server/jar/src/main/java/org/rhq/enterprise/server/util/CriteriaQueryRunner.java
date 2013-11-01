/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.enterprise.server.util;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;

import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.criteria.Criteria.Restriction;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

public class CriteriaQueryRunner<T> {

    private static final Log LOG = LogFactory.getLog(CriteriaQueryRunner.class);

    private Criteria criteria;
    private CriteriaQueryGenerator queryGenerator;
    private EntityManager entityManager;
    private boolean automaticFetching;

    private static final QueryUtility.PagedDataFetchSettings DATA_FETCH_SETTINGS;
    static {
        DATA_FETCH_SETTINGS = new QueryUtility.PagedDataFetchSettings();
        DATA_FETCH_SETTINGS.setThrowOnMaxAttempts(true);
    }

    public CriteriaQueryRunner(Criteria criteria, CriteriaQueryGenerator queryGenerator, EntityManager entityManager) {
        this(criteria, queryGenerator, entityManager, true);
    }

    public CriteriaQueryRunner(Criteria criteria, CriteriaQueryGenerator queryGenerator, EntityManager entityManager,
        boolean automaticFetching) {
        this.criteria = criteria;
        this.queryGenerator = queryGenerator;
        this.entityManager = entityManager;
        this.automaticFetching = automaticFetching;
    }

    @SuppressWarnings("unchecked")
    public PageList<T> execute() {
        PageList<T> results;
        PageControl pageControl = CriteriaQueryGenerator.getPageControl(criteria);

        Restriction criteriaRestriction = criteria.getRestriction();
        if (criteriaRestriction == null) {
            try {
                results = QueryUtility.fetchPagedDataAndCount(queryGenerator.getQuery(entityManager),
                    queryGenerator.getCountQuery(entityManager), pageControl, DATA_FETCH_SETTINGS);
            } catch (PhantomReadMaxAttemptsExceededException e) {
                LOG.warn(
                    "Could not get consistent results of the paged data and a total count for "
                        + CriteriaUtil.toString(criteria) + ". After " + e.getNumberOfAttempts()
                        + " attempts, the collection size" + " is " + e.getList().size()
                        + ", while the count query reports " + e.getList().getTotalSize() + " for " + pageControl
                        + ". The discrepancy has not cleared up in " + e.getMillisecondsSpentTrying()
                        + "ms so we're giving up, "
                        + "returning inconsistent results. Note that is most possibly NOT an error. It is likely "
                        + "caused by concurrent database activity that changes the contents of the database that the "
                        + "criteria query is querying.", new Exception());

                results = (PageList<T>) e.getList();
            }

            finalizeCollection(results);
            if (LOG.isDebugEnabled()) {
                LOG.debug("restriction=" + criteriaRestriction + ", resultSize=" + results.size() + ", resultCount="
                    + results.getTotalSize());
            }

        } else if (criteriaRestriction == Restriction.COUNT_ONLY) {
            results = new PageList<T>(getCount(), pageControl);
            if (LOG.isDebugEnabled()) {
                LOG.debug("restriction=" + criteriaRestriction + ", resultCount=" + results.getTotalSize());
            }

        } else if (criteriaRestriction == Restriction.COLLECTION_ONLY) {
            results = new PageList<T>(getCollection(), pageControl);
            if (LOG.isDebugEnabled()) {
                LOG.debug("restriction=" + criteriaRestriction + ", resultSize=" + results.size());
            }

        } else {
            throw new IllegalArgumentException(this.getClass().getSimpleName()
                + " does not support query execution for criteria with " + Restriction.class.getSimpleName() + " "
                + criteriaRestriction);
        }

        return results;
    }

    @SuppressWarnings("unchecked")
    private Collection<? extends T> getCollection() {
        Query query = queryGenerator.getQuery(entityManager);
        List<T> results = query.getResultList();

        finalizeCollection(results);

        return results;
    }

    private void finalizeCollection(List<?> results) {
        /*
         * suppression of auto-fetch useful in cases where alterProject(String) was called on the generator, which
         * changed the return type of the result set from List<T> to something else.  in that case, the caller to
         * this method must, as necessary, perform the fetch manually.
         */
        if (automaticFetching) {
            if (!queryGenerator.getPersistentBagFields().isEmpty()) {
                for (Object entity : results) {
                    initPersistentBags(entity);
                }
            }
            if (!queryGenerator.getJoinFetchFields().isEmpty()) {
                for (Object entity : results) {
                    initJoinFetchFields(entity);
                }
            }
        }
    }

    private int getCount() {
        Query countQuery = queryGenerator.getCountQuery(entityManager);
        long count = (Long) countQuery.getSingleResult();

        return (int) count;
    }

    public void initFetchFields(Object entity) {
        initPersistentBags(entity);
        initJoinFetchFields(entity);
    }

    private void initPersistentBags(Object entity) {
        for (Field persistentBagField : queryGenerator.getPersistentBagFields()) {
            initialize(entity, persistentBagField);
        }
    }

    private void initJoinFetchFields(Object entity) {
        for (Field joinFetchField : queryGenerator.getJoinFetchFields()) {
            initialize(entity, joinFetchField);
        }
    }

    /**
     * @param entity
     * @param field
     * @return true if the field was successfully initialize, false if there was any problem
     */
    private boolean initialize(Object entity, Field field) {
        boolean initialized = true;

        try {
            field.setAccessible(true);

            Object instance = field.get(entity);

            Hibernate.initialize(instance);

            if (instance instanceof Iterable) {
                Iterator<?> it = ((Iterable<?>) instance).iterator();
                while (it.hasNext())
                    it.next();
            }
        } catch (EntityNotFoundException e) {
            // TODO: See BZ 1025756, we should try and get rid of required join fields that allow 0.
            if (LOG.isDebugEnabled()) {
                String msg = "Could not initialize [" + field
                    + "]. This may happen if this entity field is required, lazy-loaded/proxied, but allows ID of 0:";
                LOG.debug(msg, e);
            }
            initialized = false;

        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Could not initialize " + field + "  Following exception has caused the problem: ", e);
            } else {
                LOG.warn("Could not initialize " + field);
            }
            initialized = false;
        }

        // instead of likely leaving an unloaded Hibernate proxy, set null
        if (!initialized) {
            try {
                field.set(entity, null);
            } catch (Throwable t) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Could not nullify non-initialized field [" + field + "].", t);
                }
            }
        }

        return initialized;
    }
}
