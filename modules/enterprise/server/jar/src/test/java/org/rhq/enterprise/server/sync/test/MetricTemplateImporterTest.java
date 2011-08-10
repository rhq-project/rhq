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

import java.util.Collections;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.jmock.Expectations;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.sync.entity.MetricTemplate;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.sync.importers.ExportedEntityMatcher;
import org.rhq.enterprise.server.sync.importers.MetricTemplateImporter;
import org.rhq.test.JMockTest;

/**
 * TODO fix the bug in the importer that doesn't disable the schedules
 *
 * @author Lukas Krejci
 */
@Test
public class MetricTemplateImporterTest extends JMockTest {

    public void testNonMatchingMeasurementDefinitionsAreIgnored() {
        //mocks we are going to need
        final EntityManager em = context.mock(EntityManager.class);
        final MeasurementScheduleManagerLocal msm = context.mock(MeasurementScheduleManagerLocal.class);

        //some data to supply
        final ResourceType rt = new ResourceType("fake", "fake", ResourceCategory.PLATFORM, null); 
        final MeasurementDefinition def = new MeasurementDefinition(rt, "fake"); 
        def.setId(1);
        def.setDefaultOn(true);
        
        //setup the expected behavior
        context.checking(new Expectations() {
            {
                Query q = context.mock(Query.class);
                
                //fake the entity manager returning no results for the first metric template
                //we try to import and the def for the second metric template we import
                
                allowing(em).createNamedQuery(with(MeasurementDefinition.FIND_RAW_OR_PER_MINUTE_BY_NAME_AND_RESOURCE_TYPE_NAME));
                will(returnValue(q));
                
                allowing(q).setParameter(with(any(String.class)), with(any(Object.class)));
                allowing(q).getResultList();
                will(onConsecutiveCalls(returnValue(Collections.emptyList()), returnValue(Collections.singletonList(def))));
                
                //this is what we expect the measurement schedule manager msm to be
                //called with with the importer finishes its work.
                one(msm).enableMeasurementTemplates(with(any(Subject.class)), with(new int[] { 1 }));
            }                          
        });
        
        MetricTemplateImporter importer = new MetricTemplateImporter(null, em, msm);
        
        MetricTemplate nonMatching = new MetricTemplate();
        MetricTemplate matching = new MetricTemplate(def);   
        
        importer.configure(null);
        
        ExportedEntityMatcher<MeasurementDefinition, MetricTemplate> matcher = importer.getExportedEntityMatcher();
        
        assertNull(matcher.findMatch(nonMatching), "The non-matching metric template shouldn't have been matched.");
        assertSame(matcher.findMatch(matching), def, "The matching metric template should have found the defined measurement definition");
        
        importer.update(null, nonMatching);
        importer.update(def, matching);
        
        //this should invoke the mocked measurement schedule manager and the expectations should check it gets called
        //correctly by the importer.
        importer.finishImport();
    }
    
    public void testCanUpdateEnablements() {
        
    }
    
    public void testCanUpdateSchedules() {
        
    }
    
    public void testGlobalUpdateSchedulesSwitch() {
        
    }
    
    public void testPerMetricUpdateScheduleOverrides() {
        
    }
}
