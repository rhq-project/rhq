/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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

package org.rhq.enterprise.gui.navigation.contextmenu;

import java.util.List;

import javax.faces.component.html.HtmlGraphicImage;
import javax.faces.component.html.HtmlOutputLink;
import javax.servlet.http.HttpServletRequest;

import org.richfaces.component.html.ContextMenu;
import org.richfaces.component.html.HtmlMenuGroup;
import org.richfaces.component.html.HtmlMenuItem;
import org.richfaces.component.html.HtmlMenuSeparator;

import org.rhq.core.domain.resource.composite.ResourceFacets;
import org.rhq.core.gui.util.FacesComponentUtility;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Lukas Krejci
 */
public abstract class TreeContextMenuBase {

    private static final String STYLE_QUICK_LINKS_ICON = "margin: 2px;";

    private ContextMenu menu;
    private ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();

    public ContextMenu getMenu() {
        return menu;
    }

    public void setMenu(ContextMenu menu) throws Exception {
        this.menu = menu;

        menu.getChildren().clear();

        init();

        if (shouldCreateMenu()) {
            for (String header : getMenuHeaders()) {
                addMenuItem(null, header, true);
            }

            ResourceFacets facets = resourceTypeManager.getResourceFacets(getResourceTypeId());

            addQuickLinks(getMenuQuickLinks(), facets);

            addAdditionalMenuItems(menu);

            menu.getChildren().add(new HtmlMenuSeparator());

            addGenericSubMenu("View Metric Chart", getViewChartsMenuItems());
            addMeasurementGraphToViewsMenu(getGraphToViewMenuItems());
            addGenericSubMenu("Execute Operation", getOperationsMenuItems());
        }
    }

    protected void addMenuItem(String id, String value, boolean disabled) {
        HtmlMenuItem nameItem = new HtmlMenuItem();
        if (id != null) {
            nameItem.setId(id);
        }
        nameItem.setValue(value);
        nameItem.setDisabled(disabled);
        nameItem.setStyle("color: black;");
        menu.getChildren().add(nameItem);
    }

    /**
     * Adds a quick links menu item to the menu.
     * 
     * The descriptor describes the URLs to use for the individual links.
     * If any of the properties in the descriptor is left null, no quick link
     * is created for the corresponding item even if there are enough privileges
     * for the user to access such tab.
     * 
     * @param menu the menu to add the quick links to
     * @param descriptor the description of the links to create
     * @param facets the facets supported by the item (resource, group, autogroup) we're creating the quick links for
     */
    private void addQuickLinks(QuickLinksDescriptor descriptor, ResourceFacets facets) {
        HtmlMenuItem quickLinksItem = new HtmlMenuItem();
        quickLinksItem.setSubmitMode("none");
        quickLinksItem.setId(descriptor.getMenuItemId());

        HtmlOutputLink link;
        HtmlGraphicImage image;

        if (descriptor.getMonitoringUrl() != null /* && LookupUtil.getSystemManager().isMonitoringEnabled()*/) {
            link = FacesComponentUtility.addOutputLink(quickLinksItem, null, descriptor.getMonitoringUrl());
            image = FacesComponentUtility.addGraphicImage(link, null, "/images/icons/Monitor_grey_16.png", "Monitor");
            image.setStyle(STYLE_QUICK_LINKS_ICON);
        }

        if (descriptor.getInventoryUrl() != null) {
            link = FacesComponentUtility.addOutputLink(quickLinksItem, null, descriptor.getInventoryUrl());
            image = FacesComponentUtility.addGraphicImage(link, null, "/images/icons/Inventory_grey_16.png",
                "Inventory");
            image.setStyle(STYLE_QUICK_LINKS_ICON);
        }

        if (descriptor.getAlertsUrl() != null /*&& LookupUtil.getSystemManager().isMonitoringEnabled()*/) {
            link = FacesComponentUtility.addOutputLink(quickLinksItem, null, descriptor.getAlertsUrl());
            image = FacesComponentUtility.addGraphicImage(link, null, "/images/icons/Alert_grey_16.png", "Alerts");
            image.setStyle(STYLE_QUICK_LINKS_ICON);
        }

        if (descriptor.getConfigurationUrl() != null && facets.isConfiguration()) {
            link = FacesComponentUtility.addOutputLink(quickLinksItem, null, descriptor.getConfigurationUrl());
            image = FacesComponentUtility.addGraphicImage(link, null, "/images/icons/Configure_grey_16.png",
                "Configuration");
            image.setStyle(STYLE_QUICK_LINKS_ICON);
        }

        if (descriptor.getOperationUrl() != null && facets.isOperation()) {
            link = FacesComponentUtility.addOutputLink(quickLinksItem, null, descriptor.getOperationUrl());
            image = FacesComponentUtility.addGraphicImage(link, null, "/images/icons/Operation_grey_16.png",
                "Operations");
            image.setStyle(STYLE_QUICK_LINKS_ICON);
        }

        if (descriptor.getEventUrl() != null && facets.isEvent()) {
            link = FacesComponentUtility.addOutputLink(quickLinksItem, null, descriptor.getEventUrl());
            image = FacesComponentUtility.addGraphicImage(link, null, "/images/icons/Events_grey_16.png", "Events");
            image.setStyle(STYLE_QUICK_LINKS_ICON);
        }

        if (descriptor.getContentUrl() != null && facets.isContent()) {
            link = FacesComponentUtility.addOutputLink(quickLinksItem, null, descriptor.getContentUrl());
            image = FacesComponentUtility.addGraphicImage(link, null, "/images/icons/Content_grey_16.png", "Content");
            image.setStyle(STYLE_QUICK_LINKS_ICON);
        }

        menu.getChildren().add(quickLinksItem);
    }

    private void addMeasurementGraphToViewsMenu(List<MetricMenuItemDescriptor> descriptors) {
        HttpServletRequest request = FacesContextUtility.getRequest();
        String requestURL = request.getRequestURL().toString().toLowerCase();
        boolean onMonitorGraphsSubtab = (requestURL.indexOf("/monitor/graphs.xhtml") != -1);

        // addChartToGraph menu only if you're looking at the graphs
        if (onMonitorGraphsSubtab && descriptors != null) {
            HtmlMenuGroup measurementMenu = new HtmlMenuGroup();
            measurementMenu.setValue("Add Graph to View");
            menu.getChildren().add(measurementMenu);
            measurementMenu.setDisabled(descriptors.isEmpty());

            for (MetricMenuItemDescriptor descriptor : descriptors) {
                HtmlMenuItem menuItem = new HtmlMenuItem();
                menuItem.setValue(descriptor.getName());
                menuItem.setId(descriptor.getMenuItemId());

                String onClickAddMeasurements = "addMetric('" + descriptor.getMetricToken() + "');";
                String onClickRefreshPage = "setTimeout(window.location.reload(), 5000);"; // refresh after 5 secs

                menuItem.setSubmitMode("none");
                menuItem.setOnclick(onClickAddMeasurements + onClickRefreshPage);

                measurementMenu.getChildren().add(menuItem);
            }
        }
    }

    private void addGenericSubMenu(String menuName, List<MenuItemDescriptor> descriptors) {
        if (descriptors != null) {
            HtmlMenuGroup subMenu = new HtmlMenuGroup();
            subMenu.setValue(menuName);
            menu.getChildren().add(subMenu);
            subMenu.setDisabled(descriptors.isEmpty());

            for (MenuItemDescriptor descriptor : descriptors) {
                HtmlMenuItem menuItem = new HtmlMenuItem();
                menuItem.setValue(descriptor.getName());
                menuItem.setId(descriptor.getMenuItemId());

                menuItem.setSubmitMode("none");
                menuItem.setOnclick("document.location.href='" + descriptor.getUrl() + "'");

                subMenu.getChildren().add(menuItem);
            }
        }
    }

    /**
     * Subclasses can initialize themselves in this method before the other methods are called.
     * 
     * @throws Exception the initialization might fail
     */
    protected abstract void init() throws Exception;

    /**
     * @return true if the menu should be rendered, false otherwise
     */
    protected abstract boolean shouldCreateMenu();

    /**
     * @return List of strings to be used as the menu headers. For each such string a disabled menu item is created.
     */
    protected abstract List<String> getMenuHeaders();

    /**
     * @return the resource type id of the current item in the nav tree.
     */
    protected abstract int getResourceTypeId();

    /**
     * The descriptor describes the URLs to use for the individual links.
     * If any of the properties in the descriptor is left null, no quick link
     * is created for the corresponding item even if there are enough privileges
     * for the user to access such tab.
     * 
     * @return the descriptor for the quick links to be created
     */
    protected abstract QuickLinksDescriptor getMenuQuickLinks();

    /**
     * Override this method if any additional menu items need to be created in the menu.
     * 
     * This method is called after the quick links are added to the provided menu.
     * 
     * @param menu the context menu being created.
     */
    protected void addAdditionalMenuItems(ContextMenu menu) {
        //no additional menu items added by default
    }

    protected abstract List<MenuItemDescriptor> getViewChartsMenuItems();

    /**
     * The menu items of this menu must provide the metric token.
     * 
     * @return the list of menu items for the "Graph To View" sub menu
     */
    protected abstract List<MetricMenuItemDescriptor> getGraphToViewMenuItems();

    protected abstract List<MenuItemDescriptor> getOperationsMenuItems();
}
