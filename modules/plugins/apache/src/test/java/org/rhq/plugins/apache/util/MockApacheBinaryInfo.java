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

package org.rhq.plugins.apache.util;

import java.util.HashSet;
import java.util.Set;

/**
 * Class used to represent the apache binary in the runtime configuration tests.
 *
 * @author Lukas Krejci
 */
public class MockApacheBinaryInfo extends ApacheBinaryInfo {

    private Set<String> defines = new HashSet<String>();
    private Set<String> modules = new HashSet<String>();
    private String configFile;
    private String binaryPath;
    private String built;
    private long lastModified;
    private String mpm;
    private String root;
    private String version;
    
    public MockApacheBinaryInfo() {
        super(null);
    }
    
    @Override
    public String getBinaryPath() {
        return binaryPath;
    }
    
    public void setBinaryPath(String path) {
        this.binaryPath = path;
    }
    
    @Override
    public String getBuilt() {
        return built;
    }
    
    public void setBuilt(String built) {
        this.built = built;
    }
    
    @Override
    public Set<String> getCompiledInDefines() {
        return defines;
    }
    
    public void setCompiledInDefines(Set<String> defines) {
        this.defines = defines;
    }
    
    @Override
    public Set<String> getCompiledInModules() {
        return modules;
    }
    
    public void setCompiledInModules(Set<String> modules) {
        this.modules = modules;
    }
    
    @Override
    public String getCtl() {
        return configFile;
    }
    
    public void setCtl(String ctl) {
        this.configFile = ctl;
    }
    
    @Override
    public long getLastModified() {
        return lastModified;
    }
    
    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
    
    @Override
    public String getMpm() {
        return mpm;
    }
    
    public void setMpm(String mpm) {
        this.mpm = mpm;
    }

    @Override
    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
