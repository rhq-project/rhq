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

import org.jmock.Expectations;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.configuration.ResourceConfigurationFacet;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNull;

public class StructuredConfigManagementTest extends ConfigManagementTest {

    ResourceConfigurationFacet configFacet;

    StructuredConfigManagement structuredMgmt;

    @BeforeMethod
    public void setup() {
        configFacet = context.mock(ResourceConfigurationFacet.class);

        structuredMgmt = new StructuredConfigManagement();
        structuredMgmt.setComponentService(componentService);
        structuredMgmt.setConfigurationUtilityService(configUtilityService);
    }

    @Test
    public void structuredConfigShouldGetLoaded() throws Exception {
        Configuration config = new Configuration();
        config.put(new PropertySimple("x", "1"));
        config.put(new PropertySimple("y", "2"));

        addDefaultExpectationsForLoad(config);

        Configuration loadedConfig = structuredMgmt.executeLoad(resourceId);

        assertStructuredLoaded(config, loadedConfig);
    }

    @Test
    public void theConfigNotesShouldGetSet() throws Exception {
        final Configuration config = new Configuration();
        config.setNotes(null);

        addDefaultExpectationsForLoad(config);

        Configuration loadedConfig = structuredMgmt.executeLoad(resourceId);

        assertNotesSetToDefault(loadedConfig);
    }

    @Test
    public void nullShouldBeReturnedWhenStructuredIsNull() throws Exception {
        context.checking(new Expectations() {{
            atLeast(1).of(componentService).getComponent(resourceId,
                                                         ResourceConfigurationFacet.class,
                                                         FacetLockType.READ,
                                                         ConfigManagement.FACET_METHOD_TIMEOUT,
                                                         daemonThread,
                                                         onlyIfStarted);
            will(returnValue(configFacet));

            allowing(componentService).getResourceType(resourceId); will(returnValue(resourceType));

            atLeast(1).of(configFacet).loadStructuredConfiguration(); will(returnValue(null));    
        }});

        Configuration loadedConfig = structuredMgmt.executeLoad(resourceId);

        assertNull(loadedConfig, "Expected null to be returned when facet returns null for structured.");
    }

    private void addDefaultExpectationsForLoad(final Configuration config) throws Exception {
        context.checking(new Expectations() {{
            atLeast(1).of(componentService).getComponent(resourceId,
                                                         ResourceConfigurationFacet.class,
                                                         FacetLockType.READ,
                                                         ConfigManagement.FACET_METHOD_TIMEOUT,
                                                         daemonThread,
                                                         onlyIfStarted);
            will(returnValue(configFacet));

            allowing(componentService).getResourceType(resourceId); will(returnValue(resourceType));

            atLeast(1).of(configFacet).loadStructuredConfiguration(); will(returnValue(config));

            atLeast(1).of(configUtilityService).normalizeConfiguration(config, getResourceConfigDefinition());

            atLeast(1).of(configUtilityService).validateConfiguration(config, getResourceConfigDefinition());
        }});
    }

    @Test
    public void facetShouldBeCalledToUpdateStructured() throws Exception {
        final Configuration config = new Configuration();

        addDefaultExpectationsForUpdate();

        context.checking(new Expectations() {{
            oneOf(configFacet).validateStructuredConfiguration(config);

            oneOf(configFacet).persistStructuredConfiguration(config);
        }});

        structuredMgmt.executeUpdate(resourceId, config);
    }

    @Test(expectedExceptions = {ConfigurationUpdateException.class})
    public void exceptionShouldBeThrownWhenValidationFails() throws Exception {
        final Configuration config = new Configuration();

        addDefaultExpectationsForUpdate();

        context.checking(new Expectations() {{
            oneOf(configFacet).validateStructuredConfiguration(config); will(throwException(new RuntimeException()));
        }});

        structuredMgmt.executeUpdate(resourceId, config);
    }

    @Test(expectedExceptions = {ConfigurationUpdateException.class})
    public void exceptionShouldBeThrownWhenUpdateFails() throws Exception {
        final Configuration config = new Configuration();

        addDefaultExpectationsForUpdate();

        context.checking(new Expectations() {{
            oneOf(configFacet).validateStructuredConfiguration(config);

            oneOf(configFacet).persistStructuredConfiguration(config); will(throwException(new RuntimeException()));
        }});

        structuredMgmt.executeUpdate(resourceId, config);
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
