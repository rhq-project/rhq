package org.rhq.enterprise.server.search.assist;

import static org.rhq.enterprise.server.search.common.SearchQueryGenerationUtility.getJPQLForString;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.core.domain.search.assist.AlertSearchAssistParam;

public class ResourceSearchAssistant extends TabAwareSearchAssistant {

    private static final List<String> parameterizedContexts;
    private static final List<String> simpleContexts;

    static {
        parameterizedContexts = Collections.unmodifiableList(Arrays.asList("alerts", "connection", "configuration",
            "trait"));
        simpleContexts = Collections.unmodifiableList(Arrays.asList("availability", "category", "type", "plugin",
            "name"));
    }

    public ResourceSearchAssistant(String tab) {
        super(tab);
    }

    public SearchSubsystem getSearchSubsystem() {
        return SearchSubsystem.RESOURCE;
    }

    @Override
    public String getPrimarySimpleContext() {
        return "name";
    }

    @Override
    public List<String> getSimpleContexts() {
        return simpleContexts;
    }

    @Override
    public List<String> getParameterizedContexts() {
        return parameterizedContexts;
    }

    @Override
    public boolean isEnumContext(String context) {
        if (context.equals("alerts")) {
            return true;
        }
        return false;
    }

    @Override
    public List<String> getParameters(String context, String filter) {
        filter = stripQuotes(filter);
        if (context.equals("alerts")) {
            return filter(AlertSearchAssistParam.class, filter);

        } else if (context.equals("connection")) {
            return execute("" //
                + "SELECT DISTINCT definition.name " //
                + "  FROM ResourceType type, Resource res, PropertyDefinitionSimple simpleDefinition " //"
                + "  JOIN type.pluginConfigurationDefinition.propertyDefinitions definition " //
                + " WHERE res.resourceType = type " // only suggest names that exist for resources in inventory
                + "   AND simpleDefinition = definition " // only suggest names for simple properties
                + "   AND simpleDefinition.type <> 'PASSWORD' " // do not suggest hidden/password property types
                + "   AND " + getJPQLForString("definition.name", filter) //
                + "   AND " + getJPQLForString("type.category", tab) //
                + " ORDER BY definition.name ");

        } else if (context.equals("configuration")) {
            return execute("" //
                + "SELECT DISTINCT definition.name " //
                + "  FROM ResourceType type, Resource res, PropertyDefinitionSimple simpleDefinition " //"
                + "  JOIN type.resourceConfigurationDefinition.propertyDefinitions definition " //
                + " WHERE res.resourceType = type " // only suggest names that exist for resources in inventory
                + "   AND simpleDefinition = definition " // only suggest names for simple properties
                + "   AND simpleDefinition.type <> 'PASSWORD' " // do not suggest hidden/password property types
                + "   AND " + getJPQLForString("definition.name", filter) //
                + "   AND " + getJPQLForString("type.category", tab) //
                + " ORDER BY definition.name ");

        } else if (context.equals("trait")) {
            return execute("" //
                + "SELECT DISTINCT def.name " //
                + "  FROM MeasurementSchedule ms, Resource res " //
                + "  JOIN ms.definition def " //
                + " WHERE ms.resource = res " // only suggest names that exist for resources in inventory
                + "   AND def.dataType = 1 " // trait types
                + "   AND " + getJPQLForString("ms.definition.name", filter) //
                + "   AND " + getJPQLForString("res.resourceType.category", tab) //
                + " ORDER BY def.name ");

        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getValues(String context, String param, String filter) {
        filter = stripQuotes(filter);
        if (context.equals("availability")) {
            return filter(AvailabilityType.class, filter);

        } else if (context.equals("category")) {
            return filter(ResourceCategory.class, filter);

        } else if (context.equals("type")) {
            return execute("" //
                + "SELECT DISTINCT type.name " //
                + "  FROM Resource res, ResourceType type " //
                + " WHERE res.resourceType = type " //
                + "   AND " + getJPQLForString("type.name", filter) //
                + "   AND " + getJPQLForString("type.category", tab) //
                + " ORDER BY type.name ");

        } else if (context.equals("plugin")) {
            return execute("" //
                + "SELECT DISTINCT type.plugin " //
                + "  FROM Resource res, ResourceType type " //
                + " WHERE res.resourceType = type " //
                + "   AND " + getJPQLForString("type.plugin", filter) //
                + "   AND " + getJPQLForString("type.category", tab) //
                + " ORDER BY type.plugin ");

        } else if (context.equals("name")) {
            return execute("" //
                + "SELECT DISTINCT res.name " //
                + "  FROM Resource res, ResourceType type " //
                + " WHERE res.resourceType = type " //
                + "   AND " + getJPQLForString("res.name", filter) //
                + "   AND " + getJPQLForString("type.category", tab) //
                + " ORDER BY res.name ");

        } else if (context.equals("alerts")) {
            return filter(AlertPriority.class, filter, true);

        } else if (context.equals("connection")) {
            return execute("" //
                + "SELECT DISTINCT simple.stringValue " //
                + "  FROM Resource res, PropertySimple simple, PropertyDefinitionSimple simpleDefinition " //
                + "  JOIN res.pluginConfiguration.properties property " // suggest values for existing resources only
                + "  JOIN res.resourceType.pluginConfigurationDefinition.propertyDefinitions propertyDefinition " // suggest values for existing resources only
                + " WHERE simpleDefinition = propertyDefinition " // only suggest values for simple properties
                + "   AND simpleDefinition.type <> 'PASSWORD' " // do not suggest hidden/password property types
                + "   AND property = simple " // join here so we can project simple.stringValue
                + "   AND property.name = propertyDefinition.name " // property/definition are linked via name
                + "   AND " + getJPQLForString("property.name", param) //
                + "   AND " + getJPQLForString("property.stringValue", filter) //
                + "   AND " + getJPQLForString("res.resourceType.category", tab) //
                + " ORDER BY simple.stringValue ");

        } else if (context.equals("configuration")) {
            return execute("" //
                + "SELECT DISTINCT simple.stringValue " //
                + "  FROM Resource res, PropertySimple simple, PropertyDefinitionSimple simpleDefinition " //
                + "  JOIN res.resourceConfiguration.properties property " // suggest values for existing resources only
                + "  JOIN res.resourceType.resourceConfigurationDefinition.propertyDefinitions propertyDefinition " // suggest values for existing resources only
                + " WHERE simpleDefinition = propertyDefinition " // only suggest values for simple properties
                + "   AND simpleDefinition.type <> 'PASSWORD' " // do not suggest hidden/password property types
                + "   AND property = simple " // join here so we can project simple.stringValue
                + "   AND property.name = propertyDefinition.name " // property/definition are linked via name
                + "   AND " + getJPQLForString("property.name", param) //
                + "   AND " + getJPQLForString("property.stringValue", filter) //
                + "   AND " + getJPQLForString("res.resourceType.category", tab) //
                + " ORDER BY simple.stringValue ");

        } else if (context.equals("trait")) {
            return execute("" //
                + "SELECT DISTINCT trait.value " //
                + "  FROM MeasurementDataTrait trait, Resource res " //
                + "  JOIN trait.schedule ms " //
                + " WHERE ms.definition.dataType = 1 " //
                + "   AND ms.resource = res " // only suggest values that exist for inventoried resources
                + "   AND " + getJPQLForString("ms.definition.name", param) //
                + "   AND " + getJPQLForString("trait.value", filter) //
                + "   AND " + getJPQLForString("res.resourceType.category", tab) //
                + " ORDER BY trait.value ");

        } else {
            return Collections.emptyList();

        }
    }
}
