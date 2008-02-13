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

import org.rhq.enterprise.gui.legacy.portlet.DashboardBaseForm;

public class PropertiesForm extends DashboardBaseForm {
    private boolean platform;
    private boolean server;
    private boolean service;
    private boolean groupCompat;
    private boolean groupMixed;
    private boolean software;

    public boolean isPlatform() {
        return this.platform;
    }

    public void setPlatform(boolean platform) {
        this.platform = platform;
    }

    public boolean isServer() {
        return this.server;
    }

    public void setServer(boolean server) {
        this.server = server;
    }

    public boolean isService() {
        return this.service;
    }

    public void setService(boolean service) {
        this.service = service;
    }

    public boolean isGroupCompat() {
        return this.groupCompat;
    }

    public void setGroupCompat(boolean flag) {
        this.groupCompat = flag;
    }

    public boolean isGroupMixed() {
        return this.groupMixed;
    }

    public void setGroupMixed(boolean groupMixed) {
        this.groupMixed = groupMixed;
    }

    public boolean isSoftware() {
        return software;
    }

    public void setSoftware(boolean software) {
        this.software = software;
    }
}