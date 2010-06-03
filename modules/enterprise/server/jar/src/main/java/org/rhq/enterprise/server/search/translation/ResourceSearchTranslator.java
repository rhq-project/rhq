package org.rhq.enterprise.server.search.translation;

import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.search.assist.AlertSearchAssistParam;
import org.rhq.enterprise.server.search.translation.antlr.RHQLAdvancedTerm;
import org.rhq.enterprise.server.search.translation.antlr.RHQLComparisonOperator;
import org.rhq.enterprise.server.search.translation.jpql.SearchFragment;
import org.rhq.enterprise.server.search.translation.jpql.SearchFragmentType;

public class ResourceSearchTranslator extends AbstractSearchTranslator {

    public SearchFragment getSearchFragment(String alias, RHQLAdvancedTerm term) {
        String path = term.getPath();
        RHQLComparisonOperator op = term.getOperator();
        String param = term.getParam();

        String filter = term.getValue();

        if (path.equals("availability")) {
            return new SearchFragment(SearchFragmentType.WHERE_CLAUSE, //
                getJPQLForEnum(alias + ".currentAvailability.availabilityType", op, filter, AvailabilityType.class,
                    true));

        } else if (path.equals("category")) {
            return new SearchFragment(SearchFragmentType.WHERE_CLAUSE, //
                getJPQLForString(alias + ".resourceType.category", op, filter));

        } else if (path.equals("type")) {
            return new SearchFragment(SearchFragmentType.WHERE_CLAUSE, //
                getJPQLForString(alias + ".resourceType.name", op, filter));

        } else if (path.equals("plugin")) {
            return new SearchFragment(SearchFragmentType.WHERE_CLAUSE, //
                getJPQLForString(alias + ".resourceType.plugin", op, filter));

        } else if (path.equals("name")) {
            return new SearchFragment(SearchFragmentType.WHERE_CLAUSE, //
                getJPQLForString(alias + ".name", op, filter));

        } else if (path.equals("alerts")) {
            return new SearchFragment( //
                SearchFragmentType.PRIMARY_KEY_SUBQUERY, "SELECT res.id" //
                    + "  FROM Resource res " //
                    + "  JOIN res.alertDefinitions alertDef " //
                    + "  JOIN alertDef.alerts alert " //
                    + " WHERE alert.ctime > "
                    + AlertSearchAssistParam.getLastTime(param) //
                    + (filter.equalsIgnoreCase("any") ? "" : "   and "
                        + getJPQLForEnum("alertDef.priority", op, filter, AlertPriority.class, false)) //
                    + " GROUP BY res.id " //
                    + "HAVING COUNT(alert) > 0 ");

        } else if (path.equals("trait")) {
            return new SearchFragment( //
                SearchFragmentType.PRIMARY_KEY_SUBQUERY, "SELECT res.id" //
                    + "  FROM Resource res, MeasurementDataTrait trait " //
                    + "  JOIN res.schedules schedule " //
                    + " WHERE trait.schedule = schedule " //
                    + "   AND schedule.definition.dataType = 1 " //
                    + "   AND schedule.definition.name = " + quote(param) //
                    + "   AND " + getJPQLForString("trait.value", op, filter));

        } else if (path.equals("connection")) {
            return new SearchFragment( //
                SearchFragmentType.PRIMARY_KEY_SUBQUERY, "SELECT res.id" //
                    + "  FROM Resource res " //
                    + "  JOIN res.resourceType.pluginConfigurationDefinition.propertyDefinitions definition " //
                    + "  JOIN res.pluginConfiguration.properties property " //
                    + " WHERE definition.name = " + quote(param) //
                    + "   AND " + getJPQLForString("property.value", op, filter));

        } else if (path.equals("configuration")) {
            return new SearchFragment( //
                SearchFragmentType.PRIMARY_KEY_SUBQUERY, "SELECT res.id" //
                    + "  FROM Resource res " //
                    + "  JOIN res.resourceType.resourceConfigurationDefinition.propertyDefinitions definition " //
                    + "  JOIN res.resourceConfiguration.properties property " //
                    + " WHERE definition.name = " + quote(param) //
                    + "   AND " + getJPQLForString("property.value", op, filter));

        } else {
            if (param == null) {
                throw new IllegalArgumentException("No search fragment available for " + path);
            } else {
                throw new IllegalArgumentException("No search fragment available for " + path + "[" + param + "]");
            }
        }
    }
}
