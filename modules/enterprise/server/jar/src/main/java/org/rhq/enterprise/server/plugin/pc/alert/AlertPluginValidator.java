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

package org.rhq.enterprise.server.plugin.pc.alert;

import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment;
import org.rhq.enterprise.server.plugin.pc.ServerPluginValidator;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.alert.AlertPluginDescriptorType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.alert.CustomUi;

public class AlertPluginValidator implements ServerPluginValidator {

    public boolean validate(ServerPluginEnvironment env) {

        Log log = LogFactory.getLog(AlertPluginValidator.class);

        AlertPluginDescriptorType type = (AlertPluginDescriptorType) env.getPluginDescriptor();

        String className = type.getPluginClass();
        if (!className.contains(".")) {
            className = type.getPackage() + "." + className;
        }
        try {
            Class.forName(className, false, env.getPluginClassLoader());
        }
        catch (Exception e) {
            log.error("Can't find pluginClass " + className + " for plugin " + env.getPluginKey().getPluginName());
            return false;
        }

        // The short name is basically the key into the plugin
        String shortName = type.getShortName();

        //
        // Ok, we have a valid plugin class, so we can look for other things
        // and store the info
        //

        String uiSnippetPath;
        String beanName;
        CustomUi customUI = type.getCustomUi();
        if (customUI != null) {
            uiSnippetPath = customUI.getUiSnippetName();

            try {
                URL uiSnippetUrl = env.getPluginClassLoader().getResource(uiSnippetPath);
                log.info("UI snipped for " + shortName + " is at " + uiSnippetUrl);
            }
            catch (Exception e) {
                log.error("No valid ui snippet provided, but <custom-ui> given for sender plugin " + shortName +
                        "Error is " + e.getMessage());
                return false;
            }

            // Get the backing bean class
            className = customUI.getBackingBeanClass();
            if (!className.contains(".")) {
                className = type.getPackage() + "." + className;
            }
            try {
                Class.forName(className, true, env.getPluginClassLoader());
            }
            catch (Throwable t) {
                log.error("Backing bean " + className + " not found for plugin " + shortName);
                return false;
            }

            beanName = customUI.getBackingBeanName();

            if (beanName == null || beanName.length() == 0) {
                log.error("Must provide a <backing-bean-name> for " + className + " in plugin " + shortName);
                return false;
            }
        }
        return true;
    }
}
