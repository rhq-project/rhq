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

/**
 * A class to describe the quick links to be created in the
 * {@link ContextMenuHelper#addQuickLinks(org.richfaces.component.html.ContextMenu, QuickLinksDescriptor)}
 * method.
 * 
 * The menuItemId is the id of the quick links menu item to be created. The rest of the properties
 * describe the url for the individual tabs.
 * 
 * @author Lukas Krejci
 */
public class QuickLinksDescriptor {
    private String menuItemId;
    private String monitoringUrl;
    private String inventoryUrl;
    private String alertsUrl;
    private String configurationUrl;
    private String operationUrl;
    private String eventUrl;
    private String contentUrl;

    /**
     * The id of the quick links menu item
     */
    public String getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(String menuItemId) {
        this.menuItemId = menuItemId;
    }

    public String getMonitoringUrl() {
        return monitoringUrl;
    }

    public void setMonitoringUrl(String monitoringUrl) {
        this.monitoringUrl = monitoringUrl;
    }

    public String getInventoryUrl() {
        return inventoryUrl;
    }

    public void setInventoryUrl(String inventoryUrl) {
        this.inventoryUrl = inventoryUrl;
    }

    public String getAlertsUrl() {
        return alertsUrl;
    }

    public void setAlertsUrl(String alertsUrl) {
        this.alertsUrl = alertsUrl;
    }

    public String getConfigurationUrl() {
        return configurationUrl;
    }

    public void setConfigurationUrl(String configurationUrl) {
        this.configurationUrl = configurationUrl;
    }

    public String getOperationUrl() {
        return operationUrl;
    }

    public void setOperationUrl(String operationUrl) {
        this.operationUrl = operationUrl;
    }

    public String getEventUrl() {
        return eventUrl;
    }

    public void setEventUrl(String eventUrl) {
        this.eventUrl = eventUrl;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }
}
