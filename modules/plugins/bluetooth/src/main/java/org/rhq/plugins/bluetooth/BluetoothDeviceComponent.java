package org.rhq.plugins.bluetooth;

import java.util.Set;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.LocalDevice;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

/**
 * @author Mark Spritzler
 */
public class BluetoothDeviceComponent implements ResourceComponent, MeasurementFacet {
    private final Log log = LogFactory.getLog(BluetoothDeviceServiceComponent.class);

    protected ResourceContext resourceContext;

    public AvailabilityType getAvailability() {
        try {
            LocalDevice localDevice = LocalDevice.getLocalDevice();
            localDevice.getFriendlyName();
        } catch (BluetoothStateException e) {
            log.error("No Bluetooth found on local device", e);
            return AvailabilityType.DOWN;
        }

        return AvailabilityType.UP;
    }

    public void start(ResourceContext resourceContext) throws Exception {
        this.resourceContext = resourceContext;
    }

    public void stop() {
        this.resourceContext = null;
    }

    public void getValues(MeasurementReport measurementReport, Set<MeasurementScheduleRequest> set) throws Exception {
        for (MeasurementScheduleRequest request : set) {
            String name = request.getName();
            PropertySimple simpleValue = resourceContext.getPluginConfiguration().getSimple(name);
            measurementReport.addData(new MeasurementDataTrait(request, simpleValue.getStringValue()));
        }
    }
}
