/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5.test;

import java.util.Set;
import java.util.TreeSet;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.rhq.core.domain.resource.Resource;

/**
 * @author Ian Springer
 * @author Noam Malki
 */
@Test(groups = { "as5-plugin", "as5-plugin-topic" })
public class TopicResourceTest extends AbstractDestinationTest {

	
	protected String getDestinationName()
	{
		return "TestTopic";
	}
	
	protected String getDestinationJndi()
	{
		return "/topic/testTopic";
	}
	
	
    @BeforeTest(groups = "as5-plugin-topic" )
    public void setup() throws Exception {
        System.out.println("Running Topic test...");
        
		initDestination();
    }

    @Override
    public void testMetrics() throws Exception {
        super.testMetrics();
    }

    @Override
    @Test(dependsOnMethods = "testMetrics") //the test may affect the metrics so it is important that this test will run after the metric test
    public void testOperations() throws Exception {

    	Set<Resource> resources = getResources();
        for (Resource resource : resources) {
        	if(resource.getName().equals(getDestinationName()))
        	{
        		Set<Resource> testResources = new TreeSet<Resource>();
        		testResources.add(resource);
        		
        		Set<String> testOperation = new TreeSet<String>();
        		testOperation.add("listAllSubscriptions");
        		testOperation.add("listAllSubscriptionsAsHTML");
        		testOperation.add("listDurableSubscriptions");
        		testOperation.add("listDurableSubscriptionsAsHTML");
        		testOperation.add("listNonDurableSubscriptions");
        		testOperation.add("listNonDurableSubscriptionsAsHTML");
        		testOperations(testResources , testOperation);
        		
        		Thread.sleep(2000);
        		testOperation = new TreeSet<String>();
        		testOperation.add("stop");
        		testOperations(testResources , testOperation);
        		
        		Thread.sleep(2000);
        		testOperation = new TreeSet<String>();
        		testOperation.add("start");
        		testOperations(testResources , testOperation);
        		
        	}
        }
    }
    
    protected String getResourceTypeName() {
        return "Topic";
    }

    @Override
    protected void validateNumericMetricValue(String metricName, Double value, Resource resource) {
        if (metricName.endsWith("Count")) {
            assert value >= 0;
        } else {
            super.validateNumericMetricValue(metricName, value, resource);
        }
    }

    @Override
    protected void validateTraitMetricValue(String metricName, String value, Resource resource) {
        if (metricName.equals("createdProgrammatically")) {
            assert value.equals("false"); //In this case we are using DeploymentTemplates a deployment descriptor is generated and this value should be false.
        }
    }
}
