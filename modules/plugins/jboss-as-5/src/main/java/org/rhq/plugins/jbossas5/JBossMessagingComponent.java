/*
  * RHQ Management Platform
  * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.plugins.jbossas5.helper.CreateChildResourceFacetDelegate;

import java.util.Set;

/**
 * The ResourceComponent for the singleton JBoss Messaging ResourceType, which supports creation of two child
 * ResourceTypes - Queue and Topic.
 *
 * @author Ian Springer
 */
public class JBossMessagingComponent extends ManagedComponentComponent
        implements CreateChildResourceFacet, MeasurementFacet {
    private CreateChildResourceFacetDelegate createChildResourceDelegate;

    public AvailabilityType getAvailability() {
        return super.getAvailability();
    }

    public void start(ResourceContext resourceContext) throws Exception {
        super.start(resourceContext);
        this.createChildResourceDelegate = new CreateChildResourceFacetDelegate(this);
    }

    public void stop() {
        super.stop();
    }

    // MeasurementFacet ---------------------------------------------------

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) throws Exception {
        super.getValues(report, requests);
    }

    // CreateChildResourceFacet --------------------------------------------

    public CreateResourceReport createResource(CreateResourceReport createResourceReport) {
        return this.createChildResourceDelegate.createResource(createResourceReport);
    }
}