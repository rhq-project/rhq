package org.rhq.enterprise.server.inventory;

import java.util.List;

import javax.ejb.Local;

import org.rhq.core.domain.resource.ResourceType;

/**
 * Provides methods for carrying resource type deletion.
 */
@Local
public interface InventoryManagerLocal {

    /**
     * Marks the specified resource types, including all of their child types, for deletion by setting the deleted
     * flag on each one. All resources of the affected types are removed from inventory as well. Marking a resource
     * type for deletion will effectively result in the resource type and all of its associated meta data being removed
     * from the system. That meta data includes metric definitions, operation definitions, resource configuration
     * definitions, plugin configuration definitions, event definitions, etc. Note that this method only markes the
     * resource types and their resources for deletion. The actual deletion is carried out by asynchronously by a
     * scheduled job.
     *
     * @param resourceTypeIds The ids of the resource types to delete
     * @return The number of types marked for deletion.
     */
    int markTypesDeleted(List<Integer> resourceTypeIds);

    /**
     * @return A list of all resource types that are marked for deletion
     */
    List<ResourceType> getDeletedTypes();

    /**
     * Determines whether or not a resource type is ready to be permanently removed from the database. A resource type
     * is ready to be removed if 1) its deleted flag has been set and 2) if all of its resources have already been
     * removed from the database.
     *
     * @param resourceType The resource type to check
     * @return <code>true</code> if the resource type is ready to be permanently deleted.
     */
    boolean isReadyForPermanentRemoval(ResourceType resourceType);

    /**
     * Permanently removes the resource type from the database along with all associated meta data such as metric
     * definitions, operation definitions, and resource configuration definitions. This method is intended to be called
     * by the scheduled job {@link org.rhq.enterprise.server.scheduler.jobs.PurgeResourceTypesJob}.
     *
     * @param resourceType The resource type to delete
     */
    void purgeDeletedResourceType(ResourceType resourceType);

}
