package org.rhq.plugins.test.measurement;

import java.util.Iterator;
import java.util.Set;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

public class BZ834019ResourceComponent implements ResourceComponent<ResourceComponent<?>>, MeasurementFacet {

    @Override
    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    @Override
    public void start(ResourceContext<ResourceComponent<?>> context) throws Exception {
    }

    @Override
    public void stop() {
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        //  TODO: do things to test BZ 834019
        for (Iterator<MeasurementScheduleRequest> i = metrics.iterator(); i.hasNext();) {
            MeasurementScheduleRequest metric = i.next();
            report.addData(new MeasurementDataNumeric(metric, new Double(1.0)));
        }
        return;
    }
}