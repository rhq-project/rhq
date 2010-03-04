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
import java.util.Arrays;
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
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.jboss.seam.log.Log;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertDampening;
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
    private Map<String, String> categories;
    private List<ConditionDescription> conditionDescriptions;
    private AlertCondition currentCondition;

    private Integer measurementDefinitionId = 0;
    private Double threshold;
    private Integer resourceId;

    @RequestParameter("id")
    public void setResourceId(Integer resourceId) {
        if (resourceId != null) {
            this.resourceId = resourceId;
        }
    }

    public String getMeasurementDefinitionId() {
        if (this.measurementDefinitionId != null) {
            return measurementDefinitionId.toString();
        }

        return null;
    }

    public void setMeasurementDefinitionId(String measurementDefinitionId) {
        try {
            this.measurementDefinitionId  = Integer.parseInt(measurementDefinitionId);
        } catch (NumberFormatException e) {
            this.measurementDefinitionId = null;
        }
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

    public Map<String, String> getCategories() {
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

                if (this.currentCondition.getCategory() == AlertConditionCategory.THRESHOLD) {
                    this.threshold = metricAbsoluteConverter.getForDisplay(this.currentCondition.getThreshold(), measurement);
                }
            }
        }
    }

    @Create
    public void init() {
        this.conditionDescriptions = createDescriptions();
        this.categories = createCategories();

        // start out with an empty condition
        createCondition();
    }

    private List<ConditionDescription> createDescriptions() {
        Set<AlertCondition> conditions = alertDefinition.getConditions();
        List<ConditionDescription> descriptions = new ArrayList<ConditionDescription>(conditions.size());

        for (AlertCondition condition : conditions) {
            descriptions.add(new ConditionDescription(condition));
        }

        return descriptions;
    }

    public void createCondition() {
        setCurrentCondition(new AlertCondition());
    }

    public void updateCondition() {
        if (shouldSetMeasurementDefinition()) {
            MeasurementDefinition measurementDefinition = this.measurementDefinitionManager.getMeasurementDefinition(subject, this.measurementDefinitionId);
            currentCondition.setMeasurementDefinition(measurementDefinition);
            currentCondition.setName(measurementDefinition.getDisplayName());

            if (measurementDefinition != null && this.threshold != null) {
                currentCondition.setThreshold(metricAbsoluteConverter.getForThreshold(threshold, measurementDefinition));
            }
        }

        for (ConditionDescription description : conditionDescriptions) {
            if (description.condition.equals(this.currentCondition)) {
                description.description = this.alertDescriber.describeCondition(description.condition);

                return;
            }
        }

        // otherwise, this is a new condition
        this.conditionDescriptions.add(new ConditionDescription(this.currentCondition));
    }

    public void removeCondition() {
        this.conditionDescriptions.remove(findCurrentDescription());
        setCurrentCondition(null);
    }

    public String saveAlertDefinition() {
        if (!validateDefinition()) {
            return null;
        }

        alertDefinition.setConditions(findConditions());

        try {
            alertDefinitionManager.updateAlertDefinition(subject, alertDefinition.getId(), alertDefinition, true);
        } catch(Exception e) {
            this.facesMessages.add(Severity.ERROR, "There was an error saving the alert conditions.");
            this.log.error("Error persisting AlertDefinition:  " + alertDefinition.getName(), e);

            return null;
        }

        return SUCCESS_OUTCOME;
    }

    public String newAlertDefinition() {
        if (!validateDefinition()) {
            return null;
        }

        alertDefinition.setConditions(findConditions());

        try {
            alertDefinitionManager.createAlertDefinition(subject, alertDefinition, resourceId);
        } catch(Exception e) {
            this.facesMessages.add(Severity.ERROR, "There was an error creating the alert definition.");
            this.log.error("Error persisting AlertDefinition:  " + alertDefinition.getName(), e);

            return null;
        }

        return SUCCESS_OUTCOME;
    }

    private boolean validateDefinition() {
        Set<AlertCondition> conditions = findConditions();

        if (conditions.isEmpty()) {
            this.facesMessages.add(Severity.ERROR, "Please add at least one condition.");

            return false;
        }

        AlertDampening dampening = alertDefinition.getAlertDampening();

        if (dampening.getCategory() == AlertDampening.Category.PARTIAL_COUNT) {
            if (dampening.getValue() > dampening.getPeriod()) {
                this.facesMessages.addFromResourceBundle(Severity.ERROR, "alert.config.error.PartialCountRangeTooSmall");

                return false;
            }
        }

        return true;
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

    private Map<String, String> createCategories() {
        Map<String, String> categoryMap = new HashMap<String, String>();
        List<AlertConditionCategory> categoryList = new ArrayList(Arrays.asList(AlertConditionCategory.values()));
        categoryList.remove(AlertConditionCategory.ALERT);

        if (configurationManager.getResourceConfigurationDefinitionForResourceType(subject, resourceType.getId()) == null) {
            categoryList.remove(AlertConditionCategory.RESOURCE_CONFIG);
        }

        for (AlertConditionCategory category : categoryList) {
            categoryMap.put(category.toString(), category.getName());
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