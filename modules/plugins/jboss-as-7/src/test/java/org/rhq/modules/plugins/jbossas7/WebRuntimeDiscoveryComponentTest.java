/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.jbossas7;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.rhq.modules.plugins.jbossas7.json.Result.SUCCESS;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.util.ResponseTimeConfiguration;
import org.rhq.modules.plugins.jbossas7.helper.ServerPluginConfiguration;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * @author Thomas Segismont
 */
public class WebRuntimeDiscoveryComponentTest {
    private static final String WEBAPP = "coregui";
    private static final String PATH_ATTRIBUTE = "path";
    private static final String PARENT_COMPONENT_PATH_IN_STANDALONE_SERVER = "subdeployment=" + WEBAPP + ".war";
    private static final String MANAGED_SERVER_NAME = "server-one";
    private static final String PARENT_COMPONENT_PATH_IN_MANAGED_SERVER = "server=" + MANAGED_SERVER_NAME
        + ",subdeployment=" + WEBAPP + ".war";
    private static final String CONF_PATH = "subsystem=web";

    private WebRuntimeDiscoveryComponent discoveryComponent;

    @Mock
    private ResourceDiscoveryContext discoveryContext;

    @Mock
    private ResourceContext parentResourceContext;

    @Mock
    private BaseComponent parentResourceComponent;

    @Mock
    private ASConnection asConnection;

    @Mock
    private ResourceType resourceType;

    @Mock
    private StandaloneASComponent standaloneASComponent;

    @Mock
    private HostControllerComponent hostControllerComponent;

    @BeforeMethod(alwaysRun = true)
    public void setup() throws Exception {
        discoveryComponent = new WebRuntimeDiscoveryComponent();
        MockitoAnnotations.initMocks(this);
        when(discoveryContext.getParentResourceComponent()).thenReturn(parentResourceComponent);
        when(discoveryContext.getParentResourceContext()).thenReturn(parentResourceContext);
        when(parentResourceComponent.getASConnection()).thenReturn(asConnection);
        when(discoveryContext.getDefaultPluginConfiguration()).thenReturn(createDefaultPluginConfig());
        when(asConnection.execute(any(ReadResource.class))).thenReturn(createReadResourceResult());
        when(discoveryContext.getResourceType()).thenReturn(resourceType);
        when(resourceType.getDescription()).thenReturn("Description");
        when(asConnection.execute(any(ReadAttribute.class))).thenReturn(createReadAttributeResult());
    }

    private Configuration createDefaultPluginConfig() {
        Configuration configuration = new Configuration();
        configuration.setSimpleValue(PATH_ATTRIBUTE, CONF_PATH);
        return configuration;
    }

    private Result createReadResourceResult() {
        Result result = new Result();
        result.setOutcome(SUCCESS);
        return result;
    }

    private Result createReadAttributeResult() {
        Result result = new Result();
        result.setOutcome(SUCCESS);
        result.setResult("/" + WEBAPP);
        return result;
    }

    @Test
    public void shouldFindResponseTimeFilterLogInStandaloneServer() throws Exception {
        when(parentResourceComponent.getPath()).thenReturn(PARENT_COMPONENT_PATH_IN_STANDALONE_SERVER);
        when(parentResourceComponent.getServerComponent()).thenReturn(standaloneASComponent);
        when(standaloneASComponent.isManuallyAddedServer()).thenReturn(Boolean.FALSE);
        ServerPluginConfiguration standaloneServerPluginConfig = createStandaloneServerPluginConfig();
        when(standaloneASComponent.getServerPluginConfiguration()).thenReturn(standaloneServerPluginConfig);
        File standaloneResponseTimeLogFile = createStandaloneServerResponseTimeLogFile(standaloneServerPluginConfig);

        Set<DiscoveredResourceDetails> details = discoveryComponent.discoverResources(discoveryContext);
        assertNotNull(details);
        assertEquals(details.size(), 1);
        DiscoveredResourceDetails discoveredResourceDetails = details.iterator().next();
        String rtFilePath = discoveredResourceDetails.getPluginConfiguration().getSimpleValue(
            ResponseTimeConfiguration.RESPONSE_TIME_LOG_FILE_CONFIG_PROP);
        assertNotNull(rtFilePath);
        assertEquals(rtFilePath, standaloneResponseTimeLogFile.getAbsolutePath());
    }

    private ServerPluginConfiguration createStandaloneServerPluginConfig() throws Exception {
        ServerPluginConfiguration mock = mock(ServerPluginConfiguration.class);
        when(mock.getLogDir()).thenReturn(createTempDir());
        return mock;
    }

    private File createStandaloneServerResponseTimeLogFile(ServerPluginConfiguration standaloneServerPluginConfig)
        throws IOException {
        File rtDir = new File(standaloneServerPluginConfig.getLogDir(), "rt");
        rtDir.mkdir();
        File logFile = new File(rtDir, WEBAPP + "_rt.log");
        logFile.createNewFile();
        return logFile;
    }

    @Test
    public void shouldNotFindResponseTimeFilterLogInManuallyAddedStandaloneServer() throws Exception {
        when(parentResourceComponent.getPath()).thenReturn(PARENT_COMPONENT_PATH_IN_STANDALONE_SERVER);
        when(parentResourceComponent.getServerComponent()).thenReturn(standaloneASComponent);
        when(standaloneASComponent.isManuallyAddedServer()).thenReturn(Boolean.TRUE);

        Set<DiscoveredResourceDetails> details = discoveryComponent.discoverResources(discoveryContext);
        assertNotNull(details);
        assertEquals(details.size(), 1);
        DiscoveredResourceDetails discoveredResourceDetails = details.iterator().next();
        String rtFilePath = discoveredResourceDetails.getPluginConfiguration().getSimpleValue(
            ResponseTimeConfiguration.RESPONSE_TIME_LOG_FILE_CONFIG_PROP);
        assertNull(rtFilePath);
    }

    @Test
    public void shouldFindResponseTimeFilterLogInManagedServer() throws Exception {
        when(parentResourceComponent.getPath()).thenReturn(PARENT_COMPONENT_PATH_IN_MANAGED_SERVER);
        when(parentResourceComponent.getServerComponent()).thenReturn(hostControllerComponent);
        when(hostControllerComponent.isManuallyAddedServer()).thenReturn(Boolean.FALSE);
        ServerPluginConfiguration hostControllerServerPluginConfig = createHostControllerServerPluginConfig();
        when(hostControllerComponent.getServerPluginConfiguration()).thenReturn(hostControllerServerPluginConfig);
        File managedServerResponseTimeLogFile = createManagedServerResponseTimeLogFile(hostControllerServerPluginConfig);

        Set<DiscoveredResourceDetails> details = discoveryComponent.discoverResources(discoveryContext);
        assertNotNull(details);
        assertEquals(details.size(), 1);
        DiscoveredResourceDetails discoveredResourceDetails = details.iterator().next();
        String rtFilePath = discoveredResourceDetails.getPluginConfiguration().getSimpleValue(
            ResponseTimeConfiguration.RESPONSE_TIME_LOG_FILE_CONFIG_PROP);
        assertNotNull(rtFilePath);
        assertEquals(rtFilePath, managedServerResponseTimeLogFile.getAbsolutePath());
    }

    private ServerPluginConfiguration createHostControllerServerPluginConfig() throws Exception {
        ServerPluginConfiguration mock = mock(ServerPluginConfiguration.class);
        when(mock.getBaseDir()).thenReturn(createTempDir());
        return mock;
    }

    private File createManagedServerResponseTimeLogFile(ServerPluginConfiguration hostControllerServerPluginConfig)
        throws IOException {
        File rtDir = new File(hostControllerServerPluginConfig.getBaseDir(), "servers" + File.separator
            + MANAGED_SERVER_NAME + File.separator + "log" + File.separator + "rt");
        rtDir.mkdirs();
        File logFile = new File(rtDir, WEBAPP + "_rt.log");
        logFile.createNewFile();
        return logFile;
    }

    @Test
    public void shouldNotFindResponseTimeFilterLogInManuallyAddedManagedServer() throws Exception {
        when(parentResourceComponent.getPath()).thenReturn(PARENT_COMPONENT_PATH_IN_MANAGED_SERVER);
        when(parentResourceComponent.getServerComponent()).thenReturn(hostControllerComponent);
        when(hostControllerComponent.isManuallyAddedServer()).thenReturn(Boolean.TRUE);

        Set<DiscoveredResourceDetails> details = discoveryComponent.discoverResources(discoveryContext);
        assertNotNull(details);
        assertEquals(details.size(), 1);
        DiscoveredResourceDetails discoveredResourceDetails = details.iterator().next();
        String rtFilePath = discoveredResourceDetails.getPluginConfiguration().getSimpleValue(
            ResponseTimeConfiguration.RESPONSE_TIME_LOG_FILE_CONFIG_PROP);
        assertNull(rtFilePath);
    }

    private static File createTempDir() throws Exception {
        // Java 1.6 has no temp dir creation util
        File tempDir = File.createTempFile("pipo-", ".tmp");
        if (tempDir.delete() && tempDir.mkdir()) {
            tempDir.deleteOnExit();
            return tempDir;
        }
        throw new RuntimeException("Could not create temp directory");
    }
}
