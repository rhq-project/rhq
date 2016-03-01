/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.modules.plugins.wildfly10;

import java.io.File;

import org.rhq.core.pluginapi.util.CommandLineOption;

/**
 * Various definitions for the operation modes of AS7 (HOST is strictly not a mode, but fits here nicely)
 *
 * @author Heiko W. Rupp
 */
public enum AS7Mode {

    STANDALONE("standalone.xml", "standalone.xml", "standalone", "--server-config", "standalone", new CommandLineOption('c', "server-config"), "config-file", "jboss.server.default.config"),
    DOMAIN("domain.xml", "host.xml", "domain", "--domain-config", "domain", new CommandLineOption(null, "host-config"), "host-config-file", "jboss.host.default.config"),
    HOST("host.xml", null, "domain", "--host-config", "domain", null, null, null);

    private static final boolean OS_IS_WINDOWS = (File.separatorChar == '\\');
    private static final String SCRIPT_EXTENSION = (OS_IS_WINDOWS) ? "bat" : "sh";

    private String defaultXmlFile;
    private String defaultHostConfigFileName;
    private String defaultBaseDir;
    private String configArg;
    private String startScriptBaseName;
    private CommandLineOption hostConfigFileNameOption;
    private String hostConfigAttributeName;
    private String defaultHostConfigSystemPropertyName;

    private AS7Mode(String defaultXmlFile, String defaultHostConfigFileName, String defaultBaseDir, String configArg,
                    String startScriptBaseName, CommandLineOption hostConfigFileNameOption,
                    String hostConfigAttributeName, String defaultHostConfigSystemPropertyName) {
        this.defaultXmlFile = defaultXmlFile;
        this.defaultHostConfigFileName = defaultHostConfigFileName;
        this.defaultBaseDir = defaultBaseDir;
        this.configArg = configArg;
        this.startScriptBaseName = startScriptBaseName;
        this.hostConfigFileNameOption = hostConfigFileNameOption;
        this.hostConfigAttributeName = hostConfigAttributeName;
        this.defaultHostConfigSystemPropertyName = defaultHostConfigSystemPropertyName;
    }

    public String getDefaultXmlFile() {
        return defaultXmlFile;
    }

    public String getDefaultHostConfigFileName() {
        return defaultHostConfigFileName;
    }

    public String getDefaultBaseDir() {
        return defaultBaseDir;
    }

    public String getConfigArg() {
        return configArg;
    }

    public String getStartScriptFileName() {
        return startScriptBaseName + '.' + SCRIPT_EXTENSION;
    }
    public String getCliScriptFileName() {
        return "jboss-cli" + '.' + SCRIPT_EXTENSION;
    }

    public CommandLineOption getHostConfigFileNameOption() {
        return hostConfigFileNameOption;
    }

    public String getHostConfigAttributeName() {
        return hostConfigAttributeName;
    }

    public String getDefaultHostConfigSystemPropertyName() {
        return defaultHostConfigSystemPropertyName;
    }

}