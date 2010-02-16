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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.jboss.seam.log.Log;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.alert.converter.MetricAbsoluteConverter;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;

@Scope(ScopeType.PAGE)
@Name("alertConditionsUIBean")
public class AlertConditionsUIBean {

    private final static String SUCCESS_OUTCOME = "success";

    @Logger
    private Log log;

    @In
    private FacesMessages facesMessages;

    @In("#{webUser.subject}")
    private Subject subject;

    @In
    private ResourceType resourceType;

    @In
    private AlertDefinitionManagerLocal alertDefinitionManager;

    @In
    private MeasurementDefinitionManagerLocal measurementDefinitionManager;

    @In
    private ConfigurationManagerLocal configurationManager;

    @In
    private AlertDefinition alertDefinition;

    @In
    private AlertDescriber alertDescriber;

    private MetricAbsoluteConverter metricAbsoluteConverter = new MetricAbsoluteConverter();
    private Map<AlertConditionCategory, String> categories;
    private List<ConditionDescription> conditionDescriptions;
    private AlertCondition currentCondition;

    private Integer measurementDefinitionId = 0;
    private Double threshold;

    public String getMeasurementDefinitionId() {
        return measurementDefinitionId.toString();
    }

    public void setMeasurementDefinitionId(String measurementDefinitionId) {
        this.measurementDefinitionId  = Integer.parseInt(measurementDefinitionId);
    }

    public String getThreshold() {
        if (threshold != null) {
            return threshold.toString();
        }

        return null;
    }

    public void setThreshold(String threshold) {
        this.threshold = Double.parseDouble(threshold);
    }

    public List<ConditionDescription> getConditionDescriptions() {
        return conditionDescriptions;
    }

    public Map<AlertConditionCategory, String> getCategories() {
        return categories;
    }

    public AlertCondition getCurrentCondition() {
        return currentCondition;
    }

    public void setCurrentCondition(AlertCondition currentCondition) {
        this.currentCondition = currentCondition;
        
        if (shouldSetMeasurementDefinition()) {
            MeasurementDefinition measurement = this.currentCondition.getMeasurementDefinition();

            if (measurement != null) {
                setMeasurementDefinitionId(Integer.toString(measurement.getId()));
                this.threshold = metricAbsoluteConverter.getForDisplay(this.currentCondition.getThreshold(), measurement);
            }
        }
    }

    @Create
    public void init() {
        this.conditionDescriptions = createDescriptions();
        this.categories = createCategoryMap();
    }

    private List<ConditionDescription> createDescriptions() {
        Set<AlertCondition> conditions = alertDefinition.getConditions();
        List<ConditionDescription> descriptions = new ArrayList<ConditionDescription>(conditions.size());

        for (AlertCondition condition : conditions) {
            descriptions.add(new ConditionDescription(condition));
        }

        return descriptions;
    }

    public void addCondition() {
        setCurrentCondition(new AlertCondition());
    }

    public String removeCondition() {
        this.conditionDescriptions.remove(findCurrentDescription());
        setCurrentCondition(null);

        // persist to DB
        return saveConditions();
    }

    public String saveConditions() {
        if (shouldSetMeasurementDefinition()) {
            MeasurementDefinition measurementDefinition = this.measurementDefinitionManager.getMeasurementDefinition(subject, this.measurementDefinitionId);
            currentCondition.setMeasurementDefinition(measurementDefinition);
            currentCondition.setName(measurementDefinition.getDisplayName());

            if (measurementDefinition != null && this.threshold != null) {
                currentCondition.setThreshold(metricAbsoluteConverter.getForThreshold(threshold, measurementDefinition));
            }
        }

        Set<AlertCondition> conditions = findConditions();

        // in case of a newly added condition
        if (this.currentCondition != null && !conditions.contains(this.currentCondition)) {
            conditions.add(this.currentCondition);
        }

        alertDefinition.setConditions(conditions);

        return saveAlertDefinition();
    }

    public String saveAlertDefinition() {
        try {
            alertDefinitionManager.updateAlertDefinition(subject, alertDefinition.getId(), alertDefinition, true);
        } catch(Exception e) {
            this.facesMessages.add(Severity.ERROR, "There was an error saving the alert conditions.");
            this.log.error("Error persisting AlertCondition on AlertDefinition:  " + alertDefinition.getName(), e);

            return null;
        }

        return SUCCESS_OUTCOME;
    }

    private boolean shouldSetMeasurementDefinition() {
        if (currentCondition != null) {
            AlertConditionCategory category = currentCondition.getCategory();

            return category == AlertConditionCategory.TRAIT ||
                    category == AlertConditionCategory.CHANGE ||
                    category == AlertConditionCategory.THRESHOLD ||
                    category == AlertConditionCategory.BASELINE;
        }

        return false;
    }
    
    private ConditionDescription findCurrentDescription() {
        for (ConditionDescription description : conditionDescriptions) {
            if (description.condition == this.currentCondition) {
                return description;
            }
        }

        return null;
    }

    private Set<AlertCondition> findConditions() {
        Set<AlertCondition> conditionSet = new HashSet<AlertCondition>(conditionDescriptions.size());

        for (ConditionDescription description : conditionDescriptions) {
            conditionSet.add(description.condition);
        }

        return conditionSet;
    }

    private Map<AlertConditionCategory, String> createCategoryMap() {
        Map<AlertConditionCategory, String> categoryMap = new HashMap<AlertConditionCategory, String>();

        for (AlertConditionCategory category : AlertConditionCategory.values()) {
            categoryMap.put(category, category.getName());
        }

        categoryMap.remove(AlertConditionCategory.ALERT);

        if (configurationManager.getResourceConfigurationDefinitionForResourceType(subject, resourceType.getId()) == null) {
            categoryMap.remove(AlertConditionCategory.RESOURCE_CONFIG);
        }

        return categoryMap;
    }

    public class ConditionDescription {
        private AlertCondition condition;
        private String description;

        public ConditionDescription(AlertCondition condition) {
            this.condition = condition;
            this.description = alertDescriber.describeCondition(condition);
        }

        public AlertCondition getCondition() {
            return condition;
        }

        public String getDescription() {
            return description;
        }
    }

}