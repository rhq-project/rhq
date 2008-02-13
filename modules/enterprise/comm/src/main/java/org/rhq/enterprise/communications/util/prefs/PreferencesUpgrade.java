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

import java.util.List;
import java.util.prefs.Preferences;
import mazz.i18n.Logger;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * Performs an upgrade of a set of preferences from one configuration schema version to another (where a set of
 * preferences is tagged by what is called a "configuration schema version" or simply a "configuration version").
 *
 * <p>Subclasses need only implement {@link #getConfigurationSchemaVersionPreference()} to indicate the name of the
 * preference key that contains the configuration schema version number for the preferences to be upgraded and to ensure
 * they override the constructor {@link PreferencesUpgrade#PreferencesUpgrade(List)} passing in the appropriate upgrade
 * steps. Calling {@link #upgrade(Preferences, int)} on instances of a subclass will use those installed
 * {@link PreferencesUpgradeStep upgrade steps} to perform the actual upgrade.</p>
 *
 * @author John Mazzitelli
 */
public abstract class PreferencesUpgrade {
    /**
     * Logger
     */
    private static final Logger LOG = CommI18NFactory.getLogger(PreferencesUpgrade.class);

    /**
     * The list of upgrade steps that perform the version-to-version upgrades.
     */
    private List<PreferencesUpgradeStep> m_upgradeSteps;

    /**
     * Constructor for {@link PreferencesUpgrade} given a set of upgrade step instructions for each known, supported
     * configuration schema version. The given list must be ordered from the lowest supported configuration version to
     * the latest supported version.
     *
     * @param upgrade_steps the list of upgrade steps that are used to perform the upgrades from one version to another
     */
    public PreferencesUpgrade(List<PreferencesUpgradeStep> upgrade_steps) {
        m_upgradeSteps = upgrade_steps;
    }

    /**
     * This will upgrade the given set of preferences to the given configuration schema version. If the preferences are
     * at that version or higher, this method does nothing.
     *
     * @param preferences        the set of preferences to upgrade
     * @param upgrade_to_version the configuration version to upgrade the preferences to
     */
    public void upgrade(Preferences preferences, int upgrade_to_version) {
        String version_pref_name = getConfigurationSchemaVersionPreference();
        int current_version = getConfigurationSchemaVersion(preferences);

        if (current_version < upgrade_to_version) {
            for (PreferencesUpgradeStep step : m_upgradeSteps) {
                int step_version = step.getSupportedConfigurationSchemaVersion();

                if (current_version < step_version) {
                    LOG.debug(CommI18NResourceKeys.CONFIG_SCHEMA_VERSION_STEP_UPGRADING, version_pref_name,
                        current_version, step_version);

                    // upgrade to the step version and once that's done, bump up the version number
                    step.upgrade(preferences);
                    setConfigurationSchemaVersion(preferences, step_version);
                    current_version = step_version;

                    LOG.debug(CommI18NResourceKeys.CONFIG_SCHEMA_VERSION_STEP_UPGRADED, version_pref_name,
                        current_version);
                }

                if (current_version >= upgrade_to_version) {
                    LOG.info(CommI18NResourceKeys.CONFIG_SCHEMA_VERSION_UPGRADED, version_pref_name, current_version);
                    break;
                }
            }

            if (current_version < upgrade_to_version) {
                LOG.error(CommI18NResourceKeys.CONFIG_SCHEMA_VERSION_NOT_UPTODATE, version_pref_name,
                    upgrade_to_version, current_version);
            }
        } else {
            LOG.debug(CommI18NResourceKeys.CONFIG_SCHEMA_VERSION_UPTODATE, version_pref_name, current_version);
        }

        return;
    }

    /**
     * Given a set of preferences, this will ask for its configuration schema version. If the version could not be
     * determined, 0 is returned.
     *
     * @param  preferences the set of preferences whose version is to be returned
     *
     * @return the version that the given set of preferences conforms to or 0 if unknown
     */
    public int getConfigurationSchemaVersion(Preferences preferences) {
        return preferences.getInt(getConfigurationSchemaVersionPreference(), 0);
    }

    /**
     * Given a set of preferences, this will set its configuration schema version to the given version number.
     *
     * @param preferences the set of preferences whose version is to be set
     * @param version     the new version of the preferences
     */
    public void setConfigurationSchemaVersion(Preferences preferences, int version) {
        preferences.putInt(getConfigurationSchemaVersionPreference(), version);
    }

    /**
     * Returns the name of the preference whose value is the configuration schema version of the entire set of
     * preferences.
     *
     * @return the name of the preference whose value is the configuration version integer
     */
    public abstract String getConfigurationSchemaVersionPreference();
}