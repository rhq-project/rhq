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
package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.groups;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.MultipleAppearance;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.components.configuration.PropertyValueChangeEvent;
import org.rhq.enterprise.gui.coregui.client.components.configuration.PropertyValueChangeListener;
import org.rhq.enterprise.gui.coregui.client.components.measurement.CustomConfigMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.dashboard.AutoRefreshPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.CustomSettingsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.gwt.AlertGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.summary.AbstractActivityView;
import org.rhq.enterprise.gui.coregui.client.resource.disambiguation.ReportDecorator;
import org.rhq.enterprise.gui.coregui.client.util.GwtRelativeDurationConverter;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableCanvas;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**This portlet allows the end user to customize the:
 * i)range
 * ii)priority
 * iii)etc.
 * of alerts to display for the given group
 *
 * @author Simeon Pinder
 */
public class GroupAlertsPortlet extends LocatableVLayout implements CustomSettingsPortlet, AutoRefreshPortlet,
    PropertyValueChangeListener {

    public interface Constant {
        String ALERT_PRIORITY = "ALERT_PRIORITY";
        String ALERT_METRIC_RANGE_ENABLE = "ALERT_METRIC_RANGE_ENABLE";
        String ALERT_METRIC_RANGE_BEGIN_END_FLAG = "ALERT_METRIC_RANGE_BEGIN_END_FLAG";
        String ALERT_METRIC_RANGE = "ALERT_METRIC_RANGE";
        String ALERT_METRIC_RANGE_LASTN = "ALERT_METRIC_RANGE_LASTN";
        String ALERT_METRIC_RANGE_LASTN_DEFAULT = String.valueOf(8);
        String ALERT_METRIC_RANGE_UNIT = "ALERT_METRIC_RANGE_UNIT";
        String RESULT_SORT_ORDER = "RESULT_SORT_ORDER";
        String RESULT_COUNT = "RESULT_COUNT";
        String RESULT_COUNT_DEFAULT = "5";
        String CUSTOM_REFRESH = "CUSTOM_REFRESH";
    }

    //configuration map initialization
    private static Map<String, String> CONFIG_PROPERTY_INITIALIZATION = new HashMap<String, String>();
    static {// Key, Default value
        //alert priority, if empty initialize to "" i.e. all priorities
        CONFIG_PROPERTY_INITIALIZATION.put(Constant.ALERT_PRIORITY, "");
        //result sort order, if empty initialize to "DESC"
        CONFIG_PROPERTY_INITIALIZATION.put(Constant.RESULT_SORT_ORDER, PageOrdering.DESC.name());
        //result count, if empty initialize to 5
        CONFIG_PROPERTY_INITIALIZATION.put(Constant.RESULT_COUNT, Constant.RESULT_COUNT_DEFAULT);
        //whether to specify time range for alerts. Defaults to false
        CONFIG_PROPERTY_INITIALIZATION.put(Constant.ALERT_METRIC_RANGE_ENABLE, String.valueOf(false));
        //whether Begin and End values set for time. Aka. Advanced/full range setting Defaults to false
        CONFIG_PROPERTY_INITIALIZATION.put(Constant.ALERT_METRIC_RANGE_BEGIN_END_FLAG, String.valueOf(false));
        //whether in simple mode. Ex. 8 hrs. Defaults to 8
        CONFIG_PROPERTY_INITIALIZATION
            .put(Constant.ALERT_METRIC_RANGE_LASTN, Constant.ALERT_METRIC_RANGE_LASTN_DEFAULT);

    }

    private int groupId = -1;
    protected LocatableCanvas recentAlertsContent = new LocatableCanvas(extendLocatorId("RecentAlerts"));
    private static AlertGWTServiceAsync alertService = GWTServiceLookup.getAlertService();
    private boolean currentlyLoading = false;
    private Configuration portletConfig = null;
    private SelectItem alertPrioritySelector = null;
    private SelectItem resultSortSelector = null;
    private SelectItem resultCountSelector = null;
    private CustomConfigMeasurementRangeEditor measurementRangeEditor = null;
    private DashboardPortlet storedPortlet;

    public GroupAlertsPortlet(String locatorId) {
        super(locatorId);
        //figure out which page we're loading
        String currentPage = History.getToken();
        String[] elements = currentPage.split("/");
        int currentGroupIdentifier = Integer.valueOf(elements[1]);
        this.groupId = currentGroupIdentifier;
        initializeUi();
    }

    @Override
    protected void onInit() {
        super.onInit();
        loadData();
    }

    /**Defines layout for the Activity page.
     */
    protected void initializeUi() {
        setPadding(5);
        setMembersMargin(5);
        addMember(recentAlertsContent);
    }

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "GroupAlerts";
    // A default displayed, persisted name for the portlet
    public static final String NAME = "Group: Alerts";
    public static final String ID = "id";

    // set on initial configuration, the window for this portlet view.
    private PortletWindow portletWindow;
    //instance ui widgets

    private Timer refreshTimer;

    /** Responsible for initialization and lazy configuration of the portlet values
     */
    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {
        //populate portlet configuration details
        if (null == this.portletWindow && null != portletWindow) {
            this.portletWindow = portletWindow;
        }

        if ((null == storedPortlet) || (null == storedPortlet.getConfiguration())) {
            return;
        }
        this.storedPortlet = storedPortlet;
        portletConfig = storedPortlet.getConfiguration();

        //lazy init any elements not yet configured.
        for (String key : CONFIG_PROPERTY_INITIALIZATION.keySet()) {
            if (portletConfig.getSimple(key) == null) {
                portletConfig.put(new PropertySimple(key, CONFIG_PROPERTY_INITIALIZATION.get(key)));
            }
        }

        //custom refresh
        //CUSTOM_REFRESH
    }

    public Canvas getHelpCanvas() {
        return new HTMLFlow(MSG.view_portlet_help_recentAlerts());
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(String locatorId) {
            return new GroupAlertsPortlet(locatorId);
        }
    }

    /** Fetches alerts and updates the DynamicForm instance with the latest
     *  alert information.
     */
    private void getRecentAlerts() {
        final int groupId = this.groupId;
        currentlyLoading = false;
        //fetches last five alerts for this resource
        AlertCriteria criteria = new AlertCriteria();
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
        property = portletConfig.getSimple(Constant.ALERT_METRIC_RANGE_ENABLE);
        if (Boolean.valueOf(property.getBooleanValue())) {//then proceed setting
            property = portletConfig.getSimple(Constant.ALERT_METRIC_RANGE);
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
        criteria.addFilterResourceGroupIds(groupId);
        alertService.findAlertsByCriteria(criteria, new AsyncCallback<PageList<Alert>>() {
            @Override
            public void onSuccess(PageList<Alert> result) {
                VLayout column = new VLayout();
                column.setHeight(10);
                if (!result.isEmpty()) {
                    int rowNum = 0;
                    for (Alert alert : result) {
                        // alert history records do not have a usable locatorId, we'll use rownum, which is unique and
                        // may be repeatable.
                        LocatableDynamicForm row = new LocatableDynamicForm(recentAlertsContent.extendLocatorId(String
                            .valueOf(rowNum++)));
                        row.setNumCols(3);

                        StaticTextItem iconItem = AbstractActivityView.newTextItemIcon(ImageManager.getAlertIcon(alert
                            .getAlertDefinition().getPriority()), alert.getAlertDefinition().getPriority()
                            .getDisplayName());
                        LinkItem link = AbstractActivityView.newLinkItem(alert.getAlertDefinition().getName() + ": ",
                            ReportDecorator.GWT_GROUP_URL + groupId + "/Alerts/History/" + alert.getId());
                        StaticTextItem time = AbstractActivityView.newTextItem(GwtRelativeDurationConverter
                            .format(alert.getCtime()));
                        row.setItems(iconItem, link, time);

                        column.addMember(row);
                    }
                    //link to more details
                    LocatableDynamicForm row = new LocatableDynamicForm(recentAlertsContent.extendLocatorId(String
                        .valueOf(rowNum++)));
                    AbstractActivityView.addSeeMoreLink(row, ReportDecorator.GWT_GROUP_URL + groupId
                        + "/Alerts/History/", column);
                } else {
                    LocatableDynamicForm row = AbstractActivityView.createEmptyDisplayRow(recentAlertsContent
                    //                        .extendLocatorId("None"), AbstractActivityView.RECENT_ALERTS_NONE);
                        .extendLocatorId("None"), "No results using criteria specified.");
                    column.addMember(row);
                }
                for (Canvas child : recentAlertsContent.getChildren()) {
                    child.destroy();
                }
                recentAlertsContent.addChild(column);
                recentAlertsContent.markForRedraw();
            }

            @Override
            public void onFailure(Throwable caught) {
                Log.debug("Error retrieving recent alerts for group [" + groupId + "]:" + caught.getMessage());
            }
        });
    }

    protected void loadData() {
        currentlyLoading = true;
        getRecentAlerts();
    }

    @Override
    public DynamicForm getCustomSettingsForm() {
        LocatableDynamicForm customSettings = new LocatableDynamicForm(extendLocatorId("customSettings"));
        LocatableVLayout page = new LocatableVLayout(customSettings.extendLocatorId("page"));
        //build editor form container
        final LocatableDynamicForm form = new LocatableDynamicForm(page.extendLocatorId("alert-filter"));
        form.setMargin(5);

        //add label about what configuration affects

        //add alert priority selector
        alertPrioritySelector = getAlertPriorityEditor();
        //add sort priority selector
        resultSortSelector = getResulSortOrderEditor("sort.priority", "Sort Order", "Sets sort order for results.");
        //add result count selector
        //TODO: spinder 3/4/11 this is arbitrary. Get UXD input for better acceptable defaults
        int[] countSelections = { 5, 10, 30, 100 };
        resultCountSelector = getResultCountEditor(Constant.RESULT_COUNT, "Results Count",
            "Displays N results with alerts", countSelections);

        //add range selector
        measurementRangeEditor = new CustomConfigMeasurementRangeEditor(page.extendLocatorId("alertTimeFrame"),
            portletConfig);

        form.setItems(alertPrioritySelector, resultSortSelector, resultCountSelector);

        //submit handler
        customSettings.addSubmitValuesHandler(new SubmitValuesHandler() {

            @Override
            public void onSubmitValues(SubmitValuesEvent event) {
                //alert severity
                String selectedValue = alertPrioritySelector.getValue().toString();
                if ((selectedValue.trim().isEmpty()) || (selectedValue.split(",").length == 3)) {//then no alertPriority specified
                    portletConfig.put(new PropertySimple(Constant.ALERT_PRIORITY, ""));
                } else {//some subset of available alertPriorities will be used
                    portletConfig.put(new PropertySimple(Constant.ALERT_PRIORITY, selectedValue));
                }
                //result sort order
                selectedValue = resultSortSelector.getValue().toString();
                if ((selectedValue.trim().isEmpty()) || (selectedValue.equalsIgnoreCase(PageOrdering.DESC.name()))) {//then desc
                    portletConfig.put(new PropertySimple(Constant.RESULT_SORT_ORDER, PageOrdering.DESC));
                } else {
                    portletConfig.put(new PropertySimple(Constant.RESULT_SORT_ORDER, PageOrdering.ASC));
                }
                //result count
                selectedValue = resultCountSelector.getValue().toString();
                if ((selectedValue.trim().isEmpty()) || (selectedValue.equalsIgnoreCase(Constant.RESULT_COUNT_DEFAULT))) {//then 5
                    portletConfig.put(new PropertySimple(Constant.RESULT_COUNT, Constant.RESULT_COUNT_DEFAULT));
                } else {
                    portletConfig.put(new PropertySimple(Constant.RESULT_COUNT, selectedValue));
                }

                //alert time range filter. Check for enabled and then persist property. Dealing with compound widget.
                FormItem item = measurementRangeEditor.getItem(CustomConfigMeasurementRangeEditor.ENABLE_RANGE_ITEM);
                CheckboxItem itemC = (CheckboxItem) item;
                selectedValue = String.valueOf(itemC.getValueAsBoolean());
                if (!selectedValue.trim().isEmpty()) {//then call
                    portletConfig.put(new PropertySimple(Constant.ALERT_METRIC_RANGE_ENABLE, selectedValue));
                }

                //alert time advanced time filter enabled.
                selectedValue = String.valueOf(measurementRangeEditor.isAdvanced());
                if ((selectedValue != null) && (!selectedValue.trim().isEmpty())) {
                    portletConfig.put(new PropertySimple(Constant.ALERT_METRIC_RANGE_BEGIN_END_FLAG, selectedValue));
                }

                //alert time frame
                List<Long> begEnd = measurementRangeEditor.getBeginEndTimes();
                if (begEnd.get(0) != 0) {//advanced settings
                    portletConfig.put(new PropertySimple(Constant.ALERT_METRIC_RANGE, (begEnd.get(0) + "," + begEnd
                        .get(1))));
                }

                //persist
                storedPortlet.setConfiguration(portletConfig);
                configure(portletWindow, storedPortlet);
                loadData();
            }
        });
        form.markForRedraw();
        page.addMember(measurementRangeEditor);
        page.addMember(form);
        customSettings.addChild(page);
        return customSettings;
    }

    /* single select combobox for number of items to display on the dashboard
     *
     * @param identifier Form identifier
     * @param selectionTitle Title to left of combobox
     * @param selectionHint Hint to display on mouseover
     * @param selectionValues Integer[] to show as drop down options.
     * @return Populated selectItem instance.
     */
    private SelectItem getResultCountEditor(String identifier, String selectionTitle, String selectionHint,
        int[] selectionValues) {

        final SelectItem maximumResultsComboBox = new SelectItem(identifier);
        maximumResultsComboBox.setTitle(selectionTitle);
        maximumResultsComboBox.setWrapTitle(false);
        maximumResultsComboBox.setTooltip("<nobr><b> " + selectionHint + "</b></nobr>");
        //spinder 9/3/10: the following is required workaround to disable editability of combobox.
        maximumResultsComboBox.setType("selection");
        //set width of dropdown display region
        maximumResultsComboBox.setWidth(100);

        //define acceptable values for display amount
        String[] displayValues = new String[selectionValues.length];
        int i = 0;
        for (int selection : selectionValues) {
            displayValues[i++] = String.valueOf(selection);
        }
        maximumResultsComboBox.setValueMap(displayValues);
        //reload current settings if they exist, otherwise enable all.
        String currentValue = portletConfig.getSimple(Constant.RESULT_COUNT).getStringValue();
        if (currentValue.isEmpty() || currentValue.equalsIgnoreCase(Constant.RESULT_COUNT_DEFAULT)) {
            maximumResultsComboBox.setValue(Constant.RESULT_COUNT_DEFAULT);
        } else {
            maximumResultsComboBox.setValue(currentValue);
        }
        return maximumResultsComboBox;
    }

    private SelectItem getAlertPriorityEditor() {
        SelectItem priorityFilter = new SelectItem("severities", MSG.view_alerts_table_filter_priority());
        priorityFilter.setWrapTitle(false);
        priorityFilter.setWidth(200);
        priorityFilter.setMultiple(true);
        priorityFilter.setMultipleAppearance(MultipleAppearance.PICKLIST);

        LinkedHashMap<String, String> priorities = new LinkedHashMap<String, String>(3);
        priorities.put(AlertPriority.HIGH.name(), MSG.common_alert_high());
        priorities.put(AlertPriority.MEDIUM.name(), MSG.common_alert_medium());
        priorities.put(AlertPriority.LOW.name(), MSG.common_alert_low());
        LinkedHashMap<String, String> priorityIcons = new LinkedHashMap<String, String>(3);
        priorityIcons.put(AlertPriority.HIGH.name(), ImageManager.getAlertIcon(AlertPriority.HIGH));
        priorityIcons.put(AlertPriority.MEDIUM.name(), ImageManager.getAlertIcon(AlertPriority.MEDIUM));
        priorityIcons.put(AlertPriority.LOW.name(), ImageManager.getAlertIcon(AlertPriority.LOW));
        priorityFilter.setValueMap(priorities);
        priorityFilter.setValueIcons(priorityIcons);
        //reload current settings if they exist, otherwise enable all.
        String currentValue = portletConfig.getSimple(Constant.ALERT_PRIORITY).getStringValue();
        if (currentValue.isEmpty() || currentValue.split(",").length == AlertPriority.values().length) {
            priorityFilter.setValues(AlertPriority.HIGH.name(), AlertPriority.MEDIUM.name(), AlertPriority.LOW.name());
        } else {
            //spinder:3/4/11 doing this nonsense due to some weird smartgwt issue with SelectItem in VLayout.
            if (currentValue.equalsIgnoreCase("HIGH")) {
                priorityFilter.setValues(AlertPriority.HIGH.name());
            } else if (currentValue.equalsIgnoreCase("HIGH,MEDIUM")) {
                priorityFilter.setValues(AlertPriority.HIGH.name(), AlertPriority.MEDIUM.name());
            } else if (currentValue.equalsIgnoreCase("HIGH,LOW")) {
                priorityFilter.setValues(AlertPriority.HIGH.name(), AlertPriority.LOW.name());
            } else if (currentValue.equalsIgnoreCase("MEDIUM")) {
                priorityFilter.setValues(AlertPriority.MEDIUM.name());
            } else if (currentValue.equalsIgnoreCase("MEDIUM,LOW")) {
                priorityFilter.setValues(AlertPriority.MEDIUM.name(), AlertPriority.LOW.name());
            } else {
                priorityFilter.setValues(AlertPriority.LOW.name());
            }
        }
        return priorityFilter;
    }

    private SelectItem getResulSortOrderEditor(String identifier, String selectionTitle, String selectionHint) {
        SelectItem sortPrioritySelection = new SelectItem(identifier, selectionTitle);
        sortPrioritySelection.setWrapTitle(false);
        sortPrioritySelection.setTooltip(selectionHint);
        LinkedHashMap<String, String> priorities = new LinkedHashMap<String, String>(2);
        priorities.put(PageOrdering.ASC.name(), "Ascending");
        priorities.put(PageOrdering.DESC.name(), "Descending");
        LinkedHashMap<String, String> priorityIcons = new LinkedHashMap<String, String>(2);
        priorityIcons.put(PageOrdering.ASC.name(), "ascending");
        priorityIcons.put(PageOrdering.DESC.name(), "descending");

        sortPrioritySelection.setValueMap(priorities);
        sortPrioritySelection.setValueIcons(priorityIcons);
        //TODO: spinder 3/4/11 not sure why this is necessary. [SKIN] not being interpreted.
        String skinDir = "../org.rhq.enterprise.gui.coregui.CoreGUI/sc/skins/Enterprise/images";
        sortPrioritySelection.setImageURLPrefix(skinDir + "/actions/sort_");
        sortPrioritySelection.setImageURLSuffix(".png");

        //reload current settings if they exist, otherwise enable all.
        String currentValue = portletConfig.getSimple(Constant.RESULT_SORT_ORDER).getStringValue();
        if (currentValue.isEmpty() || currentValue.equalsIgnoreCase(PageOrdering.DESC.name())) {//default to descending order
            sortPrioritySelection.setDefaultValue(PageOrdering.DESC.name());
        } else {
            sortPrioritySelection.setDefaultValue(PageOrdering.ASC.name());
        }
        return sortPrioritySelection;
    }

    @Override
    public void startRefreshCycle() {
        //current setting
        final int refreshInterval = UserSessionManager.getUserPreferences().getPageRefreshInterval();

        //cancel any existing timer
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }

        if (refreshInterval >= MeasurementUtility.MINUTES) {

            refreshTimer = new Timer() {
                public void run() {
                    if (!currentlyLoading) {
                        loadData();
                        redraw();
                    }
                }
            };

            refreshTimer.scheduleRepeating(refreshInterval);
        }
    }

    @Override
    protected void onDestroy() {
        if (refreshTimer != null) {

            refreshTimer.cancel();
        }
        super.onDestroy();
    }

    @Override
    public void redraw() {
        super.redraw();
        loadData();
    }

    @Override
    public void propertyValueChanged(PropertyValueChangeEvent event) {
        // TODO Auto-generated method stub

    }
}