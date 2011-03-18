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

import com.google.gwt.user.client.History;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.alert.AlertPortletConfigurationDataSource;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.groups.GroupAlertsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.alerts.PortletAlertSelector;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Simeon Pinder
 */
public class ResourceAlertsPortlet extends GroupAlertsPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "ResourceAlerts";
    // A default displayed, persisted name for the portlet
    public static final String NAME = MSG.view_portlet_defaultName_resource_alerts();

    private int resourceId;

    public ResourceAlertsPortlet(String locatorId) {
        super(locatorId);

        //override the shared datasource
        //figure out which page we're loading
        String currentPage = History.getToken();
        String[] elements = currentPage.split("/");
        this.resourceId = Integer.valueOf(elements[1]);
    }

    /**Defines layout for the portlet page.
     */
    protected void initializeUi() {
        //initalize the datasource
        Integer[] resourceIds = new Integer[1];
        resourceIds[0] = this.resourceId;
        this.dataSource = new AlertPortletConfigurationDataSource(storedPortlet, portletConfig, null, resourceIds);
        setDataSource(this.dataSource);

        setShowHeader(false);
        setShowFooter(true);
        setShowFooterRefresh(false); //disable footer refresh
        setShowFilterForm(false); //disable filter form for portlet

        getListGrid().setEmptyMessage(MSG.view_portlet_results_empty());
    }

    @Override
    protected void onInit() {
        super.onInit();
        initializeUi();
    }

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

        if (!portletConfigInitialized) {
            Integer[] resourceIds = new Integer[1];
            resourceIds[0] = this.resourceId;
            this.dataSource = new AlertPortletConfigurationDataSource(storedPortlet, portletConfig, null, resourceIds);
            setDataSource(this.dataSource);
            portletConfigInitialized = true;
        }

        //lazy init any elements not yet configured.
        for (String key : PortletConfigurationEditorComponent.CONFIG_PROPERTY_INITIALIZATION.keySet()) {
            if (portletConfig.getSimple(key) == null) {
                portletConfig.put(new PropertySimple(key,
                    PortletConfigurationEditorComponent.CONFIG_PROPERTY_INITIALIZATION.get(key)));
            }
        }

        //resource ids to be conditionally included in the query
        Integer[] filterResourceIds = null;
        filterResourceIds = getDataSource().extractFilterResourceIds(storedPortlet, filterResourceIds);
        //no defaults

        if (filterResourceIds != null) {
            getDataSource().setAlertFilterResourceId(filterResourceIds);
        }

        //        //conditionally display the selected resources ui
        //        if (containerCanvas != null) {
        //            //empty out earlier canvas
        //            for (Canvas c : containerCanvas.getChildren()) {
        //                c.destroy();
        //            }
        //            if ((resourceSelector != null) && getDataSource().getAlertResourcesToUse().equals(RESOURCES_SELECTED)) {
        //                containerCanvas.addChild(resourceSelector.getCanvas());
        //            } else {
        //                containerCanvas.addChild(new Canvas());
        //            }
        //        }

    }

    public AlertPortletConfigurationDataSource getDataSource() {
        return dataSource;
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(String locatorId) {

            return new ResourceAlertsPortlet(locatorId);
        }
    }

    @Override
    protected void configureTable() {
        super.configureTable();

        setListGridDoubleClickHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                ListGrid listGrid = (ListGrid) event.getSource();
                ListGridRecord[] selectedRows = listGrid.getSelection();
                if (selectedRows != null && selectedRows.length == 1) {
                    Integer recordId = getId(selectedRows[0]);
                    Integer resourceId = selectedRows[0].getAttributeAsInt("resourceId");
                    CoreGUI.goToView(LinkManager.getSubsystemAlertHistoryLink(resourceId, recordId));
                }
            }
        });
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
}

/** Bundles a ResourceSelector instance with labeling in Canvas for display.
 *  Also modifies the AssignedGrid to listen for AvailbleGrid completion and act accordingly.
 */
class AlertResourceSelectorRegion extends LocatableVLayout {
    public AlertResourceSelectorRegion(String locatorId, Integer[] assigned) {
        super(locatorId);
        this.currentlyAssignedIds = assigned;
    }

    private static final Messages MSG = CoreGUI.getMessages();
    private PortletAlertSelector selector = null;

    private Integer[] currentlyAssignedIds;

    public Integer[] getCurrentlyAssignedIds() {
        return currentlyAssignedIds;
    }

    public Integer[] getListGridValues() {
        Integer[] listGridValues = new Integer[0];
        if (null != selector) {
            listGridValues = selector.getAssignedListGridValues();
        }
        return listGridValues;
    }

    public Canvas getCanvas() {
        if (selector == null) {
            selector = new PortletAlertSelector(extendLocatorId("AlertSelector"), this.currentlyAssignedIds,
                ResourceType.ANY_PLATFORM_TYPE, false);
        }
        return selector;
    }

    public void setCurrentlyAssignedIds(Integer[] currentlyAssignedIds) {
        this.currentlyAssignedIds = currentlyAssignedIds;
    }
}