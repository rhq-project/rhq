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
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationFormat;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.clientapi.agent.configuration.ConfigurationAgentService;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.jmock.Expectations;
import org.jmock.Sequence;
import org.hamcrest.Matcher;

import javax.persistence.EntityManager;
import java.lang.reflect.Field;

public class ConfigurationManagerBeanUnitTest extends JMockTest {

    static final boolean FROM_STRUCTURED = true;
    static final boolean FROM_RAW = false;

    ConfigurationManagerLocal configurationMgrLocal;

    AgentManagerLocal agentMgr;

    AuthorizationManagerLocal authorizationMgr;

    EntityManager entityMgr;

    ConfigurationManagerBean configurationMgr;

    @BeforeMethod
    public void setup() throws Exception {
        configurationMgr = new ConfigurationManagerBean();

        configurationMgrLocal = context.mock(ConfigurationManagerLocal.class);
        setField(configurationMgr, "configurationManager", configurationMgrLocal);

        agentMgr = context.mock(AgentManagerLocal.class);
        setField(configurationMgr, "agentManager", agentMgr);

        authorizationMgr = context.mock(AuthorizationManagerLocal.class);
        setField(configurationMgr, "authorizationManager", authorizationMgr);

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

        assertSame(actualUpdate, expectedUpdate, "Expected to get back the persisted configuration update");
    }

    @Test
    public void updateResourceConfigShouldTranslateStructuredWhenStructredConfigHasEdits() throws Exception {
        final Subject subject = new Subject("rhqadmin", true, true);
        final int resourceId = -1;
        final Configuration newConfig = new Configuration();
        final boolean isPartOfGroupUpdate = false;

        final Configuration translatedConfig = newConfig.deepCopy();
        translatedConfig.addRawConfiguration(new RawConfiguration());

        final Resource resource = new Resource(resourceId);
        resource.setResourceType(createStructuredAndRaw());
        resource.setAgent(new Agent("test-agent", "localhost", 7080, null, null));

        final ResourceConfigurationUpdate expectedUpdate = new ResourceConfigurationUpdate(resource, translatedConfig,
            subject.getName());
        expectedUpdate.setId(-1);

        final AgentClient agentClient = context.mock(AgentClient.class);
        final ConfigurationAgentService configAgentService = context.mock(ConfigurationAgentService.class);

        final ConfigurationUpdateRequest expectedUpdateRequest = new ConfigurationUpdateRequest(expectedUpdate.getId(),
            expectedUpdate.getConfiguration(), expectedUpdate.getResource().getId());

        final Sequence configUdpate = context.sequence("structured-config-update");

        context.checking(new Expectations() {{
            allowing(entityMgr).find(Resource.class, resourceId); will(returnValue(resource));

            allowing(authorizationMgr).canViewResource(subject, resourceId); will(returnValue(true));

            oneOf(configAgentService).merge(newConfig, resourceId, FROM_STRUCTURED);
            will(returnValue(translatedConfig)); inSequence(configUdpate);

            oneOf(configurationMgrLocal).persistNewResourceConfigurationUpdateHistory(subject, resourceId,
                translatedConfig, INPROGRESS, subject.getName(), isPartOfGroupUpdate);
            will(returnValue(expectedUpdate));
            inSequence(configUdpate);

            allowing(agentMgr).getAgentClient(expectedUpdate.getResource().getAgent()); will(returnValue(agentClient));

            allowing(agentClient).getConfigurationAgentService(); will(returnValue(configAgentService));

            oneOf(configAgentService).updateResourceConfiguration(with(matchingUpdateRequest(expectedUpdateRequest)));
            inSequence(configUdpate);
        }});

        ResourceConfigurationUpdate actualUpdate = configurationMgr.updateResourceConfiguration(subject, resourceId,
            newConfig, FROM_STRUCTURED);

        assertSame(actualUpdate, expectedUpdate, "Expected to get back the persisted configuration update");
    }

    @Test
    public void updateResourceConfigShouldNotTranslateStructuredWhenRawNotSupported() throws Exception {
        final Subject subject = new Subject("rhqadmin", true, true);
        final int resourceId = -1;
        final Configuration newConfig = new Configuration();
        final boolean isPartOfGroupUpdate = false;

        final Resource resource = new Resource(resourceId);
        resource.setAgent(new Agent("test-agent", "localhost", 7080, null, null));
        resource.setResourceType(createStructurerdOnly());

        final ResourceConfigurationUpdate expectedUpdate = new ResourceConfigurationUpdate(resource, newConfig,
            subject.getName());
        expectedUpdate.setId(-1);

        final AgentClient agentClient = context.mock(AgentClient.class);
        final ConfigurationAgentService configAgentService = context.mock(ConfigurationAgentService.class);

        final ConfigurationUpdateRequest expectedUpdateRequest = new ConfigurationUpdateRequest(expectedUpdate.getId(),
            expectedUpdate.getConfiguration(), expectedUpdate.getResource().getId());

        final Sequence configUdpate = context.sequence("structured-config-update");

        context.checking(new Expectations() {{
            allowing(entityMgr).find(Resource.class, resourceId); will(returnValue(resource));

            oneOf(configurationMgrLocal).persistNewResourceConfigurationUpdateHistory(subject, resourceId, newConfig,
                INPROGRESS, subject.getName(), isPartOfGroupUpdate);
            will(returnValue(expectedUpdate));
            inSequence(configUdpate);

            allowing(agentMgr).getAgentClient(expectedUpdate.getResource().getAgent()); will(returnValue(agentClient));

            allowing(agentClient).getConfigurationAgentService(); will(returnValue(configAgentService));

            oneOf(configAgentService).updateResourceConfiguration(with(matchingUpdateRequest(expectedUpdateRequest)));
            inSequence(configUdpate);
        }});

        ResourceConfigurationUpdate actualUpdate = configurationMgr.updateResourceConfiguration(subject, resourceId,
            newConfig, FROM_STRUCTURED);

        assertSame(actualUpdate, expectedUpdate, "Expected to get back the persisted configuration update");
    }

    @Test
    public void updateResourceConfigShouldTranslateRawWhenRawConfigHasEdits() throws Exception {
        final Subject subject = new Subject("rhqadmin", true, true);
        final int resourceId = -1;
        final Configuration newConfig = new Configuration();
        newConfig.addRawConfiguration(new RawConfiguration());
        final boolean isPartOfGroupUpdate = false;

        final Configuration translatedConfig = newConfig.deepCopy();
        translatedConfig.put(new PropertySimple("x", "y"));

        final Resource resource = new Resource(resourceId);
        resource.setResourceType(createStructuredAndRaw());
        resource.setAgent(new Agent("test-agent", "localhost", 7080, null, null));

        final ResourceConfigurationUpdate expectedUpdate = new ResourceConfigurationUpdate(resource, translatedConfig,
            subject.getName());
        expectedUpdate.setId(-1);

        final AgentClient agentClient = context.mock(AgentClient.class);
        final ConfigurationAgentService configAgentService = context.mock(ConfigurationAgentService.class);

        final ConfigurationUpdateRequest expectedUpdateRequest = new ConfigurationUpdateRequest(expectedUpdate.getId(),
            expectedUpdate.getConfiguration(), expectedUpdate.getResource().getId());

        final Sequence configUdpate = context.sequence("raw-config-update");

        context.checking(new Expectations() {{
            allowing(entityMgr).find(Resource.class, resourceId); will(returnValue(resource));

            allowing(authorizationMgr).canViewResource(subject, resourceId); will(returnValue(true));

            oneOf(configAgentService).merge(newConfig, resourceId, FROM_RAW);
            will(returnValue(translatedConfig)); inSequence(configUdpate);

            oneOf(configurationMgrLocal).persistNewResourceConfigurationUpdateHistory(subject, resourceId,
                translatedConfig, INPROGRESS, subject.getName(), isPartOfGroupUpdate);
            will(returnValue(expectedUpdate));
            inSequence(configUdpate);

            allowing(agentMgr).getAgentClient(expectedUpdate.getResource().getAgent()); will(returnValue(agentClient));

            allowing(agentClient).getConfigurationAgentService(); will(returnValue(configAgentService));

            oneOf(configAgentService).updateResourceConfiguration(with(matchingUpdateRequest(expectedUpdateRequest)));
            inSequence(configUdpate);
        }});

        ResourceConfigurationUpdate actualUpdate = configurationMgr.updateResourceConfiguration(subject, resourceId,
            newConfig, FROM_RAW);

        assertSame(actualUpdate, expectedUpdate, "Expected to get back the persisted configuration update");
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

            oneOf(configurationMgrLocal).mergeConfigurationUpdate(expectedUpdate);

            allowing(agentMgr).getAgentClient(expectedUpdate.getResource().getAgent());
            will(throwException(exception));
        }});

        ResourceConfigurationUpdate actualUpdate = configurationMgr.updateResourceConfiguration(subject, resourceId,
            newConfig);

        assertPropertiesMatch(expectedUpdate, actualUpdate, "Expected to get back a failure update");
    }

    public static Matcher<ConfigurationUpdateRequest> matchingUpdateRequest(ConfigurationUpdateRequest expected) {
        return new PropertyMatcher<ConfigurationUpdateRequest>(expected);
    }

    ResourceType createStructuredAndRaw() {
        ConfigurationDefinition configDef = new ConfigurationDefinition("structured-and-raw", "structured and raw");
        configDef.setConfigurationFormat(ConfigurationFormat.STRUCTURED_AND_RAW);

        ResourceType resourceType = new ResourceType();
        resourceType.setResourceConfigurationDefinition(configDef);

        return resourceType;
    }

    ResourceType createStructurerdOnly() {
        ConfigurationDefinition configDef = new ConfigurationDefinition("structred-config-def", "structured config def");
        configDef.setConfigurationFormat(ConfigurationFormat.STRUCTURED);

        ResourceType resourceType = new ResourceType();
        resourceType.setResourceConfigurationDefinition(configDef);

        return resourceType;
    }

}
