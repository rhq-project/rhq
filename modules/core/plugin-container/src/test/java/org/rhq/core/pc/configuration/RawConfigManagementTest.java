/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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

package org.rhq.core.pc.configuration;

import static org.testng.Assert.*;
import static java.util.Collections.EMPTY_SET;

import org.jmock.Expectations;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.configuration.ResourceConfigurationFacet;
import static org.testng.Assert.assertNull;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

public class RawConfigManagementTest extends ConfigManagementTest {

    ResourceConfigurationFacet configFacet;

    RawConfigManagement rawConfigMgmt;

    @BeforeMethod
    public void setup() {
        configFacet = context.mock(ResourceConfigurationFacet.class);

        rawConfigMgmt = new RawConfigManagement();
        rawConfigMgmt.setComponentService(componentService);
        rawConfigMgmt.setConfigurationUtilityService(configUtilityService);
    }

    @Test
    public void rawConfigsShouldGetLoaded() throws Exception {
        Set<RawConfiguration> rawConfigs = toSet(
            createRawConfiguration("/tmp/foo.txt"),
            createRawConfiguration("/tmp/bar.txt")
        );

        addDefaultExpectationsForLoad(rawConfigs);

        Configuration loadedConfig = rawConfigMgmt.executeLoad(resourceId);

        assertRawsLoaded(rawConfigs, loadedConfig);
    }

    @Test
    public void theConfigNotesShouldGetSet() throws Exception {
        final Configuration config = new Configuration();
        config.setNotes(null);

        addDefaultExpectationsForLoad(EMPTY_SET);

        Configuration loadedConfig = rawConfigMgmt.executeLoad(resourceId);

        assertNotesSetToDefault(loadedConfig);
    }

    @Test
    public void nullShouldBeReturnedWhenRawIsNull() throws Exception {
        Set<RawConfiguration> rawConfigs = null;
        
        addDefaultExpectationsForLoad(rawConfigs);

        Configuration loadedConfig = rawConfigMgmt.executeLoad(resourceId);

        assertNull(loadedConfig, "Expected null to be returned when facet returns null for raw.");
    }

    private void addDefaultExpectationsForLoad(final Set<RawConfiguration> rawConfigs)
        throws Exception {

        context.checking(new Expectations() {{
            atLeast(1).of(componentService).getComponent(resourceId,
                                                         ResourceConfigurationFacet.class,
                                                         FacetLockType.READ,
                                                         ConfigManagement.FACET_METHOD_TIMEOUT,
                                                         daemonThread,
                                                         onlyIfStarted);
            will(returnValue(configFacet));

            atLeast(1).of(configFacet).loadRawConfigurations(); will(returnValue(rawConfigs));

            allowing(componentService).getResourceType(resourceId); will(returnValue(resourceType));
        }});
    }

    @Test
    public void facetShouldBeCalledToUpdateASingleRaw() throws Exception {
        final RawConfiguration raw = createRawConfiguration("/tmp/raw.txt");

        final Configuration config = new Configuration();
        config.addRawConfiguration(raw);

        addDefaultExpectationsForUpdate();

        context.checking(new Expectations() {{
            oneOf(configFacet).validateRawConfiguration(raw);

            oneOf(configFacet).persistRawConfiguration(raw);
        }});

        rawConfigMgmt.executeUpdate(resourceId, config);
    }

//    @Test(expectedExceptions = {RawUpdateException.class})
    @Test(expectedExceptions = {ConfigurationUpdateException.class})
    public void exceptionShouldBeThrownWhenValidationFailsForSingleRaw() throws Exception {
        final RawConfiguration raw = createRawConfiguration("/tmp/raw.txt");

        final Configuration config = new Configuration();
        config.addRawConfiguration(raw);

        addDefaultExpectationsForUpdate();

        context.checking(new Expectations() {{
            oneOf(configFacet).validateRawConfiguration(raw); will(throwException(new RuntimeException()));
        }});

        rawConfigMgmt.executeUpdate(resourceId, config);
    }

//    @Test(expectedExceptions = {RawUpdateException.class})
    @Test(expectedExceptions = {ConfigurationUpdateException.class})
    public void exceptionShouldBeThrownWhenUpdateFailsForSingleRaw() throws Exception {
        final RawConfiguration raw = createRawConfiguration("/tmp/raw.txt");

        final Configuration config = new Configuration();
        config.addRawConfiguration(raw);

        addDefaultExpectationsForUpdate();

        context.checking(new Expectations() {{
            oneOf(configFacet).validateRawConfiguration(raw);

            oneOf(configFacet).persistRawConfiguration(raw); will(throwException(new RuntimeException()));
        }});

        rawConfigMgmt.executeUpdate(resourceId, config);
    }

    @Test
    public void facetShouldBeCalledToUpdateMultipleRaws() throws Exception {
        final RawConfiguration raw1 = createRawConfiguration("/tmp/raw1.txt");
        final RawConfiguration raw2 = createRawConfiguration("/tmp/raw2.txt");

        final Configuration config = new Configuration();
        config.addRawConfiguration(raw1);
        config.addRawConfiguration(raw2);

        addDefaultExpectationsForUpdate();

        context.checking(new Expectations() {{
            oneOf(configFacet).validateRawConfiguration(raw1);

            oneOf(configFacet).persistRawConfiguration(raw1);

            oneOf(configFacet).validateRawConfiguration(raw2);
            
            oneOf(configFacet).persistRawConfiguration(raw2);
        }});

        rawConfigMgmt.executeUpdate(resourceId, config);        
    }

//    @Test
    @Test(expectedExceptions = {ConfigurationUpdateException.class})
    public void secondRawShouldStillGetUpdatedWhenFirstRawFailsValidation() throws Exception {
        final RawConfiguration raw1 = createRawConfiguration("/tmp/raw1.txt");
        final RawConfiguration raw2 = createRawConfiguration("/tmp/raw2.txt");

        final Configuration config = new Configuration();
        config.addRawConfiguration(raw1);
        config.addRawConfiguration(raw2);

        addDefaultExpectationsForUpdate();

        context.checking(new Expectations() {{
            oneOf(configFacet).validateRawConfiguration(raw1); will(throwException(new RuntimeException()));

            oneOf(configFacet).validateRawConfiguration(raw2);

            oneOf(configFacet).persistRawConfiguration(raw2);
        }});

        rawConfigMgmt.executeUpdate(resourceId, config);

//        RawUpdateException exception = null;
//
//        try {
//            rawConfigMgmt.executeUpdate(resourceId, config);
//        }
//        catch (RawUpdateException e) {
//            exception = e;
//        }
//
//        assertNotNull(exception, "Expected a " + RawUpdateException.class.getSimpleName() + " to be thrown when " +
//            " validation fails");
//
//        List<RawUpdateErrorDetail> errors = exception.getDetails();
//
//        assertEquals(errors.size(), 1, "Expected to find only a single error since only the update of one raw failed");
//        assertEquals(
//            errors.get(0).getRawConfiguration(),
//            raw1,
//            "Expected to find " + raw1 + " since validation for it failed"
//        );
    }

//    @Test
    @Test(expectedExceptions = {ConfigurationUpdateException.class})
    public void secondRawShouldStillGetUpdateWhenFirstRawUpdateFails() throws Exception {
        final RawConfiguration raw1 = createRawConfiguration("/tmp/raw1.txt");
        final RawConfiguration raw2 = createRawConfiguration("/tmp/raw2.txt");

        final Configuration config = new Configuration();
        config.addRawConfiguration(raw1);
        config.addRawConfiguration(raw2);

        addDefaultExpectationsForUpdate();

        context.checking(new Expectations() {{
            oneOf(configFacet).validateRawConfiguration(raw1);

            oneOf(configFacet).persistRawConfiguration(raw1); will(throwException(new RuntimeException()));

            oneOf(configFacet).validateRawConfiguration(raw2);

            oneOf(configFacet).persistRawConfiguration(raw2);
        }});

        rawConfigMgmt.executeUpdate(resourceId, config);

//        RawUpdateException exception = null;
//
//        try {
//            rawConfigMgmt.executeUpdate(resourceId, config);
//        }
//        catch (RawUpdateException e) {
//            exception = e;
//        }
//
//        assertNotNull(exception, "Expected a " + RawUpdateException.class.getSimpleName() + " to be thrown when " +
//            " update fails");
//
//        List<RawUpdateErrorDetail> errors = exception.getDetails();
//
//        assertEquals(errors.size(), 1, "Expected to find only a single error since only the update of one raw failed");
//        assertEquals(
//            errors.get(0).getRawConfiguration(),
//            raw1,
//            "Expected to find " + raw1 + " since update for it failed"
//        );            
    }

//    @Test
    @Test(expectedExceptions = {ConfigurationUpdateException.class})
    public void noUpdatesShouldHappenWhenValidationFailsForBothRaws() throws Exception {
        final RawConfiguration raw1 = createRawConfiguration("/tmp/raw1.txt");
        final RawConfiguration raw2 = createRawConfiguration("/tmp/raw2.txt");

        final Configuration config = new Configuration();
        config.addRawConfiguration(raw1);
        config.addRawConfiguration(raw2);

        addDefaultExpectationsForUpdate();

        context.checking(new Expectations() {{
            oneOf(configFacet).validateRawConfiguration(raw1); will(throwException(new RuntimeException()));

            oneOf(configFacet).validateRawConfiguration(raw2); will(throwException(new RuntimeException()));
        }});

        rawConfigMgmt.executeUpdate(resourceId, config);

//        RawUpdateException exception = null;
//
//        try {
//            rawConfigMgmt.executeUpdate(resourceId, config);
//        }
//        catch (RawUpdateException e) {
//            exception = e;
//        }
//
//        assertNotNull(exception, "Expected a " + RawUpdateException.class.getSimpleName() + " to be thrown when " +
//            " update fails");
//
//        List<RawUpdateErrorDetail> errors = exception.getDetails();
//
//        assertEquals(errors.size(), 2, "Expected to find two errors since validation failed for both raw config files");
//        assertErrorDetailsContainsRawConfig(errors, raw1);
//        assertErrorDetailsContainsRawConfig(errors, raw2);
    }

    private void assertErrorDetailsContainsRawConfig(List<RawUpdateErrorDetail> errors, RawConfiguration rawConfig) {
        for (RawUpdateErrorDetail detail : errors) {
            if (detail.getRawConfiguration().equals(rawConfig)) {
                return;
            }
        }
        fail("Expected to find " + rawConfig + " in update error details");
    }

    private void addDefaultExpectationsForUpdate() throws Exception {
        final boolean isDaemonThread = false;

        context.checking(new Expectations() {{
            atLeast(1).of(componentService).getComponent(resourceId,
                                                         ResourceConfigurationFacet.class,
                                                         FacetLockType.WRITE,
                                                         ConfigManagement.FACET_METHOD_TIMEOUT,
                                                         isDaemonThread,
                                                         onlyIfStarted);
            will(returnValue(configFacet));
        }});
    }

}
