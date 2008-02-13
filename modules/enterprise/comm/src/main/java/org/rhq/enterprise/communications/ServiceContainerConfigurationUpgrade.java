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
package org.rhq.enterprise.communications;

import java.util.ArrayList;
import java.util.prefs.Preferences;
import org.rhq.enterprise.communications.util.prefs.PreferencesUpgrade;
import org.rhq.enterprise.communications.util.prefs.PreferencesUpgradeStep;

/**
 * Upgrades the configuration of the communications preferences.
 *
 * @author John Mazzitelli
 */
public class ServiceContainerConfigurationUpgrade extends PreferencesUpgrade {
    /**
     * This is a convenience method that upgrades the given communications preferences to the latest configuration
     * schema version.
     *
     * @param preferences the preferences to upgrade
     */
    public static void upgradeToLatest(Preferences preferences) {
        new ServiceContainerConfigurationUpgrade().upgrade(preferences,
            ServiceContainerConfigurationConstants.CURRENT_CONFIG_SCHEMA_VERSION);
    }

    /**
     * Constructor for {@link ServiceContainerConfigurationUpgrade}.
     */
    public ServiceContainerConfigurationUpgrade() {
        // we currently only support one version - there are no supported upgrades
        super(new ArrayList<PreferencesUpgradeStep>());
    }

    /**
     * @see PreferencesUpgrade#getConfigurationSchemaVersionPreference()
     */
    public String getConfigurationSchemaVersionPreference() {
        return ServiceContainerConfigurationConstants.CONFIG_SCHEMA_VERSION;
    }
}