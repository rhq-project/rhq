package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.alerts;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.alert.AlertDataSource;
import org.rhq.enterprise.gui.coregui.client.alert.AlertHistoryView;
import org.rhq.enterprise.gui.coregui.client.components.measurement.CustomConfigMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.dashboard.AutoRefreshPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.AutoRefreshPortletUtil;
import org.rhq.enterprise.gui.coregui.client.dashboard.CustomSettingsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent.Constant;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * A base class for deriving recent alerts portlets for different entity contexts.  In this way the
 * basic plumbing is shared, giving a consistent behavior and configuration for the concrete portlets.
 *  
 * @author Jay Shaughnessy
 * @author Simeon Pinder 
 */
public abstract class AbstractRecentAlertsPortlet extends AlertHistoryView implements CustomSettingsPortlet,
    AutoRefreshPortlet {

    // set on initial configuration, the window for this portlet view. 
    private PortletWindow portletWindow;

    private AlertsPortletDataSource dataSource;

    // autorefresh timer
    private Timer refreshTimer;

    public AbstractRecentAlertsPortlet(String locatorId, EntityContext entityContext) {
        super(locatorId, entityContext);

        setShowFilterForm(false); //disable filter form for portlet
        setOverflow(Overflow.VISIBLE);
    }

    public Timer getRefreshTimer() {
        return refreshTimer;
    }

    public void setRefreshTimer(Timer refreshTimer) {
        this.refreshTimer = refreshTimer;
    }

    public PortletWindow getPortletWindow() {
        return portletWindow;
    }

    @Override
    public AlertsPortletDataSource getDataSource() {
        if (null == this.dataSource) {
            this.dataSource = new AlertsPortletDataSource(getContext());
        }
        return this.dataSource;
    }

    @Override
    public Canvas getHelpCanvas() {
        return new HTMLFlow(MSG.view_portlet_help_recentAlerts());
    }

    @Override
    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {

        // the portletWindow does not change, so we can hold onto it locally
        if (null == this.portletWindow && null != portletWindow) {
            this.portletWindow = portletWindow;
        }

        // if there is no configuration there is nothing to set
        if ((null == storedPortlet) || (null == storedPortlet.getConfiguration())) {
            return;
        }

        Configuration config = storedPortlet.getConfiguration();

        // not sure I love the fact that this common portlet config assigns some irrelevant/unused config props,
        // may be better to prune the common set and add the specific properties locally in this method
        for (String key : PortletConfigurationEditorComponent.CONFIG_PROPERTY_INITIALIZATION.keySet()) {
            if (config.getSimple(key) == null) {
                config.put(new PropertySimple(key, PortletConfigurationEditorComponent.CONFIG_PROPERTY_INITIALIZATION
                    .get(key)));
            }
        }

        getDataSource().setConfiguration(config);
    }

    @Override
    public DynamicForm getCustomSettingsForm() {

        LocatableDynamicForm customSettingsForm = new LocatableDynamicForm(extendLocatorId("CustomSettings"));
        LocatableVLayout page = new LocatableVLayout(customSettingsForm.extendLocatorId("Page"));
        LocatableDynamicForm filterForm = new LocatableDynamicForm(page.extendLocatorId("Filter"));
        filterForm.setMargin(5);

        final DashboardPortlet storedPortlet = this.portletWindow.getStoredPortlet();
        final Configuration portletConfig = storedPortlet.getConfiguration();

        // alert priority selector
        final SelectItem alertPrioritySelector = PortletConfigurationEditorComponent
            .getAlertPriorityEditor(portletConfig);

        // result count selector
        final SelectItem resultCountSelector = PortletConfigurationEditorComponent.getResultCountEditor(portletConfig);

        // range selector
        final CustomConfigMeasurementRangeEditor measurementRangeEditor = PortletConfigurationEditorComponent
            .getMeasurementRangeEditor(portletConfig);

        filterForm.setItems(alertPrioritySelector, resultCountSelector);

        //submit handler
        customSettingsForm.addSubmitValuesHandler(new SubmitValuesHandler() {

            @Override
            public void onSubmitValues(SubmitValuesEvent event) {
                // alert severity
                String selectedValue = alertPrioritySelector.getValue().toString();
                if ((selectedValue.trim().isEmpty())
                    || (selectedValue.split(",").length == AlertPriority.values().length)) {
                    // no severity filter
                    selectedValue = Constant.ALERT_PRIORITY_DEFAULT;
                }
                portletConfig.put(new PropertySimple(Constant.ALERT_PRIORITY, selectedValue));

                // result count
                selectedValue = resultCountSelector.getValue().toString();
                if (selectedValue.trim().isEmpty()) {
                    selectedValue = Constant.RESULT_COUNT_DEFAULT;
                }
                portletConfig.put(new PropertySimple(Constant.RESULT_COUNT, selectedValue));

                // time range settings
                saveMeasurementRangeEditorSettings(measurementRangeEditor, portletConfig);

                // persist and reload portlet
                storedPortlet.setConfiguration(portletConfig);
                configure(portletWindow, storedPortlet);
            }
        });

        page.addMember(measurementRangeEditor);
        page.addMember(filterForm);
        customSettingsForm.addChild(page);

        return customSettingsForm;
    }

    /** 
     * Takes the current value of the widget and persists it into the configuration object passed in.
    *
    * @param measurementRangeEditor
    * @param portletConfig
    * returns populated configuration object.
    */
    private void saveMeasurementRangeEditorSettings(final CustomConfigMeasurementRangeEditor measurementRangeEditor,
        Configuration portletConfig) {
        String selectedValue = null;
        if ((measurementRangeEditor != null) && (portletConfig != null)) {
            //time range filter. Check for enabled and then persist property. Dealing with compound widget.
            FormItem item = measurementRangeEditor.getItem(CustomConfigMeasurementRangeEditor.ENABLE_RANGE_ITEM);
            CheckboxItem itemC = (CheckboxItem) item;
            boolean persistTimeRangeSettings = itemC.getValueAsBoolean();
            if (persistTimeRangeSettings) {//retrieve values and persist
                selectedValue = String.valueOf(itemC.getValueAsBoolean());
                if (!selectedValue.trim().isEmpty()) {//then call
                    portletConfig.put(new PropertySimple(Constant.METRIC_RANGE_ENABLE, selectedValue));
                }

                //time advanced time filter enabled.
                boolean isAdvanceTimeSetting = false;
                selectedValue = String.valueOf(measurementRangeEditor.isAdvanced());
                if ((selectedValue != null) && (!selectedValue.trim().isEmpty())) {
                    portletConfig.put(new PropertySimple(Constant.METRIC_RANGE_BEGIN_END_FLAG, selectedValue));
                    isAdvanceTimeSetting = Boolean.valueOf(selectedValue);
                }

                //time frame
                List<Long> begEnd = measurementRangeEditor.getBeginEndTimes();
                if (isAdvanceTimeSetting) {//advanced settings
                    portletConfig.put(new PropertySimple(Constant.METRIC_RANGE, (begEnd.get(0) + "," + begEnd.get(1))));
                } else {
                    //save not advanced time range
                    portletConfig.put(new PropertySimple(Constant.METRIC_RANGE_LASTN, measurementRangeEditor
                        .getMetricRangePreferences().lastN));
                    portletConfig.put(new PropertySimple(Constant.METRIC_RANGE_UNIT, measurementRangeEditor
                        .getMetricRangePreferences().unit));
                }
            } else {//if disabled, reset time defaults
                portletConfig.put(new PropertySimple(Constant.METRIC_RANGE_ENABLE, false));
                portletConfig.put(new PropertySimple(Constant.METRIC_RANGE_BEGIN_END_FLAG, false));
                List<Long> rangeArray = MeasurementUtility.calculateTimeFrame(Integer
                    .valueOf(Constant.METRIC_RANGE_LASTN_DEFAULT), Integer.valueOf(Constant.METRIC_RANGE_UNIT_DEFAULT));
                //                String[] range = {String.valueOf(rangeArray.get(0)),String.valueOf(rangeArray.get(1))};
                portletConfig.put(new PropertySimple(Constant.METRIC_RANGE,
                    (String.valueOf(rangeArray.get(0)) + "," + String.valueOf(rangeArray.get(1)))));
            }
        }
    }

    @Override
    protected void setupTableInteractions(boolean hasWriteAccess) {
        if (!hasWriteAccess) {
            Set<Permission> globalPerm = this.getPortletWindow().getGlobalPermissions();
            ResourcePermission resPerm = this.getPortletWindow().getResourcePermissions();
            hasWriteAccess = (globalPerm.contains(Permission.MANAGE_INVENTORY) || (null != resPerm && resPerm.isAlert()));
        }
        super.setupTableInteractions(hasWriteAccess);
    }

    public void startRefreshCycle() {
        refreshTimer = AutoRefreshPortletUtil.startRefreshCycle(this, this, refreshTimer);
    }

    public boolean isRefreshing() {
        // TODO: actually keep track of when the portlet is refreshing data
        return false;
    }

    @Override
    protected void onDestroy() {
        AutoRefreshPortletUtil.onDestroy(this, refreshTimer);

        super.onDestroy();
    }

    static public class AlertsPortletDataSource extends AlertDataSource {
        private Configuration configuration;

        public AlertsPortletDataSource(EntityContext entityContext) {
            this(entityContext, null);
        }

        public AlertsPortletDataSource(EntityContext entityContext, Configuration configuration) {
            super(entityContext);
            this.configuration = configuration;
        }

        public Configuration getConfiguration() {
            return configuration;
        }

        public void setConfiguration(Configuration configuration) {
            this.configuration = configuration;
        }

        /* (non-Javadoc)
         * This override allows us to set the total rows to the number of recent alerts configured for
         * the portlet. This sets the counter appropriately and stops further queries to the server.
         * 
         * @see org.rhq.enterprise.gui.coregui.client.alert.AlertDataSource#dataRetrieved(org.rhq.core.domain.util.PageList, com.smartgwt.client.data.DSResponse, com.smartgwt.client.data.DSRequest)
         */
        @Override
        protected void dataRetrieved(PageList<Alert> result, DSResponse response, DSRequest request) {
            super.dataRetrieved(result, response, request);

            response.setTotalRows(result.size());
        }

        @Override
        protected AlertCriteria getFetchCriteria(DSRequest request) {
            AlertCriteria criteria = new AlertCriteria();

            // result count
            String currentSetting = this.configuration.getSimpleValue(Constant.RESULT_COUNT,
                Constant.RESULT_COUNT_DEFAULT);

            // We have to set a PageControl override here, or RPCDataSource will apply default paging based on the
            // request. But, once setting a paging override the CriteriaQueryGenerator will use it for
            // paging *and* sorting, so we need to also ensure our desired sorting is included in the override. So,
            // to get the most recent alerts, apply a descending ordering on create time.
            int pageNumber = 0;
            int pageSize = Integer.valueOf(currentSetting);
            OrderingField orderingField = new OrderingField("ctime", PageOrdering.DESC);
            criteria.setPageControl(new PageControl(pageNumber, pageSize, orderingField));

            // filter priority
            currentSetting = this.configuration
                .getSimpleValue(Constant.ALERT_PRIORITY, Constant.ALERT_PRIORITY_DEFAULT);
            String[] parsedValues = currentSetting.trim().split(",");
            if (!(currentSetting.trim().isEmpty() || parsedValues.length == 3)) {
                AlertPriority[] filterPriorities = new AlertPriority[parsedValues.length];
                int indx = 0;
                for (String priority : parsedValues) {
                    AlertPriority p = AlertPriority.valueOf(priority);
                    filterPriorities[indx++] = p;
                }
                criteria.addFilterPriorities(filterPriorities);
            }

            //result timeframe if enabled
            currentSetting = this.configuration.getSimpleValue(Constant.METRIC_RANGE_ENABLE, null);
            if (Boolean.valueOf(currentSetting)) {//then proceed setting

                boolean isAdvanced = false;
                //detect type of widget[Simple|Advanced]
                PropertySimple property = this.configuration.getSimple(Constant.METRIC_RANGE_BEGIN_END_FLAG);
                if (property != null) {
                    isAdvanced = property.getBooleanValue();
                }
                if (isAdvanced) {
                    //Advanced time settings
                    property = this.configuration.getSimple(Constant.METRIC_RANGE);
                    if (property != null) {
                        currentSetting = property.getStringValue();
                        String[] range = currentSetting.split(",");
                        criteria.addFilterStartTime(Long.valueOf(range[0]));
                        criteria.addFilterEndTime(Long.valueOf(range[1]));
                    }
                } else {
                    //Simple time settings
                    property = this.configuration.getSimple(Constant.METRIC_RANGE_LASTN);
                    if (property != null) {
                        int lastN = property.getIntegerValue();
                        property = this.configuration.getSimple(Constant.METRIC_RANGE_UNIT);
                        int lastUnits = property.getIntegerValue();
                        ArrayList<Long> beginEnd = MeasurementUtility.calculateTimeFrame(lastN, Integer
                            .valueOf(lastUnits));
                        criteria.addFilterStartTime(Long.valueOf(beginEnd.get(0)));
                        criteria.addFilterEndTime(Long.valueOf(beginEnd.get(1)));
                    }
                }
            }

            // add any context related filters
            switch (getEntityContext().type) {
            case Resource:
                criteria.addFilterResourceIds(getEntityContext().getResourceId());
                break;

            case ResourceGroup:
                criteria.addFilterResourceGroupIds(getEntityContext().getGroupId());
            }

            criteria.fetchAlertDefinition(true);
            criteria.fetchRecoveryAlertDefinition(true);

            return criteria;
        }
    }
}
