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

import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.BlurbItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangeHandler;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.CustomSettingsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.discovery.ResourceAutodiscoveryView;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;

/**
 * @author Greg Hinkle
 */
public class AutodiscoveryPortlet extends ResourceAutodiscoveryView implements CustomSettingsPortlet {

    public static final String KEY = "Discovery Queue";
    private static final String AUTODISCOVERY_PLATFORM_MAX = "auto-discovery-platform-max";
    private String unlimited = "unlimited";
    private String defaultValue = unlimited;
    private DashboardPortlet storedPortlet;
    private int maximumPlatformsToDisplay = -1;

    public AutodiscoveryPortlet(String locatorId) {
        super(locatorId, true);
    }

    /** Implement configure action.
    */
    @Override
    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {
        this.storedPortlet = storedPortlet;
        if (storedPortlet.getConfiguration().getSimple(AUTODISCOVERY_PLATFORM_MAX) != null) {
            //retrieve and translate to int
            String retrieved = storedPortlet.getConfiguration().getSimple(AUTODISCOVERY_PLATFORM_MAX).getStringValue();
            if (retrieved.equals(unlimited)) {
                maximumPlatformsToDisplay = -1;
            } else {
                maximumPlatformsToDisplay = Integer.parseInt(retrieved);
            }
        } else {//create setting
            storedPortlet.getConfiguration().put(new PropertySimple(AUTODISCOVERY_PLATFORM_MAX, defaultValue));
        }
    }

    public Canvas getHelpCanvas() {
        return new HTMLFlow("This portlet offers the ability to import newly discovered resources into the inventory "
            + "for monitoring and management or to ingnore them from further action.");
    }

    /** Build custom for to dispaly the Portlet Configuration settings.
    *
    */
    public DynamicForm getCustomSettingsForm() {

        final DynamicForm form = new DynamicForm();
        form.setLayoutAlign(VerticalAlignment.CENTER);

        //horizontal display component
        LocatableHLayout row = new LocatableHLayout("auto-discovery.configuration");
        BlurbItem label = new BlurbItem("discovery-platform-count-label");
        label.setValue("Number of platforms to display");

        //-------------combobox for number of platforms to display on the dashboard
        final SelectItem maximumPlatformsComboBox = new SelectItem(AUTODISCOVERY_PLATFORM_MAX);
        maximumPlatformsComboBox.setTitle("");
        //spinder 9/3/10: the following is required workaround to disable editability of combobox.
        maximumPlatformsComboBox.setType("selection");
        //define acceptable values for display amount
        String[] acceptableDisplayValues = { "1", "2", "5", "10", unlimited };
        maximumPlatformsComboBox.setValueMap(acceptableDisplayValues);
        //set width of dropdown display region
        maximumPlatformsComboBox.setWidth(100);
        maximumPlatformsComboBox.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                String selectedItem = "" + event.getValue();
                //stuff into the master form for retrieval
                form.setValue(AUTODISCOVERY_PLATFORM_MAX, selectedItem);
            }
        });

        //wrap field item in dynamicform for addition as a field item
        DynamicForm item = new DynamicForm();
        item.setFields(label);

        row.addMember(item);
        DynamicForm item2 = new DynamicForm();
        item2.setFields(maximumPlatformsComboBox);
        row.addMember(item2);

        //default selected value to 'unlimited'(live lists) and check both combobox settings here.
        String selectedValue = defaultValue;
        if (storedPortlet != null) {
            //if property exists retrieve it
            if (storedPortlet.getConfiguration().getSimple(AUTODISCOVERY_PLATFORM_MAX) != null) {
                selectedValue = storedPortlet.getConfiguration().getSimple(AUTODISCOVERY_PLATFORM_MAX).getStringValue();
            } else {//insert default value
                storedPortlet.getConfiguration().put(new PropertySimple(AUTODISCOVERY_PLATFORM_MAX, defaultValue));
            }
        }
        //prepopulate the combobox with the previously stored selection
        maximumPlatformsComboBox.setDefaultValue(selectedValue);

        form.addChild(row);

        //submit handler
        form.addSubmitValuesHandler(new SubmitValuesHandler() {
            @Override
            public void onSubmitValues(SubmitValuesEvent event) {
                if (form.getValue(AUTODISCOVERY_PLATFORM_MAX) != null) {
                    storedPortlet.getConfiguration().put(
                        new PropertySimple(AUTODISCOVERY_PLATFORM_MAX, form.getValue(AUTODISCOVERY_PLATFORM_MAX)));
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
}
