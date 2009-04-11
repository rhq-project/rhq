package org.rhq.enterprise.server.resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.resource.composite.ResourceFacets;

/**
 * @author Joseph Marques
 */
public class ResourceFacetsCache {

    private final Log log = LogFactory.getLog(ResourceFacetsCache.class);

    /*
     * Do not need concurrent collections as long as the cache does not take direct modifications.  The algorithm below
     * is lockless because when the cache needs to be reloaded the collection is fully constructed BEFORE replacing the
     * reference to the private member variable.  In this way, the private member variable become a true read-only
     * construct.  Furthermore, concurrent access to the contents of the cache is safe because the ResourceFacets object
     * is immutable.
     */
    private Map<Integer, ResourceFacets> cache;
    private static ResourceFacetsCache singleton = new ResourceFacetsCache();

    private ResourceFacetsCache() {
        cache = new HashMap<Integer, ResourceFacets>();
    }

    public static ResourceFacetsCache getSingleton() {
        return singleton;
    }

    public ResourceFacets getResourceFacets(int resourceTypeId) {
        /*
         * concurrent access is safe because we're accessing an (effectively) read-only cache object whose contents
         * are composed solely of immutable objects 
         */
        return cache.get(resourceTypeId);
    }

    public void reload(List<ResourceFacets> facets) {
        try {
            Map<Integer, ResourceFacets> reloadedFacets;
            reloadedFacets = new HashMap<Integer, ResourceFacets>();

            for (ResourceFacets facet : facets) {
                reloadedFacets.put(facet.getResourceTypeId(), facet);
            }

            cache = reloadedFacets;
        } catch (Throwable t) {
            log.error("Could not reload ResourceFacets cache", t);
        }
    }
}
