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

import static org.rhq.core.domain.configuration.ConfigurationUpdateStatus.*;
import static org.rhq.test.AssertUtils.*;
import org.rhq.test.jmock.PropertyMatcher;

import org.testng.annotations.Test;
import org.rhq.core.clientapi.server.configuration.ConfigurationServerService;
import org.rhq.core.clientapi.server.configuration.ConfigurationUpdateResponse;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.jmock.Expectations;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class UpdateResourceConfigurationRunnerTest extends ConfigManagementTest {

    @Test
    public void successfulUpdateShouldSendSuccessResponseToServer() throws Exception {
        final Configuration config = createStructuredConfig();

        final ConfigurationServerService configServerService = context.mock(ConfigurationServerService.class);

        final ConfigurationFacet facet = context.mock(ConfigurationFacet.class);

        final ConfigurationUtilityService configUtilService = context.mock(ConfigurationUtilityService.class);

        int configUpdateId = -1;

        ConfigurationUpdateRequest updateRequest = new ConfigurationUpdateRequest(configUpdateId, config, resourceId);

        UpdateResourceConfigurationRunner updateRunner = new UpdateResourceConfigurationRunner(configServerService,
            resourceType, facet, updateRequest);
        updateRunner.setConfigUtilService(configUtilService);

        final ConfigurationUpdateResponse successResponse = new ConfigurationUpdateResponse(configUpdateId, null,
            SUCCESS, null);

        context.checking(new Expectations() {{
            oneOf(facet).updateResourceConfiguration(with(any(ConfigurationUpdateReport.class)));
            will(updateReportTo(SUCCESS));

            oneOf(configUtilService).normalizeConfiguration(config, resourceType.getResourceConfigurationDefinition());
            oneOf(configUtilService).validateConfiguration(config, resourceType.getResourceConfigurationDefinition());

            oneOf(configServerService).completeConfigurationUpdate(with(matchingResponse(successResponse)));
        }});

        updateRunner.call();
    }

    @Test
    public void successfulUpdateShouldReturnSuccessResponseInEmbeddedMode() throws Exception {
        final Configuration config = createStructuredConfig();

        final ConfigurationFacet facet = context.mock(ConfigurationFacet.class);

        final ConfigurationUtilityService configUtilService = context.mock(ConfigurationUtilityService.class);

        int configUpdateId = -1;

        ConfigurationUpdateRequest updateRequest = new ConfigurationUpdateRequest(configUpdateId, config, resourceId);

        UpdateResourceConfigurationRunner updateRunner = new UpdateResourceConfigurationRunner(null, resourceType,
            facet, updateRequest);
        updateRunner.setConfigUtilService(configUtilService);

        final ConfigurationUpdateResponse successResponse = new ConfigurationUpdateResponse(configUpdateId, null,
            SUCCESS, null);

        context.checking(new Expectations() {{
            oneOf(facet).updateResourceConfiguration(with(any(ConfigurationUpdateReport.class)));
            will(updateReportTo(SUCCESS));

            oneOf(configUtilService).normalizeConfiguration(config, resourceType.getResourceConfigurationDefinition());
            oneOf(configUtilService).validateConfiguration(config, resourceType.getResourceConfigurationDefinition());

        }});

        ConfigurationUpdateResponse actualResponse = updateRunner.call();

        assertConfigurationUpdateResponseMatches(successResponse, actualResponse, "Expected a success response");
    }

    @Test
    public void inProgressUpdateShouldSendFailureResponseToServer() throws Exception {
        final Configuration config = createStructuredConfig();

        final ConfigurationServerService configServerService = context.mock(ConfigurationServerService.class);

        final ConfigurationFacet facet = context.mock(ConfigurationFacet.class);

        final ConfigurationUtilityService configUtilService = context.mock(ConfigurationUtilityService.class);

        int configUpdateId = -1;

        ConfigurationUpdateRequest updateRequest = new ConfigurationUpdateRequest(configUpdateId, config, resourceId);

        UpdateResourceConfigurationRunner updateRunner = new UpdateResourceConfigurationRunner(configServerService,
            resourceType, facet, updateRequest);
        updateRunner.setConfigUtilService(configUtilService);

        String errorMsg = "Configuration facet did not indicate success or failure - assuming failure.";

        final ConfigurationUpdateResponse failureResponse = new ConfigurationUpdateResponse(configUpdateId, config,
            FAILURE, errorMsg);

        context.checking(new Expectations() {{
            oneOf(facet).updateResourceConfiguration(with(any(ConfigurationUpdateReport.class)));
            will(updateReportTo(INPROGRESS));

            oneOf(configUtilService).normalizeConfiguration(config, resourceType.getResourceConfigurationDefinition());
            oneOf(configUtilService).validateConfiguration(config, resourceType.getResourceConfigurationDefinition());

            oneOf(configServerService).completeConfigurationUpdate(with(matchingResponse(failureResponse)));
        }});

        updateRunner.call();
    }

    @Test
    public void failedUpdateShouldReturnFailureResponseInEmbeddedMode() throws Exception {
        final Configuration config = createStructuredConfig();

        final ConfigurationFacet facet = context.mock(ConfigurationFacet.class);

        final ConfigurationUtilityService configUtilService = context.mock(ConfigurationUtilityService.class);

        int configUpdateId = -1;

        ConfigurationUpdateRequest updateRequest = new ConfigurationUpdateRequest(configUpdateId, config, resourceId);

        UpdateResourceConfigurationRunner updateRunner = new UpdateResourceConfigurationRunner(null, resourceType,
            facet, updateRequest);
        updateRunner.setConfigUtilService(configUtilService);

        String errorMsg = "Configuration facet did not indicate success or failure - assuming failure.";

        final ConfigurationUpdateResponse failureResponse = new ConfigurationUpdateResponse(configUpdateId, config,
            FAILURE, errorMsg);

        context.checking(new Expectations() {{
            oneOf(facet).updateResourceConfiguration(with(any(ConfigurationUpdateReport.class)));
            will(updateReportTo(INPROGRESS));

            oneOf(configUtilService).normalizeConfiguration(config, resourceType.getResourceConfigurationDefinition());
            oneOf(configUtilService).validateConfiguration(config, resourceType.getResourceConfigurationDefinition());
        }});

        ConfigurationUpdateResponse actualResponse = updateRunner.call();

        assertConfigurationUpdateResponseMatches(failureResponse, actualResponse, "Expected a failure response");
    }

    @Test
    public void failureResponseShouldBeSentToServerWhenFacetThrowsAnException() throws Exception {
        final Configuration config = createStructuredConfig();

        final ConfigurationServerService configServerService = context.mock(ConfigurationServerService.class);

        final ConfigurationFacet facet = context.mock(ConfigurationFacet.class);

        int configUpdateId = -1;

        ConfigurationUpdateRequest updateRequest = new ConfigurationUpdateRequest(configUpdateId, config, resourceId);

        UpdateResourceConfigurationRunner updateRunner = new UpdateResourceConfigurationRunner(configServerService,
            resourceType, facet, updateRequest);

        final NullPointerException exception = new NullPointerException("Unexpected error during update");

        final ConfigurationUpdateResponse failureResponse = new ConfigurationUpdateResponse(configUpdateId, config,
            exception);

        context.checking(new Expectations() {{
            oneOf(facet).updateResourceConfiguration(with(any(ConfigurationUpdateReport.class)));
            will(throwExceptionFromFacet(exception));

            oneOf(configServerService).completeConfigurationUpdate(with(matchingResponse(failureResponse)));
        }});

        updateRunner.call();                
    }

    @Test
    public void failureResponseShouldBeReturnedWhenFacetThrowsAnExceptionInEmbeddedMode() throws Exception {
        final Configuration config = createStructuredConfig();

        final ConfigurationFacet facet = context.mock(ConfigurationFacet.class);

        int configUpdateId = -1;

        ConfigurationUpdateRequest updateRequest = new ConfigurationUpdateRequest(configUpdateId, config, resourceId);

        UpdateResourceConfigurationRunner updateRunner = new UpdateResourceConfigurationRunner(null, resourceType,
            facet, updateRequest);

        final NullPointerException exception = new NullPointerException("Unexpected error during update");

        final ConfigurationUpdateResponse failureResponse = new ConfigurationUpdateResponse(configUpdateId, config,
            exception);

        context.checking(new Expectations() {{
            oneOf(facet).updateResourceConfiguration(with(any(ConfigurationUpdateReport.class)));
            will(throwExceptionFromFacet(exception));
        }});

        ConfigurationUpdateResponse actualResponse = updateRunner.call();

        assertConfigurationUpdateResponseMatches(failureResponse, actualResponse, "Expected a failure response");
    }

    void assertConfigurationUpdateResponseMatches(ConfigurationUpdateResponse expected,
        ConfigurationUpdateResponse actual, String msg) {
        assertPropertiesMatch(expected, actual, msg);
    }

    boolean propertyEquals(Object expected, Object actual) {
        if (expected == null && actual == null) {
            return true;
        }

        if (expected == null && actual != null) {
            return false;
        }
        return expected.equals(actual);
    }

    public static Matcher<ConfigurationUpdateResponse> matchingResponse(ConfigurationUpdateResponse expected) {
        return new PropertyMatcher<ConfigurationUpdateResponse>(expected);
    }

    public static Action updateReportTo(ConfigurationUpdateStatus status) {
        return new UpdateResourceConfigurationAction(status);
    }

    public static Action throwExceptionFromFacet(Throwable t) {
        return new ThrowFacetExceptionAction(t);
    }

    static class UpdateResourceConfigurationAction implements Action {
        ConfigurationUpdateStatus status;

        public UpdateResourceConfigurationAction(ConfigurationUpdateStatus status) {
            this.status = status;
        }

        public void describeTo(Description description) {
            description.appendText("Updating status of " + ConfigurationUpdateReport.class.getSimpleName() + " to " +
                status);
        }

        public Object invoke(Invocation invocation) throws Throwable {
            ConfigurationUpdateReport report = (ConfigurationUpdateReport) invocation.getParameter(0);
            report.setStatus(status);
            return null;
        }
    }

    static class ThrowFacetExceptionAction implements Action {
        Throwable throwable;

        public ThrowFacetExceptionAction(Throwable t) {
            throwable = t;
        }

        public void describeTo(Description description) {
            description.appendText("throws <" + throwable.getClass().getName() + ">: " + throwable.getMessage());
        }

        public Object invoke(Invocation invocation) throws Throwable {
            throw throwable;
        }
    }

}
