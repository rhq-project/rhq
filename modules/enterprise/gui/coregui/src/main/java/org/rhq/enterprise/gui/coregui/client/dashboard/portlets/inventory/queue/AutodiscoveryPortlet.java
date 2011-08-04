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
package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.inventory.queue;

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangeHandler;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.AutoRefreshPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.AutoRefreshPortletUtil;
import org.rhq.enterprise.gui.coregui.client.dashboard.CustomSettingsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.discovery.AutodiscoveryQueueDataSource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.discovery.ResourceAutodiscoveryView;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;

/**
 * @author Simeon Pinder
 * @author Greg Hinkle
 */
public class AutodiscoveryPortlet extends ResourceAutodiscoveryView implements CustomSettingsPortlet,
    AutoRefreshPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "Autodiscovery";
    // A default displayed, persisted name for the portlet    
    public static final String NAME = MSG.view_portlet_defaultName_autodiscovery();

    //ui attributes/properties/indentifiers
    private static final String AUTODISCOVERY_PLATFORM_MAX = "auto-discovery-platform-max";

    private String unlimited = MSG.common_label_unlimited();
    private String defaultValue = unlimited;

    // set on initial configuration, the window for this portlet view. 
    private PortletWindow portletWindow;

    private AutodiscoveryQueueDataSource dataSource;
    private Timer refreshTimer;

    public AutodiscoveryPortlet(String locatorId) {
        super(locatorId, true);

        //initialize the datasource to include Portlet instance
        this.dataSource = new AutodiscoveryQueueDataSource(getTreeGrid());
        if (getTreeGrid() != null) {
            getTreeGrid().setDataSource(getDataSource());
        }
    }

    /** Implements configure action.  Stores reference to encompassing window.
    */
    @Override
    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {
        if (null == this.portletWindow && null != portletWindow) {
            this.portletWindow = portletWindow;
        }

        if ((null == storedPortlet) || (null == storedPortlet.getConfiguration())) {
            return;
        }

        // loads/retrieves initial portlet settings for datasource
        String retrieved = null;

        //if settings already exist for this portlet
        if (storedPortlet.getConfiguration().getSimple(AUTODISCOVERY_PLATFORM_MAX) != null) {
            //retrieve and translate to int
            retrieved = storedPortlet.getConfiguration().getSimple(AUTODISCOVERY_PLATFORM_MAX).getStringValue();
        } else {//create setting
            storedPortlet.getConfiguration().put(new PropertySimple(AUTODISCOVERY_PLATFORM_MAX, defaultValue));
            retrieved = defaultValue;
        }

        if (retrieved.equals(unlimited)) {
            getDataSource().setMaximumPlatformsToDisplay(-1);
        } else {
            getDataSource().setMaximumPlatformsToDisplay(Integer.parseInt(retrieved));
        }
    }

    public Canvas getHelpCanvas() {
        return new HTMLFlow(MSG.view_portlet_help_autodiscovery());
    }

    /** Build custom settings form.
    */
    public DynamicForm getCustomSettingsForm() {
        final LocatableDynamicForm form = new LocatableDynamicForm(extendLocatorId("Settings"));
        form.setLayoutAlign(VerticalAlignment.CENTER);

        final DashboardPortlet storedPortlet = portletWindow.getStoredPortlet();

        //horizontal display component
        LocatableHLayout row = new LocatableHLayout(extendLocatorId("auto-discovery.configuration"));

        //-------------combobox for number of platforms to display on the dashboard
        final SelectItem maximumPlatformsComboBox = new SelectItem(AUTODISCOVERY_PLATFORM_MAX);
        maximumPlatformsComboBox.setTitle(MSG.common_title_show());
        maximumPlatformsComboBox.setHint("<nobr><b> " + MSG.view_portlet_autodiscovery_setting_platforms()
            + "</b></nobr>");
        //spinder 9/3/10: the following is required workaround to disable editability of combobox.
        maximumPlatformsComboBox.setType("selection");
        //define acceptable values for display amount
        String[] displayValues = { "1", "2", "5", "10", unlimited };
        maximumPlatformsComboBox.setValueMap(displayValues);
        //set width of dropdown display region
        maximumPlatformsComboBox.setWidth(100);
        maximumPlatformsComboBox.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                String selectedItem = "" + event.getValue();
                //store in master form for retrieval
                form.setValue(AUTODISCOVERY_PLATFORM_MAX, selectedItem);
            }
        });

        DynamicForm item = new DynamicForm();
        item.setFields(maximumPlatformsComboBox);
        row.addMember(item);

        //default selected value to 'unlimited'(live lists) and check both combobox settings here.
        String selectedValue = defaultValue;
        PropertySimple simpleProperty = null;
        if ((simpleProperty = storedPortlet.getConfiguration().getSimple(AUTODISCOVERY_PLATFORM_MAX)) != null) {
            String retrieved = simpleProperty.getStringValue();
            if (retrieved.equals(unlimited)) {
                selectedValue = unlimited;
            } else {
                selectedValue = String.valueOf(retrieved);
            }
        }

        //prepopulate the combobox with the previously stored selection
        maximumPlatformsComboBox.setDefaultValue(selectedValue);

        form.addChild(row);

        //submit handler
        form.addSubmitValuesHandler(new SubmitValuesHandler() {
            //specify submit action.
            public void onSubmitValues(SubmitValuesEvent event) {

                if (form.getValue(AUTODISCOVERY_PLATFORM_MAX) != null) {
                    //persist this value to configuration
                    storedPortlet.getConfiguration().put(
                        new PropertySimple(AUTODISCOVERY_PLATFORM_MAX, form.getValue(AUTODISCOVERY_PLATFORM_MAX)));

                    configure(portletWindow, storedPortlet);

                    markForRedraw();
                }
            }
        });

        return form;
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(String locatorId) {

            return new AutodiscoveryPortlet(locatorId);
        }
    }

    public AutodiscoveryQueueDataSource getDataSource() {
        return dataSource;
    }

    public void startRefreshCycle() {
        refreshTimer = AutoRefreshPortletUtil.startRefreshCycle(this, this, refreshTimer);
    }

    @Override
    protected void onDestroy() {
        AutoRefreshPortletUtil.onDestroy(this, refreshTimer);

        super.onDestroy();
    }

    public boolean isRefreshing() {
        return false;
    }

    @Override
    public void refresh() {
        if (!isRefreshing()) {
            if (null != dataSource) {
                dataSource.invalidateCache();
            }
            markForRedraw();
        }
    }
}
