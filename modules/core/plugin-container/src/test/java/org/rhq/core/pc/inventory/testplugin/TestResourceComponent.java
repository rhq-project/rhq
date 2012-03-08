package org.rhq.core.pc.inventory.testplugin;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * @author Ian Springer
 */
public class TestResourceComponent implements ResourceComponent<ResourceComponent<?>> {

    private ResourceContext<ResourceComponent<?>> resourceContext;

    @Override
    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    @Override
    public void start(ResourceContext<ResourceComponent<?>> resourceContext) throws InvalidPluginConfigurationException,
        Exception {
        this.resourceContext = resourceContext;
        System.out.println("Starting " + this.resourceContext.getResourceType() + " [" + this.resourceContext.getResourceKey() + "]...");
    }

    @Override
    public void stop() {
        System.out.println("Stopping " + this.resourceContext.getResourceType() + " [" + this.resourceContext.getResourceKey() + "]...");
    }

}
