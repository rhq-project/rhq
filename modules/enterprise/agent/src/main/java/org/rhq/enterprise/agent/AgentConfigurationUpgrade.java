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
package org.rhq.enterprise.agent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import mazz.i18n.Logger;

import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.obfuscation.ObfuscatedPreferences;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;
import org.rhq.enterprise.communications.ServiceContainerConfigurationConstants;
import org.rhq.enterprise.communications.util.prefs.PreferencesUpgrade;
import org.rhq.enterprise.communications.util.prefs.PreferencesUpgradeStep;

/**
 * Upgrades the configuration of the agent preferences.
 *
 * @author John Mazzitelli
 */
public class AgentConfigurationUpgrade extends PreferencesUpgrade {
    private static final Logger LOG = AgentI18NFactory.getLogger(AgentConfigurationUpgrade.class);

    /**
     * This is a convenience method that upgrades the given agent preferences to the latest configuration schema
     * version.
     *
     * @param preferences the preferences to upgrade
     */
    public static void upgradeToLatest(Preferences preferences) {
        new AgentConfigurationUpgrade().upgrade(preferences, AgentConfigurationConstants.CURRENT_CONFIG_SCHEMA_VERSION);
    }

    /**
     * Constructor for {@link AgentConfigurationUpgrade}.
     */
    public AgentConfigurationUpgrade() {
        super(getSteps());
    }

    public String getConfigurationSchemaVersionPreference() {
        return AgentConfigurationConstants.CONFIG_SCHEMA_VERSION;
    }

    private static List<PreferencesUpgradeStep> getSteps() {
        List<PreferencesUpgradeStep> list = new ArrayList<PreferencesUpgradeStep>();
        list.add(new Step1to2()); // goes from v1 to v2
        list.add(new Step2to3()); // goes from v2 to v3
        list.add(new Step3to4()); // goes from v3 to v4
        list.add(new Step4to5()); // goes from v4 to v5
        list.add(new Step5to6()); // goes from v5 to v6
        list.add(new Step6to7()); // goes from v6 to v7
        list.add(new Step7to8()); // goes from v7 to v8
        list.add(new Step8to9()); // goes from v8 to v9
        return list;
    }

    static class Step1to2 extends PreferencesUpgradeStep {
        public int getSupportedConfigurationSchemaVersion() {
            return 2;
        }

        public void upgrade(Preferences preferences) {
            // add the switchover check interval - just set it to its hardcoded default
            preferences.putLong(AgentConfigurationConstants.PRIMARY_SERVER_SWITCHOVER_CHECK_INTERVAL_MSECS,
                AgentConfigurationConstants.DEFAULT_PRIMARY_SERVER_SWITCHOVER_CHECK_INTERVAL_MSECS);
        }
    }

    static class Step2to3 extends PreferencesUpgradeStep {
        public int getSupportedConfigurationSchemaVersion() {
            return 3;
        }

        public void upgrade(Preferences preferences) {
            // add the vm health check settings - just set them to their hardcoded defaults
            preferences.putLong(AgentConfigurationConstants.VM_HEALTH_CHECK_INTERVAL_MSECS,
                AgentConfigurationConstants.DEFAULT_VM_HEALTH_CHECK_INTERVAL_MSECS);
            preferences.putFloat(AgentConfigurationConstants.VM_HEALTH_CHECK_LOW_HEAP_MEM_THRESHOLD,
                AgentConfigurationConstants.DEFAULT_VM_HEALTH_CHECK_LOW_HEAP_MEM_THRESHOLD);
            preferences.putFloat(AgentConfigurationConstants.VM_HEALTH_CHECK_LOW_NONHEAP_MEM_THRESHOLD,
                AgentConfigurationConstants.DEFAULT_VM_HEALTH_CHECK_LOW_NONHEAP_MEM_THRESHOLD);

            // add the agent update enable flag
            preferences.putBoolean(AgentConfigurationConstants.AGENT_UPDATE_ENABLED,
                AgentConfigurationConstants.DEFAULT_AGENT_UPDATE_ENABLED);
        }
    }

    static class Step3to4 extends PreferencesUpgradeStep {
        public int getSupportedConfigurationSchemaVersion() {
            return 4;
        }

        public void upgrade(Preferences preferences) {
            // availability reports have a new default, make sure it isn't lower than the new default
            long val;
            val = preferences.getLong(AgentConfigurationConstants.PLUGINS_AVAILABILITY_SCAN_PERIOD, Long.MAX_VALUE);
            if (val < AgentConfigurationConstants.DEFAULT_PLUGINS_AVAILABILITY_SCAN_PERIOD) {
                preferences.putLong(AgentConfigurationConstants.PLUGINS_AVAILABILITY_SCAN_PERIOD,
                    AgentConfigurationConstants.DEFAULT_PLUGINS_AVAILABILITY_SCAN_PERIOD);
            }
        }
    }

    static class Step4to5 extends PreferencesUpgradeStep {
        public int getSupportedConfigurationSchemaVersion() {
            return 5;
        }

        public void upgrade(Preferences preferences) {
            // This new schema version added a new preprocessor to support proper serialization to/from server.
            // If the preprocessor value is null, then don't do anything (we are probably running inside of tests).
            // A "real" agent from a previous schema version already had one preprocessor, we just need to add another.
            String newPreprocessor = ExternalizableStrategyCommandPreprocessor.class.getName();
            String val = preferences.get(AgentConfigurationConstants.CLIENT_SENDER_COMMAND_PREPROCESSORS, null);
            if (val != null && !val.contains(newPreprocessor)) {
                val = val + ':' + newPreprocessor;
                preferences.put(AgentConfigurationConstants.CLIENT_SENDER_COMMAND_PREPROCESSORS, val);
            }
        }
    }

    static class Step5to6 extends PreferencesUpgradeStep {
        public int getSupportedConfigurationSchemaVersion() {
            return 6;
        }

        public void upgrade(Preferences preferences) {
            // This new schema version added rhq.server.alias - to support backwards compatibility, we want
            // to set this to "rhqserver" which will cause the same behavior that was exhibited in previous versions
            preferences.put(AgentConfigurationConstants.SERVER_ALIAS, "rhqserver");
        }
    }

    static class Step6to7 extends PreferencesUpgradeStep {
        public int getSupportedConfigurationSchemaVersion() {
            return 7;
        }

        public void upgrade(Preferences preferences) {
            // This new schema version indicates when we changed the default locations for our keystore/truststore
            // files. Before if these comm settings weren't set, we assumed a default of "data" directory, but now
            // we assume a default of "conf" directory (if that directory exists). See BZ 951382.
            File confDir = new File("conf");
            if (!confDir.exists()) {
                return; // conf/ doesn't exist (perhaps we are running in a test?) - do nothing and just fallback to the standard defaults
            }

            String dataDir = preferences.get(AgentConfigurationConstants.DATA_DIRECTORY,
                AgentConfigurationConstants.DEFAULT_DATA_DIRECTORY);

            String prefNamesFileNames[][] = {
                { ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_KEYSTORE_FILE,
                    ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_SECURITY_KEYSTORE_FILE_NAME },
                { ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_TRUSTSTORE_FILE,
                    ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_SECURITY_TRUSTSTORE_FILE_NAME },
                { AgentConfigurationConstants.CLIENT_SENDER_SECURITY_KEYSTORE_FILE,
                    AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_SECURITY_KEYSTORE_FILE_NAME },
                { AgentConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_FILE,
                    AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_SECURITY_TRUSTSTORE_FILE_NAME } };

            for (String[] prefNameFileName : prefNamesFileNames) {
                String value = preferences.get(prefNameFileName[0], null);
                if (value == null) {
                    File newFile = new File(confDir, prefNameFileName[1]);
                    value = newFile.getAbsolutePath();
                    preferences.put(prefNameFileName[0], value);

                    File oldFile = new File(dataDir, prefNameFileName[1]);
                    if (oldFile.exists()) {
                        try {
                            FileUtil.copyFile(oldFile, newFile);
                            oldFile.delete();
                        } catch (Exception e) {
                            LOG.error(e, AgentI18NResourceKeys.CERT_FILE_COPY_ERROR, oldFile, newFile);
                        }
                    }
                    LOG.debug(AgentI18NResourceKeys.CERT_FILE_LOCATION, prefNameFileName[0], value);
                }
            }

            return;
        }
    }

    static class Step7to8 extends PreferencesUpgradeStep {
        public int getSupportedConfigurationSchemaVersion() {
            return 8;
        }

        public void upgrade(Preferences preferences) {
            // This new schema version added support for the new command authenticator so the agent authenticates servers.
            // Note that to support tests, we only upgrade the pref if it wasn't set (it should not have been set in production
            // builds - it was only ever set in tests in older versions).
            if (null == preferences.get(ServiceContainerConfigurationConstants.COMMAND_AUTHENTICATOR, null)) {
                preferences.put(ServiceContainerConfigurationConstants.COMMAND_AUTHENTICATOR,
                    SecurityTokenCommandAuthenticator.class.getName());
            }
        }
    }

    static class Step8to9 extends PreferencesUpgradeStep {
        public int getSupportedConfigurationSchemaVersion() {
            return 9;
        }

        public void upgrade(Preferences preferences) {
            @SuppressWarnings("unused")
            ObfuscatedPreferences obfuscatedPreferences = new ObfuscatedPreferences(preferences,
                AgentConfigurationConstants.class);
        }
    }
}