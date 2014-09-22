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

package org.rhq.core.domain.plugin;

import java.io.Serializable;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.util.StringUtils;

/**
 * Provides metadata that describes control operations that are defined in a server plugin descriptor.
 * This is a domain object so it can be used by remote clients that do not have access to the
 * server-side only ControlDefinition object (such as GWT clients).
 * 
 * @author John Mazzitelli
 */
public class ServerPluginControlDefinition implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String displayName;
    private String description;
    private ConfigurationDefinition parameters;
    private ConfigurationDefinition results;

    public ServerPluginControlDefinition(String name, String displayName, String description,
        ConfigurationDefinition parameters, ConfigurationDefinition results) {

        if (name == null) {
            throw new NullPointerException("name == null");
        }

        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.parameters = parameters;
        this.results = results;
    }

    protected ServerPluginControlDefinition() {
        // needed for GWT
    }

    public String getName() {
        return this.name;
    }

    public String getDisplayName() {
        String retStr = this.displayName;
        if (retStr == null) {
            retStr = StringUtils.deCamelCase(this.name);
        }
        return retStr;
    }

    public String getDescription() {
        return this.description;
    }

    public ConfigurationDefinition getParameters() {
        return this.parameters;
    }

    public ConfigurationDefinition getResults() {
        return this.results;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ControlDefinition name=[").append(this.name).append("]");
        builder.append(", description=[").append(this.description).append("]");
        return builder.toString();
    }
}
