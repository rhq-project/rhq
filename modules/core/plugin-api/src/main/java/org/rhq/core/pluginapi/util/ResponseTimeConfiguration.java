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
package org.rhq.core.pluginapi.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;

/**
 * @author Ian Springer
 */
public class ResponseTimeConfiguration {
    public static final String RESPONSE_TIME_LOG_FILE_CONFIG_PROP = "responseTimeLogFile";
    public static final String RESPONSE_TIME_URL_EXCLUDES_CONFIG_PROP = "responseTimeUrlExcludes";
    public static final String RESPONSE_TIME_URL_TRANSFORMS_CONFIG_PROP = "responseTimeUrlTransforms";

    private Configuration pluginConfig;

    public ResponseTimeConfiguration(Configuration pluginConfig) {
        this.pluginConfig = pluginConfig;
    }

    @Nullable
    public File getLogFile() {
        PropertySimple logFileProp = this.pluginConfig.getSimple(RESPONSE_TIME_LOG_FILE_CONFIG_PROP);
        File logFile;
        if ((logFileProp != null) && (logFileProp.getStringValue() != null)) {
            logFile = new File(logFileProp.getStringValue());
        } else {
            logFile = null;
        }

        return logFile;
    }

    @NotNull
    public List<Pattern> getExcludes() {
        List<Pattern> excludes = new ArrayList<Pattern>();
        PropertySimple excludesProp = this.pluginConfig.getSimple(RESPONSE_TIME_URL_EXCLUDES_CONFIG_PROP);
        if ((excludesProp != null) && (excludesProp.getStringValue() != null)) {
            StringTokenizer tokenizer = new StringTokenizer(excludesProp.getStringValue(), " ");
            while (tokenizer.hasMoreTokens()) {
                String regEx = tokenizer.nextToken();
                try {
                    Pattern exclude = Pattern.compile(regEx);
                    excludes.add(exclude);
                } catch (PatternSyntaxException e) {
                    throw new InvalidPluginConfigurationException("'" + RESPONSE_TIME_URL_EXCLUDES_CONFIG_PROP
                        + "' connection property contains an invalid exclude expression: " + regEx, e);
                }
            }
        }

        return excludes;
    }

    @NotNull
    public List<RegexSubstitution> getTransforms() {
        List<RegexSubstitution> transforms = new ArrayList<RegexSubstitution>();
        PropertySimple transformsProp = this.pluginConfig.getSimple(RESPONSE_TIME_URL_TRANSFORMS_CONFIG_PROP);
        if ((transformsProp != null) && (transformsProp.getStringValue() != null)) {
            StringTokenizer tokenizer = new StringTokenizer(transformsProp.getStringValue(), " ");
            while (tokenizer.hasMoreTokens()) {
                String value = tokenizer.nextToken();
                String delimiter = value.substring(0, 1); // first character is the delimiter (sed-style)
                StringTokenizer valueTokenizer = new StringTokenizer(value, delimiter);
                if (valueTokenizer.countTokens() != 2) {
                    throw new InvalidPluginConfigurationException("'" + RESPONSE_TIME_URL_TRANSFORMS_CONFIG_PROP
                        + "' connection property contains an invalid transform expression: " + value);
                }

                String regEx = valueTokenizer.nextToken();
                String replacement = valueTokenizer.nextToken();
                try {
                    Pattern pattern = Pattern.compile(regEx);
                    RegexSubstitution transform = new RegexSubstitution(pattern, replacement);
                    transforms.add(transform);
                } catch (PatternSyntaxException e) {
                    throw new InvalidPluginConfigurationException("'" + RESPONSE_TIME_URL_TRANSFORMS_CONFIG_PROP
                        + "' connection property contains an invalid transform expression: " + value, e);
                }
            }
        }

        return transforms;
    }
}