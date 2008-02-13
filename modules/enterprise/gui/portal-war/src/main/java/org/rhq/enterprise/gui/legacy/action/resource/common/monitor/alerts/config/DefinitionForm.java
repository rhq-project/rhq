/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.validator.GenericValidator;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.event.alert.AlertCondition;
import org.rhq.core.domain.event.alert.AlertDampening;
import org.rhq.core.domain.event.alert.AlertDefinition;
import org.rhq.core.domain.event.alert.AlertPriority;
import org.rhq.core.domain.event.alert.BooleanExpression;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.action.resource.ResourceForm;
import org.rhq.enterprise.gui.legacy.beans.OptionItem;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.alert.AlertTemplateManagerLocal;
import org.rhq.enterprise.server.legacy.events.EventConstants;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Form for editing / creating new alert definitions.
 */
public final class DefinitionForm extends ResourceForm {
    private Log log = LogFactory.getLog(DefinitionForm.class);

    // alert definition properties
    private Integer ad; // nullable
    private String name;
    private String description;
    private int priority;
    private boolean active;
    private int recoverId;
    private String conditionExpression;

    // conditions
    private List<ConditionBean> conditions;

    // default metric to select, if available
    private int metricId;
    private String metricName;

    // dampening rules (1 = every time)
    // 2) Each time condition set is true
    private String consecutiveCountValue;

    // 3) If condition set is true X times out of the last Y times it is evaluated
    private String partialCountValue;
    private String partialCountPeriod;

    // 4) If condition set is true, but ignoring it the next X times
    private String inverseCountValue;

    // 5) Once every X times conditions are exceeded within a time period of Y <time>
    private String durationCountValue;
    private String durationCountPeriod;
    private int durationCountPeriodUnits;

    // enable action recovery & filters
    private int whenEnabled; // alertDef.id of "Recovery Alert for <alertDef.name>"
    private boolean disableForRecovery; // whenEnable isn't valid if this is checked
    private boolean filteringControlActions;
    private boolean filteringNotificationActions;

    // for drop-downs
    private Collection baselines;
    private List<MeasurementDefinition> metrics;
    private List<MeasurementDefinition> traits;
    private Collection<Map.Entry<String, Integer>> alertnames;
    private Collection<Map.Entry<String, String>> conditionExpressionNames;

    private boolean cascade;

    private static String[] controlActionStatuses = { OperationRequestStatus.INPROGRESS.name(),
        OperationRequestStatus.SUCCESS.name(), OperationRequestStatus.FAILURE.name(),
        OperationRequestStatus.CANCELED.name() };

    private static OptionItem[] availabilityOptions;

    static {
        AvailabilityType[] values = AvailabilityType.values();

        availabilityOptions = new OptionItem[values.length];
        for (int i = 0; i < availabilityOptions.length; i++) {
            availabilityOptions[i] = new OptionItem("Goes " + values[i].name(), values[i].name());
        }
    }

    private static int[] priorities = new int[] { AlertPriority.HIGH.ordinal(), AlertPriority.MEDIUM.ordinal(),
        AlertPriority.LOW.ordinal() };

    private static int[] timeUnits = new int[] { Constants.ALERT_ACTION_ENABLE_UNITS_MINUTES,
        Constants.ALERT_ACTION_ENABLE_UNITS_HOURS, Constants.ALERT_ACTION_ENABLE_UNITS_DAYS,
        Constants.ALERT_ACTION_ENABLE_UNITS_WEEKS };

    private static String[] comparators = new String[] { Constants.ALERT_THRESHOLD_COMPARATOR_GT,
        Constants.ALERT_THRESHOLD_COMPARATOR_EQ, Constants.ALERT_THRESHOLD_COMPARATOR_LT };

    // special form handlers
    private boolean addingCondition;
    private int deletedCondition;

    public DefinitionForm() {
        // do nothing
    }

    public Integer getAd() {
        return ad;
    }

    public void setAd(Integer ad) {
        this.ad = ad;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * @return Returns the recoverId.
     */
    public int getRecoverId() {
        return recoverId;
    }

    /**
     * @param recoverId The recoverId to set.
     */
    public void setRecoverId(int recoverId) {
        this.recoverId = recoverId;
    }

    /*----------------------------------------------------------------
      This method is needed for some fancy Javascript processing.  Do
      not remove.
      ----------------------------------------------------------------*/
    public ConditionBean[] getConditions() {
        ConditionBean[] conds = new ConditionBean[conditions.size()];
        return conditions.toArray(conds);
    }

    public ConditionBean getCondition(int index) {
        if (index >= conditions.size()) {
            setNumConditions(index + 1);
        }

        return conditions.get(index);
    }

    /**
     * @return Returns the metricId.
     */
    public int getMetricId() {
        return metricId;
    }

    /**
     * @param metricId The metricId to set.
     */
    public void setMetricId(int metricId) {
        this.metricId = metricId;

        // Select default metric
        if (metricId > 0) {
            this.getCondition(0).setMetricId(metricId);
            this.getCondition(0).setTrigger("onMeasurement");
        }
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;

        // Select default metric
        this.getCondition(0).setMetricName(metricName);
    }

    /*
     * NEVER INVOKED public void setCondition(int index, ConditionBean condition) { log.trace("setCondition(" + index +
     * ", " + condition + ")"); if (conditions.size() > index) { conditions.set(index, condition); } else {
     * conditions.add(condition); } }
     */

    public int getNumConditions() {
        return conditions.size();
    }

    public void setNumConditions(int numConditions) {
        while (conditions.size() < numConditions) {
            conditions.add(new ConditionBean());
        }
        while (conditions.size() > numConditions) {
            conditions.remove(conditions.size() - 1);
        }
    }

    /**
     * Get when to enable the actions.
     */
    public int getWhenEnabled() {
        return whenEnabled;
    }

    public void setWhenEnabled(int whenEnabled) {
        this.whenEnabled = whenEnabled;
    }

    public String getPartialCountValue() {
        return partialCountValue;
    }

    public void setPartialCountValue(String partialCountValue) {
        this.partialCountValue = partialCountValue;
    }

    public String getPartialCountPeriod() {
        return partialCountPeriod;
    }

    public void setPartialCountPeriod(String partialCountPeriod) {
        this.partialCountPeriod = partialCountPeriod;
    }

    public String getDurationCountValue() {
        return durationCountValue;
    }

    public void setDurationCountValue(String durationCountValue) {
        this.durationCountValue = durationCountValue;
    }

    public String getDurationCountPeriod() {
        return durationCountPeriod;
    }

    public void setDurationCountPeriod(String durationCountPeriod) {
        this.durationCountPeriod = durationCountPeriod;
    }

    public int getDurationCountPeriodUnits() {
        return durationCountPeriodUnits;
    }

    public void setDurationCountPeriodUnits(int durationCountPeriodUnits) {
        this.durationCountPeriodUnits = durationCountPeriodUnits;
    }

    /**
     * @return Returns the disableForRecovery.
     */
    public boolean isDisableForRecovery() {
        return disableForRecovery;
    }

    /**
     * @param disableForRecovery The disableForRecovery to set.
     */
    public void setDisableForRecovery(boolean disableForRecovery) {
        this.disableForRecovery = disableForRecovery;
    }

    public boolean isFilteringControlActions() {
        return filteringControlActions;
    }

    public void setFilteringControlActions(boolean filteringControlActions) {
        this.filteringControlActions = filteringControlActions;
    }

    public boolean isFilteringNotificationActions() {
        return filteringNotificationActions;
    }

    public void setFilteringNotificationActions(boolean filteringNotificationActions) {
        this.filteringNotificationActions = filteringNotificationActions;
    }

    public boolean isAddingCondition() {
        return addingCondition;
    }

    public void setAddingCondition(boolean addingCondition) {
        log.trace("setAddingCondition(" + addingCondition + ")");
        this.addingCondition = addingCondition;
    }

    public int getDeletedCondition() {
        return deletedCondition;
    }

    public void setDeletedCondition(int deletedCondition) {
        log.trace("setDeletedCondition(" + deletedCondition + ")");
        this.deletedCondition = deletedCondition;
    }

    public void deleteCondition(int deletedCondition) {
        if (deletedCondition < conditions.size()) {
            conditions.remove(deletedCondition);
        }
    }

    public Collection getBaselines() {
        return baselines;
    }

    public void setBaselines(Collection baselines) {
        this.baselines = baselines;
    }

    public List<MeasurementDefinition> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<MeasurementDefinition> metrics) {
        this.metrics = metrics;
    }

    public List<MeasurementDefinition> getTraits() {
        return traits;
    }

    public void setTraits(List<MeasurementDefinition> traits) {
        this.traits = traits;
    }

    /**
     * @return Returns the alertnames.
     */
    public Collection<Map.Entry<String, Integer>> getAlertnames() {
        return alertnames;
    }

    /**
     * @param alertnames The alertnames to set.
     */
    public void setAlertnames(Collection<Map.Entry<String, Integer>> alertnames) {
        this.alertnames = alertnames;
    }

    public String[] getControlActionStatuses() {
        return controlActionStatuses;
    }

    public OptionItem[] getAvailabilityOptions() {
        return availabilityOptions;
    }

    public int[] getPriorities() {
        return priorities;
    }

    public int[] getTimeUnits() {
        return timeUnits;
    }

    public String[] getComparators() {
        return comparators;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        setDefaults();

        try {
            Subject subject = RequestUtils.getSubject(request);

            List<AlertDefinition> alertDefinitions = null;

            if (isAlertTemplate()) {
                AlertDefinitionManagerLocal alertDefinitionManager = LookupUtil.getAlertDefinitionManager();
                Integer resourceId = RequestUtils.getResourceId(request);

                alertDefinitions = alertDefinitionManager.getAlertDefinitions(subject, resourceId, PageControl
                    .getUnlimitedInstance());
            } else {
                AlertTemplateManagerLocal alertTemplateManager = LookupUtil.getAlertTemplateManager();
                Integer resourceTypeId = RequestUtils.getResourceTypeId(request);

                alertDefinitions = alertTemplateManager.getAlertTemplates(subject, resourceTypeId, PageControl
                    .getUnlimitedInstance());
            }

            Map<String, Integer> alertDefinitionNameToIdMap = new HashMap<String, Integer>(alertDefinitions.size());
            for (AlertDefinition alertDefinition : alertDefinitions) {
                alertDefinitionNameToIdMap.put(alertDefinition.getName(), alertDefinition.getId());
            }

            setAlertnames(alertDefinitionNameToIdMap.entrySet());
        } catch (Exception e) {
            setAlertnames(new ArrayList<Map.Entry<String, Integer>>(0));
        }

        Map<String, String> expressionsMap = new HashMap<String, String>();
        expressionsMap.put(BooleanExpression.ALL.toString(), BooleanExpression.ALL.name());
        expressionsMap.put(BooleanExpression.ANY.toString(), BooleanExpression.ANY.name());
        setConditionExpressionNames(expressionsMap.entrySet());
    }

    /**
     * Import basic properties from The AlertDefinitionValue to this form.
     *
     * @param alertDef
     */
    public void importProperties(AlertDefinition alertDef) {
        this.setAd(alertDef.getId());
        this.setName(alertDef.getName());
        this.setDescription(alertDef.getDescription());
        this.setActive(alertDef.getEnabled());
        this.setPriority(alertDef.getPriority().ordinal());

        // no point is getting this here, because it's edited on the conditions tiles
        //this.setConditionExpression( alertDef.getConditionExpression().name() );
    }

    /**
     * Export basic properties from this form to the AlertDefinitionValue.
     *
     * @param alertDef
     */
    public void exportProperties(AlertDefinition alertDef) {
        //alertDef.setId( this.getId() );
        alertDef.setName(this.getName());
        alertDef.setDescription(this.getDescription());
        alertDef.setEnabled(this.isActive());
        alertDef.setPriority(AlertPriority.values()[this.getPriority()]);

        // no point is setting this here, because it's edited on the conditions tiles
        //alertDef.setConditionExpression( BooleanExpression.valueOf( this.getConditionExpression() ) );
    }

    /**
     * Import the conditions and enablement properties from the AlertDefinitionValue to this form.
     */
    public void importConditionsEnablement(AlertDefinition alertDef, Subject subject) throws Exception {
        // we import the id here, too, so that the update will work
        this.setAd(alertDef.getId());

        this.setConditionExpression(alertDef.getConditionExpression().name());

        boolean isTypeAlert = EventConstants.TYPE_ALERT_DEF_ID.equals(alertDef.getParentId());

        /*
         * conditions
         */
        Set<AlertCondition> conds = alertDef.getConditions();
        setNumConditions(conds.size());
        int i = 0;
        for (AlertCondition cond : conds) {
            ConditionBean condBean = conditions.get(i++);
            condBean.importProperties(cond, isTypeAlert, subject);
        }

        /*
         * recovery
         */
        recoverId = alertDef.getRecoveryId();
        disableForRecovery = alertDef.getWillRecover();
        filteringControlActions = alertDef.getControlFiltered();
        filteringNotificationActions = alertDef.getNotifyFiltered();

        /*
         * dampening
         */
        AlertDampening alertDampening = alertDef.getAlertDampening();
        AlertDampening.Category category = alertDampening.getCategory();

        if (AlertDampening.Category.CONSECUTIVE_COUNT == category) {
            consecutiveCountValue = String.valueOf(alertDampening.getValue());
        } else if (AlertDampening.Category.PARTIAL_COUNT == category) {
            partialCountPeriod = String.valueOf(alertDampening.getPeriod());
            partialCountValue = String.valueOf(alertDampening.getValue());
        } else if (AlertDampening.Category.INVERSE_COUNT == category) {
            inverseCountValue = String.valueOf(alertDampening.getValue());
        } else if (AlertDampening.Category.DURATION_COUNT == category) {
            durationCountValue = String.valueOf(alertDampening.getValue());
            durationCountPeriod = String.valueOf(alertDampening.getPeriod());
            durationCountPeriodUnits = alertDampening.getPeriodUnits().ordinal() + 1;
        }

        whenEnabled = alertDampening.getCategory().ordinal();
    }

    /**
     * Export the conditions and enablement properties from this form to the specified alert def.
     */
    public void exportConditionsEnablement(AlertDefinition alertDef, HttpServletRequest request, Subject subject,
        boolean typeAlert) throws Exception {
        alertDef.setConditionExpression(BooleanExpression.valueOf(this.getConditionExpression()));

        /*
         * conditions
         */
        log.debug("Exporting " + this.getNumConditions() + " conditions...");
        alertDef.removeAllConditions();
        for (int i = 0; i < this.getNumConditions(); ++i) {
            ConditionBean condBean = this.getCondition(i);
            AlertCondition newCondition = condBean.exportProperties(request, subject, typeAlert);
            alertDef.addCondition(newCondition);
        }

        /*
         * recovery
         */
        alertDef.setRecoveryId(this.getRecoverId());
        alertDef.setWillRecover(this.isDisableForRecovery());
        alertDef.setNotifyFiltered(this.isFilteringNotificationActions());
        alertDef.setControlFiltered(this.isFilteringControlActions());

        /*
         * dampening
         */
        AlertDampening alertDampening = new AlertDampening(AlertDampening.Category.values()[this.getWhenEnabled()]);
        alertDef.setAlertDampening(alertDampening);

        AlertDampening.Category category = alertDampening.getCategory();
        if (category == AlertDampening.Category.CONSECUTIVE_COUNT) {
            alertDampening.setValue(Integer.valueOf(this.getConsecutiveCountValue()));
        } else if (category == AlertDampening.Category.PARTIAL_COUNT) {
            alertDampening.setValue(Integer.valueOf(this.getPartialCountValue()));
            alertDampening.setPeriod(Integer.valueOf(this.getPartialCountPeriod()));
        } else if (category == AlertDampening.Category.INVERSE_COUNT) {
            alertDampening.setValue(Integer.valueOf(this.getInverseCountValue()));
        } else if (alertDampening.getCategory() == AlertDampening.Category.DURATION_COUNT) {
            alertDampening.setValue(Integer.valueOf(this.getDurationCountValue()));
            alertDampening.setPeriod(Integer.valueOf(this.getDurationCountPeriod()));

            alertDampening.setPeriodUnits(AlertDampening.TimeUnits.values()[this.getDurationCountPeriodUnits() - 1]);
        }
    }

    public void resetConditions() {
        conditions = new ArrayList<ConditionBean>();
        setNumConditions(1);
    }

    @Override
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        // don't validate if we are preparing the form ...
        if (!shouldValidate(mapping, request)) {
            return null;
        }

        ActionErrors errs = super.validate(mapping, request);
        if (null == errs) {
            errs = new ActionErrors();
        }

        // only do this advanced validation if we are editing
        // conditions or creating a new definition
        if (mapping.getName().equals("NewAlertDefinitionForm")
            || mapping.getName().equals("EditAlertDefinitionConditionsForm")) {
            for (int i = 0; i < getNumConditions(); ++i) {
                ConditionBean cond = getCondition(i);
                cond.validate(request, errs, i);
            }

            AlertDampening.Category selectedCategory = AlertDampening.Category.values()[getWhenEnabled()];
            if (selectedCategory == AlertDampening.Category.CONSECUTIVE_COUNT) {
                validatePositive(getConsecutiveCountValue(), "consecutiveCountValue", errs);
            } else if (selectedCategory == AlertDampening.Category.PARTIAL_COUNT) {
                // using a boolean expression will shortcut the checking to the first failure
                if (validatePositive(getPartialCountValue(), "partialCountValue", errs)
                    && validatePositive(getPartialCountPeriod(), "partialCountPeriod", errs)
                    && (Integer.parseInt(getPartialCountValue()) > Integer.parseInt(getPartialCountPeriod()))) {
                    errs.add("partialCountPeriod", new ActionMessage("alert.config.error.PartialCountRangeTooSmall"));
                }
            } else if (selectedCategory == AlertDampening.Category.INVERSE_COUNT) {
                validatePositive(getInverseCountValue(), "inverseCountValue", errs);
            } else if (selectedCategory == AlertDampening.Category.DURATION_COUNT) {
                // using a boolean expression will shortcut the checking to the first failure
                boolean valid = validatePositive(getDurationCountValue(), "durationCountValue", errs)
                    && validatePositive(getDurationCountPeriod(), "durationCountPeriod", errs);
            }
        }

        return errs;
    }

    private boolean validatePositive(String field, String fieldName, ActionErrors errs) {
        if (GenericValidator.isBlankOrNull(field)) {
            errs.add(fieldName, new ActionMessage("alert.config.error.DampeningFieldRequired"));
            return false;
        }

        if ((GenericValidator.isInt(field) == false) || (Integer.parseInt(field) < 1)) {
            errs.add(fieldName, new ActionMessage("alert.config.error.InvalidDampeningField"));
            return false;
        }

        return true;
    }

    private void setDefaults() {
        ad = null;
        name = null;
        description = null;
        priority = AlertPriority.MEDIUM.ordinal();
        active = true;
        resetConditions();
        conditionExpression = BooleanExpression.ALL.name();
        whenEnabled = AlertDampening.Category.NONE.ordinal();
        consecutiveCountValue = null;
        partialCountValue = null;
        partialCountPeriod = null;
        inverseCountValue = null;
        durationCountValue = null;
        durationCountPeriod = null;
        durationCountPeriodUnits = AlertDampening.TimeUnits.MINUTES.ordinal() + 1;
        filteringControlActions = false;
        filteringNotificationActions = false;
        addingCondition = false;
        deletedCondition = Constants.ALERT_CONDITION_NONE_DELETED;
        baselines = null;
        cascade = false;
    }

    public void setAvailabilityActions(List<OptionItem> availabilityActions) {
        // TODO Auto-generated method stub
    }

    public String getConditionExpression() {
        return conditionExpression;
    }

    public void setConditionExpression(String conditionExpression) {
        this.conditionExpression = conditionExpression;
    }

    public Collection<Map.Entry<String, String>> getConditionExpressionNames() {
        return conditionExpressionNames;
    }

    public void setConditionExpressionNames(Collection<Map.Entry<String, String>> conditionExpressionNames) {
        this.conditionExpressionNames = conditionExpressionNames;
    }

    public String getConsecutiveCountValue() {
        return consecutiveCountValue;
    }

    public void setConsecutiveCountValue(String consecutiveTimes) {
        this.consecutiveCountValue = consecutiveTimes;
    }

    public String getInverseCountValue() {
        return inverseCountValue;
    }

    public void setInverseCountValue(String inverseCountValue) {
        this.inverseCountValue = inverseCountValue;
    }

    public boolean isAlertTemplate() {
        return ((getType() != null) && (getType() != 0));
    }

    public boolean isCascade() {
        return cascade;
    }

    public void setCascade(boolean cascade) {
        this.cascade = cascade;
    }
}