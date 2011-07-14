package org.rhq.enterprise.server.resource.metadata;

import javax.ejb.Local;

import org.rhq.core.domain.resource.ResourceType;

@Local
public interface OperationMetadataManagerLocal {

    /**
     * Update the operation definitions of existingType with the ones from resource type.
     *
     * @param existingType The existing resource type with operation Definitions
     * @param newType New resourceType definition with operationDefinitions
     */
    void updateMetadata(ResourceType existingType, ResourceType newType);

}
