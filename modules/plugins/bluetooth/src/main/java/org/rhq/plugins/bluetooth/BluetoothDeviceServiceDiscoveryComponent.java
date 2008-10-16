package org.rhq.plugins.bluetooth;

import java.util.HashSet;
import java.util.Set;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * @author: Mark Spritzler
 */
public class BluetoothDeviceServiceDiscoveryComponent implements ResourceDiscoveryComponent {
    private final Log log = LogFactory.getLog(BluetoothDeviceServiceDiscoveryComponent.class);

    static final BluetoothPluginDiscoveryListener LISTENER = new BluetoothPluginDiscoveryListener();;

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
        log.info("Discovering The services of a Bluetooth devices in the area...");
        Set<DiscoveredResourceDetails> set = new HashSet<DiscoveredResourceDetails>();

        try {
            LocalDevice local = LocalDevice.getLocalDevice();

            DiscoveryAgent agent = local.getDiscoveryAgent();
            //agent.startInquiry(DiscoveryAgent.GIAC, LISTENER);

            //while (!LISTENER.isDiscoveryFinished())
            //{}

            /*try {
               synchronized(this){
                  this.wait();
               }
            }
            catch (InterruptedException e)
            {
               log.error(e);
            }*/

        } catch (BluetoothStateException e) {
            log.error("Unable to discover devices", e);
        }

        return set;
    }
}
