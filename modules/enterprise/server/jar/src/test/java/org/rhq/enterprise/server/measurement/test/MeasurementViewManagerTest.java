package org.rhq.enterprise.server.measurement.test;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementViewManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

public class MeasurementViewManagerTest extends AbstractEJB3Test {

    private MeasurementViewManagerLocal viewManager;
    private SubjectManagerLocal subjectManager;

    private static final int SIZE = 5;

    private ResourceType type;
    private List<Resource> resources;
    private List<MeasurementDefinition> definitions;
    private List<MeasurementSchedule> schedules;

    private Subject overlord;

    public void testAll() {
        // TODO: jmarques - write unit tests for MeasurementViewManager
    }

    @BeforeSuite
    public void beforeSuite() throws Exception {
        viewManager = LookupUtil.getMeasurementViewManager();
        subjectManager = LookupUtil.getSubjectManager();

        overlord = subjectManager.getOverlord();
    }

    @BeforeTest
    public void beforeTest() throws Exception {
        getTransactionManager().begin();
        try {
            EntityManager em = getEntityManager();

            String prefix = MeasurementViewManagerTest.class.getSimpleName();

            type = new ResourceType(prefix + "type", prefix + "plugin", ResourceCategory.PLATFORM, null);
            em.persist(type);

            resources = new ArrayList<Resource>();
            for (int i = 0; i < SIZE; i++) {
                Resource resource = new Resource(prefix + "key " + i, prefix + "key " + i, type);
                resources.add(resource);
                em.persist(resource);
            }

            definitions = new ArrayList<MeasurementDefinition>();
            for (int i = 0; i < SIZE; i++) {
                MeasurementDefinition definition = new MeasurementDefinition(type, prefix + "definition " + i);
                definitions.add(definition);
                type.addMetricDefinition(definition);
                em.persist(definition);
            }
            type = em.merge(type);

            schedules = new ArrayList<MeasurementSchedule>();
            for (int i = 0; i < SIZE; i++) {
                for (int j = 0; j < SIZE; j++) {
                    MeasurementDefinition definition = definitions.get(i);
                    Resource resource = resources.get(j);
                    MeasurementSchedule schedule = new MeasurementSchedule(definition, resource);
                    schedules.add(schedule);
                    definition.addSchedule(schedule);
                    resource.addSchedule(schedule);
                    em.persist(schedule);
                }
            }
            for (int i = 0; i < SIZE; i++) {
                MeasurementDefinition definition = definitions.get(i);
                definitions.set(i, em.merge(definition));
            }
            for (int j = 0; j < SIZE; j++) {
                Resource resource = resources.get(j);
                resources.set(j, em.merge(resource));
            }
        } finally {
            getTransactionManager().commit();
        }
    }

    @AfterTest
    public void afterTest() throws Exception {
        getTransactionManager().begin();
        try {
            EntityManager em = getEntityManager();

            List<Integer> measurementDefinitionIds = new ArrayList<Integer>();
            for (int i = 0; i < definitions.size(); i++) {
                measurementDefinitionIds.add(definitions.get(i).getId());
            }
            Query removeSchedules = em
                .createQuery("delete from MeasurementSchedule ms where ms.definition.id IN ( :ids ) ");
            removeSchedules.setParameter("ids", measurementDefinitionIds);
            removeSchedules.executeUpdate();

            List<Integer> resourceIds = new ArrayList<Integer>();
            for (int i = 0; i < resources.size(); i++) {
                resourceIds.add(resources.get(i).getId());
            }
            if (type != null) {
                Query removeDefinitions = em
                    .createQuery("delete from MeasurementDefinition md where md.resourceType.id = :resourceTypeId ");
                removeDefinitions.setParameter("resourceTypeId", type.getId());
                removeDefinitions.executeUpdate();
            }

            Query removeResources = em.createQuery("delete from Resource res where res.id IN ( :ids ) ");
            removeResources.setParameter("ids", resourceIds);
            removeResources.executeUpdate();

            if (type != null) {
                type = em.find(ResourceType.class, type.getId());
                em.remove(type);
            }
        } finally {
            getTransactionManager().commit();
        }

    }
}
