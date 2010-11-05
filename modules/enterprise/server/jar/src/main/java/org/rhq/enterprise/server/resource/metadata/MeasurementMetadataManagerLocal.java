package org.rhq.enterprise.server.resource.metadata;

import org.rhq.core.domain.resource.ResourceType;

import javax.ejb.Local;

@Local
public interface MeasurementMetadataManagerLocal {

    void updateMetadata(ResourceType existingType, ResourceType newType);

    void deleteMetadata(ResourceType resourceType);

}
