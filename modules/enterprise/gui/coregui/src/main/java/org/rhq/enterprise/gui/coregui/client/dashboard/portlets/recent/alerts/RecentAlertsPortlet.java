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
package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.alerts;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangeHandler;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.enterprise.gui.coregui.client.alert.AlertPortletDataSource;
import org.rhq.enterprise.gui.coregui.client.alert.AlertsView;
import org.rhq.enterprise.gui.coregui.client.dashboard.CustomSettingsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableLabel;

/**
 * @author Greg Hinkle
 */
public class RecentAlertsPortlet extends AlertsView implements CustomSettingsPortlet {

    public static final String KEY = "Recent Alerts";
    public static final String TITLE = KEY;
    //widget keys also used in form population
    private static final String ALERT_RANGE_DISPLAY_AMOUNT_VALUE = "alert-range-display-amount-value";
    private static final String ALERT_RANGE_PRIORITY_VALUE = "alert-range-priority-value";
    private static final String ALERT_RANGE_TIME_VALUE = "alert-range-time-value";
    private static final String ALERT_RANGE_RESOURCES_VALUE = "alert-range-resource-value";
    //configuration default information
    private static final String defaultAlertCountValue = "5";
    private static final String PRIORITY_ALL = "ALL";
    private static final String PRIORITY_HIGH = AlertPriority.HIGH.getDisplayName();
    private static final String PRIORITY_MEDIUM = AlertPriority.MEDIUM.getDisplayName();
    private static final String PRIORITY_LOW = AlertPriority.LOW.getDisplayName();
    private static final String defaultPriorityValue = PRIORITY_ALL;
    private static final String TIME_30_MINS = "30 minutes";
    private static final String TIME_HOUR = "hour";
    private static final String TIME_12_HRS = "12 hours";
    private static final String TIME_DAY = "day";
    private static final String TIME_WEEK = "week";
    private static final String TIME_MONTH = "month";
    private static final String defaultTimeValue = TIME_DAY;
    private static final String RESOURCES_ALL = "all resources";
    private static final String RESOURCES_SELECTED = "selected resources";
    private static final String defaultResourceValue = RESOURCES_ALL;
    private static final String unlimited = "unlimited";
    //configuration container element
    private DashboardPortlet storedPortlet = null;
    private AlertPortletDataSource dataSource;

    public RecentAlertsPortlet(String locatorId) {
        //        super(locatorId);
        this(locatorId, null, null);

        setShowHeader(false);
        setShowFooter(true);
        //disable footer refresh
        setShowFooterRefresh(false);

        setOverflow(Overflow.HIDDEN);
    }

    public RecentAlertsPortlet(String locatorId, Criteria criteria, String[] excludedFieldNames) {
        super(locatorId, criteria, excludedFieldNames);

        //override the shared datasource
        this.dataSource = new AlertPortletDataSource();
        setDataSource(this.dataSource);
    }

    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {
        this.storedPortlet = storedPortlet;
        //Operation range property - retrieve existing value
        PropertySimple property = storedPortlet.getConfiguration().getSimple(ALERT_RANGE_DISPLAY_AMOUNT_VALUE);
        if ((property != null) && (property.getStringValue() != null)) {
            //retrieve and translate to int
            String retrieved = property.getStringValue();
            int translatedAlertRangeSelection = translatedAlertRangeSelection(retrieved);
            //            getDataSource().setAlertRangeCompleted(Integer.parseInt(retrieved));
            getDataSource().setAlertRangeCompleted(translatedAlertRangeSelection);
        } else {//create setting
            storedPortlet.getConfiguration().put(
                new PropertySimple(ALERT_RANGE_DISPLAY_AMOUNT_VALUE, defaultAlertCountValue));
            getDataSource().setAlertRangeCompleted(Integer.parseInt(defaultAlertCountValue));
        }
        //Operation priority property setting
        property = storedPortlet.getConfiguration().getSimple(ALERT_RANGE_PRIORITY_VALUE);
        if ((property != null) && (property.getStringValue() != null)) {
            //retrieve and translate to int
            String retrieved = property.getStringValue();
            int translatedPriorityIndex = translatedPriorityToValidIndex(retrieved);
            getDataSource().setAlertPriorityIndex(translatedPriorityIndex);
        } else {//create setting
            storedPortlet.getConfiguration().put(new PropertySimple(ALERT_RANGE_PRIORITY_VALUE, defaultPriorityValue));
            getDataSource().setAlertPriorityIndex(translatedPriorityToValidIndex(PRIORITY_ALL));
        }

        //Range to time that alerts will be shown for
        property = storedPortlet.getConfiguration().getSimple(ALERT_RANGE_TIME_VALUE);
        if ((property != null) && (property.getStringValue() != null)) {
            //retrieve and translate to int
            String retrieved = property.getStringValue();
            long translatedRange = translateTimeToValidRange(retrieved);
            getDataSource().setAlertTimeRange(translatedRange);
        } else {//create setting
            storedPortlet.getConfiguration().put(new PropertySimple(ALERT_RANGE_TIME_VALUE, defaultTimeValue));
            getDataSource().setAlertTimeRange(translateTimeToValidRange(defaultTimeValue));
        }

        //Range of resources to be included in the query
        property = storedPortlet.getConfiguration().getSimple(ALERT_RANGE_RESOURCES_VALUE);
        if ((property != null) && (property.getStringValue() != null)) {
            //retrieve and translate to int
            String retrieved = property.getStringValue();
            if (retrieved.trim().equalsIgnoreCase(RESOURCES_SELECTED)) {
                getDataSource().setAlertResourcesToUse("selected");
            } else {
                getDataSource().setAlertResourcesToUse("all");
            }
        } else {//create setting
            storedPortlet.getConfiguration().put(new PropertySimple(ALERT_RANGE_RESOURCES_VALUE, defaultResourceValue));
            getDataSource().setAlertResourcesToUse("all");
        }
    }

    private int translatedAlertRangeSelection(String retrieved) {
        int translated = -1;
        if ((retrieved != null) && (!retrieved.trim().isEmpty())) {
            if (retrieved.equalsIgnoreCase(unlimited)) {
                translated = -1;
            } else {
                translated = Integer.parseInt(retrieved);//default to all
            }
        } else {//default to defaultValue
            if (defaultAlertCountValue.equalsIgnoreCase(unlimited)) {
                translated = -1;
            } else {
                translated = Integer.parseInt(defaultAlertCountValue);
            }
        }
        return translated;
    }

    private int translatedPriorityToValidIndex(String retrieved) {
        int translatedPriority = 0;//default to all
        if ((retrieved != null) && (!retrieved.trim().isEmpty())) {
            if (retrieved.equalsIgnoreCase(PRIORITY_HIGH)) {
                translatedPriority = 3;
            } else if (retrieved.equalsIgnoreCase(PRIORITY_MEDIUM)) {
                translatedPriority = 2;
            } else if (retrieved.equalsIgnoreCase(PRIORITY_LOW)) {
                translatedPriority = 1;
            } else {
                translatedPriority = 0;//default to all
            }
        }
        return translatedPriority;
    }

    /**Translates the UI selection options into time values for alert query.
     *
     * @param retrieved
     * @return long value mapping to string passed in.
     */
    private long translateTimeToValidRange(String retrieved) {
        long translated = 0;//default to ALL
        if ((retrieved != null) && (!retrieved.trim().isEmpty())) {
            if (retrieved.equalsIgnoreCase(TIME_30_MINS)) {
                translated = MeasurementUtility.MINUTES * 30;
            } else if (retrieved.equalsIgnoreCase(TIME_HOUR)) {
                translated = MeasurementUtility.HOURS;
            } else if (retrieved.equalsIgnoreCase(TIME_12_HRS)) {
                translated = MeasurementUtility.HOURS * 12;
            } else if (retrieved.equalsIgnoreCase(TIME_DAY)) {
                translated = MeasurementUtility.DAYS;
            } else if (retrieved.equalsIgnoreCase(TIME_WEEK)) {
                translated = MeasurementUtility.WEEKS;
            } else if (retrieved.equalsIgnoreCase(TIME_MONTH)) {
                translated = MeasurementUtility.DAYS * 28;//replicated from old struts def.
            } else {
                translated = MeasurementUtility.DAYS;//default to day otherwise.
            }
        }
        return translated;
    }

    public Canvas getHelpCanvas() {
        return new HTMLFlow("Displays recent alerts fired on resources visible to the current user login.");
    }

    public DynamicForm getCustomSettingsForm() {

        //root dynamic form instance
        final DynamicForm form = new DynamicForm();
        form.setWidth(200);

        VLayout column = new VLayout();

        //label
        LocatableLabel alertRangeLabel = new LocatableLabel("DynamicForm_Label_Alert_Range", "<b>Alert Range</b>");

        //horizontal layout
        LocatableHLayout row = new LocatableHLayout("alert-range-settings-row-1");
        row.setMembersMargin(10);

        //-------------combobox for number of completed scheduled ops to display on the dashboard
        final SelectItem alertRangeLastComboBox = new SelectItem(ALERT_RANGE_DISPLAY_AMOUNT_VALUE);
        alertRangeLastComboBox.setTitle("Last");
        alertRangeLastComboBox.setType("selection");
        //define acceptable values for display amount
        String[] acceptableDisplayValues = { "5", "10", "unlimited" };
        alertRangeLastComboBox.setValueMap(acceptableDisplayValues);
        //set width of dropdown display region
        alertRangeLastComboBox.setWidth(100);
        alertRangeLastComboBox.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                String selectedItem = "" + event.getValue();
                //stuff into the master form for retrieval
                form.setValue(ALERT_RANGE_DISPLAY_AMOUNT_VALUE, selectedItem);
            }
        });

        //default selected value to 'unlimited'(live lists) and check both combobox settings here.
        String selectedValue = defaultAlertCountValue;
        if (storedPortlet != null) {
            //if property exists retrieve it
            if (storedPortlet.getConfiguration().getSimple(ALERT_RANGE_DISPLAY_AMOUNT_VALUE) != null) {
                selectedValue = storedPortlet.getConfiguration().getSimple(ALERT_RANGE_DISPLAY_AMOUNT_VALUE)
                    .getStringValue();
            } else {//insert default value
                storedPortlet.getConfiguration().put(
                    new PropertySimple(ALERT_RANGE_DISPLAY_AMOUNT_VALUE, defaultAlertCountValue));
            }
        }
        //prepopulate the combobox with the previously stored selection
        alertRangeLastComboBox.setDefaultValue(selectedValue);

        //-------------combobox for number of completed scheduled ops to display on the dashboard
        final SelectItem alertRangePriorityComboBox = new SelectItem(ALERT_RANGE_PRIORITY_VALUE);
        alertRangePriorityComboBox.setTitle("");
        alertRangePriorityComboBox.setHint("<nobr> <b> priority Alerts,</b></nobr>");
        alertRangePriorityComboBox.setType("selection");
        //define acceptable values for display amount
        String[] acceptablePriorityDisplayValues = { PRIORITY_ALL, PRIORITY_HIGH, PRIORITY_MEDIUM, PRIORITY_LOW };
        alertRangePriorityComboBox.setValueMap(acceptablePriorityDisplayValues);
        //set width of dropdown display region
        alertRangePriorityComboBox.setWidth(100);
        alertRangePriorityComboBox.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                String selectedItem = "" + event.getValue();
                //stuff into the master form for retrieval
                form.setValue(ALERT_RANGE_PRIORITY_VALUE, selectedItem);
            }
        });

        //default selected value to 'unlimited'(live lists) and check both combobox settings here.
        selectedValue = defaultPriorityValue;
        if (storedPortlet != null) {
            //if property exists retrieve it
            if (storedPortlet.getConfiguration().getSimple(ALERT_RANGE_PRIORITY_VALUE) != null) {
                selectedValue = storedPortlet.getConfiguration().getSimple(ALERT_RANGE_PRIORITY_VALUE).getStringValue();
            } else {//insert default value
                storedPortlet.getConfiguration().put(
                    new PropertySimple(ALERT_RANGE_PRIORITY_VALUE, defaultPriorityValue));
            }
        }
        //prepopulate the combobox with the previously stored selection
        alertRangePriorityComboBox.setDefaultValue(selectedValue);
        row.addMember(alertRangeLabel);
        DynamicForm wrappedRange = new DynamicForm();
        wrappedRange.setFields(alertRangeLastComboBox);
        row.addMember(wrappedRange);

        DynamicForm wrappedPriority = new DynamicForm();
        wrappedPriority.setFields(alertRangePriorityComboBox);
        row.addMember(wrappedPriority);

        //horizontal layout
        LocatableHLayout row2 = new LocatableHLayout("alert-range-settings-row-2");

        Label alertRangeSpanLabel = new Label("<b>within the past<b>");
        //------------- Build second combobox for timeframe for problem resources search.
        final SelectItem alertRangeTimeComboBox = new SelectItem(ALERT_RANGE_TIME_VALUE);
        alertRangeTimeComboBox.setTitle("");
        alertRangeTimeComboBox.setHint("");
        alertRangeTimeComboBox.setType("selection");
        String[] acceptableTimeDisplayValues = { TIME_30_MINS, TIME_HOUR, TIME_12_HRS, TIME_DAY, TIME_WEEK, TIME_MONTH };
        alertRangeTimeComboBox.setValueMap(acceptableTimeDisplayValues);
        alertRangeTimeComboBox.setWidth(100);
        alertRangeTimeComboBox.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                String selectedItem = "" + event.getValue();
                //stuff into the master form for retrieval
                form.setValue(ALERT_RANGE_TIME_VALUE, selectedItem);
            }
        });

        //set to default
        selectedValue = defaultTimeValue;
        if (storedPortlet != null) {
            //if property exists retrieve it
            if (storedPortlet.getConfiguration().getSimple(ALERT_RANGE_TIME_VALUE) != null) {
                selectedValue = storedPortlet.getConfiguration().getSimple(ALERT_RANGE_TIME_VALUE).getStringValue();
            } else {//insert default value
                storedPortlet.getConfiguration().put(new PropertySimple(ALERT_RANGE_TIME_VALUE, defaultTimeValue));
            }
        }
        //prepopulate the combobox with the previously stored selection
        alertRangeTimeComboBox.setDefaultValue(selectedValue);
        DynamicForm timeSelectionWrapper = new DynamicForm();
        timeSelectionWrapper.setFields(alertRangeTimeComboBox);

        // build resource selection drop down
        //------------- Build second combobox for timeframe for problem resources search.
        final SelectItem alertResourcesComboBox = new SelectItem(ALERT_RANGE_RESOURCES_VALUE);
        alertResourcesComboBox.setTitle("for");
        alertResourcesComboBox.setHint("");
        alertResourcesComboBox.setType("selection");
        String[] acceptableResourceDisplayValues = { RESOURCES_ALL, RESOURCES_SELECTED };
        alertResourcesComboBox.setValueMap(acceptableResourceDisplayValues);
        alertResourcesComboBox.setWidth(100);
        alertResourcesComboBox.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                String selectedItem = "" + event.getValue();
                //stuff into the master form for retrieval
                form.setValue(ALERT_RANGE_RESOURCES_VALUE, selectedItem);
            }
        });

        //set to default
        selectedValue = defaultResourceValue;
        if (storedPortlet != null) {
            //if property exists retrieve it
            if (storedPortlet.getConfiguration().getSimple(ALERT_RANGE_RESOURCES_VALUE) != null) {
                selectedValue = storedPortlet.getConfiguration().getSimple(ALERT_RANGE_RESOURCES_VALUE)
                    .getStringValue();
            } else {//insert default value
                storedPortlet.getConfiguration().put(
                    new PropertySimple(ALERT_RANGE_RESOURCES_VALUE, defaultResourceValue));
            }
        }
        //prepopulate the combobox with the previously stored selection
        alertResourcesComboBox.setDefaultValue(selectedValue);
        DynamicForm resourceSelectionWrapper = new DynamicForm();
        resourceSelectionWrapper.setFields(alertResourcesComboBox);

        alertRangeSpanLabel.setWrap(false);
        alertRangeSpanLabel.setWidth(150);
        row2.addMember(alertRangeSpanLabel);
        row2.addMember(timeSelectionWrapper);
        row2.addMember(resourceSelectionWrapper);
        column.addMember(row);
        column.addMember(row2);
        form.addChild(column);

        //submit handler
        form.addSubmitValuesHandler(new SubmitValuesHandler() {
            @Override
            public void onSubmitValues(SubmitValuesEvent event) {
                //no need to insert validation here as user not allowed to enter values
                parseFormAndPopulateConfiguration(form, storedPortlet, ALERT_RANGE_DISPLAY_AMOUNT_VALUE,
                    ALERT_RANGE_PRIORITY_VALUE, ALERT_RANGE_RESOURCES_VALUE, ALERT_RANGE_TIME_VALUE);
                refresh();//
            }
        });

        return form;

    }

    /**Iterates over DynamicForm instance to check for properties passed in and if they have been set
     * to put that property into the DashboardPortlet configuration.
     *
     * @param form Dynamic form storing user selections
     * @param portlet Container for configuration changes
     * @param properties Variable list of keys used to verify or populate properties.
     */
    private void parseFormAndPopulateConfiguration(final DynamicForm form, DashboardPortlet portlet,
        String... properties) {
        if ((form != null) && (portlet != null)) {
            for (String property : properties) {
                if (form.getValue(property) != null) {//if new value supplied
                    storedPortlet.getConfiguration().put(new PropertySimple(property, form.getValue(property)));
                }
            }
        }
    }

    public AlertPortletDataSource getDataSource() {
        return dataSource;
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(String locatorId) {
            return new RecentAlertsPortlet(locatorId);
        }
    }
}
