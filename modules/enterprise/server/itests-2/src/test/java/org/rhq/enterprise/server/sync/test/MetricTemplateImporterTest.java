/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.sync.test;

import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.sync.entity.MetricTemplate;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.sync.importers.ExportedEntityMatcher;
import org.rhq.enterprise.server.sync.importers.MetricTemplateImporter;
import org.rhq.test.JMockTest;

/**
 * @author Lukas Krejci
 */
@Test
public class MetricTemplateImporterTest extends JMockTest {

    private static class TestPrerequisities {
        private EntityManager em;
        private Query q;
        private MeasurementScheduleManagerLocal msm;
        private List<MeasurementDefinition> defs;
        
        public TestPrerequisities(Mockery context) {
            em = context.mock(EntityManager.class);
            msm = context.mock(MeasurementScheduleManagerLocal.class);
            q = context.mock(Query.class);
        }

        public Query getQuery() {
            return q;
        }
        
        public EntityManager getEntityManager() {
            return em;
        }

        public MeasurementScheduleManagerLocal getMeasurementScheduleManager() {
            return msm;
        }

        public void setMeasurementDefinitions(List<MeasurementDefinition> defs) {
            this.defs = defs;
        }

        @SuppressWarnings("static-access")
        public void addExpectations(Expectations expectations) {
            expectations.allowing(em).createQuery(expectations.with(expectations.any(String.class)));
            expectations.will(Expectations.returnValue(q));
            
            expectations.allowing(q).getResultList();
            expectations.will(Expectations.returnValue(defs));
        }
    };

    private static final ResourceType FAKE_RESOURCE_TYPE = new ResourceType("fake", "fake", ResourceCategory.PLATFORM,
        null);

    public void testNonMatchingMeasurementDefinitionsAreIgnored() {
        final TestPrerequisities prereqs = new TestPrerequisities(context);

        //some data to supply
        MeasurementDefinition def = new MeasurementDefinition(FAKE_RESOURCE_TYPE, "fake");
        def.setId(1);
        def.setDefaultOn(true);
        def.setDefaultInterval(1000);

        //now we setup the prerequisites to return the faked results from the database
        //for the non-matching template, we want the "database" to return no results
        //while for the matching template, we want the "database" to return our own def.
        prereqs.setMeasurementDefinitions(Collections.singletonList(def));

        //setup the expected behavior
        context.checking(new Expectations() {
            {
                prereqs.addExpectations(this);

                MeasurementScheduleManagerLocal msm = prereqs.getMeasurementScheduleManager();

                //this is what we expect the measurement schedule manager msm to be
                //called with when the importer finishes its work.
                one(msm).updateDefaultCollectionIntervalAndEnablementForMeasurementDefinitions(with(any(Subject.class)),
                    with(new int[] { 1 }), with(1000L), with(true), with(false));
            }
        });

        MetricTemplateImporter importer =
            new MetricTemplateImporter(null, prereqs.getEntityManager(), prereqs.getMeasurementScheduleManager());

        MetricTemplate nonMatching = new MetricTemplate();
        nonMatching.setResourceTypeName("asf");
        nonMatching.setResourceTypePlugin("asfd");
        nonMatching.setMetricName("asf");
        
        MetricTemplate matching = new MetricTemplate(def);

        importer.configure(null);

        ExportedEntityMatcher<MeasurementDefinition, MetricTemplate> matcher = importer.getExportedEntityMatcher();

        assertNull(matcher.findMatch(nonMatching), "The non-matching metric template shouldn't have been matched.");
        assertSame(matcher.findMatch(matching), def,
            "The matching metric template should have found the defined measurement definition");

        //this is the "meat" of the test.. 
        //we call update() twice but only get 1 actual change pour down to database
        //and the importer not choking on this.
        importer.update(null, nonMatching);
        importer.update(def, matching);

        //this should invoke the mocked measurement schedule manager and the expectations should check it gets called
        //correctly by the importer.
        importer.finishImport();
    }

    public void testCanUpdateEnablements() {
        MeasurementDefinition defToEnable = new MeasurementDefinition(FAKE_RESOURCE_TYPE, "enabled");
        MeasurementDefinition defToDisable = new MeasurementDefinition(FAKE_RESOURCE_TYPE, "disable");

        defToEnable.setId(1);
        defToEnable.setDefaultOn(true);
        defToEnable.setDefaultInterval(1000);

        defToDisable.setId(2);
        defToDisable.setDefaultOn(false);
        defToDisable.setDefaultInterval(2000);

        final TestPrerequisities prereqs = new TestPrerequisities(context);
        prereqs.setMeasurementDefinitions(Arrays.asList(defToEnable, defToDisable));

        context.checking(new Expectations() {
            {
                prereqs.addExpectations(this);
                MeasurementScheduleManagerLocal msm = prereqs.getMeasurementScheduleManager();

                //this is what we expect the measurement schedule manager msm to be
                //called with when the importer finishes its work.
                one(msm).updateDefaultCollectionIntervalAndEnablementForMeasurementDefinitions(with(any(Subject.class)),
                    with(new int[] { 1 }), with(1000L), with(true), with(false));
                one(msm).updateDefaultCollectionIntervalAndEnablementForMeasurementDefinitions(with(any(Subject.class)),
                    with(new int[] { 2 }), with(2000L), with(false), with(false));
            }
        });

        MetricTemplateImporter importer =
            new MetricTemplateImporter(null, prereqs.getEntityManager(), prereqs.getMeasurementScheduleManager());

        importer.configure(null);

        ExportedEntityMatcher<MeasurementDefinition, MetricTemplate> matcher = importer.getExportedEntityMatcher();

        MetricTemplate match1 = new MetricTemplate(defToEnable);
        MetricTemplate match2 = new MetricTemplate(defToDisable);

        assertSame(matcher.findMatch(match1), defToEnable,
            "The matching metric template should have found the defined measurement definition");
        assertSame(matcher.findMatch(match2), defToDisable,
            "The matching metric template should have found the defined measurement definition");

        importer.update(defToEnable, match1);
        importer.update(defToDisable, match2);

        //this should invoke the mocked measurement schedule manager and the expectations should check it gets called
        //correctly by the importer.
        importer.finishImport();

    }

    public void testCanUpdateSchedules() {
        MeasurementDefinition def = new MeasurementDefinition(FAKE_RESOURCE_TYPE, "def");
        def.setId(1);
        def.setDefaultOn(true);
        def.setDefaultInterval(1000);

        final TestPrerequisities prereqs = new TestPrerequisities(context);
        prereqs.setMeasurementDefinitions(Collections.singletonList(def));

        context.checking(new Expectations() {
            {
                prereqs.addExpectations(this);

                MeasurementScheduleManagerLocal msm = prereqs.getMeasurementScheduleManager();

                one(msm).updateDefaultCollectionIntervalAndEnablementForMeasurementDefinitions(with(any(Subject.class)),
                    with(new int[] { 1 }), with(1000L), with(true), with(true));
            }
        });

        MetricTemplateImporter importer =
            new MetricTemplateImporter(null, prereqs.getEntityManager(), prereqs.getMeasurementScheduleManager());

        Configuration importConfig = new Configuration();
        importConfig.put(new PropertySimple(MetricTemplateImporter.UPDATE_ALL_SCHEDULES_PROPERTY, true));

        importer.configure(importConfig);

        ExportedEntityMatcher<MeasurementDefinition, MetricTemplate> matcher = importer.getExportedEntityMatcher();

        MetricTemplate template = new MetricTemplate(def);

        assertSame(matcher.findMatch(template), def,
            "The matching metric template should have found the defined measurement definition");

        importer.update(def, template);

        //this should invoke the mocked measurement schedule manager and the expectations should check it gets called
        //correctly by the importer.
        importer.finishImport();
    }

    public void testPerMetricUpdateScheduleOverrides() {
        MeasurementDefinition updatedDef = new MeasurementDefinition(FAKE_RESOURCE_TYPE, "def");
        updatedDef.setId(1);
        updatedDef.setDefaultOn(true);
        updatedDef.setDefaultInterval(1000);
        MeasurementDefinition notUpdatedDef = new MeasurementDefinition(FAKE_RESOURCE_TYPE, "ndef");
        notUpdatedDef.setId(2);
        notUpdatedDef.setDefaultOn(true);
        notUpdatedDef.setDefaultInterval(2000);

        final TestPrerequisities prereqs = new TestPrerequisities(context);
        prereqs.setMeasurementDefinitions(Arrays.asList(updatedDef, notUpdatedDef));

        context.checking(new Expectations() {
            {
                prereqs.addExpectations(this);

                MeasurementScheduleManagerLocal msm = prereqs.getMeasurementScheduleManager();

                one(msm).updateDefaultCollectionIntervalAndEnablementForMeasurementDefinitions(with(any(Subject.class)),
                    with(new int[] { 1 }), with(1000L), with(true), with(true));
                one(msm).updateDefaultCollectionIntervalAndEnablementForMeasurementDefinitions(with(any(Subject.class)),
                    with(new int[] { 2 }), with(2000L), with(true), with(false));
            }
        });

        MetricTemplateImporter importer =
            new MetricTemplateImporter(null, prereqs.getEntityManager(), prereqs.getMeasurementScheduleManager());

        Configuration importConfig = new Configuration();

        //this is the default, but let's be specific here so that this doesn't start failing if we change the
        //default in the future.
        importConfig.put(new PropertySimple(MetricTemplateImporter.UPDATE_ALL_SCHEDULES_PROPERTY, false));

        //set the def to update the schedules anyway
        PropertyList list = new PropertyList(MetricTemplateImporter.METRIC_UPDATE_OVERRIDES_PROPERTY);
        importConfig.put(list);
        PropertyMap map = new PropertyMap(MetricTemplateImporter.METRIC_UPDATE_OVERRIDE_PROPERTY);
        list.add(map);
        map.put(new PropertySimple(MetricTemplateImporter.METRIC_NAME_PROPERTY, "def"));
        map.put(new PropertySimple(MetricTemplateImporter.RESOURCE_TYPE_NAME_PROPERTY, FAKE_RESOURCE_TYPE.getName()));
        map.put(new PropertySimple(MetricTemplateImporter.RESOURCE_TYPE_PLUGIN_PROPERTY, FAKE_RESOURCE_TYPE.getPlugin()));
        map.put(new PropertySimple(MetricTemplateImporter.UPDATE_SCHEDULES_PROPERTY, true));

        importer.configure(importConfig);

        ExportedEntityMatcher<MeasurementDefinition, MetricTemplate> matcher = importer.getExportedEntityMatcher();

        MetricTemplate template1 = new MetricTemplate(updatedDef);
        MetricTemplate template2 = new MetricTemplate(notUpdatedDef);

        assertSame(matcher.findMatch(template1), updatedDef,
            "The matching metric template should have found the defined measurement definition");
        assertSame(matcher.findMatch(template2), notUpdatedDef,
            "The matching metric template should have found the defined measurement definition");

        importer.update(updatedDef, template1);
        importer.update(notUpdatedDef, template2);

        //this should invoke the mocked measurement schedule manager and the expectations should check it gets called
        //correctly by the importer.
        importer.finishImport();
    }

    public void testNullConfigurationInterpretedAsDefault() {
        //this doesn't need to be implemented as long as at least one of the other
        //test methods passes null to the importer.configure() and succeeds.
        testNonMatchingMeasurementDefinitionsAreIgnored();
    }

    public void testDistinguishingBetweenNormalAndPerMinuteTemplates() {
        MeasurementDefinition normalDef = new MeasurementDefinition(FAKE_RESOURCE_TYPE, "def"); 
        MeasurementDefinition perMinuteDef = new MeasurementDefinition(FAKE_RESOURCE_TYPE, "def");
        
        normalDef.setId(1);
        normalDef.setDefaultOn(true);
        normalDef.setDefaultInterval(1000);
        
        perMinuteDef.setId(2);
        perMinuteDef.setDefaultOn(false);
        perMinuteDef.setDefaultInterval(2000);
        perMinuteDef.setRawNumericType(NumericType.TRENDSUP);
        
        final TestPrerequisities prereqs = new TestPrerequisities(context);
        prereqs.setMeasurementDefinitions(Arrays.asList(normalDef, perMinuteDef));
        
        context.checking(new Expectations() {
            {
                prereqs.addExpectations(this);

                MeasurementScheduleManagerLocal msm = prereqs.getMeasurementScheduleManager();

                one(msm).updateDefaultCollectionIntervalAndEnablementForMeasurementDefinitions(with(any(Subject.class)),
                    with(new int[] { 1 }), with(1000L), with(true), with(false));
                one(msm).updateDefaultCollectionIntervalAndEnablementForMeasurementDefinitions(with(any(Subject.class)),
                    with(new int[] { 2 }), with(2000L), with(false), with(false));
            }
        });

        MetricTemplateImporter importer =
            new MetricTemplateImporter(null, prereqs.getEntityManager(), prereqs.getMeasurementScheduleManager());

        importer.configure(null);

        ExportedEntityMatcher<MeasurementDefinition, MetricTemplate> matcher = importer.getExportedEntityMatcher();

        MetricTemplate template1 = new MetricTemplate(normalDef);
        MetricTemplate template2 = new MetricTemplate(perMinuteDef);

        assertSame(matcher.findMatch(template1), normalDef,
            "The matching metric template should have found the defined measurement definition");
        assertSame(matcher.findMatch(template2), perMinuteDef,
            "The matching metric template should have found the defined measurement definition");

        importer.update(normalDef, template1);
        importer.update(perMinuteDef, template2);

        //this should invoke the mocked measurement schedule manager and the expectations should check it gets called
        //correctly by the importer.
        importer.finishImport();
    }
    
    public void testNotifiesAboutIgnoredMetricTemplates() throws Exception {
        final TestPrerequisities prereqs = new TestPrerequisities(context);
        prereqs.setMeasurementDefinitions(Collections.<MeasurementDefinition>emptyList());
        
        context.checking(new Expectations() {
            {
                prereqs.addExpectations(this);
            }
        });

        MetricTemplateImporter importer =
            new MetricTemplateImporter(null, prereqs.getEntityManager(), prereqs.getMeasurementScheduleManager());

        importer.configure(null);

        importer.configure(null);

        MetricTemplate unmatched = new MetricTemplate();
        unmatched.setMetricName("made-up");
        unmatched.setResourceTypeName("made-up");
        unmatched.setResourceTypePlugin("made-up");

        ExportedEntityMatcher<MeasurementDefinition, MetricTemplate> matcher = importer.getExportedEntityMatcher();

        assertNull(matcher.findMatch(unmatched));

        importer.update(null, unmatched);

        String notes = importer.finishImport();

        String expectedNotes =
            MetricTemplateImporter.getUnmatchedMetricTemplatesReport(Collections.singleton(unmatched));

        assertEquals(notes, expectedNotes);
    }
}
