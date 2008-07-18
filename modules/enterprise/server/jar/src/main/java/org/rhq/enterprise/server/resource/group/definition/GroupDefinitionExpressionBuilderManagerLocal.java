package org.rhq.enterprise.server.resource.group.definition;

import java.util.List;

import javax.ejb.Local;

@Local
public interface GroupDefinitionExpressionBuilderManagerLocal {

    public List<String> getTraitPropertyNames(int resourceTypeId);

    public List<String> getPluginConfigurationPropertyNames(int resourceTypeId);

    public List<String> getResourceConfigurationPropertyNames(int resourceTypeId);

}
