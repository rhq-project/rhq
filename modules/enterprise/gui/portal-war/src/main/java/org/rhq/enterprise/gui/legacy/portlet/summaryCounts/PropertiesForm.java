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
package org.rhq.enterprise.gui.legacy.portlet.summaryCounts;

import org.rhq.enterprise.gui.legacy.WebUserPreferences.SummaryCountPortletPreferences;
import org.rhq.enterprise.gui.legacy.portlet.DashboardBaseForm;

public class PropertiesForm extends DashboardBaseForm {

    SummaryCountPortletPreferences counts = new SummaryCountPortletPreferences();

    public boolean isPlatform() {
        return this.counts.showPlatforms;
    }

    public void setPlatform(boolean platform) {
        this.counts.showPlatforms = platform;
    }

    public boolean isServer() {
        return this.counts.showServers;
    }

    public void setServer(boolean server) {
        this.counts.showServers = server;
    }

    public boolean isService() {
        return this.counts.showServices;
    }

    public void setService(boolean service) {
        this.counts.showServices = service;
    }

    public boolean isGroupCompat() {
        return this.counts.showCompatibleGroups;
    }

    public void setGroupCompat(boolean flag) {
        this.counts.showCompatibleGroups = flag;
    }

    public boolean isGroupMixed() {
        return this.counts.showMixedGroups;
    }

    public void setGroupMixed(boolean groupMixed) {
        this.counts.showMixedGroups = groupMixed;
    }

    public boolean isGroupDefinition() {
        return this.counts.showGroupDefinitions;
    }

    public void setGroupDefinition(boolean groupDefinition) {
        this.counts.showGroupDefinitions = groupDefinition;
    }

    public SummaryCountPortletPreferences getSummaryCounts() {
        return this.counts;
    }

    public void setSummaryCounts(SummaryCountPortletPreferences counts) {
        this.counts = counts;
    }
}