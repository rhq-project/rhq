package org.rhq.plugin1;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dummy.Dummy;

import org.rhq.core.pluginapi.inventory.ClassLoaderFacet;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

public class SamplePlugin1BDiscoveryComponent implements ResourceDiscoveryComponent, ClassLoaderFacet {

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

        System.out.println("+ plugin1-1B discover Dummy.VERSION=" + Dummy.VERSION);
        System.out.println("+ plugin1-1B discover Dummy.getVersion=" + (new Dummy()).getVersion());
        System.out.println("+ plugin1-1B discover Dummy classloader=" + Dummy.class.getClassLoader());
        System.out.println("+ plugin1-1B discover resourcetype=" + this.context.getResourceType());
        System.out.println("+ plugin1-1B discover this classloader=" + this.getClass().getClassLoader());
        System.out.println("+ plugin1-1B discover ctx classloader=" + Thread.currentThread().getContextClassLoader());
        System.out.println("==========");

        return set;
    }

    @SuppressWarnings("unchecked")
    public List<URL> getAdditionalClasspathUrls(ResourceDiscoveryContext context, DiscoveredResourceDetails details) {
        ResourceComponent parentComponent = context.getParentResourceComponent();
        ResourceContext parentContext = context.getParentResourceContext();

        System.out.println("+ plugin1-1B cl-facet parent context resource key=" + parentContext.getResourceKey());
        System.out.println("+ plugin1-1B cl-facet parent context resource type=" + parentContext.getResourceType());
        System.out.println("+ plugin1-1B cl-facet parent component=" + parentComponent);
        System.out.println("+ plugin1-1B cl-facet parent component CL=" + parentComponent.getClass().getClassLoader());
        System.out.println("+ plugin1-1B cl-facet ctx CL=" + Thread.currentThread().getContextClassLoader());
        System.out.println("~~~~~~~~~~");

        return Collections.emptyList();
    }

}
