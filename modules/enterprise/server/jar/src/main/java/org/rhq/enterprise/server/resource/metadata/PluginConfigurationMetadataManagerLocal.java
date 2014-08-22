package org.rhq.enterprise.server.resource.metadata;

import java.util.List;

import javax.ejb.Local;

import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.configuration.metadata.ConfigurationDefinitionUpdateReport;

@Local
public interface PluginConfigurationMetadataManagerLocal {

    void updatePluginConfigurationDefinition(ResourceType existingType, ResourceType newType);

    void updateResourcePluginConfigurationsInNewTransaction(List<Integer> resourceIds,
        ConfigurationDefinitionUpdateReport updateReport);
}
