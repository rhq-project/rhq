/*
* RHQ Management Platform
* Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.core.pluginapi.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.configuration.ListPropertySimpleWrapper;
import org.rhq.core.pluginapi.configuration.MapPropertySimpleWrapper;

/**
 * @author Ian Springer
 */
public class StartScriptConfiguration {

    public static final String START_SCRIPT_PROP = "startScript";
    public static final String START_SCRIPT_PREFIX_PROP = "startScriptPrefix";
    public static final String START_SCRIPT_ENV_PROP = "startScriptEnv";
    public static final String START_SCRIPT_ARGS_PROP = "startScriptArgs";

    private final Log log = LogFactory.getLog(this.getClass());

    private Configuration pluginConfig;

    public StartScriptConfiguration(Configuration pluginConfig) {
        if (pluginConfig == null) {
            throw new IllegalArgumentException("'pluginConfig' parameter is null.");
        }
        this.pluginConfig = pluginConfig;
    }

    @Nullable
    public File getStartScript() {
        String startScript = this.pluginConfig.getSimpleValue(START_SCRIPT_PROP);
        return (startScript != null) ? new File(startScript) : null;
    }

    public void setStartScript(File startScript) {
        PropertySimple prop = this.pluginConfig.getSimple(START_SCRIPT_PROP);
        if (prop == null) {
            prop = new PropertySimple(START_SCRIPT_PROP, null);
            this.pluginConfig.put(prop);
        }
        prop.setValue(startScript);
    }

    @Nullable
    public String getStartScriptPrefix() {
        return this.pluginConfig.getSimpleValue(START_SCRIPT_PREFIX_PROP);
    }

    public void setStartScriptPrefix(String startScriptPrefix) {
        this.pluginConfig.setSimpleValue(START_SCRIPT_PREFIX_PROP, startScriptPrefix);
    }

    @NotNull
    public Map<String, String> getStartScriptEnv() {
        PropertySimple prop = this.pluginConfig.getSimple(START_SCRIPT_ENV_PROP);
        Map<String, String> map = (prop != null) ? new MapPropertySimpleWrapper(prop).getValue()
            : new HashMap<String, String>();
        return map;
    }

    public void setStartScriptEnv(Map<String, String> startScriptEnv) {
        PropertySimple prop = this.pluginConfig.getSimple(START_SCRIPT_ENV_PROP);
        if (prop == null) {
            prop = new PropertySimple(START_SCRIPT_ENV_PROP, null);
            this.pluginConfig.put(prop);
        }

        try {
            new MapPropertySimpleWrapper(prop).setValue(startScriptEnv);
        } catch (IllegalArgumentException e) {
            setPropertyError(startScriptEnv, prop);
        }
    }

    @NotNull
    public List<String> getStartScriptArgs() {
        PropertySimple prop = this.pluginConfig.getSimple(START_SCRIPT_ARGS_PROP);
        List<String> list = (prop != null) ? new ArgsPropertySimpleWrapper(prop).getValue() : new ArrayList<String>();
        return list;
    }

    public void setStartScriptArgs(List<String> startScriptArgs) {
        PropertySimple prop = this.pluginConfig.getSimple(START_SCRIPT_ARGS_PROP);
        if (prop == null) {
            prop = new PropertySimple(START_SCRIPT_ARGS_PROP, null);
            this.pluginConfig.put(prop);
        }

        try {
            new ArgsPropertySimpleWrapper(prop).setValue(startScriptArgs);
        } catch (IllegalArgumentException e) {
            setPropertyError(startScriptArgs, prop);
        }
    }

    private void setPropertyError(Object rawPropValue, PropertySimple prop) {
        prop.setErrorMessage("The discovered value of this property was longer than "
            + PropertySimple.MAX_VALUE_LENGTH
            + " characters (the maximum length of an RHQ property). It has been left unset.  Please see the Agent log for the full value.");
        log.warn("The discovered value for property [" + prop.getName()
            + "] was too long and was left unset. It is logged here for reference: " + rawPropValue);
    }

    public Configuration getPluginConfig() {
        return pluginConfig;
    }

    private static class ArgsPropertySimpleWrapper extends ListPropertySimpleWrapper {

        public ArgsPropertySimpleWrapper(PropertySimple prop) {
            super(prop);
        }

        // For better readability, put space delimited option values on same line. For example:
        //   -x some value
        // as opposed to:
        //   -x
        //   some value
        //
        @Override
        public void setValue(List list) {
            String stringValue;
            if (list != null && !list.isEmpty()) {
                StringBuilder buffer = new StringBuilder(list.get(0).toString());
                for (int i = 1; i < list.size(); ++i) {
                    String arg = list.get(i).toString();
                    // put options on new line, keep space delimited options on same line  
                    buffer.append(arg.startsWith("-") ? '\n' : ' ').append(arg);
                }
                stringValue = buffer.toString();
            } else {
                stringValue = null;
            }

            // If the value is too long then don't store it, because it will likely invalidate the Map on the way back out.
            if (null != stringValue && stringValue.length() > PropertySimple.MAX_VALUE_LENGTH) {
                throw new IllegalArgumentException(stringValue);
            }

            this.prop.setStringValue(stringValue);
        }

        // Ensure one arg per List entry, split up space delimited options with value on same line. This
        // protects users that hand enter in this fashion, and also values entered with the above setter.
        @Override
        public List<String> getValue() {
            List<String> list = new ArrayList<String>();

            String stringValue = this.prop.getStringValue();
            if (stringValue != null) {
                String[] lines = stringValue.split("\n+");
                for (String line : lines) {
                    String element = line.trim();
                    //element = replacePropertyPatterns(element); // TODO
                    // separate an option and its value if on one line
                    if (element.startsWith("-")) {
                        boolean added = false;
                        for (int i = 1, len = element.length(); (i < len); ++i) {
                            char ch = element.charAt(i);
                            if (ch == ' ' || ch == '\t') {
                                String option = element.substring(0, i);
                                String value = element.substring(i).trim();
                                list.add(option);
                                if (!value.isEmpty()) {
                                    list.add(value);
                                }
                                added = true;
                                break;
                            }
                        }
                        if (!added) {
                            list.add(element);
                        }
                    } else {
                        list.add(element);
                    }
                }
            }

            return list;
        }
    }

}