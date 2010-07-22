package org.rhq.core.pc.inventory;

import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.inventory.ChildResourceTypeDiscoveryFacet;
import org.rhq.core.util.exception.ThrowableUtil;

public class ChildResourceTypeDiscoveryRunner implements Callable<Set<ResourceType>>, Runnable {

    private Log log = LogFactory.getLog(ChildResourceTypeDiscoveryRunner.class);
    //private MeasurementManager measurementManager;
    private int resourceId;

    //TODO: maybe to be implemented for later usage    
    //    public ChildResourceTypeDiscoveryRunner(MeasurementManager measurementManager) {
    //       
    //    }

    public ChildResourceTypeDiscoveryRunner(int resourceId) {
        this.resourceId = resourceId;
    }

    public void run() {
        try {
            call();
        } catch (Exception e) {
            log.error("Could not get measurement report.", e);
        }
    }

    public Set<ResourceType> call() {

        Set<ResourceType> resourceTypes = null;

        try {
            //TODO: 
            //this.measurementManager.getLock().readLock().lock();
            //report = this.measurementManager.getActiveReport();

            long start = System.currentTimeMillis();

            InventoryManager im = PluginContainer.getInstance().getInventoryManager();

            //Set<ScheduledMeasurementInfo> requests = this.measurementManager.getNextScheduledSet();
            //            if (requests != null) {
            //                if ((System.currentTimeMillis() - 30000L) > requests.iterator().next().getNextCollection()) {
            //                    this.measurementManager.incrementLateCollections(requests.size());
            //                    log.debug("Measurement collection is falling behind... Missed requested time by ["
            //                        + (System.currentTimeMillis() - requests.iterator().next().getNextCollection()) + "ms]");
            //
            //                    this.measurementManager.reschedule(requests);
            //                    return report;
            //                }
            //Integer resourceId = requests.iterator().next().getResourceId();

            ResourceContainer container = im.getResourceContainer(this.resourceId);

            if (container.getResourceComponentState() != ResourceContainer.ResourceComponentState.STARTED
                || container.getAvailability() == null
                || container.getAvailability().getAvailabilityType() == AvailabilityType.DOWN) {
                // Don't collect metrics for resources that are down
                if (log.isDebugEnabled()) {
                    log.debug("ChildType not discoverd for inactive resource component: " + container.getResource());
                }
            } else {
                ChildResourceTypeDiscoveryFacet discoveryComponent = ComponentUtil.getComponent(resourceId,
                    ChildResourceTypeDiscoveryFacet.class, FacetLockType.READ, 30 * 1000, true, true);

                resourceTypes = discoverChildResourceTypes(discoveryComponent);
            }

        } catch (Throwable t) {
            log.error("Failed to run ChildType discovery", t);
        }
        return resourceTypes;
    }

    /**
     * 
     * @param discoveryComponent
     * @return
     */
    private Set<ResourceType> discoverChildResourceTypes(ChildResourceTypeDiscoveryFacet discoveryComponent) {

        Set<ResourceType> resourceTypes = null;
        try {
            long start = System.currentTimeMillis();
            resourceTypes = discoveryComponent.discoverChildResourceTypes();
            long duration = (System.currentTimeMillis() - start);

            if (duration > 2000) {
                log.info("[PERF] Discovery of childResourceTypes for [" + discoveryComponent + "] took [" + duration
                    + "ms]");
            }
        } catch (Throwable t) {

            log.warn("Failure to discover childResourceType data - cause: " + ThrowableUtil.getAllMessages(t));
        }

        return resourceTypes;
    }
}
