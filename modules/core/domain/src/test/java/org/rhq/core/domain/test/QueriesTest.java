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
package org.rhq.core.domain.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.InstalledPackageHistory;
import org.rhq.core.domain.content.PackageBits;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.measurement.MeasurementOOB;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.resource.CreateResourceHistory;
import org.rhq.core.domain.resource.DeleteResourceHistory;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.shared.TransactionCallback;
import org.rhq.core.util.exception.ThrowableUtil;
import org.testng.annotations.Test;

/**
 * Use this to explicitly test any of our named queries with any set of parameters. Useful to make sure these run on
 * both postgres and oracle, specifically those that try to do select distinct queries while retrieve LOB columns.
 */
@Test(groups = "integration.ejb3")
public class QueriesTest extends AbstractEJB3Test {
    private Map<String, Map<String, Object>> queries; // here just so we dont have to pass it to the add()

    public void testQueries() throws Exception {

        queries = new HashMap<String, Map<String, Object>>();

        //////////////////////////////////////////
        // ADD YOUR QUERIES WITH THEIR PARAMS HERE
        add(Configuration.QUERY_GET_PLUGIN_CONFIG_MAP_BY_GROUP_ID, new Object[] { "resourceGroupId", 1 });
        add(Configuration.QUERY_GET_RESOURCE_CONFIG_MAP_BY_GROUP_ID, new Object[] { "resourceGroupId", 1 });
        add(PackageVersion.QUERY_FIND_BY_REPO_ID, new Object[] { "repoId", 1 });
        add(PackageVersion.QUERY_FIND_METADATA_BY_RESOURCE_ID, new Object[] { "resourceId", 1 });
        add(PackageVersion.QUERY_FIND_BY_REPO_ID_WITH_PACKAGE, new Object[] { "repoId", 1 });
        add(MeasurementOOB.COUNT_FOR_DATE, new Object[] { "timestamp", 1L });

        add(Repo.QUERY_FIND_AVAILABLE_REPO_COMPOSITES_BY_RESOURCE_ID_ADMIN, new Object[] { "resourceId", 1 });

        add(PackageBits.QUERY_PACKAGE_BITS_LOADED_STATUS_PACKAGE_VERSION_ID, new Object[] { "id", 1 });
        add(CreateResourceHistory.QUERY_FIND_BY_PARENT_RESOURCE_ID, new Object[] { "id", 1, "startTime", null,
            "endTime", null });
        add(DeleteResourceHistory.QUERY_FIND_BY_PARENT_RESOURCE_ID, new Object[] { "id", 1, "startTime", null,
            "endTime", null });
        add(ContentSource.QUERY_FIND_ALL_WITH_CONFIG, new Object[] {});

        add(InstalledPackageHistory.QUERY_FIND_BY_CSR_ID_AND_PKG_VER_ID, new Object[] { "contentServiceRequestId", 1,
            "packageVersionId", 1 });

        add(PackageVersion.QUERY_GET_PKG_BITS_LENGTH_BY_PKG_DETAILS_AND_RES_ID, new Object[] { "packageName", "foo",
            "packageTypeName", "bar", "resourceId", 1, "architectureName", "blah", "version", "ver" });

        add(MeasurementBaseline.QUERY_FIND_BY_COMPUTE_TIME, new Object[] { "computeTime", 1L, "numericType",
            NumericType.DYNAMIC });

        Object a[] = new Object[]{ "agentId", null };
        Object ac[] = new Object[]{ "agentId", null, "category", null };
        add(AlertCondition.QUERY_BY_CATEGORY_AVAILABILITY, ac);
        add(AlertCondition.QUERY_BY_CATEGORY_BASELINE, a);
        add(AlertCondition.QUERY_BY_CATEGORY_CHANGE, a);
        add(AlertCondition.QUERY_BY_CATEGORY_CONTROL, a);
        add(AlertCondition.QUERY_BY_CATEGORY_COUNT_BASELINE, a);
        add(AlertCondition.QUERY_BY_CATEGORY_COUNT_PARAMETERIZED, ac);
        add(AlertCondition.QUERY_BY_CATEGORY_DRIFT, a);
        add(AlertCondition.QUERY_BY_CATEGORY_EVENT, a);
        add(AlertCondition.QUERY_BY_CATEGORY_RANGE, a);
        add(AlertCondition.QUERY_BY_CATEGORY_RESOURCE_CONFIG, a);
        add(AlertCondition.QUERY_BY_CATEGORY_THRESHOLD, a);
        add(AlertCondition.QUERY_BY_CATEGORY_TRAIT, a);

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

    public void testAsyncUninventory() throws Exception {

        executeInTransaction(new TransactionCallback() {

            public void execute() throws Exception {

                EntityManager entityManager = getEntityManager();
                Query q = entityManager.createNamedQuery(Resource.QUERY_MARK_RESOURCES_FOR_ASYNC_DELETION_QUICK);
                List<Integer> ids = new ArrayList<Integer>();
                ids.add(1);
                q.setParameter("resourceIds", ids);
                q.setParameter("status", InventoryStatus.UNINVENTORIED);
                q.executeUpdate();
            }
        });
    }

    public void testLongVarChar() throws Exception {

        executeInTransaction(new TransactionCallback() {

            public void execute() throws Exception {

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
        });
    }
}