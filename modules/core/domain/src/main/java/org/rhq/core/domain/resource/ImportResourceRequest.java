/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.core.domain.resource;

import java.io.Serializable;

import org.rhq.core.domain.configuration.Configuration;

/**
 * @author Thomas Segismont
 */
public class ImportResourceRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private int resourceTypeId;
    private int parentResourceId;
    private Configuration pluginConfiguration;

    @SuppressWarnings("unused")
    public ImportResourceRequest() {
        // Needed by GWT
    }

    public ImportResourceRequest(int resourceTypeId, int parentResourceId, Configuration pluginConfiguration) {
        this.resourceTypeId = resourceTypeId;
        this.parentResourceId = parentResourceId;
        this.pluginConfiguration = pluginConfiguration;
    }

    public int getResourceTypeId() {
        return resourceTypeId;
    }

    @SuppressWarnings("unused")
    public void setResourceTypeId(int resourceTypeId) {
        this.resourceTypeId = resourceTypeId;
    }

    public int getParentResourceId() {
        return parentResourceId;
    }

    @SuppressWarnings("unused")
    public void setParentResourceId(int parentResourceId) {
        this.parentResourceId = parentResourceId;
    }

    public Configuration getPluginConfiguration() {
        return pluginConfiguration;
    }

    @SuppressWarnings("unused")
    public void setPluginConfiguration(Configuration pluginConfiguration) {
        this.pluginConfiguration = pluginConfiguration;
    }
}
