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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.testng.Assert;
import org.testng.annotations.Test;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.auth.Principal;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleFile;
import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.bundle.BundleVersionRepo;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.common.SystemConfiguration;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.ContentServiceRequest;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceSyncResults;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.PackageBits;
import org.rhq.core.domain.content.PackageInstallationStep;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.PackageVersionContentSource;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.install.remote.AgentInstall;
import org.rhq.core.domain.measurement.MeasurementDataNumeric1D;
import org.rhq.core.domain.measurement.MeasurementDataNumeric1H;
import org.rhq.core.domain.measurement.MeasurementDataNumeric6H;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.calltime.CallTimeDataKey;
import org.rhq.core.domain.measurement.calltime.CallTimeDataValue;
import org.rhq.core.domain.operation.GroupOperationHistory;
import org.rhq.core.domain.operation.GroupOperationScheduleEntity;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.OperationScheduleEntity;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.ResourceOperationScheduleEntity;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.server.PersistenceUtility;
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.util.exception.ThrowableUtil;

@SuppressWarnings("unchecked")
public class QueryAllTest extends AbstractEJB3Test {
    protected SessionFactoryImplementor sessionFactory;

    private static final String[] ENTITY_NAMES = {
        // auth stuff
        Principal.class.getSimpleName(),
        Subject.class.getSimpleName(),
        Role.class.getSimpleName(),
        SystemConfiguration.class.getSimpleName(),

        // op stuff
        OperationDefinition.class.getSimpleName(),
        OperationHistory.class.getSimpleName(),
        ResourceOperationHistory.class.getSimpleName(),
        GroupOperationHistory.class.getSimpleName(),
        OperationScheduleEntity.class.getSimpleName(),
        ResourceOperationScheduleEntity.class.getSimpleName(),
        GroupOperationScheduleEntity.class.getSimpleName(),

        // alert stuff
        Alert.class.getSimpleName(),
        AlertCondition.class.getSimpleName(),
        AlertConditionLog.class.getSimpleName(),
        AlertDefinition.class.getSimpleName(),
        AlertNotification.class.getSimpleName(),

        // measurement stuff
        MeasurementDataTrait.class.getSimpleName(),
        MeasurementDataNumeric1H.class.getSimpleName(),
        MeasurementDataNumeric6H.class.getSimpleName(),
        MeasurementDataNumeric1D.class.getSimpleName(),
        MeasurementDefinition.class.getSimpleName(),
        MeasurementSchedule.class.getSimpleName(),

        // agent/inventory stuff
        // Plugin.class.getSimpleName(), do not test this - its got a blob that may be very large
        Agent.class.getSimpleName(),
        AgentInstall.class.getSimpleName(),
        Resource.class.getSimpleName(),
        ResourceType.class.getSimpleName(),

        // HA stuff
        Server.class.getSimpleName(),

        // content stuff
        Architecture.class.getSimpleName(), Repo.class.getSimpleName(), ContentServiceRequest.class.getSimpleName(),
        ContentSource.class.getSimpleName(), ContentSourceType.class.getSimpleName(),
        InstalledPackage.class.getSimpleName(), Package.class.getSimpleName(), PackageBits.class.getSimpleName(),
        PackageInstallationStep.class.getSimpleName(), PackageType.class.getSimpleName(),
        PackageVersion.class.getSimpleName(),
        PackageVersionContentSource.class.getSimpleName(),
        ContentSourceSyncResults.class.getSimpleName(),

        // bundle stuff
        BundleType.class.getSimpleName(), Bundle.class.getSimpleName(), BundleVersion.class.getSimpleName(),
        BundleVersionRepo.class.getSimpleName(), BundleDeployment.class.getSimpleName(),
        BundleFile.class.getSimpleName(), BundleResourceDeployment.class.getSimpleName(),

        // group stuff
        GroupDefinition.class.getSimpleName(), ResourceGroup.class.getSimpleName(),

        // event
        Event.class.getSimpleName(),

        // measurement calltime
        CallTimeDataKey.class.getSimpleName(), CallTimeDataValue.class.getSimpleName() };


    @Test(groups = "integration.ejb3")
    public void testQueryAllEntities() throws Exception {
        for (String entityName : ENTITY_NAMES) {
            System.out.print("Querying " + entityName);
            Query query = em.createQuery("SELECT e FROM " + entityName + " e");
            query.setMaxResults(100);
            try {
                Collection results = query.getResultList();
                System.out.println(", found: " + results.size());
                //             if (results.size() > 0) {
                //                System.out.println("### toString: " + results.iterator().next());
            } catch (Throwable t) {
                assert false : "Failed to query for entity " + entityName + ": "
                    + ThrowableUtil.getAllMessages(t, true);
            }
        }
    }

    @Test(groups = "integration.ejb3")
    public void testSimpleQueryWithOrderBy() throws Exception {
        Query q = em.createNamedQuery(SystemConfiguration.QUERY_FIND_ALL);
        List l = q.getResultList();
        int results = l.size();

        for (PageOrdering ord : PageOrdering.values()) {
            q = PersistenceUtility.createQueryWithOrderBy(em, SystemConfiguration.QUERY_FIND_ALL, new OrderingField(
                "propertyKey", ord));
            l = q.getResultList();
            Assert.assertEquals(results, l.size());
        }
    }

    @Test(groups = "integration.ejb3")
    public void testSimpleQueryWithMultipleOrderByAndCount() throws Exception {
        String queryString = "SELECT COUNT(*) FROM Subject s WHERE s.fsystem = false";

        Query q = em.createQuery(queryString);
        long count = (Long) q.getSingleResult();

        queryString = "SELECT s FROM Subject s WHERE s.fsystem = false ORDER BY s.firstName ASC, s.lastName DESC";
        q = em.createQuery(queryString);
        long size = q.getResultList().size();

        assert count == size;
    }

    @Test(groups = "integration.ejb3")
    public void testAdvQueryWithOrderby() throws Exception {
        Query q = em.createNamedQuery(SystemConfiguration.QUERY_FIND_ALL);
        List l = q.getResultList();
        int results = l.size();

        for (PageOrdering order : PageOrdering.values()) {
            q = PersistenceUtility.createQueryWithOrderBy(em, SystemConfiguration.QUERY_FIND_ALL, new OrderingField(
                "propertyKey", order));
            l = q.getResultList();
            Assert.assertEquals(results, l.size());
        }
    }

    @Test(groups = "integration.ejb3")
    public void testCountQuery() throws Exception {
        Long count = (Long) PersistenceUtility.createCountQuery(em, SystemConfiguration.QUERY_FIND_ALL)
            .getSingleResult();
        System.out.println("Transformed query to get count found: " + count);
    }

    /**
     * Run those NamedQueries that do not need parameters to be set.
     *
     * @throws Exception
     */
    @Test(groups = "integration.ejb3")
    public void testQueryAllSimpleNamedQueries() throws Exception {
        Object o = em.getDelegate();
        Class del = o.getClass();
        System.out.println("Delegate is " + del.toString());
        Session sess = (Session) o; // Hibernate Session in JBoss
        sessionFactory = (SessionFactoryImplementor) sess.getSessionFactory();
        Map cMeta = sessionFactory.getAllClassMetadata();

        List<String> configuredEntities = Arrays.asList(ENTITY_NAMES);

        Collection<ClassMetadata> metas = cMeta.values();

        try {
            for (ClassMetadata cm : metas) {
                String entity = cm.getEntityName();
                String shortName = entity.substring(entity.lastIndexOf(".") + 1);

                // Skip entites that are not in above list
                if (!configuredEntities.contains(shortName)) {
                    //System.out.println("== Skipping " + entity );
                    continue;
                }

                System.out.println("Testing queries for: " + shortName);
                List<NamedQuery> nqs = getNamedQueriesForEntity(entity);
                for (NamedQuery nq : nqs) {
                    boolean hasId;

                    // Skip those with non-ID parameters for now, as we dont not
                    // know how to obtain the parameters from Hibernate, as
                    // they are not set when parsing the NamedQuery annotation :-(
                    // we can just give bogus ID numbers by param indexes.
                    // I would like to use the regex (nq.query().matches(".*:(?!([a-zA-Z]*[iI]d)[ =]).*"))
                    // to match anything like :*id or :*Id but too hard to think how to
                    // set the parameter names.  so for now, just look for :id (ignore :ids too)
                    if (nq.query().matches(".*:(?!(id([^s]|$))).*")) {
                        continue;
                    }

                    hasId = nq.query().contains(":id");

                    if (nq.query().contains("UPDATE ") || nq.query().contains("update ")) {
                        continue;
                    }

                    if (nq.query().contains("DELETE ") || nq.query().contains("delete ")) {
                        continue;
                    }

                    if (nq.query().contains("INSERT ") || nq.query().contains("insert ")) {
                        continue;
                    }

                    try {
                        //System.out.print("  == query " + nq.name());
                        Query query = em.createNamedQuery(nq.name());

                        // TODO set parameters here
                        // fillParameters(query, nq);
                        // if we have some ID params, just set them to something
                        if (hasId) {
                            query.setParameter("id", Integer.valueOf(0));
                            //System.out.println("Query with ID parameter: " + nq.query());
                        }

                        query.setMaxResults(5);
                        Collection results = query.getResultList();
                        System.out.println("   " + nq.name() + "==> #results: " + results.size());
                    } catch (Throwable t) {
                        assert false : "Failed to query named query " + nq.name() + ": "
                            + ThrowableUtil.getAllMessages(t, true);
                    }
                }
            }
        } catch (NoResultException nre) {
            System.out.println("  ==> no results found");
        }
    }

    /**
     * Return the NamedQueries from the entity class
     *
     * @param  entity
     *
     * @return not null
     */
    public List<NamedQuery> getNamedQueriesForEntity(String entity) {
        List<NamedQuery> lnq = new ArrayList<NamedQuery>();
        ClassLoader cl = getClass().getClassLoader();
        Class<?> clazz;
        try {
            clazz = cl.loadClass(entity);
            //         System.out.println("  Class " + clazz.getName() + " found");
        } catch (ClassNotFoundException c) {
            //         System.err.println("  Entity class " + entity + " not found");
            return lnq;
        }

        if (clazz.isAnnotationPresent(NamedQuery.class)) {
            NamedQuery nq = clazz.getAnnotation(NamedQuery.class);
            lnq.add(nq);
        } else if (clazz.isAnnotationPresent(NamedQueries.class)) {
            NamedQueries nqs = clazz.getAnnotation(NamedQueries.class);
            NamedQuery[] nq = nqs.value();
            for (int j = 0; j < nq.length; j++) {
                lnq.add(nq[j]);
            }
        }

        return lnq;
    }

    //   public void fillParameters(Query q, NamedQuery namedQuery)
    //   {
    //      String queryName = namedQuery.name();
    //      String queryString = namedQuery.query();
    //      System.out.println(" >> Looking at " + queryString);
    //
    //      NamedQueryDefinition nqd = fac.getNamedQuery(queryName);
    //      System.out.println("NQD ist " + nqd);
    //      Map params = nqd.getParameterTypes();
    //      System.out.println("Map ist " + params);
    //      System.out.println("NQD.cache " + nqd.getCacheRegion());
    //      System.out.println("NQD.flush " + nqd.getFlushMode());
    //      System.out.println("NQD.query " + nqd.getQuery());
    //      System.out.println("NQD.queryString " + nqd.getQueryString());
    //
    //      Pattern p =  Pattern.compile(":\\w+");
    //      Matcher m = p.matcher(queryString);
    //      while (m.find())
    //      {
    //         String par = m.group();
    //         par = par.substring(1); // first char is :
    //         System.out.println("   >> " + m.group());
    //         q.setParameter(par, "1");
    //      }
    //   }
}