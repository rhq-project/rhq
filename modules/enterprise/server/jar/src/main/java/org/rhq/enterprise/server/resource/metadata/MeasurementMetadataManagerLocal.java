package org.rhq.enterprise.server.resource.metadata;

import javax.ejb.Local;

import org.rhq.core.domain.resource.ResourceType;

@Local
public interface MeasurementMetadataManagerLocal {

    void updateMetadata(ResourceType existingType, ResourceType newType);

    void deleteMetadata(ResourceType resourceType);

}
