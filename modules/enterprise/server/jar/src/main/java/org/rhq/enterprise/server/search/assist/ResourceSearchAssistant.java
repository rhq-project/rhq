package org.rhq.enterprise.server.search.assist;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.auth.Subject;
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

    public ResourceSearchAssistant(Subject subject, String tab) {
        super(subject, tab);
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
                + "   AND res.resourceType.deleted = false"
                + "   AND simpleDefinition = definition " // only suggest names for simple properties
                + "   AND simpleDefinition.type <> 'PASSWORD' " // do not suggest hidden/password property types
                + conditionallyAddJPQLString("definition.name", filter) //
                + conditionallyAddJPQLString("type.category", tab) //
                + conditionallyAddAuthzFragment(getAuthzFragment()) //
                + " ORDER BY definition.name ");

        } else if (context.equals("configuration")) {
            return execute("" //
                + "SELECT DISTINCT definition.name " //
                + "  FROM ResourceType type, Resource res, PropertyDefinitionSimple simpleDefinition " //"
                + "  JOIN type.resourceConfigurationDefinition.propertyDefinitions definition " //
                + " WHERE res.resourceType = type " // only suggest names that exist for resources in inventory
                + "   AND res.resourceType.deleted = false"
                + "   AND simpleDefinition = definition " // only suggest names for simple properties
                + "   AND simpleDefinition.type <> 'PASSWORD' " // do not suggest hidden/password property types
                + conditionallyAddJPQLString("definition.name", filter) //
                + conditionallyAddJPQLString("type.category", tab) //
                + conditionallyAddAuthzFragment(getConfigAuthzFragment()) //
                + " ORDER BY definition.name ");

        } else if (context.equals("trait")) {
            return execute("" //
                + "SELECT DISTINCT def.name " //
                + "  FROM MeasurementSchedule ms, Resource res " //
                + "  JOIN ms.definition def " //
                + " WHERE ms.resource = res " // only suggest names that exist for resources in inventory
                + "   AND def.dataType = 1 " // trait types
                + conditionallyAddJPQLString("ms.definition.name", filter) //
                + conditionallyAddJPQLString("res.resourceType.category", tab) //
                + conditionallyAddAuthzFragment(getAuthzFragment()) //
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
                + conditionallyAddJPQLString("type.name", filter) //
                + conditionallyAddJPQLString("type.category", tab) //
                + conditionallyAddAuthzFragment(getAuthzFragment()) //
                + " ORDER BY type.name ");

        } else if (context.equals("plugin")) {
            return execute("" //
                + "SELECT DISTINCT type.plugin " //
                + "  FROM Resource res, ResourceType type " //
                + " WHERE res.resourceType = type " //
                + conditionallyAddJPQLString("type.plugin", filter) //
                + conditionallyAddJPQLString("type.category", tab) //
                + conditionallyAddAuthzFragment(getAuthzFragment()) //
                + " ORDER BY type.plugin ");

        } else if (context.equals("name")) {
            return execute("" //
                + "SELECT DISTINCT res.name " //
                + "  FROM Resource res, ResourceType type " //
                + " WHERE res.resourceType = type " //
                + conditionallyAddJPQLString("res.name", filter) //
                + conditionallyAddJPQLString("type.category", tab) //
                + conditionallyAddAuthzFragment(getAuthzFragment()) //
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
                + conditionallyAddJPQLString("property.name", param) //
                + conditionallyAddJPQLString("property.stringValue", filter) //
                + conditionallyAddJPQLString("res.resourceType.category", tab) //
                + conditionallyAddAuthzFragment(getAuthzFragment()) //
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
                + conditionallyAddJPQLString("property.name", param) //
                + conditionallyAddJPQLString("property.stringValue", filter) //
                + conditionallyAddJPQLString("res.resourceType.category", tab) //
                + conditionallyAddAuthzFragment(getConfigAuthzFragment()) //
                + " ORDER BY simple.stringValue ");

        } else if (context.equals("trait")) {
            return execute("" //
                + "SELECT DISTINCT trait.value " //
                + "  FROM MeasurementDataTrait trait, Resource res " //
                + "  JOIN trait.schedule ms " //
                + " WHERE ms.definition.dataType = 1 " //
                + "   AND ms.resource = res " // only suggest values that exist for inventoried resources
                + conditionallyAddJPQLString("ms.definition.name", param) //
                + conditionallyAddJPQLString("trait.value", filter) //
                + conditionallyAddJPQLString("res.resourceType.category", tab) //
                + conditionallyAddAuthzFragment(getAuthzFragment()) //
                + " ORDER BY trait.value ");

        } else {
            return Collections.emptyList();

        }
    }

    private String getConfigAuthzFragment() {
        return "res.id IN " //
            + "(SELECT ires.id " //
            + "   FROM Resource ires " //
            + "   JOIN ires.implicitGroups igroup " //
            + "   JOIN igroup.roles irole " //
            + "   JOIN irole.subjects isubject " //
            + "   JOIN irole.permissions iperm " //
            + "  WHERE isubject.id = " + getSubjectId() //
            + "    AND iperm = 11)";
    }

    private String getAuthzFragment() {
        return "res.id IN " //
            + "(SELECT ires.id " //
            + "   FROM Resource ires " //
            + "   JOIN ires.implicitGroups igroup " //
            + "   JOIN igroup.roles irole " //
            + "   JOIN irole.subjects isubject " //
            + "  WHERE isubject.id = " + getSubjectId() + ")";
    }
}
