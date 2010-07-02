/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.core.domain.resource;

import java.io.Serializable;

import org.rhq.core.domain.configuration.Configuration;

/**
 * Represents the changes that should be applied to the existing resource 
 * in order to upgrade it to conform to the new requirements set by the 
 * changed resource component.
 * <p>
 * Null values of the properties mean no change, non-null values represent
 * the desired new values.
 * 
 * @author Lukas Krejci
 */
public class ResourceUpgradeReport implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private String newResourceKey;
    private String newName;
    
// version changes are handled differently.    
//    private String newVersion;
    
// upgrading configurations would have large consequences.
// it would be difficult to synchronize the upgrade process
// with the agent or server initiated configuration updates.
// It could also be difficult for the UI to reflect more complicated
// structural changes as currently, it is assumed that 
// the configurations are kept backwards compatible.
//    private Configuration newPluginConfiguration;
//    private Configuration newResourceConfiguration;
    private String newDescription;

    public ResourceUpgradeReport() {
        
    }

    public String getNewResourceKey() {
        return newResourceKey;
    }

    public void setNewResourceKey(String newResourceKey) {
        this.newResourceKey = newResourceKey;
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

//    public String getNewVersion() {
//        return newVersion;
//    }
//
//    public void setNewVersion(String newVersion) {
//        this.newVersion = newVersion;
//    }
//
//    public Configuration getNewPluginConfiguration() {
//        return newPluginConfiguration;
//    }
//
//    public void setNewPluginConfiguration(Configuration newPluginConfiguration) {
//        this.newPluginConfiguration = newPluginConfiguration;
//    }
//
//    public Configuration getNewResourceConfiguration() {
//        return newResourceConfiguration;
//    }
//
//    public void setNewResourceConfiguration(Configuration newResourceConfiguration) {
//        this.newResourceConfiguration = newResourceConfiguration;
//    }

    public String getNewDescription() {
        return newDescription;
    }

    public void setNewDescription(String newDescription) {
        this.newDescription = newDescription;
    }
    
    public boolean hasSomethingToUpgrade() {
        return newResourceKey != null ||
            newName != null ||
            newDescription != null;
    }
    
    public String toString() {
    	return "ResourceUpgradeReport[newResourceKey = '" + newResourceKey + "', newName = '" + newName + "', newDescription = '" + newDescription + "']";
    }
}
