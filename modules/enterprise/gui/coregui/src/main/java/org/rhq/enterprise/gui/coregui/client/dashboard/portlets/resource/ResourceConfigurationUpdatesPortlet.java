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
package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.resource;

import java.util.ArrayList;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.criteria.ResourceConfigurationUpdateCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent.Constant;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.groups.GroupConfigurationUpdatesPortlet;
import org.rhq.enterprise.gui.coregui.client.gwt.ConfigurationGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration.ConfigurationHistoryDataSource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration.ConfigurationHistoryView;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**This portlet allows the end user to customize the Package History display
 *
 * @author Simeon Pinder
 */
public class ResourceConfigurationUpdatesPortlet extends GroupConfigurationUpdatesPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "ResourceConfigurationUpdates";
    // A default displayed, persisted name for the portlet
    public static final String NAME = MSG.view_portlet_defaultName_resource_config_updates();

    private int resourceId = -1;
    private ResourceComposite resourceComposite;

    private ResourceConfigurationHistoryCriteriaView resourceHistoryTable;

    public ResourceConfigurationUpdatesPortlet(String locatorId, int resourceId) {
        super(locatorId);
        this.resourceId = resourceId;
        //figure out which page we're loading
        String currentPage = History.getToken();
        String[] elements = currentPage.split("/");
        baseViewPath = elements[0];
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        /* (non-Javadoc)
         * TODO:  This factory ASSUMES the user is currently navigated to a group detail view, and generates a portlet
         *        for that group.  It will fail in other scenarios.  This mechanism should be improved such that the
         *        factory method can take an EntityContext explicitly indicating, in this case, the group.
         * @see org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory#getInstance(java.lang.String)
         */
        public final Portlet getInstance(String locatorId) {
            //figure out which page we're loading
            String currentPage = History.getToken();
            String[] elements = currentPage.split("/");
            int currentResourceIdentifier = Integer.valueOf(elements[1]);
            int resourceId = currentResourceIdentifier;

            return new ResourceConfigurationUpdatesPortlet(locatorId, resourceId);
        }
    }

    @Override
    protected void loadData() {
        //populate composite data
        //locate resourceRef
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterId(this.resourceId);
        criteria.fetchResourceConfigurationUpdates(false);
        criteria.fetchOperationHistories(false);

        //locate the resource
        GWTServiceLookup.getResourceService().findResourceCompositesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceComposite>>() {
                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving resource composite for resource [" + resourceId + "]:"
                        + caught.getMessage());
                    currentlyLoading = false;
                }

                @Override
                public void onSuccess(PageList<ResourceComposite> results) {
                    if (!results.isEmpty()) {
                        resourceComposite = results.get(0);
                        //instantiate view

                        PageControl lastFive = new PageControl(0, 5);
                        ResourceConfigurationUpdateCriteria criteria = new ResourceConfigurationUpdateCriteria();
                        criteria.setPageControl(lastFive);
                        //TODO: spinder: move this up into the pageControl.
                        criteria.addSortStatus(PageOrdering.DESC);
                        criteria.addFilterResourceIds(resourceId);

                        resourceHistoryTable = new ResourceConfigurationHistoryCriteriaView(extendLocatorId("Table"),
                            resourceComposite.getResourcePermission().isConfigureWrite(), resourceId);
                    } else {
                        resourceHistoryTable = new ResourceConfigurationHistoryCriteriaView(extendLocatorId("Table"),
                            resourceComposite.getResourcePermission().isConfigureWrite(), -1);
                    }
                    resourceHistoryTable.setDataSource(new ConfigurationUdpatesCriteriaDataSource(portletConfig));
                    resourceHistoryTable.setShowHeader(false);
                    resourceHistoryTable.setShowFooterRefresh(false);

                    //cleanup
                    for (Canvas child : recentConfigurationContent.getChildren()) {
                        child.destroy();
                    }
                    recentConfigurationContent.addChild(resourceHistoryTable);
                    recentConfigurationContent.markForRedraw();
                    currentlyLoading = false;
                }
            });
    }

    class ResourceConfigurationHistoryCriteriaView extends ConfigurationHistoryView {

        public ResourceConfigurationHistoryCriteriaView(String locatorId, boolean hasWritePerm, int resourceId) {
            super(locatorId, hasWritePerm, resourceId);
        }

        @Override
        protected void refreshTableInfo() {
            super.refreshTableInfo();
            if (getTableInfo() != null) {
                int count = getListGrid().getSelection().length;
                getTableInfo().setContents(
                    MSG.view_table_matchingRows(String.valueOf(getListGrid().getTotalRows()), String.valueOf(count)));
            }
        }

        /**
         * Override if you don't want the detailsLinkColumn to have the default link wrapper.
         * @return the desired CellFormatter.
         */
        protected CellFormatter getDetailsLinkColumnCellFormatter() {
            return new CellFormatter() {
                public String format(Object value, ListGridRecord record, int i, int i1) {
                    Integer recordId = getId(record);
                    String detailsUrl = "#" + getBasePath() + "/" + recordId;
                    return SeleniumUtility.getLocatableHref(detailsUrl, value.toString(), null);
                }
            };
        }

        @Override
        protected String getBasePath() {
            return "Resource/" + resourceComposite.getResource().getId() + "/Configuration/History";
        }

        @Override
        protected void onInit() {
            super.onInit();
            getListGrid().setEmptyMessage(MSG.view_portlet_results_empty());
        }
    }

    class ConfigurationUdpatesCriteriaDataSource extends ConfigurationHistoryDataSource {

        public ConfigurationUdpatesCriteriaDataSource(Configuration portletConfig) {
            super();
            this.portletConfig = portletConfig;
        }

        private Configuration portletConfig;

        @Override
        protected void executeFetch(final DSRequest request, final DSResponse response,
            final ResourceConfigurationUpdateCriteria criteria) {
            ConfigurationGWTServiceAsync configurationService = GWTServiceLookup.getConfigurationService();
            configurationService.findResourceConfigurationUpdatesByCriteria(criteria,
                new AsyncCallback<PageList<ResourceConfigurationUpdate>>() {

                    @Override
                    public void onSuccess(PageList<ResourceConfigurationUpdate> result) {
                        response.setData(buildRecords(result));
                        //adjust for portlets that restrict result size
                        response.setTotalRows(result.size());
                        processResponse(request.getRequestId(), response);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_group_resConfig_table_failFetch(), caught);
                        response.setStatus(DSResponse.STATUS_FAILURE);
                        processResponse(request.getRequestId(), response);
                    }
                });
        }

        @Override
        protected ResourceConfigurationUpdateCriteria getFetchCriteria(final DSRequest request) {
            //initialize criteria
            ResourceConfigurationUpdateCriteria criteria = new ResourceConfigurationUpdateCriteria();
            criteria.addFilterResourceIds(resourceId);

            //initialize to only five for quick queries.
            PageControl pageControl = new PageControl(0, 5);//default to displaying five
            //customize query with latest configuration selections

            //retrieve previous settings from portlet config
            if (portletConfig != null) {
                //            //result sort order
                //            PropertySimple property = portletConfig.getSimple(Constant.RESULT_SORT_ORDER);
                //            if (property != null) {
                //                String currentSetting = property.getStringValue();
                //                if (currentSetting.trim().isEmpty() || currentSetting.equalsIgnoreCase(PageOrdering.DESC.name())) {
                //                    criteria.addSortStatus(PageOrdering.DESC);
                //                } else {
                //                    criteria.addSortStatus(PageOrdering.ASC);
                //                }
                //            }
                //result timeframe if enabled
                PropertySimple property = portletConfig.getSimple(Constant.METRIC_RANGE_ENABLE);
                if (Boolean.valueOf(property.getBooleanValue())) {//then proceed setting

                    boolean isAdvanced = false;
                    //detect type of widget[Simple|Advanced]
                    property = portletConfig.getSimple(Constant.METRIC_RANGE_BEGIN_END_FLAG);
                    if (property != null) {
                        isAdvanced = property.getBooleanValue();
                    }
                    if (isAdvanced) {
                        //Advanced time settings
                        property = portletConfig.getSimple(Constant.METRIC_RANGE);
                        if (property != null) {
                            String currentSetting = property.getStringValue();
                            String[] range = currentSetting.split(",");
                            criteria.addFilterStartTime(Long.valueOf(range[0]));
                            criteria.addFilterEndTime(Long.valueOf(range[1]));
                        }
                    } else {
                        //Simple time settings
                        property = portletConfig.getSimple(Constant.METRIC_RANGE_LASTN);
                        if (property != null) {
                            int lastN = property.getIntegerValue();
                            property = portletConfig.getSimple(Constant.METRIC_RANGE_UNIT);
                            int lastUnits = property.getIntegerValue();
                            ArrayList<Long> beginEnd = MeasurementUtility.calculateTimeFrame(lastN, Integer
                                .valueOf(lastUnits));
                            criteria.addFilterStartTime(Long.valueOf(beginEnd.get(0)));
                            criteria.addFilterEndTime(Long.valueOf(beginEnd.get(1)));
                        }
                    }
                }

                //result count
                property = portletConfig.getSimple(Constant.RESULT_COUNT);
                if (property != null) {
                    String currentSetting = property.getStringValue();
                    if (currentSetting.trim().isEmpty() || currentSetting.equalsIgnoreCase("5")) {
                        pageControl.setPageSize(5);
                    } else {
                        pageControl = new PageControl(0, Integer.valueOf(currentSetting));
                    }
                }
                criteria.setPageControl(pageControl);

                //detect operation status filter
                property = portletConfig.getSimple(Constant.CONFIG_UPDATE_STATUS);
                if (property != null) {
                    String currentSetting = property.getStringValue();
                    String[] parsedValues = currentSetting.trim().split(",");
                    if (currentSetting.trim().isEmpty()
                        || parsedValues.length == ConfigurationUpdateStatus.values().length) {
                        //all operation stati assumed
                    } else {
                        ConfigurationUpdateStatus[] updateStatus = new ConfigurationUpdateStatus[parsedValues.length];
                        int indx = 0;
                        for (String priority : parsedValues) {
                            ConfigurationUpdateStatus s = ConfigurationUpdateStatus.valueOf(priority);
                            updateStatus[indx++] = s;
                        }
                        criteria.addFilterStatuses(updateStatus);
                    }
                }
            }
            return criteria;
        }
    }
}