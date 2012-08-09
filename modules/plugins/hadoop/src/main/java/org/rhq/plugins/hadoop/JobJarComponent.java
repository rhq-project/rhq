/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.plugins.hadoop;

import java.io.File;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class JobJarComponent implements ResourceComponent<JobTrackerServerComponent>, OperationFacet, DeleteResourceFacet {

    public static final String RESOURCE_TYPE_NAME = "Job Jar";
    public static final String CONTENT_TYPE_NAME = "jobJar";
    public static final String JOB_JAR_PROP_NAME = "jobJar";
    
    private static final String SUBMIT_OP = "submit";
    
    private File jobJar;
    private ResourceContext<JobTrackerServerComponent> context;
    private HadoopOperationsDelegate operationsDelegate;
    
    @Override
    public AvailabilityType getAvailability() {
        return jobJar.exists() ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    @Override
    public void start(ResourceContext<JobTrackerServerComponent> context) throws InvalidPluginConfigurationException,
        Exception {
        
        jobJar = new File(context.getResourceKey());
        this.context = context;
        operationsDelegate = new HadoopOperationsDelegate(context.getParentResourceComponent().getResourceContext());
    }

    @Override
    public void stop() {
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException,
        Exception {

        if (SUBMIT_OP.equals(name)) {
            String args = parameters.getSimpleValue("args", "");
            args = context.getResourceKey() + " " + args;
            parameters.put(new PropertySimple("args", args));
            return operationsDelegate.invoke(HadoopSupportedOperations.JAR, parameters, null);
        }
        
        return null;
    }

    @Override
    public void deleteResource() throws Exception {
        jobJar.delete();
    }
}
