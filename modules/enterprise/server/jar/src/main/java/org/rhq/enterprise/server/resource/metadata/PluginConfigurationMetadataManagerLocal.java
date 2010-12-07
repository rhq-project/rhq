package org.rhq.enterprise.server.resource.metadata;


import javax.ejb.Local;

import org.rhq.core.domain.resource.ResourceType;

@Local
public interface PluginConfigurationMetadataManagerLocal {

    void updatePluginConfigurationDefinition(ResourceType existingType, ResourceType newType);

}
