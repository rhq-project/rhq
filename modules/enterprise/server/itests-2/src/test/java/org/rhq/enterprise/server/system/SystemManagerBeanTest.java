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
package org.rhq.enterprise.server.system;

import java.util.Properties;

import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.composite.SystemSetting;
import org.rhq.core.domain.common.composite.SystemSettings;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.util.obfuscation.PicketBoxObfuscator;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestServerPluginService;
import org.rhq.enterprise.server.util.LookupUtil;

@Test
public class SystemManagerBeanTest extends AbstractEJB3Test {
    private Subject overlord;
    private SystemManagerLocal systemManager;
    private TestServerPluginService testServerPluginService;

    @Override
    protected void beforeMethod() throws Exception {
        systemManager = LookupUtil.getSystemManager();

        // do this each method so it doesn't expire
        overlord = LookupUtil.getSubjectManager().getOverlord();

        //we need this because the drift plugins are referenced from the system settings that we use in our tests
        testServerPluginService = new TestServerPluginService(getTempDir());
        prepareCustomServerPluginService(testServerPluginService);
        testServerPluginService.startMasterPluginContainer();
    }

    @Override
    protected void afterMethod() throws Exception {
        unprepareServerPluginService();
    }

    public void testVacuum() {
        System.out.println("Starting Vacuum");
        systemManager.vacuum(overlord);
        System.out.println("Done with Vacuum");
    }

    @Test(dependsOnMethods = { "testVacuum" })
    public void testAnalyze() {
        System.out.println("Starting Analyze");
        systemManager.analyze(overlord);
        System.out.println("Done with Analyze");
    }

    public void testEnableHibernateStatistics() {
        systemManager.enableHibernateStatistics();
    }

    public void testReindex() {
        systemManager.reindex(overlord);
    }

    public void testVacuumAppdef() {
        systemManager.vacuumAppdef(overlord);
    }

    @SuppressWarnings("deprecation")
    public void testGetSystemSettings() {
        assert null != systemManager.getSystemSettings(overlord);
        assert null != systemManager.getSystemConfiguration(overlord);
    }

    @SuppressWarnings("deprecation")
    public void testLegacySystemSettingsInCorrectFormat() throws Exception {
        //some of the properties are represented differently
        //in the new style settings and the the old style
        //settings (and consequently database).
        //These two still co-exist together in the codebase
        //so let's make sure the values correspond to each other.

        SystemSettings settings = systemManager.getUnmaskedSystemSettings(false);
        Properties config = systemManager.getSystemConfiguration(overlord);

        SystemSettings origSettings = new SystemSettings(settings);

        try {
            //let's make sure the values are the same
            checkFormats(settings, config);

            boolean currentJaasProvider = Boolean.valueOf(settings.get(SystemSetting.LDAP_BASED_JAAS_PROVIDER));
            settings.put(SystemSetting.LDAP_BASED_JAAS_PROVIDER, Boolean.toString(!currentJaasProvider));

            boolean currentUseSslForLdap = Boolean.valueOf(settings.get(SystemSetting.USE_SSL_FOR_LDAP));
            settings.put(SystemSetting.USE_SSL_FOR_LDAP, Boolean.toString(!currentUseSslForLdap));

            systemManager.setAnySystemSettings(settings, false, true);

            settings = systemManager.getUnmaskedSystemSettings(false);
            config = systemManager.getSystemConfiguration(overlord);

            checkFormats(settings, config);

            try {
                // test this remote method but it can sometimes get hit by test interaction, so be lenient
                systemManager.setSystemSettings(overlord, origSettings);
            } catch (IllegalArgumentException e) {
                // sometimes expected due to test interaction
            } catch (Exception e) {
                fail("Should not have thrown anything other than IllegalArgumentException, not " + e);
            }
        } finally {
            systemManager.setAnySystemSettings(origSettings, false, true);
        }
    }

    public void testPasswordFieldsObfuscation() {
        SystemSettings masked = systemManager.getSystemSettings(overlord);
        SystemSettings unmasked = systemManager.getUnmaskedSystemSettings(true);
        SystemSettings obfuscated = systemManager.getObfuscatedSystemSettings(true);

        for (SystemSetting setting : SystemSetting.values()) {
            if (setting.getType() == PropertySimpleType.PASSWORD) {
                if (masked.containsKey(setting) && masked.get(setting) != null) {
                    assertEquals("Unexpected unmasked value", PropertySimple.MASKED_VALUE, masked.get(setting));
                    assertEquals("Unmasked and obfuscated values don't correspond", obfuscated.get(setting),
                        PicketBoxObfuscator.encode(unmasked.get(setting)));
                }
            }
        }

        systemManager.deobfuscate(obfuscated);

        assertEquals(unmasked, obfuscated);
    }

    public void testPasswordMaskingDoesNotPersistBack() throws Exception {
        SystemSettings settings = systemManager.getSystemSettings(overlord);
        SystemSettings copy = (SystemSettings) settings.clone();

        systemManager.setAnySystemSettings(settings, false, true);

        settings = systemManager.getSystemSettings(overlord);

        assertEquals("Password masking should not modify the database", copy, settings);
    }

    public void testPersistingDeobfuscatedSettingsDoesNotChangeTheValues() throws Exception {
        SystemSettings settings = systemManager.getObfuscatedSystemSettings(true);
        SystemSettings copy = (SystemSettings) settings.clone();

        systemManager.deobfuscate(settings);
        systemManager.setAnySystemSettings(settings, false, true);

        settings = systemManager.getObfuscatedSystemSettings(true);

        assertEquals("Password obfuscation should not modify the database if deobfuscated first", copy, settings);
    }

    private void checkFormats(SystemSettings settings, Properties config) {
        assert settings.size() == config.size() : "The old and new style system settings differ in size";

        for (String name : config.stringPropertyNames()) {
            SystemSetting setting = SystemSetting.getByInternalName(name);

            String oldStyleValue = config.getProperty(name);
            String newStyleValue = settings.get(setting);

            assert setting != null : "Could not find a system setting called '" + name + "'.";

            switch (setting) {
            case USE_SSL_FOR_LDAP:
                if (RHQConstants.LDAP_PROTOCOL_SECURED.equals(oldStyleValue)) {
                    assert Boolean.valueOf(newStyleValue) : "Secured LDAP protocol should be represented by a 'true' in new style settings.";
                } else if (RHQConstants.LDAP_PROTOCOL_UNSECURED.equals(oldStyleValue)) {
                    assert !Boolean.valueOf(newStyleValue) : "Unsecured LDAP protocol should be represented by a 'false' in the new style settings.";
                } else {
                    assert false : "Unknown value for system setting '" + setting + "': [" + oldStyleValue + "].";
                }
                break;
            case LDAP_BASED_JAAS_PROVIDER:
                if (RHQConstants.LDAPJAASProvider.equals(oldStyleValue)) {
                    assert Boolean.valueOf(newStyleValue) : "LDAP JAAS provider should be represented by a 'true' in new style settings.";
                } else if (RHQConstants.JDBCJAASProvider.equals(oldStyleValue)) {
                    assert !Boolean.valueOf(newStyleValue) : "JDBC JAAS provider should be represented by a 'false' in the new style settings.";
                } else {
                    assert false : "Unknown value for system setting '" + setting + "': [" + oldStyleValue + "].";
                }
                break;
            default:
                assert oldStyleValue != null && newStyleValue != null && oldStyleValue.equals(newStyleValue) : "Old and new style values unexpectedly differ for system setting '"
                    + setting + "': old=[" + oldStyleValue + "], new=[" + newStyleValue + "].";
            }
        }
    }
}
