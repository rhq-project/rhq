package org.rhq.plugins.bluetooth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;

/**
 * @author Mark Spritzler
 */
public class BluetoothPluginDiscoveryListener implements DiscoveryListener {

    public static final String BLUETOOTH_ADDRESS = "bluetoothAddress";
    public static final String FRIENDLY_NAME = "friendlyName";
    public static final String CLASS_DESCRIPTION = "classDescription";
    public static final String CONNECTION_URL = "connectionURL";
    public static final String PASSCODE = "passcode";

    private boolean isDiscoveryFinished = false;
    private Map<RemoteDevice, DeviceClass> discoveredDevices = new HashMap<RemoteDevice, DeviceClass>();
    private List<ServiceRecord> currentDiscoveredServices = new ArrayList<ServiceRecord>();
    private List<ServiceRecord> discoveredServices = new ArrayList<ServiceRecord>();

    public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
        discoveredDevices.put(btDevice, cod);
    }

    public void inquiryCompleted(int discType) {
        isDiscoveryFinished = true;
    }

    public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
        // @TODO Need to handle multiple discoveries occuring at the same time with transID
        currentDiscoveredServices.addAll(Arrays.asList(servRecord));
    }

    public void serviceSearchCompleted(int transID, int respCode) {
        // @TODO Need to handle multiple discoveries occuring at the same time with transID
        discoveredServices.addAll(currentDiscoveredServices);
        currentDiscoveredServices.clear();
    }

    public boolean isDiscoveryFinished() {
        return isDiscoveryFinished;
    }

    public void setDiscoveryFinished(boolean isDiscoveryFinished) {
        this.isDiscoveryFinished = isDiscoveryFinished;
    }

    public Map<RemoteDevice, DeviceClass> getDiscoveredDevices() {
        return this.discoveredDevices;
    }

    public List<ServiceRecord> getDiscoveredServices() {
        return this.discoveredServices;
    }

    /**
     * Constants for device types. There are many, which is why I placed them at the bottom of the class
     * instead
     */
    public static final Map<Integer, Map<Integer, String>> majorClassDescription = new HashMap<Integer, Map<Integer, String>>();

    {
        Map<Integer, String> deviceDescriptionMisc = new HashMap<Integer, String>();
        deviceDescriptionMisc.put(0, "Miscellaneous Major Device");
        majorClassDescription.put(0, deviceDescriptionMisc);

        Map<Integer, String> deviceDescriptionComputer = new HashMap<Integer, String>();
        deviceDescriptionComputer.put(0, "Unassigned Miscellaneous Computer");
        deviceDescriptionComputer.put(4, "Desktop Computer");
        deviceDescriptionComputer.put(8, "Server Computer");
        deviceDescriptionComputer.put(12, "Laptop Computer");
        deviceDescriptionComputer.put(16, "Sub-Laptop Computer");
        deviceDescriptionComputer.put(20, "PDA");
        deviceDescriptionComputer.put(24, "Watch Size Computer");
        majorClassDescription.put(256, deviceDescriptionComputer);

        Map<Integer, String> deviceDescriptionPhone = new HashMap<Integer, String>();
        deviceDescriptionPhone.put(0, "Unassigned Miscellaneous Phone");
        deviceDescriptionPhone.put(4, "Cellular Phone");
        deviceDescriptionPhone.put(8, "Household Cordless Phone");
        deviceDescriptionPhone.put(12, "Smart Phone");
        deviceDescriptionPhone.put(16, "Phone Modem");
        majorClassDescription.put(512, deviceDescriptionPhone);

        Map<Integer, String> deviceDescriptionNetwork = new HashMap<Integer, String>();
        deviceDescriptionNetwork.put(0, "LAN/Network access point fully available");
        deviceDescriptionNetwork.put(32, "LAN/Network access point 1-17% utilized");
        deviceDescriptionNetwork.put(64, "LAN/Network access point 17-33% utilized");
        deviceDescriptionNetwork.put(96, "LAN/Network access point 33-50% utilized");
        deviceDescriptionNetwork.put(128, "LAN/Network access point 50-67% utilized");
        deviceDescriptionNetwork.put(160, "LAN/Network access point 67-83% utilized");
        deviceDescriptionNetwork.put(192, "LAN/Network access point 83-99% utilized");
        deviceDescriptionNetwork.put(224, "LAN/Network access point 100% utilized, no service available");
        majorClassDescription.put(768, deviceDescriptionNetwork);

        Map<Integer, String> deviceDescriptionAV = new HashMap<Integer, String>();
        deviceDescriptionAV.put(0, "Unassigned Miscellaneous Audio/Visual device");
        deviceDescriptionAV.put(4, "Headset A/V Device");
        deviceDescriptionAV.put(8, "Hands Free A/V Device");
        deviceDescriptionAV.put(16, "Microphone");
        deviceDescriptionAV.put(44, "VCR");
        deviceDescriptionAV.put(72, "Video Game System");
        majorClassDescription.put(1024, deviceDescriptionAV);

        Map<Integer, String> deviceDescriptionPeripheral = new HashMap<Integer, String>();
        deviceDescriptionPeripheral.put(12, "Remote Control");
        deviceDescriptionPeripheral.put(64, "Keyboard");
        deviceDescriptionPeripheral.put(128, "Mouse, Trackball, etc.");
        majorClassDescription.put(1280, deviceDescriptionPeripheral);

        Map<Integer, String> deviceDescriptionImaging = new HashMap<Integer, String>();
        deviceDescriptionImaging.put(16, "Display Device");
        deviceDescriptionImaging.put(32, "Camera");
        deviceDescriptionImaging.put(64, "Scanner");
        deviceDescriptionImaging.put(128, "Printer");
        majorClassDescription.put(1536, deviceDescriptionImaging);

        Map<Integer, String> deviceDescriptionUnclassified = new HashMap<Integer, String>();
        deviceDescriptionUnclassified.put(0, "Unclassified major device");
        majorClassDescription.put(7936, deviceDescriptionUnclassified);

    }
}
