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

import static org.rhq.core.domain.authz.Permission.CONFIGURE_READ;
import static org.rhq.core.domain.authz.Permission.CONFIGURE_WRITE;
import static org.rhq.core.domain.configuration.ConfigurationUpdateStatus.FAILURE;
import static org.rhq.core.domain.configuration.ConfigurationUpdateStatus.INPROGRESS;
import static org.rhq.test.AssertUtils.assertPropertiesMatch;
import static org.testng.Assert.assertSame;

import java.lang.reflect.Field;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.jmock.Sequence;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.configuration.ConfigurationAgentService;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationFormat;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.test.JMockTest;
import org.rhq.test.jmock.PropertyMatcher;

public class ConfigurationManagerBeanUnitTest extends JMockTest {

    static final boolean FROM_STRUCTURED = true;
    static final boolean FROM_RAW = false;
    static final Subject OVERLORD = new Subject("overlord", true, true);

    ConfigurationManagerLocal configurationMgrLocal;

    ResourceManagerLocal resourceMgr;

    SubjectManagerLocal subjectMgr;

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

        resourceMgr = context.mock(ResourceManagerLocal.class);
        setField(configurationMgr, "resourceManager", resourceMgr);

        subjectMgr = context.mock(SubjectManagerLocal.class);
        setField(configurationMgr, "subjectManager", subjectMgr);
    }

    static void setField(Object src, String fieldName, Object value) {
        try {
            Field field = src.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(src, value);
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (NoSuchFieldException e) {
            String msg = "The field <" + fieldName + "> does not exist for " + src.getClass().getName();
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    public void updateStructuredResourceConfigShouldUpdateAuditTrailAndSendUpdateToAgent() throws Exception {
        final ResourceConfigUpdateFixture fixture = newStructuredResourceConfigUpdateFixture();

        final ResourceConfigurationUpdate expectedUpdate = new ResourceConfigurationUpdate(fixture.resource,
            fixture.configuration, fixture.subject.getName());

        final AgentClient agentClient = context.mock(AgentClient.class);
        final ConfigurationAgentService configAgentService = context.mock(ConfigurationAgentService.class);

        final ConfigurationUpdateRequest expectedUpdateRequest = new ConfigurationUpdateRequest(expectedUpdate.getId(),
            expectedUpdate.getConfiguration(), expectedUpdate.getResource().getId());

        final Query query = context.mock(Query.class);

        context.checking(new Expectations() {
            {
                allowing(subjectMgr).getOverlord();
                will(returnValue(OVERLORD));

                allowing(resourceMgr).getResource(OVERLORD, fixture.resourceId);
                will(returnValue(fixture.resource));

                oneOf(authorizationMgr).hasResourcePermission(fixture.subject, CONFIGURE_WRITE, fixture.resourceId);
                will(returnValue(true));

                allowing(entityMgr).find(Resource.class, fixture.resourceId);
                will(returnValue(fixture.resource));

                oneOf(configurationMgrLocal).persistResourceConfigurationUpdateInNewTransaction(fixture.subject,
                    fixture.resourceId, fixture.configuration, INPROGRESS, fixture.subject.getName(),
                    fixture.isPartOfGroupUpdate);
                will(returnValue(expectedUpdate));

                allowing(agentMgr).getAgentClient(expectedUpdate.getResource().getAgent());
                will(returnValue(agentClient));

                allowing(agentClient).getConfigurationAgentService();
                will(returnValue(configAgentService));

                oneOf(configAgentService).updateResourceConfiguration(
                    with(matchingUpdateRequest(expectedUpdateRequest)));

                allowing(entityMgr).createNamedQuery("ConfigurationDefinition.findResourceByResourceTypeId");
                will(returnValue(query));
                allowing(query).setParameter("resourceTypeId", fixture.resourceTypeId);
                allowing(query).getSingleResult();
                will(returnValue(fixture.configurationDefinition));

                oneOf(configurationMgrLocal).getResourceConfigurationDefinitionForResourceType(fixture.subject,
                    fixture.resourceTypeId);
                will(returnValue(fixture.configurationDefinition));
            }
        });

        ResourceConfigurationUpdate actualUpdate = configurationMgr.updateResourceConfiguration(fixture.subject,
            fixture.resourceId, fixture.configuration);

        assertSame(actualUpdate, expectedUpdate, "Expected to get back the persisted configuration update");
    }

    @Test
    public void updateRawResourceConfigShouldUpdateAuditTrailAndSendUpdateToAgent() throws Exception {
        final ResourceConfigUpdateFixture fixture = newRawResourceConfigUpdateFixture();

        final ResourceConfigurationUpdate expectedUpdate = new ResourceConfigurationUpdate(fixture.resource,
            fixture.configuration, fixture.subject.getName());

        final AgentClient agentClient = context.mock(AgentClient.class);
        final ConfigurationAgentService configAgentService = context.mock(ConfigurationAgentService.class);

        final ConfigurationUpdateRequest expectedUpdateRequest = new ConfigurationUpdateRequest(expectedUpdate.getId(),
            expectedUpdate.getConfiguration(), expectedUpdate.getResource().getId());

        final Query query = context.mock(Query.class);

        context.checking(new Expectations() {
            {
                allowing(subjectMgr).getOverlord();
                will(returnValue(OVERLORD));

                allowing(resourceMgr).getResource(OVERLORD, fixture.resourceId);
                will(returnValue(fixture.resource));

                allowing(entityMgr).find(Resource.class, fixture.resourceId);
                will(returnValue(fixture.resource));

                oneOf(authorizationMgr).hasResourcePermission(fixture.subject, CONFIGURE_WRITE, fixture.resourceId);
                will(returnValue(true));

                oneOf(configurationMgrLocal).persistResourceConfigurationUpdateInNewTransaction(fixture.subject,
                    fixture.resourceId, fixture.configuration, INPROGRESS, fixture.subject.getName(),
                    fixture.isPartOfGroupUpdate);
                will(returnValue(expectedUpdate));

                allowing(agentMgr).getAgentClient(expectedUpdate.getResource().getAgent());
                will(returnValue(agentClient));

                allowing(agentClient).getConfigurationAgentService();
                will(returnValue(configAgentService));

                oneOf(configAgentService).updateResourceConfiguration(
                    with(matchingUpdateRequest(expectedUpdateRequest)));

                allowing(entityMgr).createNamedQuery("ConfigurationDefinition.findResourceByResourceTypeId");
                will(returnValue(query));
                allowing(query).setParameter("resourceTypeId", fixture.resourceTypeId);
                allowing(query).getSingleResult();
                will(returnValue(fixture.configurationDefinition));

                oneOf(configurationMgrLocal).getResourceConfigurationDefinitionForResourceType(fixture.subject,
                    fixture.resourceTypeId);
                will(returnValue(fixture.configurationDefinition));
            }
        });

        ResourceConfigurationUpdate actualUpdate = configurationMgr.updateResourceConfiguration(fixture.subject,
            fixture.resourceId, fixture.configuration);

        assertSame(actualUpdate, expectedUpdate, "Expected to get back the persisted configuration update");
    }

    @Test(expectedExceptions = ConfigurationUpdateNotSupportedException.class)
    public void exceptionShouldBeThrownWhenCallingWrongMethodForConfigThatSupportsStructuredAndRaw() throws Exception {
        final ResourceConfigUpdateFixture fixture = newStructuredAndRawResourceConfigUpdateFixture();

        context.checking(new Expectations() {
            {
                allowing(entityMgr).find(Resource.class, fixture.resourceId);
                will(returnValue(fixture.resource));
            }
        });

        configurationMgr.updateResourceConfiguration(fixture.subject, fixture.resourceId, fixture.configuration);
    }

    @Test
    public void updateResourceConfigShouldNotTranslateStructuredWhenRawNotSupported() throws Exception {
        final ResourceConfigUpdateFixture fixture = newStructuredResourceConfigUpdateFixture();

        final ResourceConfigurationUpdate expectedUpdate = new ResourceConfigurationUpdate(fixture.resource,
            fixture.configuration, fixture.subject.getName());
        expectedUpdate.setId(-1);

        final AgentClient agentClient = context.mock(AgentClient.class);
        final ConfigurationAgentService configAgentService = context.mock(ConfigurationAgentService.class);

        final ConfigurationUpdateRequest expectedUpdateRequest = new ConfigurationUpdateRequest(expectedUpdate.getId(),
            expectedUpdate.getConfiguration(), expectedUpdate.getResource().getId());

        final Sequence configUdpate = context.sequence("structured-config-update");

        context.checking(new Expectations() {
            {
                oneOf(authorizationMgr).canViewResource(fixture.subject, fixture.resourceId);
                will(returnValue(true));

                oneOf(authorizationMgr).hasResourcePermission(fixture.subject, CONFIGURE_WRITE, fixture.resourceId);
                will(returnValue(true));

                allowing(entityMgr).find(Resource.class, fixture.resourceId);
                will(returnValue(fixture.resource));

                allowing(agentMgr).getAgentClient(expectedUpdate.getResource().getAgent());
                will(returnValue(agentClient));

                allowing(agentClient).getConfigurationAgentService();
                will(returnValue(configAgentService));

                oneOf(configAgentService).validate(fixture.configuration, fixture.resourceId, FROM_STRUCTURED);
                inSequence(configUdpate);

                oneOf(configurationMgrLocal).persistResourceConfigurationUpdateInNewTransaction(fixture.subject,
                    fixture.resourceId, fixture.configuration, INPROGRESS, fixture.subject.getName(),
                    fixture.isPartOfGroupUpdate);
                will(returnValue(expectedUpdate));
                inSequence(configUdpate);

                oneOf(configAgentService).updateResourceConfiguration(
                    with(matchingUpdateRequest(expectedUpdateRequest)));
                inSequence(configUdpate);
            }
        });

        ResourceConfigurationUpdate actualUpdate = configurationMgr.updateStructuredOrRawConfiguration(fixture.subject,
            fixture.resourceId, fixture.configuration, FROM_STRUCTURED);

        assertSame(actualUpdate, expectedUpdate, "Expected to get back the persisted configuration update");
    }

    @Test
    public void updateResourceConfigShouldTranslateRawWhenRawConfigHasEdits() throws Exception {
        final ResourceConfigUpdateFixture fixture = newStructuredAndRawResourceConfigUpdateFixture();
        fixture.configuration.addRawConfiguration(new RawConfiguration());

        final Configuration translatedConfig = fixture.configuration.deepCopy();
        translatedConfig.put(new PropertySimple("x", "y"));

        final ResourceConfigurationUpdate expectedUpdate = new ResourceConfigurationUpdate(fixture.resource,
            translatedConfig, fixture.subject.getName());
        expectedUpdate.setId(-1);

        final AgentClient agentClient = context.mock(AgentClient.class);
        final ConfigurationAgentService configAgentService = context.mock(ConfigurationAgentService.class);

        final ConfigurationUpdateRequest expectedUpdateRequest = new ConfigurationUpdateRequest(expectedUpdate.getId(),
            expectedUpdate.getConfiguration(), expectedUpdate.getResource().getId());

        final Sequence configUdpate = context.sequence("raw-config-update");

        context.checking(new Expectations() {
            {
                oneOf(authorizationMgr).hasResourcePermission(fixture.subject, CONFIGURE_READ, fixture.resourceId);
                will(returnValue(true));

                oneOf(authorizationMgr).hasResourcePermission(fixture.subject, CONFIGURE_WRITE, fixture.resourceId);
                will(returnValue(true));

                allowing(entityMgr).find(Resource.class, fixture.resourceId);
                will(returnValue(fixture.resource));

                allowing(authorizationMgr).canViewResource(fixture.subject, fixture.resourceId);
                will(returnValue(true));

                oneOf(configAgentService).merge(fixture.configuration, fixture.resourceId, FROM_RAW);
                will(returnValue(translatedConfig));
                inSequence(configUdpate);

                oneOf(configurationMgrLocal).persistResourceConfigurationUpdateInNewTransaction(fixture.subject,
                    fixture.resourceId, translatedConfig, INPROGRESS, fixture.subject.getName(),
                    fixture.isPartOfGroupUpdate);
                will(returnValue(expectedUpdate));
                inSequence(configUdpate);

                allowing(agentMgr).getAgentClient(expectedUpdate.getResource().getAgent());
                will(returnValue(agentClient));

                allowing(agentClient).getConfigurationAgentService();
                will(returnValue(configAgentService));

                oneOf(configAgentService).validate(fixture.configuration, fixture.resourceId, FROM_RAW);

                oneOf(configAgentService).updateResourceConfiguration(
                    with(matchingUpdateRequest(expectedUpdateRequest)));
                inSequence(configUdpate);
            }
        });

        ResourceConfigurationUpdate actualUpdate = configurationMgr.updateStructuredOrRawConfiguration(fixture.subject,
            fixture.resourceId, fixture.configuration, FROM_RAW);

        assertSame(actualUpdate, expectedUpdate, "Expected to get back the persisted configuration update");
    }

    @Test
    public void updateResourceConfigShouldMarkUpdateAsFailureWhenExceptionOccurs() throws Exception {
        final ResourceConfigUpdateFixture fixture = newStructuredResourceConfigUpdateFixture();

        final RuntimeException exception = new RuntimeException();

        final ResourceConfigurationUpdate expectedUpdate = new ResourceConfigurationUpdate(fixture.resource,
            fixture.configuration, fixture.subject.getName());
        expectedUpdate.setId(-1);
        expectedUpdate.setStatus(FAILURE);
        expectedUpdate.setErrorMessage(ThrowableUtil.getStackAsString(exception));

        final Query query = context.mock(Query.class);

        context.checking(new Expectations() {
            {
                allowing(subjectMgr).getOverlord();
                will(returnValue(OVERLORD));

                allowing(resourceMgr).getResource(OVERLORD, fixture.resourceId);
                will(returnValue(fixture.resource));

                allowing(entityMgr).find(Resource.class, fixture.resourceId);
                will(returnValue(fixture.resource));

                allowing(entityMgr).find(ResourceConfigurationUpdate.class, expectedUpdate.getId());
                will(returnValue(expectedUpdate));

                oneOf(authorizationMgr).hasResourcePermission(fixture.subject, CONFIGURE_WRITE, fixture.resourceId);
                will(returnValue(true));

                oneOf(configurationMgrLocal).persistResourceConfigurationUpdateInNewTransaction(fixture.subject,
                    fixture.resourceId, fixture.configuration, INPROGRESS, fixture.subject.getName(),
                    fixture.isPartOfGroupUpdate);
                will(returnValue(expectedUpdate));

                oneOf(configurationMgrLocal).mergeConfigurationUpdate(expectedUpdate);

                allowing(agentMgr).getAgentClient(expectedUpdate.getResource().getAgent());
                will(throwException(exception));

                allowing(entityMgr).createNamedQuery("ConfigurationDefinition.findResourceByResourceTypeId");
                will(returnValue(query));
                allowing(query).setParameter("resourceTypeId", fixture.resourceTypeId);
                allowing(query).getSingleResult();
                will(returnValue(fixture.configurationDefinition));

                oneOf(configurationMgrLocal).getResourceConfigurationDefinitionForResourceType(fixture.subject,
                    fixture.resourceTypeId);
                will(returnValue(fixture.configurationDefinition));
            }
        });

        ResourceConfigurationUpdate actualUpdate = configurationMgr.updateResourceConfiguration(fixture.subject,
            fixture.resourceId, fixture.configuration);

        assertPropertiesMatch(expectedUpdate, actualUpdate, "Expected to get back a failure update");
    }

    @Test
    public void translatingFromStructuredToRawShouldReturnModifiedConfig() throws Exception {
        final ConfigTranslationFixture fixture = newStructuredAndRawTranslationFixture();

        final AgentClient agentClient = context.mock(AgentClient.class);
        final ConfigurationAgentService configAgentService = context.mock(ConfigurationAgentService.class);

        final Configuration expectedConfig = new Configuration();

        context.checking(new Expectations() {
            {
                allowing(entityMgr).find(Resource.class, fixture.resourceId);
                will(returnValue(fixture.resource));

                allowing(agentMgr).getAgentClient(fixture.resource.getAgent());
                will(returnValue(agentClient));

                oneOf(authorizationMgr).hasResourcePermission(fixture.subject, CONFIGURE_READ, fixture.resourceId);
                will(returnValue(true));

                allowing(agentClient).getConfigurationAgentService();
                will(returnValue(configAgentService));

                oneOf(configAgentService).merge(fixture.configuration, fixture.resourceId, FROM_STRUCTURED);
                will(returnValue(expectedConfig));
            }
        });

        Configuration translatedConfig = configurationMgr.translateResourceConfiguration(fixture.subject,
            fixture.resourceId, fixture.configuration, FROM_STRUCTURED);

        assertSame(translatedConfig, expectedConfig, "Expected to get back the configuration translated by the "
            + ConfigurationAgentService.class.getSimpleName());
    }

    @Test
    public void translatingFromRawToStructuredShouldReturnModifiedConfig() throws Exception {
        final ConfigTranslationFixture fixture = newStructuredAndRawTranslationFixture();

        final AgentClient agentClient = context.mock(AgentClient.class);
        final ConfigurationAgentService configAgentService = context.mock(ConfigurationAgentService.class);

        final Configuration expectedConfig = new Configuration();

        context.checking(new Expectations() {
            {
                allowing(entityMgr).find(Resource.class, fixture.resourceId);
                will(returnValue(fixture.resource));

                oneOf(authorizationMgr).hasResourcePermission(fixture.subject, CONFIGURE_READ, fixture.resourceId);
                will(returnValue(true));

                allowing(agentMgr).getAgentClient(fixture.resource.getAgent());
                will(returnValue(agentClient));

                allowing(agentClient).getConfigurationAgentService();
                will(returnValue(configAgentService));

                oneOf(configAgentService).merge(fixture.configuration, fixture.resourceId, FROM_RAW);
                will(returnValue(expectedConfig));
            }
        });

        Configuration translatedConfig = configurationMgr.translateResourceConfiguration(fixture.subject,
            fixture.resourceId, fixture.configuration, FROM_RAW);

        assertSame(translatedConfig, expectedConfig, "Expected to get back the configuration translated by the "
            + ConfigurationAgentService.class.getSimpleName());
    }

    @Test(expectedExceptions = TranslationNotSupportedException.class)
    public void exceptionShouldBeThrownWhenTryingToTranslateStructuredOnlyConfig() throws Exception {
        final ConfigTranslationFixture fixture = newStructuredTranslationFixture();

        context.checking(new Expectations() {
            {
                allowing(entityMgr).find(Resource.class, fixture.resourceId);
                will(returnValue(fixture.resource));
            }
        });

        configurationMgr.translateResourceConfiguration(fixture.subject, fixture.resourceId, fixture.configuration,
            FROM_STRUCTURED);
    }

    @Test(expectedExceptions = TranslationNotSupportedException.class)
    public void exceptionShouldBeThrownWhenTryingToTranslateRawOnlyConfig() throws Exception {
        final ConfigTranslationFixture fixture = newRawTranslationFixture();

        context.checking(new Expectations() {
            {
                allowing(entityMgr).find(Resource.class, fixture.resourceId);
                will(returnValue(fixture.resource));
            }
        });

        configurationMgr.translateResourceConfiguration(fixture.subject, fixture.resourceId, fixture.configuration,
            FROM_RAW);
    }

    public static Matcher<ConfigurationUpdateRequest> matchingUpdateRequest(ConfigurationUpdateRequest expected) {
        return new PropertyMatcher<ConfigurationUpdateRequest>(expected);
    }

    ResourceConfigUpdateFixture newStructuredResourceConfigUpdateFixture() {
        ResourceConfigUpdateFixture fixture = new ResourceConfigUpdateFixture();
        fixture.resource = createResourceWithStructuredConfig(fixture.resourceId);

        return fixture;
    }

    ResourceConfigUpdateFixture newRawResourceConfigUpdateFixture() {
        ResourceConfigUpdateFixture fixture = new ResourceConfigUpdateFixture();
        fixture.resource = createResourceWithRawConfig(fixture.resourceId);

        return fixture;
    }

    ResourceConfigUpdateFixture newStructuredAndRawResourceConfigUpdateFixture() {
        ResourceConfigUpdateFixture fixture = new ResourceConfigUpdateFixture();
        fixture.resource = createResourceWithStructuredAndRawConfig(fixture.resourceId);

        return fixture;
    }

    ConfigTranslationFixture newStructuredAndRawTranslationFixture() {
        ConfigTranslationFixture fixture = new ConfigTranslationFixture();
        fixture.resource = createResourceWithStructuredAndRawConfig(fixture.resourceId);

        return fixture;
    }

    ConfigTranslationFixture newStructuredTranslationFixture() {
        ConfigTranslationFixture fixture = new ConfigTranslationFixture();
        fixture.resource = createResourceWithStructuredConfig(fixture.resourceId);

        return fixture;
    }

    ConfigTranslationFixture newRawTranslationFixture() {
        ConfigTranslationFixture fixture = new ConfigTranslationFixture();
        fixture.resource = createResourceWithRawConfig(fixture.resourceId);

        return fixture;
    }

    Resource createResource(int resourceId) {
        Resource resource = new Resource(resourceId);
        resource.setAgent(new Agent("test-agent", "localhost", 7080, null, null));
        return resource;
    }

    Resource createResourceWithStructuredConfig(int resourceId) {
        Resource resource = createResource(resourceId);
        resource.setResourceType(createStructurerdOnlyResourceType());

        return resource;
    }

    Resource createResourceWithStructuredAndRawConfig(int resourceId) {
        Resource resource = createResource(resourceId);
        resource.setResourceType(createStructuredAndRawResourceType());

        return resource;
    }

    Resource createResourceWithRawConfig(int resourceId) {
        Resource resource = createResource(resourceId);
        resource.setResourceType(createRawResourceType());

        return resource;
    }

    ResourceType createStructuredAndRawResourceType() {
        ConfigurationDefinition configDef = new ConfigurationDefinition("structured-and-raw", "structured and raw");
        configDef.setConfigurationFormat(ConfigurationFormat.STRUCTURED_AND_RAW);

        ResourceType resourceType = new ResourceType();
        resourceType.setResourceConfigurationDefinition(configDef);

        return resourceType;
    }

    ResourceType createStructurerdOnlyResourceType() {
        ConfigurationDefinition configDef = new ConfigurationDefinition("structred-config-def", "structured config def");
        configDef.setConfigurationFormat(ConfigurationFormat.STRUCTURED);

        ResourceType resourceType = new ResourceType();
        resourceType.setResourceConfigurationDefinition(configDef);

        return resourceType;
    }

    ResourceType createRawResourceType() {
        ConfigurationDefinition configDef = new ConfigurationDefinition("raw-config-def", "raw config def");
        configDef.setConfigurationFormat(ConfigurationFormat.RAW);

        ResourceType resourceType = new ResourceType();
        resourceType.setResourceConfigurationDefinition(configDef);

        return resourceType;
    }

    static class ResourceConfigUpdateFixture {
        Subject subject = new Subject("rhqadmin", true, true);
        int resourceId = -1;
        int resourceTypeId = 0;
        Configuration configuration = new Configuration();
        boolean isPartOfGroupUpdate = false;
        Resource resource;
        ConfigurationDefinition configurationDefinition = new ConfigurationDefinition("name", "description");
    }

    static class ConfigTranslationFixture {
        Subject subject = new Subject("rhqadmin", true, true);
        int resourceId = -1;
        Configuration configuration = new Configuration();
        Resource resource;
    }

}
