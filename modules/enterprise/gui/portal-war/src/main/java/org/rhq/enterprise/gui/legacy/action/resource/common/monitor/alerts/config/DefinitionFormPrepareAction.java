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
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.apache.struts.util.LabelValueBean;

import org.rhq.core.domain.alert.AlertDampening;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceFacets;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.action.resource.ResourceForm.FormContext;
import org.rhq.enterprise.gui.legacy.beans.OptionItem;
import org.rhq.enterprise.gui.legacy.beans.RelatedOptionBean;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.legacy.measurement.MeasurementConstants;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.measurement.util.MeasurementFormatter;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Prepare the alert definition form for new / edit.
 */
public abstract class DefinitionFormPrepareAction extends TilesAction {
    protected Log log = LogFactory.getLog(DefinitionFormPrepareAction.class);

    /**
     * Prepare the form for a new alert definition.
     */
    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        DefinitionForm defForm = (DefinitionForm) form;
        setupForm(defForm, request);

        if (!defForm.isOkClicked()) {
            // setting up form for the first time
            setupConditions(request, defForm);
        }

        // respond to an add or remove click- we do this here
        // rather than in the edit Action because in between these
        // two actions, struts repopulates the form bean and
        // resets numConditions to whatever value was submitted in
        // the request.
        if (defForm.isAddClicked()) {
            defForm.setNumConditions(defForm.getNumConditions() + 1);
        } else if (defForm.isRemoveClicked()) {
            int ri = Integer.parseInt(defForm.getRemove().getX());
            log.trace("deleting condition # " + ri);
            defForm.deleteCondition(ri);
        }

        log.trace("defForm.numConditions=" + defForm.getNumConditions());
        request.setAttribute("numConditions", defForm.getNumConditions());

        // process data types from bottom to top and set the initialise
        // the top-most "trigger" radio button
        boolean triggerNotSet = defForm.getCondition(0).getTrigger() == null;

        ResourceType type = null;
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        FormContext formContext = defForm.getContext();
        if (formContext == FormContext.Type) {
            type = LookupUtil.getResourceTypeManager().getResourceTypeById(overlord, defForm.getType());
        } else if (formContext == FormContext.Resource) {
            type = LookupUtil.getResourceManager().getResourceById(overlord, defForm.getId()).getResourceType();
        } else if (formContext == FormContext.Group) {
            type = LookupUtil.getResourceGroupManager().getResourceGroup(overlord, defForm.getGroupId())
                .getResourceType();
        } else {
            throw new IllegalArgumentException("Unsupported context: " + formContext);
        }
        ResourceFacets facets = LookupUtil.getResourceTypeManager().getResourceFacets(type.getId());
        request.setAttribute("showEvents", facets.isEvent());
        if (facets.isEvent() && triggerNotSet)
            defForm.getCondition(0).setTrigger("onEvent");

        request.setAttribute("showOperations", defForm.getControlActions().size() > 0);
        if (defForm.getControlActions().size() > 0 && triggerNotSet)
            defForm.getCondition(0).setTrigger("onOperation");

        request.setAttribute("showAvailability", defForm.getAvailabilityOptions().length > 0);
        if (defForm.getAvailabilityOptions().length > 0 && triggerNotSet)
            defForm.getCondition(0).setTrigger("onAvailability");

        request.setAttribute("showResourceConfiguration", defForm.isResourceConfigurationSupported());
        if (defForm.isResourceConfigurationSupported() && triggerNotSet)
            defForm.getCondition(0).setTrigger("onResourceConfiguration");

        request.setAttribute("showTraits", defForm.getTraits().size() > 0);
        if (defForm.getTraits().size() > 0 && triggerNotSet)
            defForm.getCondition(0).setTrigger("onTrait");

        boolean isExperimental = LookupUtil.getSystemManager().isExperimentalFeaturesEnabled();
        request.setAttribute("showCalltimeMetrics", isExperimental && defForm.getCalltimeMetrics().size() > 0);
        if (defForm.getCalltimeMetrics().size() > 0 && triggerNotSet)
            defForm.getCondition(0).setTrigger("onCallTime");

        request.setAttribute("showMetrics", defForm.getMetrics().size() > 0);
        if (defForm.getMetrics().size() > 0 && triggerNotSet)
            defForm.getCondition(0).setTrigger("onMeasurement");

        return null;
    }

    @SuppressWarnings("deprecation")
    protected void setupForm(DefinitionForm defForm, HttpServletRequest request) throws Exception {
        request.setAttribute("dampenNone", AlertDampening.Category.NONE.ordinal());
        request.setAttribute("dampenConsecutiveCount", AlertDampening.Category.CONSECUTIVE_COUNT.ordinal());
        request.setAttribute("dampenPartialCount", AlertDampening.Category.PARTIAL_COUNT.ordinal());
        request.setAttribute("dampenInverseCount", AlertDampening.Category.INVERSE_COUNT.ordinal());
        request.setAttribute("dampenDurationCount", AlertDampening.Category.DURATION_COUNT.ordinal());

        request.setAttribute("noneDeleted", Constants.ALERT_CONDITION_NONE_DELETED);

        MeasurementScheduleManagerLocal scheduleManager = LookupUtil.getMeasurementScheduleManager();
        MeasurementDefinitionManagerLocal definitionManager = LookupUtil.getMeasurementDefinitionManager();
        ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

        Subject subject = RequestUtils.getSubject(request);
        Resource resource = RequestUtils.getResource(request);
        ResourceGroup group = RequestUtils.getResourceGroupIfExists(request);

        ResourceType type = null;
        if (resource != null) {
            // resource alert definition
            type = resource.getResourceType();
        } else if (group != null) {
            // group alert definition
            type = group.getResourceType();
        } else {
            // template alert definition
            type = RequestUtils.getResourceType(request);
        }

        defForm.setResourceType(type.getId());

        List<MeasurementDefinition> dataDefinitions = definitionManager.findMeasurementDefinitionsByResourceType(
            subject, type.getId(), DataType.MEASUREMENT, null);

        List<MeasurementDefinition> calltimeDefinitions = definitionManager.findMeasurementDefinitionsByResourceType(
            subject, type.getId(), DataType.CALLTIME, null);

        List<MeasurementDefinition> traitDefinitions = definitionManager.findMeasurementDefinitionsByResourceType(
            subject, type.getId(), DataType.TRAIT, null);

        List<RelatedOptionBean> baselines = new ArrayList<RelatedOptionBean>();

        if (resource != null) {
            for (MeasurementDefinition definition : dataDefinitions) {
                MeasurementSchedule schedule = scheduleManager.getSchedule(subject, resource.getId(), definition
                    .getId(), false);

                RelatedOptionBean rob = new RelatedOptionBean(definition.getName(), String.valueOf(definition.getId()),
                    getBaselineList(schedule));
                baselines.add(rob);

                setDisabledName(schedule, definition);
            }

            for (MeasurementDefinition definition : traitDefinitions) {
                MeasurementSchedule schedule = scheduleManager.getSchedule(subject, resource.getId(), definition
                    .getId(), false);
                setDisabledName(schedule, definition);
            }
        } else {
            List<LabelValueBean> defaultBaselineLabels = getBaselineList(null);
            for (MeasurementDefinition definition : dataDefinitions) {
                RelatedOptionBean rob = new RelatedOptionBean(definition.getName(), String.valueOf(definition.getId()),
                    defaultBaselineLabels);
                baselines.add(rob);
            }
        }

        defForm.setMetrics(dataDefinitions);
        defForm.setCalltimeMetrics(calltimeDefinitions);
        defForm.setTraits(traitDefinitions);

        defForm.setBaselines(baselines);
        request.setAttribute("baselines", baselines); // need to duplicate this for the JavaScript on the page

        List<OptionItem> controlActions = new ArrayList<OptionItem>();
        OperationManagerLocal operationManager = LookupUtil.getOperationManager();
        // do not need to eagerly load the definitions because only name and displayName are needed
        for (OperationDefinition action : operationManager.findSupportedResourceTypeOperations(subject, type.getId(),
            false)) {
            OptionItem actionItem = new OptionItem(action.getDisplayName(), action.getName());
            controlActions.add(actionItem);
        }

        defForm.setControlActions(controlActions);

        ConfigurationDefinition configurationDefinition = configurationManager
            .getResourceConfigurationDefinitionForResourceType(subject, type.getId());
        defForm.setResourceConfigurationSupported(configurationDefinition != null);
    }

    private void setDisabledName(MeasurementSchedule schedule, MeasurementDefinition definition) {
        if (schedule == null) {
            return;
        }
        if (schedule.isEnabled() == false) {
            String definitionName = definition.getDisplayName();
            definition.setDisplayName(definitionName + " (disabled)");
        }
    }

    protected abstract void setupConditions(HttpServletRequest request, DefinitionForm defForm) throws Exception;

    @SuppressWarnings("deprecation")
    private List<LabelValueBean> getBaselineList(MeasurementSchedule schedule) {
        List<LabelValueBean> list = new ArrayList<LabelValueBean>(3);
        String minLabel = MeasurementFormatter.getBaselineText(MeasurementConstants.BASELINE_OPT_MIN, schedule);
        list.add(new LabelValueBean(minLabel, MeasurementConstants.BASELINE_OPT_MIN));

        String meanLabel = MeasurementFormatter.getBaselineText(MeasurementConstants.BASELINE_OPT_MEAN, schedule);
        list.add(new LabelValueBean(meanLabel, MeasurementConstants.BASELINE_OPT_MEAN));

        String maxLabel = MeasurementFormatter.getBaselineText(MeasurementConstants.BASELINE_OPT_MAX, schedule);
        list.add(new LabelValueBean(maxLabel, MeasurementConstants.BASELINE_OPT_MAX));

        return list;
    }
}
