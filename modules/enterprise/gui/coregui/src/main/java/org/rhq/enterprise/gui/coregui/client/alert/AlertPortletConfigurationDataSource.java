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
package org.rhq.enterprise.gui.coregui.client.alert;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.rpc.RPCResponse;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent.Constant;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.alerts.RecentAlertsPortlet;

/** Customize the AlertDataSource to pull fetch criteria information from
 *  the Configuration object passed in.
 *
 * @author Simeon
 */
public class AlertPortletConfigurationDataSource extends AlertDataSource {
    //configuration attributes
    private Integer[] alertFilterResourceIds = {};
    private DashboardPortlet portlet = null;
    private Configuration configuration = null;
    private Integer groupId = null;
    private Integer[] resourceIds = null;
    private String alertResourcesToUse;
    private EntityContext entityContext;

    public AlertPortletConfigurationDataSource() {
        super();
    }

    public AlertPortletConfigurationDataSource(DashboardPortlet recentAlertsPortlet, Configuration configuration,
        Integer groupId, Integer[] resourceIds) {
        super();
        this.portlet = recentAlertsPortlet;
        this.configuration = configuration;
        this.groupId = groupId;
        this.resourceIds = resourceIds;
        if (groupId != null) {
            entityContext = EntityContext.forGroup(groupId);
        } else if ((resourceIds != null) && (resourceIds.length > 0)) {
            entityContext = EntityContext.forResource(resourceIds[0]);
        }
    }

    /** Override the executeFetch for AlertPortlet to allow specifying smaller than total
     *  result displays.
     */
    protected void executeFetch(final DSRequest request, final DSResponse response) {
        final long start = System.currentTimeMillis();
        AlertCriteria criteria = new AlertCriteria();
        //initialize to only five for quick queries.
        criteria.setPageControl(new PageControl(0, Integer
            .valueOf(PortletConfigurationEditorComponent.Constant.RESULT_COUNT_DEFAULT)));
        //retrieve previous settings from portlet config
        if ((portlet != null) && (this.portlet instanceof DashboardPortlet)) {
            Configuration portletConfig = configuration;
            //filter priority
            PropertySimple property = portletConfig.getSimple(Constant.ALERT_PRIORITY);
            if (property != null) {
                String currentSetting = property.getStringValue();
                String[] parsedValues = currentSetting.trim().split(",");
                if (currentSetting.trim().isEmpty() || parsedValues.length == 3) {
                    //all alert priorities assumed
                } else {
                    AlertPriority[] filterPriorities = new AlertPriority[parsedValues.length];
                    int indx = 0;
                    for (String priority : parsedValues) {
                        AlertPriority p = AlertPriority.valueOf(priority);
                        filterPriorities[indx++] = p;
                    }
                    criteria.addFilterPriorities(filterPriorities);
                }
            }
            PageControl pc = new PageControl();
            //result sort order
            property = portletConfig.getSimple(Constant.RESULT_SORT_ORDER);
            if (property != null) {
                String currentSetting = property.getStringValue();
                if (currentSetting.trim().isEmpty() || currentSetting.equalsIgnoreCase(PageOrdering.DESC.name())) {
                    criteria.addSortCtime(PageOrdering.DESC);
                    pc.setPrimarySortOrder(PageOrdering.DESC);
                } else {
                    criteria.addSortCtime(PageOrdering.ASC);
                    pc.setPrimarySortOrder(PageOrdering.ASC);
                }
            }
            //result timeframe if enabled
            property = portletConfig.getSimple(Constant.METRIC_RANGE_ENABLE);
            if (Boolean.valueOf(property.getBooleanValue())) {//then proceed setting
                property = portletConfig.getSimple(Constant.METRIC_RANGE);
                if (property != null) {
                    String currentSetting = property.getStringValue();
                    String[] range = currentSetting.split(",");
                    criteria.addFilterStartTime(Long.valueOf(range[0]));
                    criteria.addFilterEndTime(Long.valueOf(range[1]));
                }
            }

            //result count
            property = portletConfig.getSimple(Constant.RESULT_COUNT);
            if (property != null) {
                String currentSetting = property.getStringValue();
                if (currentSetting.trim().isEmpty() || currentSetting.equalsIgnoreCase("5")) {
                    PageControl pageControl = new PageControl(0, 5);
                    pc.setPageSize(5);
                } else {
                    PageControl pageControl = new PageControl(0, Integer.valueOf(currentSetting));
                    pc.setPageSize(Integer.valueOf(currentSetting));
                }
            }
            criteria.setPageControl(pc);
            if (groupId != null) {
                criteria.addFilterResourceGroupIds(groupId);
            }
            if ((resourceIds != null) && (resourceIds.length > 0)) {
                criteria.addFilterResourceIds(resourceIds);
            }
        }
        criteria.fetchAlertDefinition(true);
        criteria.fetchRecoveryAlertDefinition(true);

        getAlertService().findAlertsByCriteria(criteria, new AsyncCallback<PageList<Alert>>() {

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_alerts_loadFailed(), caught);
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(PageList<Alert> result) {
                long fetchTime = System.currentTimeMillis() - start;
                Log.info(result.size() + " alerts fetched in: " + fetchTime + "ms");
                if (entityContext.type != EntityContext.Type.Resource) {
                    dataRetrieved(result, response, request);
                } else {
                    response.setData(buildRecords(result));
                    response.setTotalRows(result.size());
                    processResponse(request.getRequestId(), response);
                }
            }
        });
    }

    public String getAlertResourcesToUse() {
        return alertResourcesToUse;
    }

    public void setAlertResourcesToUse(String resourcesToUse) {
        this.alertResourcesToUse = resourcesToUse;
    }

    public Integer[] getAlertFilterResourceIds() {
        return alertFilterResourceIds;
    }

    public void setAlertFilterResourceId(Integer[] alertFilterResourceId) {
        this.alertFilterResourceIds = alertFilterResourceId;
    }

    public Integer[] extractFilterResourceIds(DashboardPortlet storedPortlet, Integer[] filterResourceIds) {
        PropertyList propertyList = storedPortlet.getConfiguration().getList(
            RecentAlertsPortlet.ALERT_RANGE_RESOURCE_IDS);
        if ((propertyList != null) && (propertyList.getList() != null) && (!propertyList.getList().isEmpty())
            && (propertyList.getList().get(0) != null)) {
            Property container = propertyList.getList().get(0);
            if (container instanceof PropertyList) {
                PropertyList anotherList = (PropertyList) container;
                if (anotherList.getList() != null) {
                    filterResourceIds = new Integer[anotherList.getList().size()];
                    int index = 0;
                    for (Property p : anotherList.getList()) {
                        filterResourceIds[index++] = ((PropertySimple) p).getIntegerValue();
                    }
                }
            }
        }
        return filterResourceIds;
    }
}
