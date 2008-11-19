package org.rhq.plugins.bluetooth;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * @author Mark Spritzler
 */
public class BluetoothDiscoveryComponent implements ResourceDiscoveryComponent {
    private final Log log = LogFactory.getLog(BluetoothDiscoveryComponent.class);

    static final BluetoothPluginDiscoveryListener LISTENER = new BluetoothPluginDiscoveryListener();

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
        log.info("Discovering Bluetooth Devices in the area...");
        Set<DiscoveredResourceDetails> set = new HashSet<DiscoveredResourceDetails>();

        try {
            LocalDevice local = LocalDevice.getLocalDevice();
            String bluetoothAddress = local.getBluetoothAddress();
            String friendlyName = local.getFriendlyName();
            DeviceClass localDeviceClass = local.getDeviceClass();

            DiscoveredResourceDetails details = createDiscoveredResourceDetails(context, localDeviceClass,
                bluetoothAddress, friendlyName);
            details.setResourceDescription("Local Bluetooth Device");
            set.add(details);

            addRemoteDevices(set, context);

        } catch (BluetoothStateException e) {
            log.error("Unable to discover devices", e);
        }

        return set;
    }

    private void addRemoteDevices(Set<DiscoveredResourceDetails> set, ResourceDiscoveryContext context) {
        discoverRemoteDevices();
        Map<RemoteDevice, DeviceClass> discoveredDevices = LISTENER.getDiscoveredDevices();
        if (discoveredDevices != null) {
            Set<RemoteDevice> devices = discoveredDevices.keySet();
            for (RemoteDevice device : devices) {

                String bluetoothAddress = device.getBluetoothAddress();
                String friendlyName = bluetoothAddress;
                try {
                    friendlyName = device.getFriendlyName(true);
                } catch (IOException e) {
                    log.error(e);
                }
                DeviceClass deviceClass = discoveredDevices.get(device);

                DiscoveredResourceDetails details = createDiscoveredResourceDetails(context, deviceClass,
                    bluetoothAddress, friendlyName);
                details.setResourceDescription("Remote Bluetooth Device");
                set.add(details);
            }
        }

    }

    private void discoverRemoteDevices() {
        try {
            LocalDevice local = LocalDevice.getLocalDevice();

            LISTENER.setDiscoveryFinished(false);
            DiscoveryAgent agent = local.getDiscoveryAgent();
            agent.startInquiry(DiscoveryAgent.GIAC, LISTENER);

            while (!LISTENER.isDiscoveryFinished()) {
                for (int i = 0; i < 5000; i++) {
                }
            }

        } catch (BluetoothStateException e) {
            log.error("Unable to discover devices", e);
        }
    }

    private String getDeviceDescription(DeviceClass deviceClass) {
        int majorDeviceClass = deviceClass.getMajorDeviceClass();
        int minorDeviceClass = deviceClass.getMinorDeviceClass();
        Map<Integer, String> minorMap = BluetoothPluginDiscoveryListener.majorClassDescription.get(majorDeviceClass);
        String classDescription = minorMap.get(0);
        if (minorMap.containsKey(minorDeviceClass)) {
            classDescription = minorMap.get(minorDeviceClass);
        }
        return classDescription;
    }

    private DiscoveredResourceDetails createDiscoveredResourceDetails(ResourceDiscoveryContext context,
        DeviceClass deviceClass, String bluetoothAddress, String friendlyName) {
        String deviceDescription = getDeviceDescription(deviceClass);
        ResourceType resourceType = context.getResourceType();

        Configuration pluginConfiguration = context.getDefaultPluginConfiguration();
        pluginConfiguration.put(new PropertySimple(BluetoothPluginDiscoveryListener.FRIENDLY_NAME, friendlyName));
        pluginConfiguration
            .put(new PropertySimple(BluetoothPluginDiscoveryListener.BLUETOOTH_ADDRESS, bluetoothAddress));
        pluginConfiguration.put(new PropertySimple(BluetoothPluginDiscoveryListener.CLASS_DESCRIPTION,
            deviceDescription));

        return new DiscoveredResourceDetails(resourceType, bluetoothAddress, friendlyName,
            "discovered using BlueCove 2.0.1", "", pluginConfiguration, null);
    }
}
