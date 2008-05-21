/*
 * JBoss, a division of Red Hat.
 * Copyright 2008, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.plugins.virt;

import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;

import java.util.Set;
import java.lang.reflect.Field;

/**
 * @author Greg Hinkle
 */
public class VirtualizationBlockDeviceComponent implements ResourceComponent<VirtualizationComponent>, MeasurementFacet {

    ResourceContext<VirtualizationComponent> resourceContext;

    public void start(ResourceContext<VirtualizationComponent> virtualizationComponentResourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = virtualizationComponentResourceContext;
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        return resourceContext.getParentResourceComponent().getAvailability();
    }


    private LibVirtConnection getConnection() {
        return this.resourceContext.getParentResourceComponent().getConnection();
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {

        LibVirt.VirDomainBlockStats stats =
                getConnection().getDomainBlockStats(
                        this.resourceContext.getParentResourceComponent().getDomainName(),
                        this.resourceContext.getResourceKey());

        for (MeasurementScheduleRequest request : metrics) {
            Field f = stats.getClass().getField(request.getName());
            report.addData(new MeasurementDataNumeric(request, (double) f.getLong(stats)));
        }
    }
}