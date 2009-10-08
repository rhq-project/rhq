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
package org.rhq.enterprise.communications.util;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.rhq.enterprise.communications.util.prefs.PreferencesUpgrade;
import org.rhq.enterprise.communications.util.prefs.PreferencesUpgradeStep;

/**
 * Tests ugprading preferences.
 *
 * @author John Mazzitelli
 */
@Test
public class PreferencesUpgradeTest {
    private static final String CONFIG_SCHEMA_VERSION_KEY_NAME = "test-schema-ver";
    private static final String KEY_NAME = "test-node";

    private Preferences m_testRoot;

    /**
     * Gets the root test node.
     *
     * @throws Exception
     */
    @BeforeMethod
    public void setUp() throws Exception {
        m_testRoot = Preferences.userRoot().node("upgrade-test");
        m_testRoot.clear();
    }

    /**
     * Clears the root test node.
     *
     * @throws Exception
     */
    @AfterMethod
    public void tearDown() throws Exception {
        m_testRoot.removeNode();
    }

    /**
     * Tests simple upgrade.
     *
     * @throws Exception
     */
    public void testUpgrade() throws Exception {
        ArrayList<PreferencesUpgradeStep> steps = new ArrayList<PreferencesUpgradeStep>();

        assert m_testRoot.getInt(CONFIG_SCHEMA_VERSION_KEY_NAME, -1) == -1;

        // with no steps, nothing is upgraded, version remains unset
        new Upgrade(steps).upgrade(m_testRoot, 2);
        assert m_testRoot.getInt(CONFIG_SCHEMA_VERSION_KEY_NAME, -1) == -1;

        // even with a version set, with no steps there is nothing to upgrade, so version goes unchanged
        m_testRoot.putInt(CONFIG_SCHEMA_VERSION_KEY_NAME, 0);
        new Upgrade(steps).upgrade(m_testRoot, 2);
        assert m_testRoot.getInt(CONFIG_SCHEMA_VERSION_KEY_NAME, 0) == 0;

        // add two upgrade steps, to version 1 and to version 2
        steps.add(new UpgradeStep(1));
        steps.add(new UpgradeStep(2));

        // show that nothing happens when version is same as the last step version
        m_testRoot.putInt(CONFIG_SCHEMA_VERSION_KEY_NAME, 2);
        new Upgrade(steps).upgrade(m_testRoot, 2);
        assert m_testRoot.getInt(CONFIG_SCHEMA_VERSION_KEY_NAME, -1) == 2; // stays at 2
        assert !keyExists(KEY_NAME);
        assert !keyExists(KEY_NAME + "1");
        assert !keyExists(KEY_NAME + "2");

        // show that nothing happens when version is higher than the last step version
        m_testRoot.putInt(CONFIG_SCHEMA_VERSION_KEY_NAME, 5);
        new Upgrade(steps).upgrade(m_testRoot, 2);
        assert m_testRoot.getInt(CONFIG_SCHEMA_VERSION_KEY_NAME, -1) == 5; // stays at 5
        assert !keyExists(KEY_NAME);
        assert !keyExists(KEY_NAME);
        assert !keyExists(KEY_NAME + "1");
        assert !keyExists(KEY_NAME + "2");

        // show that we can upgrade thru some but not all steps
        m_testRoot.putInt(CONFIG_SCHEMA_VERSION_KEY_NAME, 0);
        new Upgrade(steps).upgrade(m_testRoot, 1); // we can go to 2 but we only want to go to 1
        assert m_testRoot.getInt(CONFIG_SCHEMA_VERSION_KEY_NAME, -1) == 1; // upgraded to 1
        assert keyExists(KEY_NAME);
        assert keyExists(KEY_NAME + "1");
        assert !keyExists(KEY_NAME + "2");
        assert m_testRoot.get(KEY_NAME, "").equals("MODIFIED 1");
        assert m_testRoot.get(KEY_NAME + "1", "").equals("ADDED 1");

        // show that we can upgrade to the highest known version
        m_testRoot.clear();
        assert !keyExists(KEY_NAME); // just me being paranoid - makes sure we cleared the previous test
        m_testRoot.putInt(CONFIG_SCHEMA_VERSION_KEY_NAME, 0);
        new Upgrade(steps).upgrade(m_testRoot, 2);
        assert m_testRoot.getInt(CONFIG_SCHEMA_VERSION_KEY_NAME, -1) == 2; // upgraded to 2
        assert keyExists(KEY_NAME);
        assert keyExists(KEY_NAME + "1");
        assert keyExists(KEY_NAME + "2");
        assert m_testRoot.get(KEY_NAME, "").equals("MODIFIED 2");
        assert m_testRoot.get(KEY_NAME + "1", "").equals("ADDED 1");
        assert m_testRoot.get(KEY_NAME + "2", "").equals("ADDED 2");

        // just shows a bunch of steps to confirm we can do more than 2
        // and for giggles, show that we can skip version numbers
        steps.add(new UpgradeStep(2));
        steps.add(new UpgradeStep(3));
        steps.add(new UpgradeStep(5)); // skips 4
        steps.add(new UpgradeStep(6));
        steps.add(new UpgradeStep(7));

        m_testRoot.clear();
        assert !keyExists(KEY_NAME); // just me being paranoid - makes sure we cleared the previous test
        m_testRoot.putInt(CONFIG_SCHEMA_VERSION_KEY_NAME, 0);
        new Upgrade(steps).upgrade(m_testRoot, 7);
        assert m_testRoot.getInt(CONFIG_SCHEMA_VERSION_KEY_NAME, -1) == 7; // upgraded to 7
        assert keyExists(KEY_NAME);
        assert keyExists(KEY_NAME + "1");
        assert keyExists(KEY_NAME + "2");
        assert keyExists(KEY_NAME + "3");
        assert !keyExists(KEY_NAME + "4"); // notice we don't have this one since we stepped over 4
        assert keyExists(KEY_NAME + "5");
        assert keyExists(KEY_NAME + "6");
        assert keyExists(KEY_NAME + "7");
        assert m_testRoot.get(KEY_NAME, "").equals("MODIFIED 7");
        assert m_testRoot.get(KEY_NAME + "1", "").equals("ADDED 1");
        assert m_testRoot.get(KEY_NAME + "2", "").equals("ADDED 2");
        assert m_testRoot.get(KEY_NAME + "3", "").equals("ADDED 3");
        assert m_testRoot.get(KEY_NAME + "5", "").equals("ADDED 5");
        assert m_testRoot.get(KEY_NAME + "6", "").equals("ADDED 6");
        assert m_testRoot.get(KEY_NAME + "7", "").equals("ADDED 7");

        // show that we can upgrade to a number larger than the last step
        m_testRoot.clear();
        assert !keyExists(KEY_NAME); // just me being paranoid - makes sure we cleared the previous test
        m_testRoot.putInt(CONFIG_SCHEMA_VERSION_KEY_NAME, 0);
        new Upgrade(steps).upgrade(m_testRoot, 9); // we ask to go to 9 but our steps only go up to 7
        assert m_testRoot.getInt(CONFIG_SCHEMA_VERSION_KEY_NAME, -1) == 7; // upgraded to 7
        assert keyExists(KEY_NAME);
        assert keyExists(KEY_NAME + "1");
        assert keyExists(KEY_NAME + "2");
        assert keyExists(KEY_NAME + "3");
        assert !keyExists(KEY_NAME + "4"); // notice we don't have this one since we stepped over 4
        assert keyExists(KEY_NAME + "5");
        assert keyExists(KEY_NAME + "6");
        assert keyExists(KEY_NAME + "7");
        assert m_testRoot.get(KEY_NAME, "").equals("MODIFIED 7");
        assert m_testRoot.get(KEY_NAME + "1", "").equals("ADDED 1");
        assert m_testRoot.get(KEY_NAME + "2", "").equals("ADDED 2");
        assert m_testRoot.get(KEY_NAME + "3", "").equals("ADDED 3");
        assert m_testRoot.get(KEY_NAME + "5", "").equals("ADDED 5");
        assert m_testRoot.get(KEY_NAME + "6", "").equals("ADDED 6");
        assert m_testRoot.get(KEY_NAME + "7", "").equals("ADDED 7");
    }

    /**
     * Checks to make sure the key exists (is not null).
     *
     * @param  key
     *
     * @return <code>true</code> if the key exists, <code>false</code> if it does not exist or is null
     */
    private boolean keyExists(String key) {
        return m_testRoot.get(key, null) != null;
    }

    /**
     * Test upgrade step that upgrades to a particular version number specified by the constructor.
     *
     * @author John Mazzitelli
     */
    private class UpgradeStep extends PreferencesUpgradeStep {
        private int m_version;

        /**
         * Builds this test step so it supports upgrading to the given version number.
         *
         * @param version_supported
         */
        public UpgradeStep(int version_supported) {
            m_version = version_supported;
        }

        /**
         * @see PreferencesUpgradeStep#getSupportedConfigurationSchemaVersion()
         */
        public int getSupportedConfigurationSchemaVersion() {
            return m_version;
        }

        /**
         * @see PreferencesUpgradeStep#upgrade(java.util.prefs.Preferences)
         */
        public void upgrade(Preferences preferences) {
            preferences.put(KEY_NAME + m_version, "ADDED " + m_version);
            preferences.put(KEY_NAME, "MODIFIED " + m_version);
        }
    }

    /**
     * An upgrade class that we use in our tests..
     */
    private class Upgrade extends PreferencesUpgrade {
        /**
         * @see PreferencesUpgrade#PreferencesUpgrade(List)
         */
        public Upgrade(List<PreferencesUpgradeStep> upgrade_steps) {
            super(upgrade_steps);
        }

        /**
         * @see PreferencesUpgrade#getConfigurationSchemaVersionPreference()
         */
        public String getConfigurationSchemaVersionPreference() {
            return CONFIG_SCHEMA_VERSION_KEY_NAME;
        }
    }
}