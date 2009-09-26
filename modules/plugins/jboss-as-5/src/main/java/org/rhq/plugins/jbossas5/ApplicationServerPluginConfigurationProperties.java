/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5;

/**
 * @author Ian Springer
 */
public class ApplicationServerPluginConfigurationProperties {
    static final String SERVER_NAME = "serverName";
    static final String NAMING_URL = "namingURL";
    static final String PRINCIPAL = "principal";
    static final String CREDENTIALS = "credentials";
    static final String HOME_DIR = "homeDir";
    static final String SERVER_HOME_DIR = "serverHomeDir";
    static final String JAVA_HOME = "javaHome";
    static final String BIND_ADDRESS = "bindAddress";
    static final String START_SCRIPT_CONFIG_PROP = "startScript";
    static final String SHUTDOWN_SCRIPT_CONFIG_PROP = "shutdownScript";
    static final String SHUTDOWN_MBEAN_CONFIG_PROP = "shutdownMBeanName";
    static final String SHUTDOWN_MBEAN_OPERATION_CONFIG_PROP = "shutdownMBeanOperation";
    static final String SHUTDOWN_METHOD_CONFIG_PROP = "shutdownMethod";
    static final String SCRIPT_PREFIX_CONFIG_PROP = "scriptPrefix";
    static final String AVAIL_CHECK_PERIOD_CONFIG_PROP = "availabilityCheckPeriod";

    private ApplicationServerPluginConfigurationProperties() {
    }
}
