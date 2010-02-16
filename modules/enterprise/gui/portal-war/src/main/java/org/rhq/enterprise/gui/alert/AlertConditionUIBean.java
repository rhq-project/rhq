/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.gui.alert;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.rhq.core.domain.alert.AlertDampening;
import org.rhq.core.domain.alert.AlertDampening.TimeUnits;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.BooleanExpression;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.legacy.measurement.MeasurementConstants;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.measurement.util.MeasurementFormatter;
import org.rhq.enterprise.server.operation.OperationManagerLocal;

@Scope(ScopeType.PAGE)
@Name("alertConditionUIBean")
public class AlertConditionUIBean {

    @In("#{webUser.subject}")
    private Subject subject;

    @In
    private ResourceType resourceType;

    @In
    private AlertDefinition alertDefinition;

    @In
    private AlertDefinitionManagerLocal alertDefinitionManager;

    @In
    private MeasurementDefinitionManagerLocal measurementDefinitionManager;

    @In
    private OperationManagerLocal operationManager;

    @In
    private MeasurementScheduleManagerLocal measurementScheduleManager;

    @In
    private Map<String, String> messages;

    private Map<String, String> conditionExpressions;
    private Map<String, String> availabilities;
    private Map<String, String> severities;
    private Map<String, String> operationStatuses;
    private Map<String, String> comparators;
    private Map<String, String> baselines;

    private Map<String, Integer> measurements;
    private Map<String, Integer> traits;
    private Map<String, String> operations;
    private Map<String, Integer> existingAlerts;
    private Map<String, String> dampeningCategories;
    private Map<String, String> timeUnits;


    public Map<String, String> getConditionExpressions() {
        return conditionExpressions;
    }

    public Map<String, String> getAvailabilities() {
        return availabilities;
    }

    public Map<String, String> getSeverities() {
        return severities;
    }

    public Map<String, String> getOperationStatuses() {
        return operationStatuses;
    }

    public Map<String, String> getComparators() {
        return comparators;
    }

    public Map<String, String> getBaselines() {
        return baselines;
    }

    public Map<String, String> getOperations() {
        return operations;
    }

    public Map<String, Integer> getMeasurements() {
        return measurements;
    }

    public Map<String, Integer> getTraits() {
        return traits;
    }

    public Map<String, Integer> getExistingAlerts() {
        return existingAlerts;
    }

    public Map<String, String> getDampeningCategories() {
        return dampeningCategories;
    }

    public Map<String, String> getTimeUnits() {
        return timeUnits;
    }

    @Create
    public void init() {
        this.conditionExpressions = createBooleanMap();
        this.availabilities = createMap(AvailabilityType.class, "Goes ");
        this.severities = createMap(EventSeverity.class);
        this.operationStatuses = createMap(OperationRequestStatus.class);
        this.comparators = createComparatorMap();
        this.baselines = createBaselines();

        this.measurements = lookupMeasurements(DataType.MEASUREMENT);
        this.traits = lookupMeasurements(DataType.TRAIT);
        this.operations = lookupOperations();
        this.existingAlerts = lookupExistingAlerts();
        this.dampeningCategories = createDampeningMap();
        this.timeUnits = createTimeUnits();
    }

    private Map<String, String> createBooleanMap() {
        Map<String, String> booleanMap = new HashMap<String, String>();

        booleanMap.put(BooleanExpression.ALL.name(), BooleanExpression.ALL.name());
        booleanMap.put(BooleanExpression.ANY.name(), BooleanExpression.ANY.name());

        return booleanMap;
    }

    private Map<String, Integer> lookupExistingAlerts() {
        Map<String, Integer> alertsMap = new HashMap<String, Integer>();

        PageList<AlertDefinition> alerts = alertDefinitionManager.findAlertDefinitions(subject,
                this.alertDefinition.getResource().getId(), PageControl.getUnlimitedInstance());

        for (AlertDefinition alert : alerts) {
            alertsMap.put(alert.getName(), alert.getId());
        }

        return alertsMap;
    }

    private Map<String, String> createTimeUnits() {
        Map<String, String> timeUnitMap = new HashMap<String, String>();

        for (TimeUnits unit : TimeUnits.values()) {
            String unitName = unit.name();
            timeUnitMap.put(unitName.toLowerCase(), unitName);
        }

        return timeUnitMap;
    }

    private Map<String, String> createDampeningMap() {
        Map<String, String> dampeningMap = new HashMap<String, String>();

        dampeningMap.put("Every Time", "NONE");
        dampeningMap.put("Every X Times Consecutively", AlertDampening.Category.CONSECUTIVE_COUNT.name());
        dampeningMap.put("Every X Out of Y Evaluations", AlertDampening.Category.PARTIAL_COUNT.name());
        dampeningMap.put("Every X Times Within a Time Duration ", AlertDampening.Category.DURATION_COUNT.name());

        return dampeningMap;
    }

    private Map<String, String> createComparatorMap() {
        Map<String, String> comparatorMap = new HashMap<String, String>();

        comparatorMap.put(messages.get("alert.config.props.CB.Content.Comparator.>"), ">");
        comparatorMap.put(messages.get("alert.config.props.CB.Content.Comparator.<"), "<");
        comparatorMap.put(messages.get("alert.config.props.CB.Content.Comparator.="), "=");

        return comparatorMap;
    }

    private Map<String, String> createBaselines() {
        Map<String, String> baselineMap = new HashMap<String, String>();

        List<MeasurementDefinition> definitions = this.measurementDefinitionManager.findMeasurementDefinitionsByResourceType(subject,
                resourceType.getId(), DataType.MEASUREMENT, null);

        int[] definitionIds = new int[definitions.size()];

        for (int i = 0; i < definitionIds.length; i++) {
            definitionIds[i] = definitions.get(i).getId();
        }

        List<MeasurementSchedule> schedules = measurementScheduleManager.findSchedulesByResourceIdAndDefinitionIds(subject,
                this.alertDefinition.getResource().getId(), definitionIds);

        for (MeasurementSchedule schedule : schedules) {
            baselineMap.put(getLabel(MeasurementConstants.BASELINE_OPT_MIN, schedule), MeasurementConstants.BASELINE_OPT_MIN);
            baselineMap.put(getLabel(MeasurementConstants.BASELINE_OPT_MEAN, schedule), MeasurementConstants.BASELINE_OPT_MEAN);
            baselineMap.put(getLabel(MeasurementConstants.BASELINE_OPT_MAX, schedule), MeasurementConstants.BASELINE_OPT_MAX);
        }

        return baselineMap;

    }

    private String getLabel(String measurementConstant, MeasurementSchedule schedule) {
        return MeasurementFormatter.getBaselineText(measurementConstant, schedule);
    }

    private Map<String, Integer> lookupMeasurements(DataType type) {
        Map<String, Integer> measurementMap = new HashMap<String, Integer>();

        List<MeasurementDefinition> definitions = this.measurementDefinitionManager.findMeasurementDefinitionsByResourceType(subject,
                resourceType.getId(), type, null);

        for (MeasurementDefinition definition : definitions) {
            measurementMap.put(definition.getDisplayName(), definition.getId());
        }

        return measurementMap;
    }

    private Map<String, String> lookupOperations() {
        Map<String, String> operationMap = new HashMap<String, String>();

        List<OperationDefinition> operationDefinitions = operationManager.findSupportedResourceTypeOperations(subject,
                resourceType.getId(), false);

        for (OperationDefinition operationDefinition : operationDefinitions) {
            operationMap.put(operationDefinition.getDisplayName(), operationDefinition.getName());
        }

        return operationMap;
    }

    private <T extends Enum> Map<String, String> createMap(Class<T> enumType) {
        return createMap(enumType, "");
    }

    private <T extends Enum> Map<String, String> createMap(Class<T> enumType, String prefix) {
        Set<T> enumSet = EnumSet.allOf(enumType);
        Map<String, String> enumMap = new HashMap<String, String>(enumSet.size());

        for (T enumItem : enumSet) {
            enumMap.put(prefix + enumItem.name(), enumItem.toString());
        }

        return enumMap;
    }

}