/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7;

/**
 * Various definitions for the operation modes of AS7 (HOST is strictly no mode, but fits here nicely)
 * @author Heiko W. Rupp
 */
public enum AS7Mode {

    STANDALONE("standalone.xml", "standalone", "--server-config", "bin/standalone.sh","config"),
    DOMAIN("domain.xml", "domain", "--domain-config", "bin/domain.sh","domainConfig"),
    HOST("host.xml", "domain", "--host-config", "bin/domain.sh","hostConfig");

    private String defaultXmlFile;
    private String baseDir;
    private String configArg;
    private String startScript;
    private String configPropertyName;

    private AS7Mode(String defaultXmlFile, String baseDir, String configArg, String startScript, String configPropertyName) {
        this.defaultXmlFile = defaultXmlFile;
        this.baseDir = baseDir;
        this.configArg = configArg;
        this.startScript = startScript;
        this.configPropertyName = configPropertyName;
    }

    public String getDefaultXmlFile() {
        return defaultXmlFile;
    }

    public String getBaseDir() {
        return baseDir;
    }

    public String getConfigArg() {
        return configArg;
    }

    public String getStartScript() {
        return startScript;
    }

    public String getConfigPropertyName() {
        return configPropertyName;
    }
}