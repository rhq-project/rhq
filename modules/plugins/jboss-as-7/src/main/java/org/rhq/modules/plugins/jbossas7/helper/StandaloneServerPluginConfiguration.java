/*
 * RHQ Management Platform
 * Copyright (C) 2013 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7.helper;

import java.io.File;

import org.rhq.core.domain.configuration.Configuration;

/**
 * A wrapper for the plug-in configuration of an AS7 Standalone Server resource, which provides strongly typed accessors for each of
 * the configuration properties.
 *
 * @author Larry O'Leary
 */
public class StandaloneServerPluginConfiguration extends ServerPluginConfiguration {

    public abstract class Property {
        public static final String DEPLOYMENT_SCANNER_PATH = "deploymentScannerPath";
    }

    private Configuration pluginConfig;

    /**
     * @param pluginConfig
     */
    public StandaloneServerPluginConfiguration(Configuration pluginConfig) {
        super(pluginConfig);
        this.pluginConfig = pluginConfig;
    }

    public File getDeploymentScannerPath() {
        String stringValue = this.pluginConfig.getSimpleValue(Property.DEPLOYMENT_SCANNER_PATH);
        return (stringValue != null && !stringValue.isEmpty()) ? new File(stringValue) : null;
    }

    public void setDeploymentScannerPath(File deploymentScannerPath) {
        this.pluginConfig.setSimpleValue(Property.DEPLOYMENT_SCANNER_PATH,
            (deploymentScannerPath != null) ? deploymentScannerPath.toString() : null);
    }

}

