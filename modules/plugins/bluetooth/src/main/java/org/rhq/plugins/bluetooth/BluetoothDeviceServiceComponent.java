package org.rhq.plugins.bluetooth;

import java.util.List;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.ServiceRecord;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * @author Mark Spritzler
 */
public class BluetoothDeviceServiceComponent implements ResourceComponent {
    private final Log log = LogFactory.getLog(BluetoothDeviceServiceComponent.class);

    protected ResourceContext resourceContext;

    public AvailabilityType getAvailability() {
        try {
            LocalDevice localDevice = LocalDevice.getLocalDevice();
            DiscoveryAgent agent = localDevice.getDiscoveryAgent();

            BluetoothPluginDiscoveryListener listener = BluetoothDeviceServiceDiscoveryComponent.LISTENER;
            List<ServiceRecord> discoveredServices = listener.getDiscoveredServices();
            for (ServiceRecord record : discoveredServices) {
                //String friendlyName = record.getHostDevice().getFriendlyName(false);            
                //int[] ids = record.getAttributeIDs();
                //agent.searchServices(ids, null, null, listener);
            }
        } catch (BluetoothStateException e) {
            log.error("Bluetooth Service not found on remote device", e);
            return AvailabilityType.DOWN;
        }

        return AvailabilityType.UP;
    }

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;
    }

    public void stop() {
        this.resourceContext = null;
    }
}
