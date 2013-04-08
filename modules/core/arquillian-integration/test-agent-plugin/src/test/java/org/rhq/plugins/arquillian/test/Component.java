package org.rhq.plugins.arquillian.test;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * @author Lukas Krejci
 */
public class Component implements ResourceComponent<ResourceComponent<?>> {
    @Override
    public void start(ResourceContext<ResourceComponent<?>> context)
        throws InvalidPluginConfigurationException, Exception {
    }

    @Override
    public void stop() {
    }

    @Override
    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }
}
