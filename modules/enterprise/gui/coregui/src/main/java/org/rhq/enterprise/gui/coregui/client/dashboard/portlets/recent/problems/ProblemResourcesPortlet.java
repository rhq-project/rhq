package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.problems;

/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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

import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.grid.ListGrid;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.dashboard.CustomSettingsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.resource.ProblemResourcesDataSource;

/**
 * A view that displays a paginated table of Resources with alerts,
 * and/or Resources reported unavailable.
 *
 * @author Simeon Pinder
 */
public class ProblemResourcesPortlet extends Table implements CustomSettingsPortlet {

    public static final String PROBLEM_RESOURCE_SHOW_HRS = "max-problems-query-span";
    public static final String PROBLEM_RESOURCE_SHOW_MAX = "max-problems-shown";
    public static final String KEY = "Has Alerts or Currently Unavailable";
    private static final String TITLE = KEY;
    private int maximumProblemResourcesToDisplay = -1;
    private int maximumProblemResourcesWithinHours = -1;
    private DashboardPortlet storedPortlet;
    public static final String unlimited = "unlimited";
    public static final String defaultValue = unlimited;

    public ProblemResourcesPortlet(String locatorId) {
        super(locatorId, TITLE, true);

        setShowHeader(false);
        setShowFooter(false);

        setOverflow(Overflow.HIDDEN);

        this.dataSource = new ProblemResourcesDataSource(this);

        setDataSource(this.dataSource);
    }

    /** Gets access to the ListGrid from super class for editing.
     *
     */
    @Override
    protected void configureTable() {
        ListGrid listGrid = getListGrid();
        if (listGrid != null) {
            //extend height for displaying disambiguated resources
            listGrid.setCellHeight(50);
            //wrap to display disambiguation
            listGrid.setWrapCells(true);
            //            System.out.println("@@@@:"+listGrid.getField(ProblemResourcesDataSource.resource)+":W"+listGrid.getField(ProblemResourcesDataSource.resource).getWidth());
            listGrid.getField(ProblemResourcesDataSource.resource).setWidth("40%");
            listGrid.getField(ProblemResourcesDataSource.location).setWidth("40%");
            listGrid.getField(ProblemResourcesDataSource.alerts).setWidth("10%");
            listGrid.getField(ProblemResourcesDataSource.available).setWidth("10%");
        }
    }

    //reference to datasource
    private ProblemResourcesDataSource dataSource;

    /** Implement configure action.
     *
     */
    @Override
    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {

        this.storedPortlet = storedPortlet;
        if (storedPortlet.getConfiguration().getSimple(PROBLEM_RESOURCE_SHOW_MAX) != null) {
            //retrieve and translate to int
            String retrieved = storedPortlet.getConfiguration().getSimple(PROBLEM_RESOURCE_SHOW_MAX).getStringValue();
            if (retrieved.equals(unlimited)) {
                maximumProblemResourcesToDisplay = -1;
            } else {
                maximumProblemResourcesToDisplay = Integer.parseInt(retrieved);
            }
        } else {//create setting
            storedPortlet.getConfiguration().put(new PropertySimple(PROBLEM_RESOURCE_SHOW_MAX, defaultValue));
        }
        if (storedPortlet.getConfiguration().getSimple(PROBLEM_RESOURCE_SHOW_HRS) != null) {
            String retrieved = storedPortlet.getConfiguration().getSimple(PROBLEM_RESOURCE_SHOW_HRS).getStringValue();
            if (retrieved.equals(unlimited)) {
                setMaximumProblemResourcesWithinHours(-1);
            } else {
                setMaximumProblemResourcesWithinHours(Integer.parseInt(retrieved));
            }
        } else {
            storedPortlet.getConfiguration().put(new PropertySimple(PROBLEM_RESOURCE_SHOW_HRS, defaultValue));
        }
    }

    @Override
    public Canvas getHelpCanvas() {
        return new HTMLFlow("This portlet displays resources that have reported alerts or Down availability.");
    }

    /** Build custom for to dispaly the Portlet Configuration settings.
     *
     */
    public DynamicForm getCustomSettingsForm() {

        final DynamicForm form = new DynamicForm();

        //-------------combobox for number of resource to display on the dashboard
        final SelectItem maximumProblemResourcesComboBox = new SelectItem(PROBLEM_RESOURCE_SHOW_MAX);
        maximumProblemResourcesComboBox.setTitle("Show maximum of");
        maximumProblemResourcesComboBox.setHint("<nobr><b> problem resources for display on dashboard.</b></nobr>");
        //spinder 9/3/10: the following is required workaround to disable editability of combobox.
        maximumProblemResourcesComboBox.setType("selection");
        //define acceptable values for display amount
        String[] acceptableDisplayValues = { "5", "10", "15", "20", "30", unlimited };
        maximumProblemResourcesComboBox.setValueMap(acceptableDisplayValues);
        //set width of dropdown display region
        maximumProblemResourcesComboBox.setWidth(100);

        //default selected value to 'unlimited'(live lists) and check both combobox settings here.
        String selectedValue = defaultValue;
        if (storedPortlet != null) {
            //if property exists retrieve it
            if (storedPortlet.getConfiguration().getSimple(PROBLEM_RESOURCE_SHOW_MAX) != null) {
                selectedValue = storedPortlet.getConfiguration().getSimple(PROBLEM_RESOURCE_SHOW_MAX).getStringValue();
            } else {//insert default value
                storedPortlet.getConfiguration().put(new PropertySimple(PROBLEM_RESOURCE_SHOW_MAX, defaultValue));
            }
        }
        //prepopulate the combobox with the previously stored selection
        maximumProblemResourcesComboBox.setDefaultValue(selectedValue);

        //------------- Build second combobox for timeframe for problem resources search.
        final SelectItem maximumTimeProblemResourcesComboBox = new SelectItem(PROBLEM_RESOURCE_SHOW_HRS);
        maximumTimeProblemResourcesComboBox.setTitle("For the last ");
        maximumTimeProblemResourcesComboBox.setHint("<nobr><b> hours </b></nobr>");
        //spinder 9/3/10: the following is required workaround to disable editability of combobox.
        maximumTimeProblemResourcesComboBox.setType("selection");
        //define acceptable values for display amount
        String[] acceptableTimeValues = { "1", "4", "8", "24", "48", unlimited };
        maximumTimeProblemResourcesComboBox.setValueMap(acceptableTimeValues);
        maximumTimeProblemResourcesComboBox.setWidth(100);

        //set to default
        selectedValue = defaultValue;
        if (storedPortlet != null) {
            //if property exists retrieve it
            if (storedPortlet.getConfiguration().getSimple(PROBLEM_RESOURCE_SHOW_HRS) != null) {
                selectedValue = storedPortlet.getConfiguration().getSimple(PROBLEM_RESOURCE_SHOW_HRS).getStringValue();
            } else {//insert default value
                storedPortlet.getConfiguration().put(new PropertySimple(PROBLEM_RESOURCE_SHOW_HRS, defaultValue));
            }
        }
        //prepopulate the combobox with the previously stored selection
        maximumTimeProblemResourcesComboBox.setDefaultValue(selectedValue);

        //insert fields
        form.setFields(maximumProblemResourcesComboBox, maximumTimeProblemResourcesComboBox);

        //submit handler
        form.addSubmitValuesHandler(new SubmitValuesHandler() {
            @Override
            public void onSubmitValues(SubmitValuesEvent event) {
                if (form.getValue(PROBLEM_RESOURCE_SHOW_MAX) != null) {
                    storedPortlet.getConfiguration().put(
                        new PropertySimple(PROBLEM_RESOURCE_SHOW_MAX, form.getValue(PROBLEM_RESOURCE_SHOW_MAX)));
                }
                if (form.getValue(PROBLEM_RESOURCE_SHOW_HRS) != null) {
                    storedPortlet.getConfiguration().put(
                        new PropertySimple(PROBLEM_RESOURCE_SHOW_HRS, form.getValue(PROBLEM_RESOURCE_SHOW_HRS)));
                }
            }
        });

        return form;
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(String locatorId) {
            return new ProblemResourcesPortlet(locatorId);
        }
    }

    public ConfigurationDefinition getConfigurationDefinition() {
        ConfigurationDefinition definition = new ConfigurationDefinition("ProblemResourcesPortlet Configuration",
            "The configuration settings for the Problem resources portlet.");

        definition.put(new PropertyDefinitionSimple(PROBLEM_RESOURCE_SHOW_MAX,
            "Maximum number of Problem resources to display.", true, PropertySimpleType.INTEGER));
        definition.put(new PropertyDefinitionSimple(PROBLEM_RESOURCE_SHOW_HRS,
            "Show problem resources going back this many hours.", true, PropertySimpleType.INTEGER));

        return definition;
    }

    public int getMaximumProblemResourcesToDisplay() {
        return maximumProblemResourcesToDisplay;
    }

    public void setMaximumProblemResourcesToDisplay(int maxPerRow) {
        this.maximumProblemResourcesToDisplay = maxPerRow;
    }

    public void setMaximumProblemResourcesWithinHours(int maximumProblemResourcesWithinHours) {
        this.maximumProblemResourcesWithinHours = maximumProblemResourcesWithinHours;
    }

    public int getMaximumProblemResourcesWithinHours() {
        return maximumProblemResourcesWithinHours;
    }
}
