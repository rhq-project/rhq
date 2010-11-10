package org.rhq.enterprise.server.inventory;

import org.rhq.core.domain.resource.ResourceType;

import javax.ejb.Local;
import java.util.List;

@Local
public interface InventoryManagerLocal {

    /**
     * Marks the specified resource types, including all of their child types, for deletion by setting the deleted
     * flag on each one. All resources of the affected types are removed from inventory as well. Marking a resource
     * type for deletion will effectively result in the resource type and all of its associated meta data being removed
     * from the system. That meta data includes metric definitions, operation definitions, resource configuration
     * definitions, plugin configuration definitions, event definitions, etc. The actual deletion is carrried out by
     * asynchronously by a scheduled job.
     *
     * @param resourceTypes The types to delete
     * @return The number of types marked for deletion.
     */
    int markTypesDeleted(List<ResourceType> resourceTypes);

}
