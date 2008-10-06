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
package org.rhq.core.domain.configuration.composite;

import java.io.Serializable;

import org.rhq.core.domain.configuration.PluginConfigurationUpdate;

public class PluginConfigurationUpdateResourceComposite implements Serializable {

    private static final long serialVersionUID = 1L;

    private final PluginConfigurationUpdate update;
    private final int resourceId;
    private final String resourceName;

    public PluginConfigurationUpdateResourceComposite(PluginConfigurationUpdate update, Integer resourceId,
        String resourceName) {
        this.update = update;
        this.resourceId = resourceId;
        this.resourceName = resourceName;
    }

    public PluginConfigurationUpdate getUpdate() {
        return update;
    }

    public int getResourceId() {
        return resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

}
