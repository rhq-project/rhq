package org.rhq.coregui.client.dashboard.portlets.recent.drift;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.types.MultipleAppearance;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.GenericDriftCriteria;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.core.domain.drift.DriftComposite;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.coregui.client.components.measurement.CustomConfigMeasurementRangeEditor;
import org.rhq.coregui.client.dashboard.AutoRefreshPortlet;
import org.rhq.coregui.client.dashboard.AutoRefreshUtil;
import org.rhq.coregui.client.dashboard.CustomSettingsPortlet;
import org.rhq.coregui.client.dashboard.PortletWindow;
import org.rhq.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent;
import org.rhq.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent.Constant;
import org.rhq.coregui.client.drift.DriftDataSource;
import org.rhq.coregui.client.drift.DriftHistoryView;
import org.rhq.coregui.client.util.MeasurementUtility;
import org.rhq.coregui.client.util.Moment;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * A base class for deriving recent drift portlets for different entity contexts.  In this way the
 * basic plumbing is shared, giving a consistent behavior and configuration for the concrete portlets.
 *  
 * @author Jay Shaughnessy 
 */
public abstract class AbstractRecentDriftsPortlet extends DriftHistoryView implements CustomSettingsPortlet,
    AutoRefreshPortlet {

    private static final String DRIFT_CATEGORY = "DRIFT_CATEGORY";
    private static final String DRIFT_CATEGORY_DEFAULT = ""; // all categories

    // set on initial configuration, the window for this portlet view. 
    private PortletWindow portletWindow;

    private DriftsPortletDataSource dataSource;

    // autorefresh timer
    private Timer refreshTimer;

    public AbstractRecentDriftsPortlet(EntityContext entityContext) {
        super(null, entityContext);

        setShowFilterForm(false); //disable filter form for portlet
        setOverflow(Overflow.VISIBLE);
        setShowFooterRefresh(false); //disable footer refresh button as redundant for portlets
        setShowHeader(false);//disable header for portlets
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
    public DriftsPortletDataSource getDataSource() {
        if (null == this.dataSource) {
            this.dataSource = new DriftsPortletDataSource(getContext());
        }
        return this.dataSource;
    }

    @Override
    public Canvas getHelpCanvas() {
        return new HTMLFlow(MSG.view_portlet_help_recentDrifts());
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

        DynamicForm customSettingsForm = new DynamicForm();
        EnhancedVLayout page = new EnhancedVLayout();
        DynamicForm filterForm = new DynamicForm();
        filterForm.setMargin(5);

        final DashboardPortlet storedPortlet = this.portletWindow.getStoredPortlet();
        final Configuration portletConfig = storedPortlet.getConfiguration();

        // drift category selector
        SelectItem categoryFilter = new SelectItem("Category", MSG.common_title_category());
        categoryFilter.setWrapTitle(false);
        categoryFilter.setWidth(200);
        categoryFilter.setMultiple(true);
        categoryFilter.setMultipleAppearance(MultipleAppearance.PICKLIST);

        LinkedHashMap<String, String> categories = new LinkedHashMap<String, String>(3);
        categories.put(DriftCategory.FILE_ADDED.name(), MSG.view_drift_category_fileAdded());
        categories.put(DriftCategory.FILE_CHANGED.name(), MSG.view_drift_category_fileChanged());
        categories.put(DriftCategory.FILE_REMOVED.name(), MSG.view_drift_category_fileRemoved());
        //TODO icons?
        /*
        LinkedHashMap<String, String> priorityIcons = new LinkedHashMap<String, String>(3);
        priorityIcons.put(AlertPriority.HIGH.name(), ImageManager.getAlertIcon(AlertPriority.HIGH));
        priorityIcons.put(AlertPriority.MEDIUM.name(), ImageManager.getAlertIcon(AlertPriority.MEDIUM));
        priorityIcons.put(AlertPriority.LOW.name(), ImageManager.getAlertIcon(AlertPriority.LOW));
        */
        categoryFilter.setValueMap(categories);
        //categoryFilter.setValueIcons(priorityIcons);
        //reload current settings if they exist, otherwise enable all.
        String currentValue = portletConfig.getSimple(DRIFT_CATEGORY).getStringValue();
        if (currentValue.isEmpty() || currentValue.split(",").length == DriftCategory.values().length) {
            categoryFilter.setValues(DriftCategory.names());
        } else {
            //spinder:3/4/11 doing this nonsense due to some weird smartgwt issue with SelectItem in VLayout.
            if (currentValue.equalsIgnoreCase(DriftCategory.FILE_ADDED.name())) {
                categoryFilter.setValues(DriftCategory.FILE_ADDED.name());
            } else if (currentValue.equalsIgnoreCase(DriftCategory.FILE_CHANGED.name())) {
                categoryFilter.setValues(DriftCategory.FILE_CHANGED.name());
            } else if (currentValue.equalsIgnoreCase(DriftCategory.FILE_REMOVED.name())) {
                categoryFilter.setValues(DriftCategory.FILE_REMOVED.name());
            } else if (currentValue.equalsIgnoreCase(DriftCategory.FILE_ADDED.name() + ","
                + DriftCategory.FILE_CHANGED.name())) {
                categoryFilter.setValues(DriftCategory.FILE_ADDED.name(), DriftCategory.FILE_CHANGED.name());
            } else if (currentValue.equalsIgnoreCase(DriftCategory.FILE_ADDED.name() + ","
                + DriftCategory.FILE_REMOVED.name())) {
                categoryFilter.setValues(DriftCategory.FILE_ADDED.name(), DriftCategory.FILE_REMOVED.name());
            } else {
                categoryFilter.setValues(DriftCategory.FILE_CHANGED.name(), DriftCategory.FILE_REMOVED.name());
            }
        }

        final SelectItem driftCategorySelector = categoryFilter;

        // result count selector
        final SelectItem resultCountSelector = PortletConfigurationEditorComponent.getResultCountEditor(portletConfig);

        // range selector
        final CustomConfigMeasurementRangeEditor measurementRangeEditor = PortletConfigurationEditorComponent
            .getMeasurementRangeEditor(portletConfig);

        filterForm.setItems(driftCategorySelector, resultCountSelector);

        //submit handler
        customSettingsForm.addSubmitValuesHandler(new SubmitValuesHandler() {

            @Override
            public void onSubmitValues(SubmitValuesEvent event) {
                // category
                String selectedValue = driftCategorySelector.getValue().toString();
                if ((selectedValue.trim().isEmpty())
                    || (selectedValue.split(",").length == DriftCategory.values().length)) {
                    // no severity filter
                    selectedValue = Constant.ALERT_PRIORITY_DEFAULT;
                }
                portletConfig.put(new PropertySimple(DRIFT_CATEGORY, selectedValue));

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
                //apply latest settings to the visible result set
                refresh();
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
                List<Moment> begEnd = measurementRangeEditor.getBeginEndTimes();
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
                List<Moment> rangeArray = MeasurementUtility.calculateTimeFrame(
                    Integer.valueOf(Constant.METRIC_RANGE_LASTN_DEFAULT),
                    Integer.valueOf(Constant.METRIC_RANGE_UNIT_DEFAULT));
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
            hasWriteAccess = (globalPerm.contains(Permission.MANAGE_INVENTORY) || (null != resPerm && resPerm.isDrift()));
        }
        super.setupTableInteractions(hasWriteAccess);
    }

    @Override
    public void refreshTableInfo() {
        super.refreshTableInfo();
        if (getTableInfo() != null) {
            int count = getListGrid().getSelectedRecords().length;
            getTableInfo().setContents(
                MSG.view_table_matchingRows(String.valueOf(getListGrid().getTotalRows()), String.valueOf(count)));
        }
    }

    public void startRefreshCycle() {
        refreshTimer = AutoRefreshUtil.startRefreshCycleWithPageRefreshInterval(this, this, refreshTimer);
    }

    public boolean isRefreshing() {
        return false;
    }

    @Override
    protected void onDestroy() {
        AutoRefreshUtil.onDestroy(refreshTimer);

        super.onDestroy();
    }

    @Override
    public void refresh() {
        if (!isRefreshing()) {
            super.refresh();
        }
    }

    static public class DriftsPortletDataSource extends DriftDataSource {
        private Configuration configuration;

        public DriftsPortletDataSource(EntityContext entityContext) {
            this(entityContext, null);
        }

        public DriftsPortletDataSource(EntityContext entityContext, Configuration configuration) {
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
         * This override allows us to set the total rows to the number of recent op history configured for
         * the portlet. This sets the counter appropriately and stops further queries to the server.
         */
        @Override
        protected int getTotalRows(final PageList<DriftComposite> result, final DSResponse response,
            final DSRequest request) {

            return result.size();
        }

        @Override
        protected GenericDriftCriteria getFetchCriteria(DSRequest request) {
            GenericDriftCriteria criteria = new GenericDriftCriteria();

            // result count
            String currentSetting = this.configuration.getSimpleValue(Constant.RESULT_COUNT,
                Constant.RESULT_COUNT_DEFAULT);

            // We have to set a PageControl override here, or RPCDataSource will apply default paging based on the
            // request. But, once setting a paging override the CriteriaQueryGenerator will use it for
            // paging *and* sorting, so we need to also ensure our desired sorting is included in the override. So,
            // to get the most recent drifts, apply a descending ordering on create time.
            int pageNumber = 0;
            int pageSize = Integer.valueOf(currentSetting);
            OrderingField orderingField = new OrderingField("ctime", PageOrdering.DESC);
            criteria.setPageControl(new PageControl(pageNumber, pageSize, orderingField));

            // filter priority
            currentSetting = this.configuration.getSimpleValue(DRIFT_CATEGORY, DRIFT_CATEGORY_DEFAULT);
            String[] parsedValues = currentSetting.trim().split(",");
            if (!(currentSetting.trim().isEmpty() || parsedValues.length == 3)) {
                DriftCategory[] filterCategories = new DriftCategory[parsedValues.length];
                int i = 0;
                for (String priority : parsedValues) {
                    DriftCategory c = DriftCategory.valueOf(priority);
                    filterCategories[i++] = c;
                }
                criteria.addFilterCategories(filterCategories);
            }

            //result timeframe if enabled
            PropertySimple property = configuration.getSimple(Constant.METRIC_RANGE_ENABLE);
            if (null != property && Boolean.valueOf(property.getBooleanValue())) {//then proceed setting

                boolean isAdvanced = Boolean.valueOf(configuration.getSimpleValue(Constant.METRIC_RANGE_BEGIN_END_FLAG,
                    Constant.METRIC_RANGE_BEGIN_END_FLAG_DEFAULT));
                if (isAdvanced) {
                    //Advanced time settings
                    currentSetting = configuration.getSimpleValue(Constant.METRIC_RANGE, Constant.METRIC_RANGE_DEFAULT);
                    String[] range = currentSetting.split(",");
                    if (range.length == 2) {
                        criteria.addFilterStartTime(Long.valueOf(range[0]));
                        criteria.addFilterEndTime(Long.valueOf(range[1]));
                    }
                } else {
                    //Simple time settings
                    property = configuration.getSimple(Constant.METRIC_RANGE_LASTN);
                    if (property != null) {
                        Integer lastN = Integer.valueOf(configuration.getSimpleValue(Constant.METRIC_RANGE_LASTN,
                            Constant.METRIC_RANGE_LASTN_DEFAULT));
                        Integer units = Integer.valueOf(configuration.getSimpleValue(Constant.METRIC_RANGE_UNIT,
                            Constant.METRIC_RANGE_UNIT_DEFAULT));
                        ArrayList<Moment> beginEnd = MeasurementUtility.calculateTimeFrame(lastN, units);
                        criteria.addFilterStartTime(beginEnd.get(0).toDate().getTime());
                        criteria.addFilterEndTime(beginEnd.get(1).toDate().getTime());
                    }
                }
            }

            // add any context related filters
            switch (getEntityContext().type) {
            case Resource:
                criteria.addFilterResourceIds(getEntityContext().getResourceId());
                break;

            /*
            case ResourceGroup:
                criteria.addFilterResourceGroupIds(getEntityContext().getGroupId());
            */
            }

            criteria.fetchChangeSet(true);

            return criteria;
        }
    }

    @Override
    protected void onInit() {
        super.onInit();
        getListGrid().setEmptyMessage(MSG.view_portlet_results_empty());
    }
}
