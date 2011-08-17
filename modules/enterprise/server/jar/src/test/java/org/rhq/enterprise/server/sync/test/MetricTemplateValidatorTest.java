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

import static org.testng.Assert.*;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.jmock.Expectations;
import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.sync.entity.MetricTemplate;
import org.rhq.enterprise.server.sync.ValidationException;
import org.rhq.enterprise.server.sync.validators.MetricTemplateValidator;
import org.rhq.test.JMockTest;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test
public class MetricTemplateValidatorTest extends JMockTest {

    private static final ResourceType FAKE_RESOURCE_TYPE = new ResourceType("fake", "fake", ResourceCategory.PLATFORM, null);
    
    public void testAllValidatorsEqual() {
        MetricTemplateValidator v1 = new MetricTemplateValidator();
        MetricTemplateValidator v2 = new MetricTemplateValidator();
        MetricTemplateValidator v3 = new MetricTemplateValidator();
        
        assertEquals(v1, v2);
        assertEquals(v2, v3);
        assertEquals(v1, v3);
    }
    
    public void testDetectsUnknownTemplates() throws Exception {
        final EntityManager entityManager = context.mock(EntityManager.class);
        final Query q = context.mock(Query.class);
        
        final List<MeasurementDefinition> allDefs = new ArrayList<MeasurementDefinition>();
        
        context.checking(new Expectations() {
            {
                //make the entity manager return the test data
                allowing(entityManager).createQuery(with(any(String.class)));
                will(returnValue(q));
                
                allowing(q).getResultList();
                will(returnValue(allDefs));
            }
        });
        
        //add some test data        
        allDefs.add(createMeasurementDefinition("m1", false));
        allDefs.add(createMeasurementDefinition("m1", true));
        allDefs.add(createMeasurementDefinition("m2", false));
        allDefs.add(createMeasurementDefinition("m3", false));
        
        MetricTemplateValidator validator = new MetricTemplateValidator();
        
        validator.initialize(null, entityManager);
        validator.initializeExportedStateValidation(null);
        
        //check that all the defined measurement defs pass
        for(MeasurementDefinition md : allDefs) {
            validator.validateExportedEntity(new MetricTemplate(md));
        }
        
        //now let's create an "unknown" metric template
        MetricTemplate unknown = new MetricTemplate();
        unknown.setMetricName("unknown");
        unknown.setResourceTypeName("some");
        unknown.setResourceTypePlugin("other");
        
        try {
            validator.validateExportedEntity(unknown);
            fail("Validating a metric template " + unknown + " should have failed.");
        } catch (ValidationException e) {
            //expected
        }
    }

    public void testDetectsDuplicitEntries() throws Exception {
        final EntityManager entityManager = context.mock(EntityManager.class);
        final Query q = context.mock(Query.class);
        
        final List<MeasurementDefinition> allDefs = new ArrayList<MeasurementDefinition>();
        
        context.checking(new Expectations() {
            {
                //make the entity manager return the test data
                allowing(entityManager).createQuery(with(any(String.class)));
                will(returnValue(q));
                
                allowing(q).getResultList();
                will(returnValue(allDefs));
            }
        });
        
        //add some test data        
        allDefs.add(createMeasurementDefinition("m1", false));
        allDefs.add(createMeasurementDefinition("m1", true));
        allDefs.add(createMeasurementDefinition("m2", false));
        allDefs.add(createMeasurementDefinition("m3", false));
        
        MetricTemplateValidator validator = new MetricTemplateValidator();
        
        validator.initialize(null, entityManager);
        validator.initializeExportedStateValidation(null);
        
        //check that all the defined measurement defs pass
        for(MeasurementDefinition md : allDefs) {
            validator.validateExportedEntity(new MetricTemplate(md));
        }
        
        //now let's try and validate the first test data again.
        //this simulates the duplicate entries in the export file
        try {
            MetricTemplate duplicate = new MetricTemplate(allDefs.get(0));
            validator.validateExportedEntity(duplicate);
            fail("Validating a duplicate metric template " + duplicate + " should have failed.");
        } catch (ValidationException e) {
            //expected
        }
    }
    
    private static MeasurementDefinition createMeasurementDefinition(String name, boolean perMinute) {
        MeasurementDefinition ret = new MeasurementDefinition(FAKE_RESOURCE_TYPE, name);
        if (perMinute) {
            ret.setRawNumericType(NumericType.TRENDSUP);
        }
        
        return ret;
    }
}
