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

package org.rhq.enterprise.server.configuration;

import static org.rhq.test.AssertUtils.*;
import static org.rhq.core.domain.configuration.ConfigurationUpdateStatus.*;
import static org.testng.Assert.*;

import org.rhq.test.JMockTest;
import org.rhq.test.jmock.PropertyMatcher;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.clientapi.agent.configuration.ConfigurationAgentService;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.jmock.Expectations;
import org.hamcrest.Matcher;

import javax.persistence.EntityManager;
import java.lang.reflect.Field;

public class ConfigurationManagerBeanUnitTest extends JMockTest {

    ConfigurationManagerLocal configurationMgrLocal;

    AgentManagerLocal agentMgr;

    EntityManager entityMgr;

    ConfigurationManagerBean configurationMgr;

    @BeforeMethod
    public void setup() throws Exception {
        configurationMgr = new ConfigurationManagerBean();

        configurationMgrLocal = context.mock(ConfigurationManagerLocal.class);
        setField(configurationMgr, "configurationManager", configurationMgrLocal);

        agentMgr = context.mock(AgentManagerLocal.class);
        setField(configurationMgr, "agentManager", agentMgr);

        entityMgr = context.mock(EntityManager.class);
        setField(configurationMgr, "entityManager", entityMgr);
    }

    static void setField(Object src, String fieldName, Object value) {
        try {
            Field field = src.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(src, value);
        }
        catch (SecurityException e) {
            throw new RuntimeException(e);
        }
        catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
        catch (NoSuchFieldException e) {
            String msg = "The field <" + fieldName + "> does not exist for " +
                src.getClass().getName();
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    public void updateResourceConfigShouldAddUpdateToAuditTrailAndSendUpdateToAgent() throws Exception {
        final Subject subject = new Subject("rhqadmin", true, true);
        final int resourceId = -1;
        final Configuration newConfig = new Configuration();
        final boolean isPartOfGroupUpdate = false;

        Resource resource = new Resource(resourceId);
        resource.setAgent(new Agent("test-agent", "localhost", 7080, null, null));

        final ResourceConfigurationUpdate expectedUpdate = new ResourceConfigurationUpdate(resource, newConfig,
            subject.getName());
        expectedUpdate.setId(-1);

        final AgentClient agentClient = context.mock(AgentClient.class);
        final ConfigurationAgentService configAgentService = context.mock(ConfigurationAgentService.class);

        final ConfigurationUpdateRequest expectedUpdateRequest = new ConfigurationUpdateRequest(expectedUpdate.getId(),
            expectedUpdate.getConfiguration(), expectedUpdate.getResource().getId());

        context.checking(new Expectations() {{
            oneOf(configurationMgrLocal).persistNewResourceConfigurationUpdateHistory(subject, resourceId, newConfig,
                INPROGRESS, subject.getName(), isPartOfGroupUpdate);
            will(returnValue(expectedUpdate));

            allowing(agentMgr).getAgentClient(expectedUpdate.getResource().getAgent()); will(returnValue(agentClient));

            allowing(agentClient).getConfigurationAgentService(); will(returnValue(configAgentService));

            oneOf(configAgentService).updateResourceConfiguration(with(matchingUpdateRequest(expectedUpdateRequest)));
        }});

        ResourceConfigurationUpdate actualUpdate = configurationMgr.updateResourceConfiguration(subject, resourceId,
            newConfig);

        assertSame(actualUpdate, expectedUpdate, "");
    }

    @Test
    public void updateResourceConfigShouldMarkUpdateAsFailureWhenExceptionOccurs() throws Exception {
        final Subject subject = new Subject("rhqadmin", true, true);
        final int resourceId = -1;
        final Configuration newConfig = new Configuration();
        final boolean isPartOfGroupUpdate = false;

        Resource resource = new Resource(resourceId);
        resource.setAgent(new Agent("test-agent", "localhost", 7080, null, null));

        final RuntimeException exception = new RuntimeException();

        final ResourceConfigurationUpdate expectedUpdate = new ResourceConfigurationUpdate(resource, newConfig,
            subject.getName());
        expectedUpdate.setId(-1);
        expectedUpdate.setStatus(FAILURE);
        expectedUpdate.setErrorMessageFromThrowable(exception);

        context.checking(new Expectations() {{
            oneOf(configurationMgrLocal).persistNewResourceConfigurationUpdateHistory(subject, resourceId, newConfig,
                INPROGRESS, subject.getName(), isPartOfGroupUpdate);
            will(returnValue(expectedUpdate));

            allowing(agentMgr).getAgentClient(expectedUpdate.getResource().getAgent()); 
            will(throwException(exception));

            oneOf(configurationMgrLocal).mergeConfigurationUpdate(expectedUpdate);
        }});

        ResourceConfigurationUpdate actualUpdate = configurationMgr.updateResourceConfiguration(subject, resourceId,
            newConfig);

        assertPropertiesMatch(expectedUpdate, actualUpdate, "Expected to get back a failure update");
    }

    public static Matcher<ConfigurationUpdateRequest> matchingUpdateRequest(ConfigurationUpdateRequest expected) {
        return new PropertyMatcher<ConfigurationUpdateRequest>(expected);
    }

}
