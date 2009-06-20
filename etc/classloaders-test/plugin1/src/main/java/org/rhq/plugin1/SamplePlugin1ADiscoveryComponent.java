package org.rhq.plugin1;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import dummy.Dummy;

public class SamplePlugin1ADiscoveryComponent implements ResourceDiscoveryComponent {

    private ResourceDiscoveryContext context;

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
        System.out.println("Discovering plugin1-1A");

        this.context = context;

        HashSet<DiscoveredResourceDetails> set = new HashSet<DiscoveredResourceDetails>();

        String key = "plugin1-1A";
        String name = "plugin1-1A";
        String version = "1.0";
        String description = "This describes plugin1-1A";

        DiscoveredResourceDetails resource = new DiscoveredResourceDetails(context.getResourceType(), key, name,
            version, description, null, null);

        set.add(resource);

        System.out.println("+ plugin1-1A discover Dummy.VERSION=" + Dummy.VERSION);
        System.out.println("+ plugin1-1A discover Dummy.getVersion=" + (new Dummy()).getVersion());
        System.out.println("+ plugin1-1A discover Dummy classloader=" + Dummy.class.getClassLoader());
        System.out.println("+ plugin1-1A discover resourcetype=" + this.context.getResourceType());
        System.out.println("+ plugin1-1A discover this classloader=" + this.getClass().getClassLoader());
        System.out.println("+ plugin1-1A discover ctx classloader=" + Thread.currentThread().getContextClassLoader());
        System.out.println("==========");

        return set;
    }
}
