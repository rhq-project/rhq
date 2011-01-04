/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.gui.admin.plugin;

public class InstalledPluginsSessionUIBean {
    private boolean showAllServerPlugins;
    private boolean showAllAgentPlugins;
    private String selectedTab;

    public InstalledPluginsSessionUIBean() {
    }

    public boolean isShowAllServerPlugins() {
        return showAllServerPlugins;
    }

    public void setShowAllServerPlugins(boolean showDeletedServerPlugins) {
        this.showAllServerPlugins = showDeletedServerPlugins;
    }

    public boolean isShowAllAgentPlugins() {
        return showAllAgentPlugins;
    }

    public void setShowAllAgentPlugins(boolean showDeletedAgentPlugins) {
        showAllAgentPlugins = showDeletedAgentPlugins;
    }

    public void showUndeployedServerPlugins() {
        setShowAllServerPlugins(true);
    }

    public void showDeletedAgentPlugins() {
        setShowAllAgentPlugins(true);
    }

    public void hideUndeployedServerPlugins() {
        setShowAllServerPlugins(false);
    }

    public void hideDeletedAgentPlugins() {
        setShowAllAgentPlugins(false);
    }

    public String getSelectedTab() {
        return selectedTab;
    }

    public void setSelectedTab(String selectedTab) {
        this.selectedTab = selectedTab;
    }
}