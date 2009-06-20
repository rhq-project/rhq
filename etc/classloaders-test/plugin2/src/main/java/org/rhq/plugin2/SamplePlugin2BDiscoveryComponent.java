package org.rhq.plugin2;

import java.net.URL;
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

public class SamplePlugin2BDiscoveryComponent implements ResourceDiscoveryComponent, ClassLoaderFacet {

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

    @SuppressWarnings("unchecked")
    public List<URL> getAdditionalClasspathUrls(ResourceDiscoveryContext context, DiscoveredResourceDetails details) {
        ResourceComponent parentComponent = context.getParentResourceComponent();
        ResourceContext parentContext = context.getParentResourceContext();

        System.out.println("plugin2-2B cl-facet parent context resource key=" + parentContext.getResourceKey());
        System.out.println("plugin2-2B cl-facet parent context resource type=" + parentContext.getResourceType());
        System.out.println("plugin2-2B cl-facet parent context CL=" + parentContext.getClass().getClassLoader());
        System.out.println("plugin2-2B cl-facet parent component=" + parentComponent);
        System.out.println("plugin2-2B cl-facet parent component CL=" + parentComponent.getClass().getClassLoader());
        System.out.println("~~~~~~~~~~");

        return null;
    }
}
