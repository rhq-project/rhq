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
 * </p>
 * Null values of the properties mean no change, non-null values represent
 * the desired new values.
 * </p>
 * Configuration updates are limited to only changing values for existing
 * properties. The Configuration must still reflect the types configuration
 * definition. Also, updates must be judicious as config values can also
 * be updated by users.
 *
 * @author Lukas Krejci
 */
public class ResourceUpgradeReport implements Serializable {

    private static final long serialVersionUID = 3L;

    private String newResourceKey;
    private String newName;
    // version changes are typically handled differently, but in certain cases may be done here.
    private String newVersion;
    private String newDescription;
    // Plugin configuration changes must still conform to the configuration definition.
    private Configuration newPluginConfiguration;

    // Is resource config update useful?  Wouldn't resource config change discovery handle this?
    //    private Configuration newResourceConfiguration;

    // In some cases assume the plugin knows best, and let it force upgrade of what we call "generic" resource
    // properties (name, version, description).  If set to true by the plugin code the server will obey,
    // regardless of the value of SystemSetting.ALLOW_RESOURCE_GENERIC_PROPERTIES_UPGRADE.
    private boolean forceGenericPropertyUpgrade = false;

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

    public String getNewVersion() {
        return newVersion;
    }

    public void setNewVersion(String newVersion) {
        this.newVersion = newVersion;
    }

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

    public Configuration getNewPluginConfiguration() {
        return newPluginConfiguration;
    }

    /**
     * See class javadoc for restrictions.
     */
    public void setNewPluginConfiguration(Configuration newPluginConfiguration) {
        this.newPluginConfiguration = newPluginConfiguration;
    }

    public boolean isForceGenericPropertyUpgrade() {
        return forceGenericPropertyUpgrade;
    }

    public void setForceGenericPropertyUpgrade(boolean forceGenericPropertyUpgrade) {
        this.forceGenericPropertyUpgrade = forceGenericPropertyUpgrade;
    }

    public boolean hasSomethingToUpgrade() {
        return newResourceKey != null || newName != null || newVersion != null || newDescription != null
            || newPluginConfiguration != null;
    }

    @Override
    public String toString() {
        return "ResourceUpgradeReport [newResourceKey=" + newResourceKey + ", newName=" + newName + ", newVersion="
            + newVersion + ", newDescription=" + newDescription + ", newPluginConfiguration=" + newPluginConfiguration
            + ", forceGenericPropertyUpgrade=" + forceGenericPropertyUpgrade + "]";
    }

}
