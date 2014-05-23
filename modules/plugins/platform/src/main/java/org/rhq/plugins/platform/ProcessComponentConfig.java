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

package org.rhq.plugins.platform;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;

/**
* @author Thomas Segismont
*/
class ProcessComponentConfig {

    enum Type {
        pidFile, piql
    }

    private Type type;
    private String pidFile;
    private String piql;
    private boolean fullProcessTree;

    private ProcessComponentConfig(Type type, String pidFile, String piql, boolean fullProcessTree) {
        this.type = type;
        this.pidFile = pidFile;
        this.piql = piql;
        this.fullProcessTree = fullProcessTree;
    }

    /**
     * Create a {@link org.rhq.plugins.platform.ProcessComponentConfig} instance with the supplied
     * {@link org.rhq.core.domain.configuration.Configuration}. May throw an
     * {@link org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException} if:
     * <ul>
     * <li>type is null or unknown
     * <li>fullProcessTree is null
     * <li>type is "pidFile" and pidFile is blank</li>
     * <li>type is "piql" and piql is blank</li>
     * <ul>
     *
     * @param pluginConfig
     * @return a {@link org.rhq.plugins.platform.ProcessComponentConfig} instance
     * @throws org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException
     */
    static ProcessComponentConfig createProcessComponentConfig(Configuration pluginConfig)
        throws InvalidPluginConfigurationException {

        Type type;
        String pidFile;
        String piql;
        boolean fullProcessTree;

        try {
            type = Type.valueOf(pluginConfig.getSimpleValue("type", "pidFile"));
            pidFile = pluginConfig.getSimpleValue("pidFile", null);
            piql = pluginConfig.getSimpleValue("piql", null);
            fullProcessTree = pluginConfig.getSimple("fullProcessTree").getBooleanValue();
        } catch (Exception e) {
            throw new InvalidPluginConfigurationException(e);
        }

        // validate the plugin config some more
        if (type == Type.pidFile && (pidFile == null || pidFile.length() == 0)) {
            throw new InvalidPluginConfigurationException("Missing pidfile");
        }
        if (type == Type.piql && (piql == null || piql.length() == 0)) {
            throw new InvalidPluginConfigurationException("Missing process query");
        }

        return new ProcessComponentConfig(type, pidFile, piql, fullProcessTree);
    }

    Type getType() {
        return type;
    }

    String getPidFile() {
        return pidFile;
    }

    String getPiql() {
        return piql;
    }

    boolean isFullProcessTree() {
        return fullProcessTree;
    }
}
