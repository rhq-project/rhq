package org.rhq.enterprise.server.resource.metadata;


import org.rhq.core.domain.resource.ResourceType;

import javax.ejb.Local;

@Local
public interface ResourceConfigurationMetadataManagerLocal {

    void updateResourceConfigurationDefinition(ResourceType existingType, ResourceType newType);

}
