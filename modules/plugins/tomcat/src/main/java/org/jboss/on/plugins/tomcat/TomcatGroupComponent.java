/*
 * Jopr Management Platform
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

package org.jboss.on.plugins.tomcat;

import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * Manage a Tomcat Group
 * 
 * @author Jay Shaughnessy
 */
public class TomcatGroupComponent extends MBeanResourceComponent<TomcatUserDatabaseComponent> implements DeleteResourceFacet {

    public static final String CONFIG_DESCRIPTION = "description";
    public static final String CONFIG_GROUP_NAME = "groupname";
    public static final String CONFIG_ROLES = "roles";
    public static final String PLUGIN_CONFIG_NAME = "name";
    public static final String RESOURCE_TYPE_NAME = "Tomcat Group";

    private static final Pattern PATTERN_ROLE_NAME = Pattern.compile(TomcatRoleComponent.CONFIG_ROLE_NAME + "=(.*),");

    /**
     * Roles and Groups are handled as comma delimited lists and offered up as a String array of object names by the MBean 
     */
    @Override
    public Configuration loadResourceConfiguration() {
        Configuration configuration = super.loadResourceConfiguration();
        try {
            resetConfig(CONFIG_ROLES, PATTERN_ROLE_NAME, configuration);
        } catch (Exception e) {
            log.error("Failed to reset role property value", e);
        }

        return configuration;
    }

    // Reset the String provided by the MBean with a more user friendly longString paired down to just the relevant name
    private void resetConfig(String property, Pattern pattern, Configuration configuration) {
        EmsAttribute attribute = getEmsBean().getAttribute(property);
        Object valueObject = attribute.refresh();
        String[] vals = (String[]) valueObject;
        if (vals.length > 0) {
            String delim = "";
            StringBuilder sb = new StringBuilder();
            Matcher matcher = pattern.matcher("");
            for (String val : vals) {
                matcher.reset(val);
                matcher.find();
                sb.append(delim);
                sb.append(matcher.group(1));
                delim = "\n";
            }
            configuration.put(new PropertySimple(property, sb.toString()));
        } else {
            configuration.put(new PropertySimple(property, null));
        }
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        // updating Role membership is done via MBean operation, not manipulation of the attribute

        Configuration reportConfiguration = report.getConfiguration();
        // reserve the new role settings 
        PropertySimple newRoles = reportConfiguration.getSimple(CONFIG_ROLES);
        // get the current role settings
        resetConfig(CONFIG_ROLES, PATTERN_ROLE_NAME, reportConfiguration);
        PropertySimple currentRoles = reportConfiguration.getSimple(CONFIG_ROLES);
        // remove the role config from the report so they are ignored by the mbean config processing
        reportConfiguration.remove(CONFIG_ROLES);

        // perform standard processing on remaining config
        super.updateResourceConfiguration(report);

        // add back the role config so the report is complete
        reportConfiguration.put(newRoles);

        // if the mbean update failed, return now
        if (ConfigurationUpdateStatus.SUCCESS != report.getStatus()) {
            return;
        }

        // try updating the role settings
        try {
            consolidateSettings(newRoles, currentRoles, "addRole", "removeRole", "role");
        } catch (Exception e) {
            newRoles.setErrorMessageFromThrowable(e);
            report.setErrorMessage("Failed setting resource configuration - see property error messages for details");
            log.info("Failure setting Tomcat User Roles configuration value", e);
            return;
        }

        // If all went well, persist the changes to the Tomcat user Database
        try {
            this.getResourceContext().getParentResourceComponent().save();
        } catch (Exception e) {
            report
                .setErrorMessage("Failed to persist configuration change.  Changes will not survive Tomcat restart unless a successful Save operation is performed.");
        }
    }

    private void consolidateSettings(PropertySimple newVals, PropertySimple currentVals, String addOp, String removeOp, String arg) throws Exception {

        // add new values not in the current settings
        String currentValsLongString = currentVals.getStringValue();
        String newValsLongString = newVals.getStringValue();
        StringTokenizer tokenizer = null;
        Configuration opConfig = null;

        if (null != newValsLongString) {
            tokenizer = new StringTokenizer(newValsLongString, "\n");
            opConfig = new Configuration();
            while (tokenizer.hasMoreTokens()) {
                String newVal = tokenizer.nextToken().trim();
                if ((null == currentValsLongString) || !currentValsLongString.contains(newVal)) {
                    opConfig.put(new PropertySimple(arg, newVal));
                    try {
                        invokeOperation(addOp, opConfig);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Could not add " + arg + "=" + newVal + ". Please check spelling/existence.");
                    }
                }
            }
        }

        if (null != currentValsLongString) {
            tokenizer = new StringTokenizer(currentValsLongString, "\n");
            while (tokenizer.hasMoreTokens()) {
                String currentVal = tokenizer.nextToken().trim();
                if ((null == newValsLongString) || !newValsLongString.contains(currentVal)) {
                    opConfig.put(new PropertySimple(arg, currentVal));
                    try {
                        invokeOperation(removeOp, opConfig);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Could not remove " + arg + "=" + currentVal + ". Please check spelling/existence.");
                    }
                }
            }
        }
    }

    public void deleteResource() throws Exception {
        Configuration opConfig = new Configuration();
        ResourceContext<TomcatUserDatabaseComponent> resourceContext = getResourceContext();
        // We must strip the quotes off of name for the operation parameter
        PropertySimple nameProperty = resourceContext.getPluginConfiguration().getSimple(PLUGIN_CONFIG_NAME);
        String name = nameProperty.getStringValue();
        nameProperty = new PropertySimple(CONFIG_GROUP_NAME, name.substring(1, name.length() - 1));
        opConfig.put(nameProperty);
        resourceContext.getParentResourceComponent().invokeOperation("removeGroup", opConfig);
    }

}
