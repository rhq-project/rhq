package org.rhq.enterprise.server.search.assist;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.core.domain.search.assist.AlertSearchAssistParam;

public class ResourceSearchAssistant extends AbstractSearchAssistant {

    private static final List<String> parameterizedContexts;
    private static final List<String> simpleContexts;

    static {
        parameterizedContexts = Collections.unmodifiableList(Arrays.asList("alerts", "connection", "configuration",
            "trait"));
        simpleContexts = Collections.unmodifiableList(Arrays.asList("availability", "category", "type", "plugin",
            "name"));
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
                + "  FROM ResourceType type " //
                + "  JOIN type.pluginConfigurationDefinition.propertyDefinitions definition " //
                + add(" WHERE LOWER(definition.name) LIKE '%" + filter.toLowerCase() + "%'", filter) //
                + " ORDER BY definition.name ");

        } else if (context.equals("configuration")) {
            return execute("" //
                + "SELECT DISTINCT definition.name " //
                + "  FROM ResourceType type " //
                + "  JOIN type.resourceConfigurationDefinition.propertyDefinitions definition " //
                + add(" WHERE LOWER(definition.name) LIKE '%" + filter.toLowerCase() + "%'", filter) //
                + " ORDER BY definition.name ");

        } else if (context.equals("trait")) {
            return execute("" //
                + "SELECT DISTINCT definition.name " //
                + "  FROM MeasurementDefinition definition " //
                + " WHERE definition.dataType = 1 " //
                + add("   AND LOWER(definition.name) LIKE '%" + filter.toLowerCase() + "%'", filter) //
                + " ORDER BY definition.name ");

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
                + "  FROM ResourceType type " //
                + add(" WHERE LOWER(type.name) LIKE '%" + filter.toLowerCase() + "%'", filter) //
                + " ORDER BY type.name ");

        } else if (context.equals("plugin")) {
            return execute("" //
                + "SELECT DISTINCT type.plugin " //
                + "  FROM ResourceType type " //
                + add(" WHERE LOWER(type.plugin) LIKE '%" + filter.toLowerCase() + "%'", filter) //
                + " ORDER BY type.plugin ");

        } else if (context.equals("name")) {
            return execute("" //
                + "SELECT DISTINCT res.name " //
                + "  FROM Resource res " //
                + add(" WHERE LOWER(res.name) LIKE '%" + filter.toLowerCase() + "%'", filter) //
                + " ORDER BY res.name ");

        } else if (context.equals("alerts")) {
            return filter(AlertPriority.class, filter, true);

        } else if (context.equals("connection")) {
            return execute("" //
                + "SELECT DISTINCT simple.stringValue " //
                + "  FROM Resource res, PropertySimple simple " //
                + "  JOIN res.pluginConfiguration.properties property " //
                + " WHERE simple.id = property.id " //
                + "   AND LOWER(property.name) LIKE '%" + param + "%'" //
                + add("   AND LOWER(property.stringValue) LIKE '%" + filter.toLowerCase() + "%'", filter) //
                + " ORDER BY simple.stringValue ");

        } else if (context.equals("configuration")) {
            return execute("" //
                + "SELECT DISTINCT simple.stringValue " //
                + "  FROM Resource res, PropertySimple simple " //
                + "  JOIN res.resourceConfiguration.properties property " //
                + " WHERE simple.id = property.id " //
                + "   AND LOWER(property.name) LIKE '%" + param + "%'" //
                + add("   AND LOWER(property.stringValue) LIKE '%" + filter.toLowerCase() + "%'", filter) //
                + " ORDER BY simple.stringValue ");

        } else if (context.equals("trait")) {
            return execute("" //
                + "SELECT trait.value " //
                + "  FROM MeasurementDataTrait trait " //
                + " WHERE trait.schedule.definition.dataType = 1 " //
                + "   AND LOWER(trait.schedule.definition.name) LIKE '%" + param + "%'" //
                + add("   AND LOWER(trait.value) LIKE '%" + filter.toLowerCase() + "%'", filter) //
                + " ORDER BY trait.value ");

        } else {
            return Collections.emptyList();

        }
    }

}
