package org.rhq.plugin2;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import dummy.Dummy;

public class SamplePlugin2BDiscoveryComponent implements ResourceDiscoveryComponent {

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
        System.out.println("Discovering plugin2-2B resource");

        HashSet<DiscoveredResourceDetails> set = new HashSet<DiscoveredResourceDetails>();

        String key = "plugin2-2B";
        String name = "plugin2-2B";
        String version = "2.0";
        String description = "This describes plugin2-2B";

        DiscoveredResourceDetails resource = new DiscoveredResourceDetails(context.getResourceType(), key, name,
            version, description, null, null);

        set.add(resource);

        System.out.println("plugin2-2B discover Dummy.VERSION=" + Dummy.VERSION);
        System.out.println("plugin2-2B discover Dummy.getVersion=" + (new Dummy()).getVersion());
        System.out.println("plugin2-2B discover resourcetype=" + context.getResourceType());
        System.out.println("plugin2-2B discover classloader=" + Dummy.class.getClassLoader());
        System.out.println("plugin2-2B discover this classloader=" + this.getClass().getClassLoader());
        System.out.println("plugin2-2B discover ctx classloader=" + Thread.currentThread().getContextClassLoader());
        System.out.println("==========");

        return set;
    }
}
