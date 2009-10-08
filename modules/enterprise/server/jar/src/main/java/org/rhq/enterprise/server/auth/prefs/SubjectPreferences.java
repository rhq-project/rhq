package org.rhq.enterprise.server.auth.prefs;

import org.rhq.core.domain.auth.Subject;

public class SubjectPreferences extends SubjectPreferencesBase {

    public SubjectPreferences(Subject subject) {
        super(subject);
    }

    public static final String PREF_GROUP_CONFIG_TIMEOUT_PERIOD = ".group.configuration.timeout.period";

    public int getGroupConfigurationTimeoutPeriod() {
        return getIntPref(PREF_GROUP_CONFIG_TIMEOUT_PERIOD, 60);
    }

    public void setGroupConfigurationTimeoutPeriod(int period) {
        setPreference(PREF_GROUP_CONFIG_TIMEOUT_PERIOD, Integer.valueOf(period));
    }
}
