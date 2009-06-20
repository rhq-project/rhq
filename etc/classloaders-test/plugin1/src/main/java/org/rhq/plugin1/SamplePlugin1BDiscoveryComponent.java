package org.rhq.plugin1;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import dummy.Dummy;

public class SamplePlugin1BDiscoveryComponent implements ResourceDiscoveryComponent {

    private ResourceDiscoveryContext context;

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
        System.out.println("Discovering plugin1-1B");

        this.context = context;

        HashSet<DiscoveredResourceDetails> set = new HashSet<DiscoveredResourceDetails>();

        String key = "plugin1-1B";
        String name = "plugin1-1B";
        String version = "1.0";
        String description = "This describes plugin1-1B";

        DiscoveredResourceDetails resource = new DiscoveredResourceDetails(context.getResourceType(), key, name,
            version, description, null, null);

        set.add(resource);

        System.out.println("plugin1-1B discover Dummy.VERSION=" + Dummy.VERSION);
        System.out.println("plugin1-1B discover Dummy.getVersion=" + (new Dummy()).getVersion());
        System.out.println("plugin1-1B discover resourcetype=" + this.context.getResourceType());
        System.out.println("plugin1-1B discover classloader=" + Dummy.class.getClassLoader());
        System.out.println("plugin1-1B discover this classloader=" + this.getClass().getClassLoader());
        System.out.println("plugin1-1B discover ctx classloader=" + Thread.currentThread().getContextClassLoader());
        System.out.println("==========");

        return set;
    }
}
