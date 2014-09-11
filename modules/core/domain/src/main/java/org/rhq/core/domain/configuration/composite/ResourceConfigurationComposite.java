/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

import org.rhq.core.domain.configuration.Configuration;

/**
 * @author Ian Springer
 */
public class ResourceConfigurationComposite implements Serializable{
    private static final long serialVersionUID = 1L;

    private int resourceId;
    /**
     * This could be a resource config or a plugin config depending on the context.
     */
    private Configuration configuration;

    // Default constructor required by GWT
    public ResourceConfigurationComposite() {
    }

    public ResourceConfigurationComposite(int resourceId, Configuration configuration) {
        this.resourceId = resourceId;
        this.configuration = configuration;
    }

    public int getResourceId() {
        return resourceId;
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
