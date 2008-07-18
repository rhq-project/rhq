package org.rhq.enterprise.server.resource.group.definition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;

@Stateless
public class GroupDefinitionExpressionBuilderManagerBean implements GroupDefinitionExpressionBuilderManagerLocal {

    @EJB
    private SubjectManagerLocal subjectManager;

    @EJB
    private MeasurementDefinitionManagerLocal measurementDefinitionManager;

    @EJB
    private ResourceTypeManagerLocal resourceTypeManager;

    public List<String> getTraitPropertyNames(int resourceTypeId) {
        List<MeasurementDefinition> definitions = measurementDefinitionManager.getMeasurementDefinitionsByResourceType(
            subjectManager.getOverlord(), resourceTypeId, DataType.TRAIT, null);

        List<String> results = new ArrayList<String>();
        for (MeasurementDefinition definition : definitions) {
            results.add(definition.getName());
        }
        Collections.sort(results);
        return results;
    }

    public List<String> getPluginConfigurationPropertyNames(int resourceTypeId) {
        ResourceType type = null;
        try {
            type = resourceTypeManager.getResourceTypeById(subjectManager.getOverlord(), resourceTypeId);
        } catch (ResourceTypeNotFoundException rtnfe) {
            return Collections.emptyList();
        }

        ConfigurationDefinition pluginConfiguriatonDefinition = type.getPluginConfigurationDefinition();
        return getPropertyNames(pluginConfiguriatonDefinition);
    }

    public List<String> getResourceConfigurationPropertyNames(int resourceTypeId) {
        ResourceType type = null;
        try {
            type = resourceTypeManager.getResourceTypeById(subjectManager.getOverlord(), resourceTypeId);
        } catch (ResourceTypeNotFoundException rtnfe) {
            return Collections.emptyList();
        }

        ConfigurationDefinition resourceConfiguriatonDefinition = type.getResourceConfigurationDefinition();
        return getPropertyNames(resourceConfiguriatonDefinition);
    }

    private List<String> getPropertyNames(ConfigurationDefinition definition) {
        Map<String, PropertyDefinition> definitions = definition.getPropertyDefinitions();
        if (definitions == null) {
            return Collections.emptyList();
        }
        Set<String> uniqueNames = definitions.keySet();
        List<String> propertyNames = new ArrayList<String>(uniqueNames);
        Collections.sort(propertyNames);
        return propertyNames;
    }

}
