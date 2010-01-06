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

package org.rhq.core.pc.configuration;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import static org.testng.Assert.assertNull;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import static org.rhq.core.domain.configuration.ConfigurationUpdateStatus.*;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.test.jmock.PropertyMatcher;
import org.jmock.Expectations;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.hamcrest.Matcher;
import org.hamcrest.Description;

public class LegacyConfigManagementTest extends ConfigManagementTest {

    ConfigurationFacet facet;

    LegacyConfigManagement configMgmt;

    @BeforeMethod
    public void setup() {
        facet = context.mock(ConfigurationFacet.class);

        configMgmt = new LegacyConfigManagement();
        configMgmt.setComponentService(componentService);
        configMgmt.setConfigurationUtilityService(configUtilityService);
    }

    @Test
    public void structuredConfigShouldGetLoaded() throws Exception {
        final Configuration config = new Configuration();
        config.put(new PropertySimple("x", "1"));
        config.put(new PropertySimple("y", "2"));

        addDefaultExpectationsForLoad(config);

        Configuration loadedConfig = configMgmt.executeLoad(resourceId);

        assertStructuredLoaded(config, loadedConfig);
    }

    @Test
    public void theConfigNotesShouldGetSet() throws Exception {
        final Configuration config = new Configuration();
        config.setNotes(null);

        addDefaultExpectationsForLoad(config);

        Configuration loadedConfig = configMgmt.executeLoad(resourceId);

        assertNotesSetToDefault(loadedConfig);
    }

    @Test
    public void nullShouldBeReturnedWhenStructuredIsNull() throws Exception {
        final Configuration config = null;

        context.checking(new Expectations() {{
            atLeast(1).of(componentService).getComponent(resourceId,
                                                         ConfigurationFacet.class,
                                                         FacetLockType.READ,
                                                         ConfigManagement.FACET_METHOD_TIMEOUT,
                                                         daemonThread,
                                                         onlyIfStarted);
            will(returnValue(facet));

            allowing(componentService).getResourceType(resourceId); will(returnValue(resourceType));

            atLeast(1).of(facet).loadResourceConfiguration(); will(returnValue(null));            
        }});

        Configuration loadedConfig = configMgmt.executeLoad(resourceId);

        assertNull(loadedConfig, "Expected null to be returned when facet returns null for structured.");
    }

    private void addDefaultExpectationsForLoad(final Configuration config) throws Exception {
        context.checking(new Expectations() {{
            atLeast(1).of(componentService).getComponent(resourceId,
                                                         ConfigurationFacet.class,
                                                         FacetLockType.READ,
                                                         ConfigManagement.FACET_METHOD_TIMEOUT,
                                                         daemonThread,
                                                         onlyIfStarted);
            will(returnValue(facet));

            allowing(componentService).getResourceType(resourceId); will(returnValue(resourceType));

            atLeast(1).of(facet).loadResourceConfiguration(); will(returnValue(config));

            atLeast(1).of(configUtilityService).normalizeConfiguration(config, getResourceConfigDefinition());

            atLeast(1).of(configUtilityService).validateConfiguration(config, getResourceConfigDefinition());
        }});
    }

    @Test
    public void facetShouldBeCalledToUpdateStructured() throws Exception {
        final Configuration config = new Configuration();

        final ConfigurationUpdateReport updateReport = new ConfigurationUpdateReport(config);

        final boolean isDaemonThread = false;

        context.checking(new Expectations() {{
            atLeast(1).of(componentService).getComponent(resourceId,
                                                         ConfigurationFacet.class,
                                                         FacetLockType.WRITE,
                                                         ConfigManagement.FACET_METHOD_TIMEOUT,
                                                         isDaemonThread,
                                                         onlyIfStarted);
            will(returnValue(facet));

            oneOf(facet).updateResourceConfiguration(with(matchingUpdateReport(updateReport)));
            will(updateReportTo(SUCCESS));
        }});

        configMgmt.executeUpdate(resourceId, config);
    }

    @Test(expectedExceptions = UpdateInProgressException.class)
    public void exceptionShouldBeThrownWhenUpdateDoesNotComplete() throws Exception {
        final Configuration config = new Configuration();

        final ConfigurationUpdateReport updateReport = new ConfigurationUpdateReport(config);

        final boolean isDaemonThread = false;

        context.checking(new Expectations() {{
            atLeast(1).of(componentService).getComponent(resourceId,
                                                         ConfigurationFacet.class,
                                                         FacetLockType.WRITE,
                                                         ConfigManagement.FACET_METHOD_TIMEOUT,
                                                         isDaemonThread,
                                                         onlyIfStarted);
            will(returnValue(facet));

            oneOf(facet).updateResourceConfiguration(with(matchingUpdateReport(updateReport)));
            will(updateReportTo(INPROGRESS));
        }});

        configMgmt.executeUpdate(resourceId, config);
    }

    @Test(expectedExceptions = ConfigurationUpdateException.class)
    public void exceptionShouldBeThrownWhenUpdateFails() throws Exception {
        final Configuration config = new Configuration();

        final ConfigurationUpdateReport updateReport = new ConfigurationUpdateReport(config);

        final boolean isDaemonThread = false;

        context.checking(new Expectations() {{
            atLeast(1).of(componentService).getComponent(resourceId,
                                                         ConfigurationFacet.class,
                                                         FacetLockType.WRITE,
                                                         ConfigManagement.FACET_METHOD_TIMEOUT,
                                                         isDaemonThread,
                                                         onlyIfStarted);
            will(returnValue(facet));

            oneOf(facet).updateResourceConfiguration(with(matchingUpdateReport(updateReport)));
            will(updateReportTo(FAILURE));
        }});

        configMgmt.executeUpdate(resourceId, config);
    }

    public static Matcher<ConfigurationUpdateReport> matchingUpdateReport(ConfigurationUpdateReport expected) {
        return new PropertyMatcher<ConfigurationUpdateReport>(expected);
    }

    public static Action updateReportTo(final ConfigurationUpdateStatus status) {
        return new Action() {
            public void describeTo(Description description) {
                description.appendText("Updates " + ConfigurationUpdateReport.class.getSimpleName() + " to " + status);
            }

            public Object invoke(Invocation invocation) throws Throwable {
                ConfigurationUpdateReport report = (ConfigurationUpdateReport) invocation.getParameter(0);
                report.setStatus(status);
                return null;
            }
        };
    }

}
