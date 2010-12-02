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
package org.rhq.enterprise.gui.subsystem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.composite.AlertDefinitionComposite;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.util.IntExtractor;
import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.enterprise.gui.common.converter.SelectItemUtils;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.ResourceNameDisambiguatingPagedListDataModel;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.AlertDefUtil;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.alert.AlertTemplateManagerLocal;
import org.rhq.enterprise.server.alert.GroupAlertDefinitionManagerLocal;
import org.rhq.enterprise.server.subsystem.AlertSubsystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Joseph Marques
 */
public class SubsystemAlertDefinitionUIBean extends SubsystemView {
    public static final String MANAGED_BEAN_NAME = "SubsystemAlertDefinitionUIBean";
    private static final String FORM_PREFIX = "alertDefinitionSubsystemForm:";
    private final String CALENDAR_SUFFIX = "InputDate";

    private AlertSubsystemManagerLocal manager = LookupUtil.getAlertSubsystemManager();
    private AlertDefinitionManagerLocal alertDefinitionManager = LookupUtil.getAlertDefinitionManager();
    private GroupAlertDefinitionManagerLocal groupAlertDefinitionManager = LookupUtil.getGroupAlertDefinitionManager();
    private AlertTemplateManagerLocal alertTemplateManager = LookupUtil.getAlertTemplateManager();

    private static String datePattern;
    private String resourceFilter;
    private String parentFilter;
    private Date dateBeginFilter;
    private Date dateEndFilter;
    private String categoryFilter;
    private SelectItem[] categoryFilterItems;

    private IntExtractor<AlertDefinitionComposite> RESOURCE_ID_EXTRACTOR = new IntExtractor<AlertDefinitionComposite>() {

        public int extract(AlertDefinitionComposite object) {
            Resource resource = object.getAlertDefinition().getResource();
            return resource == null ? 0 : resource.getId();
        }
    };

    public SubsystemAlertDefinitionUIBean() {
        datePattern = EnterpriseFacesContextUtility.getWebUser().getWebPreferences().getDateTimeDisplayPreferences()
            .getDateTimeFormatTrigger();
        categoryFilterItems = SelectItemUtils.convertFromEnum(AlertConditionCategory.class, true);
        categoryFilter = (String) categoryFilterItems[0].getValue();
    }

    public String getDatePattern() {
        return datePattern;
    }

    public String getResourceFilter() {
        return resourceFilter;
    }

    public void setResourceFilter(String resourceFilter) {
        this.resourceFilter = resourceFilter;
    }

    public String getParentFilter() {
        return parentFilter;
    }

    public void setParentFilter(String parentFilter) {
        this.parentFilter = parentFilter;
    }

    public Date getDateBeginFilter() {
        return dateBeginFilter;
    }

    public void setDateBeginFilter(Date dateSubmittedFilter) {
        this.dateBeginFilter = dateSubmittedFilter;
    }

    public Date getDateEndFilter() {
        return dateEndFilter;
    }

    public void setDateEndFilter(Date dateCompletedFilter) {
        this.dateEndFilter = dateCompletedFilter;
    }

    public String getCategoryFilter() {
        return categoryFilter;
    }

    public void setCategoryFilter(String statusFilter) {
        this.categoryFilter = statusFilter;
    }

    public SelectItem[] getCategoryFilterItems() {
        return categoryFilterItems;
    }

    public void setCategoryFilterItems(SelectItem[] statusFilterItems) {
        this.categoryFilterItems = statusFilterItems;
    }

    public String deleteSelected() {
        Integer[] selected = getSelectedItems();

        try {
            Subject subject = getSubject();

            List<Integer> resourceDefinitions = new ArrayList<Integer>();
            List<Integer> groupDefinitions = new ArrayList<Integer>();
            List<Integer> typeDefinitions = new ArrayList<Integer>();

            for (Integer definitionId : selected) {
                if (alertDefinitionManager.isTemplate(definitionId)) {
                    typeDefinitions.add(definitionId);
                } else if (alertDefinitionManager.isGroupAlertDefinition(definitionId)) {
                    groupDefinitions.add(definitionId);
                } else {
                    resourceDefinitions.add(definitionId);
                }
            }

            // delete resources first
            alertDefinitionManager.removeAlertDefinitions(subject, ArrayUtils.unwrapCollection(resourceDefinitions));

            // then delete templates and group alert defs, which are both tolerant of missing child definitions
            groupAlertDefinitionManager.removeGroupAlertDefinitions(subject,
                groupDefinitions.toArray(new Integer[groupDefinitions.size()]));
            alertTemplateManager.removeAlertTemplates(subject,
                typeDefinitions.toArray(new Integer[typeDefinitions.size()]));

            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Deleted selected alert definitions.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to delete selected alert definitions.",
                e);
        }

        return "success";
    }

    public String disableSelected() {
        Integer[] selected = getSelectedItems();

        try {
            Subject subject = getSubject();

            List<Integer> resourceDefinitions = new ArrayList<Integer>();
            List<Integer> groupDefinitions = new ArrayList<Integer>();
            List<Integer> typeDefinitions = new ArrayList<Integer>();

            for (Integer definitionId : selected) {
                if (alertDefinitionManager.isTemplate(definitionId)) {
                    typeDefinitions.add(definitionId);
                } else if (alertDefinitionManager.isGroupAlertDefinition(definitionId)) {
                    groupDefinitions.add(definitionId);
                } else {
                    resourceDefinitions.add(definitionId);
                }
            }

            // delete resources first
            alertDefinitionManager.disableAlertDefinitions(subject, ArrayUtils.unwrapCollection(resourceDefinitions));
            //resourceDefinitions.toArray(new int[resourceDefinitions.size()]));

            // then delete templates and group alert defs, which are both tolerant of missing child definitions
            groupAlertDefinitionManager.disableGroupAlertDefinitions(subject,
                groupDefinitions.toArray(new Integer[groupDefinitions.size()]));

            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Disable selected alert definitions.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "Failed to disable selected alert definitions.", e);
        }

        return "success";
    }

    public String enableSelected() {
        Integer[] selected = getSelectedItems();

        try {
            Subject subject = getSubject();

            List<Integer> resourceDefinitions = new ArrayList<Integer>();
            List<Integer> groupDefinitions = new ArrayList<Integer>();
            List<Integer> typeDefinitions = new ArrayList<Integer>();

            for (Integer definitionId : selected) {
                if (alertDefinitionManager.isTemplate(definitionId)) {
                    typeDefinitions.add(definitionId);
                } else if (alertDefinitionManager.isGroupAlertDefinition(definitionId)) {
                    groupDefinitions.add(definitionId);
                } else {
                    resourceDefinitions.add(definitionId);
                }
            }

            // delete resources first
            alertDefinitionManager.enableAlertDefinitions(subject, ArrayUtils.unwrapCollection(resourceDefinitions));

            // then delete templates and group alert defs, which are both tolerant of missing child definitions
            groupAlertDefinitionManager.enableGroupAlertDefinitions(subject,
                groupDefinitions.toArray(new Integer[groupDefinitions.size()]));

            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Enable selected alert definitions.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to enable selected alert definitions.",
                e);
        }

        return "success";
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ResultsDataModel(PageControlView.SubsystemAlertDefinition, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ResultsDataModel extends ResourceNameDisambiguatingPagedListDataModel<AlertDefinitionComposite> {
        public ResultsDataModel(PageControlView view, String beanName) {
            super(view, beanName, true);
        }

        @Override
        public PageList<AlertDefinitionComposite> fetchDataForPage(PageControl pc) {
            getDataFromRequest();

            String resourceFilter = getResourceFilter();
            String parentFilter = getParentFilter();
            Long startMillis = getDateBeginFilter() == null ? null : getDateBeginFilter().getTime();
            Long endMillis = getDateEndFilter() == null ? null : getDateEndFilter().getTime();
            String cleansedStatus = SelectItemUtils.cleanse(getCategoryFilter());
            AlertConditionCategory category = cleansedStatus == null ? null : AlertConditionCategory
                .valueOf(cleansedStatus);

            PageList<AlertDefinitionComposite> result;
            result = manager.getAlertDefinitions(getSubject(), resourceFilter, parentFilter, startMillis, endMillis,
                category, pc);

            // format UI-layer display column attribute values
            HttpServletRequest request = FacesContextUtility.getRequest();
            for (AlertDefinitionComposite history : result) {
                Set<AlertCondition> acs = history.getAlertDefinition().getConditions();
                if (acs.size() > 1) {
                    history.setConditionText("Multiple Conditions");
                } else if (acs.size() == 1) {
                    AlertCondition condition = acs.iterator().next();
                    String displayText = AlertDefUtil.formatAlertConditionForDisplay(condition, request);

                    history.setConditionText(displayText);
                } else {
                    history.setConditionText("No Conditions");
                }
            }
            return result;
        }

        protected IntExtractor<AlertDefinitionComposite> getResourceIdExtractor() {
            return RESOURCE_ID_EXTRACTOR;
        }

        private void getDataFromRequest() {
            SubsystemAlertDefinitionUIBean outer = SubsystemAlertDefinitionUIBean.this;
            outer.resourceFilter = FacesContextUtility.getOptionalRequestParameter(FORM_PREFIX + "resourceFilter");
            outer.parentFilter = FacesContextUtility.getOptionalRequestParameter(FORM_PREFIX + "parentFilter");
            outer.dateBeginFilter = getDate(FacesContextUtility.getOptionalRequestParameter(FORM_PREFIX
                + "dateBeginFilter" + CALENDAR_SUFFIX));
            outer.dateEndFilter = getDate(FacesContextUtility.getOptionalRequestParameter(FORM_PREFIX + "dateEndFilter"
                + CALENDAR_SUFFIX));
            outer.categoryFilter = FacesContextUtility.getOptionalRequestParameter(FORM_PREFIX + "categoryFilter");
        }

        private Date getDate(String dateAsString) {
            if (dateAsString == null || dateAsString.trim().equals("")) {
                return null;
            }
            try {
                String datePattern = getDatePattern();
                return new SimpleDateFormat(datePattern).parse(dateAsString);
            } catch (ParseException pe) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Could not parse '" + dateAsString
                    + "' using the following format '" + datePattern + "'");
            }
            return null;
        }
    }
}
