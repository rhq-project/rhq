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
package org.rhq.core.domain.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.TransactionManager;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.content.ContentServiceRequest;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.InstalledPackageHistory;
import org.rhq.core.domain.content.PackageBits;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.measurement.oob.MeasurementOutOfBounds;
import org.rhq.core.domain.resource.CreateResourceHistory;
import org.rhq.core.domain.resource.DeleteResourceHistory;
import org.rhq.core.util.exception.ThrowableUtil;

/**
 * Use this to explicitly test any of our named queries with any set of parameters. Useful to make sure these run on
 * both postgres and oracle, specifically those that try to do select distinct queries while retrieve LOB columns.
 */
@Test
public class QueriesTest extends AbstractEJB3Test {
    private Map<String, Map<String, Object>> queries; // here just so we dont have to pass it to the add()

    @AfterMethod
    public void afterMethod() throws Exception {
        try {
            TransactionManager tx = getTransactionManager();
            if (tx != null) {
                tx.rollback();
            }
        } catch (Exception who_cares) {
        }
    }

    public void testQueries() throws Exception {
        queries = new HashMap<String, Map<String, Object>>();

        //////////////////////////////////////////
        // ADD YOUR QUERIES WITH THEIR PARAMS HERE

        add(PackageVersion.QUERY_FIND_BY_CHANNEL_ID, new Object[] { "channelId", 1 });
        add(PackageVersion.QUERY_FIND_METADATA_BY_RESOURCE_ID, new Object[] { "resourceId", 1 });
        add(PackageVersion.QUERY_FIND_BY_CHANNEL_ID_WITH_PACKAGE, new Object[] { "channelId", 1 });
        add(ContentServiceRequest.QUERY_FIND_BY_ID_WITH_INSTALLED_PKG_HIST, new Object[] { "id", 1 });
        add(ContentServiceRequest.QUERY_FIND_BY_RESOURCE_WITH_INSTALLED_PKG_HIST, new Object[] { "resourceId", 1 });

        // great, empty sub-selects on oracle makes the query croak?
        //add(Channel.QUERY_FIND_AVAILABLE_CHANNEL_COMPOSITES_BY_RESOURCE_ID, new Object[] { "resourceId", 1 });

        add(PackageBits.QUERY_PACKAGE_BITS_LOADED_STATUS_PACKAGE_VERSION_ID, new Object[] { "id", 1 });
        add(CreateResourceHistory.QUERY_FIND_BY_PARENT_RESOURCE_ID, new Object[] { "id", 1 });
        add(DeleteResourceHistory.QUERY_FIND_BY_PARENT_RESOURCE_ID, new Object[] { "id", 1 });
        add(ContentSource.QUERY_FIND_ALL_WITH_CONFIG, new Object[] {});

        add(InstalledPackageHistory.QUERY_FIND_BY_CSR_ID_AND_PKG_VER_ID, new Object[] { "contentServiceRequestId", 1,
            "packageVersionId", 1 });

        add(PackageVersion.QUERY_GET_PKG_BITS_LENGTH_BY_PKG_DETAILS_AND_RES_ID, new Object[] { "packageName", "foo",
            "packageTypeName", "bar", "resourceId", 1, "architectureName", "blah", "version", "ver" });

        add(MeasurementBaseline.QUERY_FIND_BY_COMPUTE_TIME, new Object[] { "computeTime", 1L, "numericType",
            NumericType.DYNAMIC });

        add(MeasurementBaseline.QUERY_FIND_ALL_DYNAMIC_MEASUREMENT_BASELINES, new Object[] { "numericType",
            NumericType.DYNAMIC });

        add(MeasurementOutOfBounds.QUERY_COUNT_FOR_SCHEDULE_IDS_ADMIN, new Object[] { "scheduleIds",
            new ArrayList<Integer>(Arrays.asList(Integer.MAX_VALUE)), // an empty list caused a problem
            "begin", 0L, "end", System.currentTimeMillis() });

        //
        ////////////////////////////////////////////

        Map<String, Throwable> errors = new TreeMap<String, Throwable>(); // tree map so I sort output by query name

        for (Map.Entry<String, Map<String, Object>> entry : queries.entrySet()) {
            getTransactionManager().begin();

            Query q = getEntityManager().createNamedQuery(entry.getKey());
            for (Map.Entry<String, Object> param : entry.getValue().entrySet()) {
                q.setParameter(param.getKey(), param.getValue());
            }

            try {
                assert null != q.getResultList();
            } catch (Throwable t) {
                errors.put(entry.getKey(), t);
            } finally {
                getTransactionManager().rollback();
            }
        }

        if (errors.size() > 0) {
            System.out.println("---" + errors.size() + " OUT OF " + queries.size() + " QUERIES FAILED---");
            System.out.println("---" + errors.size() + " QUERIES THAT FAILED (start)---");

            int i = 1;
            for (Map.Entry<String, Throwable> entry : errors.entrySet()) {
                System.out.println();
                System.out.println("QUERY FAILURE #" + i++);
                System.out.println(entry.getKey() + " --> " + ThrowableUtil.getAllMessages(entry.getValue()));
            }

            System.out.println("---" + errors.size() + " QUERIES THAT FAILED (end)---");
            System.out.println("---" + errors.size() + " OUT OF " + queries.size() + " QUERIES FAILED---");

            assert false : "See stdout for the list of " + errors.size() + " queries that failed";
        }
    }

    private void add(String queryName, Object[] params) {
        Map<String, Object> paramsMap = new HashMap<String, Object>();
        for (int i = 0; i < params.length; i += 2) {
            paramsMap.put(params[i].toString(), params[i + 1]);
        }

        queries.put(queryName, paramsMap);
    }

    public void testLongVarChar() throws Exception {
        TransactionManager tx = getTransactionManager();
        tx.begin();

        EntityManager entityManager = getEntityManager();

        // I just want to see this tested even though I haven't seen this fail on oracle or postgres ever
        ContentSourceType cst = new ContentSourceType("testLongVarCharCST");
        entityManager.persist(cst);
        ContentSource cs = new ContentSource("testLongVarCharCS", cst);
        cs.setLoadErrorMessage("longvarchar column here");
        entityManager.persist(cs);

        Query q = entityManager.createNamedQuery(ContentSource.QUERY_FIND_BY_ID_WITH_CONFIG);
        ContentSource result = (ContentSource) q.setParameter("id", cs.getId()).getSingleResult();
        assert result != null;
        assert "longvarchar column here".equals(result.getLoadErrorMessage());
    }
}