/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.communications.util.prefs;

import java.util.prefs.Preferences;

/**
 * This object upgrades a set of preferences to a particular configuration schema version. Implementations only need to
 * upgrade a single version step - that is to say, if an upgrade step upgrades to the configuration schema version of
 * "5", it need only worry about upgrading preferences that are already at the version level of "4"
 *
 * @author John Mazzitelli
 */
public abstract class PreferencesUpgradeStep {
    /**
     * Returns the configuration schema version that this upgrade step supports; that is to say, this upgrade step
     * object will {@link #upgrade(Preferences) upgrade} a set of preferences to the version returned by this method.
     *
     * @return the version that this object will upgrade a set of preferences to
     */
    public abstract int getSupportedConfigurationSchemaVersion();

    /**
     * Upgrades the given set of preferences to the
     * {@link #getSupportedConfigurationSchemaVersion() supported configuration schema version}.
     *
     * @param preferences the preferences to upgrade
     */
    public abstract void upgrade(Preferences preferences);
}