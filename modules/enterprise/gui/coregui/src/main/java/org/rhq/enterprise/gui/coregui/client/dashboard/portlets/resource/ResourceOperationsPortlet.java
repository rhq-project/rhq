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
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.SelectItem;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.measurement.CustomConfigMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent.Constant;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.groups.GroupOperationsPortlet;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.history.AbstractOperationHistoryListView;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.summary.AbstractActivityView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.history.ResourceOperationHistoryDataSource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.history.ResourceOperationHistoryDetailsView;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Simeon Pinder
 */
public class ResourceOperationsPortlet extends GroupOperationsPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "ResourceOperations";
    // A default displayed, persisted name for the portlet
    public static final String NAME = MSG.view_portlet_defaultName_resource_operations();

    private int resourceId;
    private ResourceComposite resourceComposite;
    private ResourceOperationsCriteriaHistoryListView resourceOperations;

    public ResourceOperationsPortlet(String locatorId) {
        super(locatorId);
        this.locatorId = locatorId;
        //figure out which page we're loading
        String currentPage = History.getToken();
        String[] elements = currentPage.split("/");
        this.resourceId = Integer.valueOf(elements[1]);
        //populate basepath
        baseViewPath = elements[0];
    }

    @Override
    protected void onInit() {
        //        super.onInit();
        initializeUi();
        loadData();
    }

    @Override
    public void redraw() {
        loadData();
    }

    @Override
    public DynamicForm getCustomSettingsForm() {
        LocatableDynamicForm customSettings = new LocatableDynamicForm(extendLocatorId("customSettings"));
        LocatableVLayout page = new LocatableVLayout(customSettings.extendLocatorId("page"));
        //build editor form container
        final LocatableDynamicForm form = new LocatableDynamicForm(page.extendLocatorId("alert-filter"));
        form.setMargin(5);

        //add label about what configuration affects? redundant?
        //add filter operation status type selector
        final SelectItem operationStatusSelector = PortletConfigurationEditorComponent
            .getOperationStatusEditor(portletConfig);
        //        //add sort priority selector
        //        final SelectItem resultSortSelector = PortletConfigurationEditorComponent
        //            .getResulSortOrderEditor(portletConfig);
        //add result count selector
        final SelectItem resultCountSelector = PortletConfigurationEditorComponent.getResultCountEditor(portletConfig);

        //add range selector
        final CustomConfigMeasurementRangeEditor measurementRangeEditor = PortletConfigurationEditorComponent
            .getMeasurementRangeEditor(portletConfig);

        form.setItems(operationStatusSelector, resultCountSelector);

        //submit handler
        customSettings.addSubmitValuesHandler(new SubmitValuesHandler() {

            @Override
            public void onSubmitValues(SubmitValuesEvent event) {

                //result count
                portletConfig = AbstractActivityView.saveResultCounterSettings(resultCountSelector, portletConfig);

                //time range configuration
                portletConfig = AbstractActivityView.saveMeasurementRangeEditorSettings(measurementRangeEditor,
                    portletConfig);

                //operation priority
                portletConfig = AbstractActivityView.saveOperationStatusSelectorSettings(operationStatusSelector,
                    portletConfig);

                //persist and reload portlet
                storedPortlet.setConfiguration(portletConfig);
                configure(portletWindow, storedPortlet);
                //resynch the config object in the datasource
                resourceOperations.setDatasource(new ResourceOperationsCriteriaDataSource(portletConfig));
                //apply latest settings to the visible result set
                redraw();
            }
        });
        form.markForRedraw();
        page.addMember(measurementRangeEditor);
        page.addMember(form);
        customSettings.addChild(page);
        return customSettings;
    }

    private void loadData() {
        //populate composite data
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterId(this.resourceId);

        criteria.fetchOperationHistories(false);

        //locate the resource
        GWTServiceLookup.getResourceService().findResourceCompositesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceComposite>>() {
                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving resource composite for resource [" + resourceId + "]:"
                        + caught.getMessage());
                }

                @Override
                public void onSuccess(PageList<ResourceComposite> results) {
                    if (!results.isEmpty()) {
                        resourceComposite = results.get(0);
                        //instantiate view
                        //populated GWT criteria objects
                        Criteria criteria = new Criteria(ResourceOperationHistoryDataSource.CriteriaField.RESOURCE_ID,
                            String.valueOf(resourceComposite.getResource().getId()));

                        resourceOperations = new ResourceOperationsCriteriaHistoryListView(locatorId,
                            new ResourceOperationsCriteriaDataSource(portletConfig), null, criteria, resourceComposite);

                        //cleanup
                        for (Canvas child : recentOperationsContent.getChildren()) {
                            child.destroy();
                        }
                        recentOperationsContent.addChild(resourceOperations);
                        recentOperationsContent.markForRedraw();
                    }
                }
            });
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(String locatorId) {
            return new ResourceOperationsPortlet(locatorId);
        }
    }
}

/** Provide implementation of ResourceOperationsHistoryListView using datasource
 *  that customizes fetch based on Configuration parameters.
 *
 * @author spinder
 */
class ResourceOperationsCriteriaHistoryListView extends
    AbstractOperationHistoryListView<ResourceOperationsCriteriaDataSource> {

    private ResourceOperationsCriteriaDataSource datasource;

    public ResourceOperationsCriteriaHistoryListView(String locatorId, ResourceOperationsCriteriaDataSource dataSource,
        String title, Criteria criteria, ResourceComposite composite) {
        super(locatorId, dataSource, title, criteria);
        this.datasource = dataSource;
        this.resourceComposite = composite;
        setShowFooterRefresh(false); //disable footer refresh
    }

    private ResourceComposite resourceComposite;

    @Override
    protected boolean hasControlPermission() {
        return this.resourceComposite.getResourcePermission().isControl();
    }

    @Override
    public Canvas getDetailsView(int id) {
        return new ResourceOperationHistoryDetailsView(extendLocatorId("DetailsView"), this.resourceComposite);
    }

    public ResourceOperationsCriteriaDataSource getDatasource() {
        return datasource;
    }

    public void setDatasource(ResourceOperationsCriteriaDataSource datasource) {
        this.datasource = datasource;
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

    @Override
    protected String getBasePath() {
        return "Resource/" + resourceComposite.getResource().getId() + "/Operations/History";
    }
}

/** Provide implementation of ResourceOperationHistoryDataSource that dynamically
 *  configures fetch requests for this table view.
 *
 * @author spinder
 */
class ResourceOperationsCriteriaDataSource extends ResourceOperationHistoryDataSource {

    public ResourceOperationsCriteriaDataSource(Configuration portletConfig) {
        this.portletConfig = portletConfig;
    }

    private Configuration portletConfig;

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response,
        final ResourceOperationHistoryCriteria criteria) {
        operationService.findResourceOperationHistoriesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceOperationHistory>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.dataSource_operationHistory_error_fetchFailure(), caught);
                }

                public void onSuccess(PageList<ResourceOperationHistory> result) {
                    response.setData(buildRecords(result));
                    processResponse(request.getRequestId(), response);
                }
            });
    }

    @Override
    protected ResourceOperationHistoryCriteria getFetchCriteria(final DSRequest request) {
        ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();

        if (request.getCriteria().getValues().containsKey(CriteriaField.RESOURCE_ID)) {
            int resourceId = Integer
                .parseInt((String) request.getCriteria().getValues().get(CriteriaField.RESOURCE_ID));
            Integer[] resourceIds = new Integer[1];
            resourceIds[0] = resourceId;
            criteria.addFilterResourceIds(resourceIds);
        }

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
            property = portletConfig.getSimple(Constant.OPERATION_STATUS);
            if (property != null) {
                String currentSetting = property.getStringValue();
                String[] parsedValues = currentSetting.trim().split(",");
                if (currentSetting.trim().isEmpty() || parsedValues.length == OperationRequestStatus.values().length) {
                    //all operation stati assumed
                } else {
                    OperationRequestStatus[] operationStati = new OperationRequestStatus[parsedValues.length];
                    int indx = 0;
                    for (String priority : parsedValues) {
                        OperationRequestStatus s = OperationRequestStatus.valueOf(priority);
                        operationStati[indx++] = s;
                    }
                    criteria.addFilterStatuses(operationStati);
                }
            }
        }
        return criteria;
    }
}