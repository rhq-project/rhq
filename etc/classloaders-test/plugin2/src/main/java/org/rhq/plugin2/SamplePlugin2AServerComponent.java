package org.rhq.plugin2;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import dummy.Dummy;

public class SamplePlugin2AServerComponent implements ResourceComponent {

    private ResourceContext context;

    public void start(ResourceContext context) {
       this.context = context;
    }

    public void stop() {
    }
    public AvailabilityType getAvailability() {
        System.out.println("* plugin2-2A avail Dummy.VERSION=" + Dummy.VERSION);
        System.out.println("* plugin2-2A avail Dummy.getVersion=" + (new Dummy()).getVersion());
        System.out.println("* plugin2-2A avail Dummy classloader=" + Dummy.class.getClassLoader());
        System.out.println("* plugin2-2A avail resourcetype=" + this.context.getResourceType());
        System.out.println("* plugin2-2A avail this classloader=" + this.getClass().getClassLoader());
        System.out.println("* plugin2-2A avail ctx classloader=" + Thread.currentThread().getContextClassLoader());
        System.out.println("==========");
        return AvailabilityType.UP;
    }
}
