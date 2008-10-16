/*
 * JBoss, a division of Red Hat.
 * Copyright 2007, Red Hat Middleware, LLC. All rights reserved.
 */

package org.jboss.on.plugins.mock.jboss;

import java.util.HashMap;
import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.jboss.on.plugins.mock.jboss.artifacts.ArtifactHolder;
import org.jboss.on.plugins.mock.jboss.metrics.MetricValueCalculator;
import org.jboss.on.plugins.mock.jboss.operations.OperationHandler;
import org.jboss.on.plugins.mock.jboss.configuration.ConfigurationHandler;

/**
 * Stateful cache of resources and their associated data for the mock plugin. Data is populated during discovery, while
 * further updates from using the mock are made against this cache as well. This enables the mock to field operations
 * such as updating the configuration and renaming a resource.
 *
 * @author Jason Dobies
 */
public class ResourceCache
{
   // Static  --------------------------------------------

   private static final Map<ResourceCacheKey, Map<String, DiscoveredResourceDetails>> RESOURCE_CACHE = new HashMap<ResourceCacheKey, Map<String, DiscoveredResourceDetails>>();

   private static final Map<String, Configuration> CONFIGURATIONS = new HashMap<String, Configuration>();

   private static final Map<String, AvailabilityType> AVAILABILITY = new HashMap<String, AvailabilityType>();

   private static final Map<String, Map<String, MetricValueCalculator>> METRIC_CALCULATORS = new HashMap<String, Map<String, MetricValueCalculator>>();

   private static final Map<String, Map<String, OperationHandler>> OPERATION_HANDLERS = new HashMap<String, Map<String, OperationHandler>>();

   private static final Map<String, Map<String, ArtifactHolder>> ARTIFACTS = new HashMap<String, Map<String, ArtifactHolder>>();

   private static final Map<String, ConfigurationHandler> ERRORS = new HashMap<String, ConfigurationHandler>();
   // Public  --------------------------------------------

   /**
    * Clears all data stored in the cache.
    */
   public static void reset()
   {
      RESOURCE_CACHE.clear();
      CONFIGURATIONS.clear();
      AVAILABILITY.clear();
      METRIC_CALCULATORS.clear();
      OPERATION_HANDLERS.clear();
      ARTIFACTS.clear();
      ERRORS.clear();
   }

   /**
    * Returns whether or not a resource map is defined for the specified values.
    *
    * @param resourceType      type of resources in the map
    * @param resourceComponent server under which the resources live
    *
    * @return <code>true</code> if a resource map is defined; <code>false</code> otherwise
    */
   public synchronized static boolean resourceMapExists(ResourceType resourceType, ResourceComponent resourceComponent)
   {
      ResourceCacheKey key = new ResourceCacheKey(resourceType, resourceComponent);
      return RESOURCE_CACHE.get(key) != null;
   }

   /**
    * Returns the map of names to resources for the specified resource type and server.
    *
    * @param resourceType      type of resources in the map
    * @param resourceComponent server under which the resources live
    *
    * @return map of names to resources; will create new if this is the first request
    */
   public synchronized static Map<String, DiscoveredResourceDetails> getResourceMap(ResourceType resourceType, ResourceComponent resourceComponent)
   {
      ResourceCacheKey key = new ResourceCacheKey(resourceType, resourceComponent);

      Map<String, DiscoveredResourceDetails> resourceMap = RESOURCE_CACHE.get(key);
      if (resourceMap == null)
      {
         resourceMap = new HashMap<String, DiscoveredResourceDetails>();
         RESOURCE_CACHE.put(key, resourceMap);
      }

      return resourceMap;
   }

   /**
    * Stores a configuration for a resource.
    *
    * @param resourceKey   resource being configured
    * @param configuration configuration for the resource
    */
   public static void putConfiguration(String resourceKey, Configuration configuration)
   {
      CONFIGURATIONS.put(resourceKey, configuration);
   }

   /**
    * Returns the configuration for the specified resource.
    *
    * @param resourceKey resource in question
    *
    * @return <code>Configuration</code> instance if one exists for this resource, <code>null</code> otherwise.
    */
   public static Configuration getConfiguration(String resourceKey)
   {
      return CONFIGURATIONS.get(resourceKey);
   }

   /**
    * Stores the availability for a resource.
    *
    * @param resourceKey  resource being updated
    * @param availability availability of the resource
    */
   public static void putAvailability(String resourceKey, AvailabilityType availability)
   {
      AVAILABILITY.put(resourceKey, availability);
   }

   /**
    * Returns the availability for the specified resource.
    *
    * @param resourceKey resource in question
    *
    * @return <code>AvailabilityType</code> if it is known for this resource; <code>null</code> otherwise.
    */
   public static AvailabilityType getAvailability(String resourceKey)
   {
      return AVAILABILITY.get(resourceKey);
   }

   /**
    * Stores the map of metric names to calculators for the specified resource.
    *
    * @param resourceKey resource being updated
    * @param calculators map of calculators
    */
   public static void putMetricCalculators(String resourceKey, Map<String, MetricValueCalculator> calculators)
   {
      METRIC_CALCULATORS.put(resourceKey, calculators);
   }

   /**
    * Returns the mapping of metric names to calculators.
    *
    * @param resourceKey resource in question
    *
    * @return <code>Map</code> if it is known for this resource; <code>null</code> otherwise.
    */
   public static Map<String, MetricValueCalculator> getMetricCalculators(String resourceKey)
   {
      return METRIC_CALCULATORS.get(resourceKey);
   }

   /**
    * Stores the map of operation names to handlers for the specified resource.
    *
    * @param resourceKey resource being updated
    * @param handlers    map of handlers
    */
   public static void putOperationHandlers(String resourceKey, Map<String, OperationHandler> handlers)
   {
      OPERATION_HANDLERS.put(resourceKey, handlers);
   }

   /**
    * Returns the mapping of operation names to operation handlers.
    *
    * @param resourceKey resource in question
    *
    * @return <code>Map</code> if it is known for this resource; <code>null</code> otherwise.
    */
   public static Map<String, OperationHandler> getOperationHandlers(String resourceKey)
   {
      return OPERATION_HANDLERS.get(resourceKey);
   }

   /**
    * Stores the artifacts for a particular resource.
    *
    * @param resourceKey cannot be <code>null</code>
    * @param artifacts   set of artifacts for the resource
    */
   public static void putArtifacts(String resourceKey, Map<String, ArtifactHolder> artifacts)
   {
      ARTIFACTS.put(resourceKey, artifacts);
   }

   /**
    * Returns the set of artifacts (packaged with all associated data) for the resource.
    *
    * @param resourceKey cannot be <code>null</code>
    *
    * @return set of artifacts for the resource if there are any; <code>null</code> otherwise
    */
   public static Map<String, ArtifactHolder> getArtifacts(String resourceKey)
   {
      return ARTIFACTS.get(resourceKey);
   }

   /**
    * Stores the errors for a particular resource.
    *
    * @param resourceKey cannot be <code>null</code>
    * @param errorHolder   error holder for the resource
    */
   public static void putErrors(String resourceKey, ConfigurationHandler errorHolder)
   {
      ERRORS.put(resourceKey, errorHolder);
   }

   /**
    * Returns the set of errors (packaged with all associated data) for the resource.
    *
    * @param resourceKey cannot be <code>null</code>
    *
    * @return errorHolder of errors for the resource if there are any; <code>null</code> otherwise
    */
   public static ConfigurationHandler getErrors(String resourceKey)
   {
      return ERRORS.get(resourceKey);
   }

   // Inner Classes  --------------------------------------------

   /**
    * Key to find a resource that has been loaded into the cache.
    */
   private static class ResourceCacheKey
   {

      private ResourceType resourceType;
      private ResourceComponent resourceComponent;

      // Constructors  --------------------------------------------

      public ResourceCacheKey(ResourceType resourceType, ResourceComponent resourceComponent)
      {
         this.resourceType = resourceType;
         this.resourceComponent = resourceComponent;
      }

      // Object Overridden  --------------------------------------------

      public boolean equals(Object o)
      {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;

         ResourceCacheKey cacheKey = (ResourceCacheKey)o;

         if (!resourceComponent.equals(cacheKey.resourceComponent)) return false;
         if (!resourceType.equals(cacheKey.resourceType)) return false;

         return true;
      }

      public int hashCode()
      {
         int result;
         result = resourceType.hashCode();
         result = 31 * result + resourceComponent.hashCode();
         return result;
      }


      public String toString()
      {
         return "Key: " + resourceType.getName() + " + " + resourceComponent;
      }
   }
}
