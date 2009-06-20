package org.rhq.plugin2;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import dummy.Dummy;

public class SamplePlugin2ADiscoveryComponent implements ResourceDiscoveryComponent {

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
        System.out.println("Discovering plugin2-2A resource");

        HashSet<DiscoveredResourceDetails> set = new HashSet<DiscoveredResourceDetails>();

        String key = "plugin2-2A";
        String name = "plugin2-2A";
        String version = "2.0";
        String description = "This describes plugin2";

        DiscoveredResourceDetails resource = new DiscoveredResourceDetails(context.getResourceType(), key, name,
            version, description, null, null);

        set.add(resource);

        System.out.println("plugin2-2A discover Dummy.VERSION=" + Dummy.VERSION);
        System.out.println("plugin2-2A discover Dummy.getVersion=" + (new Dummy()).getVersion());
        System.out.println("plugin2-2A discover resourcetype=" + context.getResourceType());
        System.out.println("plugin2-2A discover classloader=" + Dummy.class.getClassLoader());
        System.out.println("plugin2-2A discover this classloader=" + this.getClass().getClassLoader());
        System.out.println("plugin2-2A discover ctx classloader=" + Thread.currentThread().getContextClassLoader());
        System.out.println("==========");

        return set;
    }
}
