/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.imported;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * This class is just for use in prototyping the TreeGrid and DataSource classes.
 *
 * @author John Sanda
 */
public class RecentlyAddedResource implements IsSerializable {

    private RecentlyAddedResource parent;

    private String resourceName;

    private String timestamp;

    public RecentlyAddedResource() {
    }

    public RecentlyAddedResource(String resourceName, String timestamp) {
        this(resourceName, timestamp, null);
    }

    public RecentlyAddedResource(String resourceName, String timestamp, RecentlyAddedResource parent) {
        this.resourceName = resourceName;
        this.timestamp = timestamp;
        this.parent = parent;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public RecentlyAddedResource getParent() {
        return parent;
    }

    public void setParent(RecentlyAddedResource parent) {
        this.parent = parent;
    }

}
