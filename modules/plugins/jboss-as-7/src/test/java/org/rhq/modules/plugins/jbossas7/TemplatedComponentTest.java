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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.testng.Assert;
import org.testng.IObjectFactory;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * @author Stefan Negrea
 *
 */
@PrepareForTest({ TemplatedComponent.class })
public class TemplatedComponentTest {

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void loadResourceConfigurationWithType() throws Exception {
        //tell the method story as it happens: mock or create dependencies and configure
        //those dependencies to get the method under test to completion.
        ResourceContext mockResourceContext = mock(ResourceContext.class);

        ResourceType mockResourceType = mock(ResourceType.class);
        when(mockResourceContext.getResourceType()).thenReturn(mockResourceType);

        ConfigurationDefinition mockConfigurationDefinition = mock(ConfigurationDefinition.class);
        when(mockResourceType.getResourceConfigurationDefinition()).thenReturn(mockConfigurationDefinition);

        ConfigurationTemplate mockConfigurationTemplate = mock(ConfigurationTemplate.class);
        when(mockConfigurationDefinition.getDefaultTemplate()).thenReturn(mockConfigurationTemplate);

        Configuration mockConfiguration = mock(Configuration.class);
        when(mockConfigurationTemplate.getConfiguration()).thenReturn(mockConfiguration);

        Property mockProperty = mock(Property.class);
        when(mockConfiguration.get(eq("__type"))).thenReturn(mockProperty);

        Map<String, PropertyDefinition> mockMap = (Map<String, PropertyDefinition>) mock(Map.class);
        when(mockConfigurationDefinition.getPropertyDefinitions()).thenReturn(mockMap);

        ConfigurationLoadDelegate mockConfigurationLoadDelegate = mock(ConfigurationLoadDelegate.class);
        PowerMockito.whenNew(ConfigurationLoadDelegate.class)
            .withParameterTypes(ConfigurationDefinition.class, ASConnection.class, Address.class)
            .withArguments(any(ConfigurationDefinition.class), any(ASConnection.class), any(Address.class))
            .thenReturn(mockConfigurationLoadDelegate);

        when(mockConfigurationLoadDelegate.loadResourceConfiguration()).thenReturn(mockConfiguration);

        PropertySimple pathPropertySimple = new PropertySimple("path", "abc=def,xyz=test1");
        when(mockConfiguration.get(eq("path"))).thenReturn(pathPropertySimple);
        when(mockResourceContext.getPluginConfiguration()).thenReturn(mockConfiguration);

        ASConnection mockASConnection = mock(ASConnection.class);

        PropertySimple typePropertySimple = new PropertySimple("__type", "xyz");
        PowerMockito.whenNew(PropertySimple.class).withParameterTypes(String.class, Object.class)
            .withArguments(eq("__type"), eq("xyz")).thenReturn(typePropertySimple);

        //create object to test and inject required dependencies
        TemplatedComponent objectUnderTest = new TemplatedComponent();

        objectUnderTest.context = mockResourceContext;
        objectUnderTest.testConnection = mockASConnection;

        //run code under test
        Configuration result = objectUnderTest.loadResourceConfiguration();

        //verify the results (Assert and mock verification)
        Assert.assertEquals(result, mockConfiguration);

        verify(mockMap, times(1)).remove(eq("__type"));
        verify(mockConfiguration, times(1)).get(eq("__type"));
        verify(mockConfiguration, never()).get(eq("__name"));
        verify(mockConfiguration, times(1)).get(eq("path"));
        verify(mockConfiguration, times(1)).put(eq(typePropertySimple));

        PowerMockito.verifyNew(PropertySimple.class).withArguments(eq("__type"), eq("xyz"));
        PowerMockito.verifyNew(ConfigurationLoadDelegate.class).withArguments(any(ConfigurationDefinition.class),
            eq(mockASConnection), any(Address.class));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void loadResourceConfigurationWithName() throws Exception {
        //tell the method story as it happens: mock or create dependencies and configure
        //those dependencies to get the method under test to completion.
        ResourceContext mockResourceContext = mock(ResourceContext.class);

        ResourceType mockResourceType = mock(ResourceType.class);
        when(mockResourceContext.getResourceType()).thenReturn(mockResourceType);

        ConfigurationDefinition mockConfigurationDefinition = mock(ConfigurationDefinition.class);
        when(mockResourceType.getResourceConfigurationDefinition()).thenReturn(mockConfigurationDefinition);

        ConfigurationTemplate mockConfigurationTemplate = mock(ConfigurationTemplate.class);
        when(mockConfigurationDefinition.getDefaultTemplate()).thenReturn(mockConfigurationTemplate);

        Configuration mockConfiguration = mock(Configuration.class);
        when(mockConfigurationTemplate.getConfiguration()).thenReturn(mockConfiguration);

        Property mockProperty = mock(Property.class);
        when(mockConfiguration.get("_type")).thenReturn(null);
        when(mockConfiguration.get(eq("__name"))).thenReturn(mockProperty);


        Map<String, PropertyDefinition> mockMap = (Map<String, PropertyDefinition>) mock(Map.class);
        when(mockConfigurationDefinition.getPropertyDefinitions()).thenReturn(mockMap);

        ConfigurationLoadDelegate mockConfigurationLoadDelegate = mock(ConfigurationLoadDelegate.class);
        PowerMockito.whenNew(ConfigurationLoadDelegate.class)
            .withParameterTypes(ConfigurationDefinition.class, ASConnection.class, Address.class)
            .withArguments(any(ConfigurationDefinition.class), any(ASConnection.class), any(Address.class))
            .thenReturn(mockConfigurationLoadDelegate);

        when(mockConfigurationLoadDelegate.loadResourceConfiguration()).thenReturn(mockConfiguration);

        PropertySimple pathPropertySimple = new PropertySimple("path", "abc=def,xyz=test1");
        when(mockConfiguration.get(eq("path"))).thenReturn(pathPropertySimple);
        when(mockResourceContext.getPluginConfiguration()).thenReturn(mockConfiguration);

        ASConnection mockASConnection = mock(ASConnection.class);

        PropertySimple namePropertySimple = new PropertySimple("__name", "test1");
        PowerMockito.whenNew(PropertySimple.class).withParameterTypes(String.class, Object.class)
            .withArguments(eq("__name"), eq("test1")).thenReturn(namePropertySimple);

        //create object to test and inject required dependencies
        TemplatedComponent objectUnderTest = new TemplatedComponent();

        objectUnderTest.context = mockResourceContext;
        objectUnderTest.testConnection = mockASConnection;

        //run code under test
        Configuration result = objectUnderTest.loadResourceConfiguration();

        //verify the results (Assert and mock verification)
        Assert.assertEquals(result, mockConfiguration);

        verify(mockMap, times(1)).remove(eq("__name"));
        verify(mockConfiguration, times(1)).get(eq("__type"));
        verify(mockConfiguration, times(1)).get(eq("__name"));
        verify(mockConfiguration, times(1)).get(eq("path"));
        verify(mockConfiguration, times(1)).put(eq(namePropertySimple));

        PowerMockito.verifyNew(PropertySimple.class).withArguments(eq("__name"), eq("test1"));
        PowerMockito.verifyNew(ConfigurationLoadDelegate.class).withArguments(any(ConfigurationDefinition.class),
            eq(mockASConnection), any(Address.class));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void updateResourceConfigurationWithType() throws Exception {
        //tell the method story as it happens: mock or create dependencies and configure
        //those dependencies to get the method under test to completion.
        ResourceContext mockResourceContext = mock(ResourceContext.class);

        ResourceType mockResourceType = mock(ResourceType.class);
        when(mockResourceContext.getResourceType()).thenReturn(mockResourceType);

        ConfigurationDefinition mockConfigurationDefinition = mock(ConfigurationDefinition.class);
        when(mockResourceType.getResourceConfigurationDefinition()).thenReturn(mockConfigurationDefinition);

        ConfigurationTemplate mockConfigurationTemplate = mock(ConfigurationTemplate.class);
        when(mockConfigurationDefinition.getDefaultTemplate()).thenReturn(mockConfigurationTemplate);

        Configuration mockConfiguration = mock(Configuration.class);
        when(mockConfigurationTemplate.getConfiguration()).thenReturn(mockConfiguration);

        Property mockProperty = mock(Property.class);
        when(mockConfiguration.get(eq("__type"))).thenReturn(mockProperty);

        Map<String, PropertyDefinition> mockMap = (Map<String, PropertyDefinition>) mock(Map.class);
        when(mockConfigurationDefinition.getPropertyDefinitions()).thenReturn(mockMap);

        ConfigurationUpdateReport mockReport = mock(ConfigurationUpdateReport.class);
        when(mockReport.getConfiguration()).thenReturn(mockConfiguration);

        ConfigurationWriteDelegate mockConfigurationWriteDelegate = mock(ConfigurationWriteDelegate.class);
        PowerMockito.whenNew(ConfigurationWriteDelegate.class)
            .withParameterTypes(ConfigurationDefinition.class, ASConnection.class, Address.class)
            .withArguments(any(ConfigurationDefinition.class), any(ASConnection.class), any(Address.class))
            .thenReturn(mockConfigurationWriteDelegate);

        ASConnection mockASConnection = mock(ASConnection.class);
        when(mockASConnection.execute(any(ReadResource.class))).thenReturn(new Result());


        //create object to test and inject required dependencies
        TemplatedComponent objectUnderTest = new TemplatedComponent();

        objectUnderTest.context = mockResourceContext;
        objectUnderTest.testConnection = mockASConnection;

        //run code under test
        objectUnderTest.updateResourceConfiguration(mockReport);

        //verify the results (Assert and mock verification)
        verify(mockMap, times(1)).remove(eq("__type"));
        verify(mockConfiguration, times(1)).get(eq("__type"));
        verify(mockConfiguration, never()).get(eq("__name"));
        verify(mockConfiguration, times(1)).remove(eq("__type"));

        PowerMockito.verifyNew(ConfigurationWriteDelegate.class).withArguments(any(ConfigurationDefinition.class),
            eq(mockASConnection), any(Address.class));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void updateResourceConfigurationWithName() throws Exception {
        //tell the method story as it happens: mock or create dependencies and configure
        //those dependencies to get the method under test to completion.
        ResourceContext mockResourceContext = mock(ResourceContext.class);

        ResourceType mockResourceType = mock(ResourceType.class);
        when(mockResourceContext.getResourceType()).thenReturn(mockResourceType);

        ConfigurationDefinition mockConfigurationDefinition = mock(ConfigurationDefinition.class);
        when(mockResourceType.getResourceConfigurationDefinition()).thenReturn(mockConfigurationDefinition);

        ConfigurationTemplate mockConfigurationTemplate = mock(ConfigurationTemplate.class);
        when(mockConfigurationDefinition.getDefaultTemplate()).thenReturn(mockConfigurationTemplate);

        Configuration mockConfiguration = mock(Configuration.class);
        when(mockConfigurationTemplate.getConfiguration()).thenReturn(mockConfiguration);

        Property mockProperty = mock(Property.class);
        when(mockConfiguration.get(eq("__type"))).thenReturn(null);
        when(mockConfiguration.get(eq("__name"))).thenReturn(mockProperty);

        Map<String, PropertyDefinition> mockMap = (Map<String, PropertyDefinition>) mock(Map.class);
        when(mockConfigurationDefinition.getPropertyDefinitions()).thenReturn(mockMap);

        ConfigurationUpdateReport mockReport = mock(ConfigurationUpdateReport.class);
        when(mockReport.getConfiguration()).thenReturn(mockConfiguration);

        ConfigurationWriteDelegate mockConfigurationWriteDelegate = mock(ConfigurationWriteDelegate.class);
        PowerMockito.whenNew(ConfigurationWriteDelegate.class)
            .withParameterTypes(ConfigurationDefinition.class, ASConnection.class, Address.class)
            .withArguments(any(ConfigurationDefinition.class), any(ASConnection.class), any(Address.class))
            .thenReturn(mockConfigurationWriteDelegate);

        ASConnection mockASConnection = mock(ASConnection.class);
        when(mockASConnection.execute(any(ReadResource.class))).thenReturn(new Result());

        //create object to test and inject required dependencies
        TemplatedComponent objectUnderTest = new TemplatedComponent();

        objectUnderTest.context = mockResourceContext;
        objectUnderTest.testConnection = mockASConnection;

        //run code under test
        objectUnderTest.updateResourceConfiguration(mockReport);

        //verify the results (Assert and mock verification)
        verify(mockMap, times(1)).remove(eq("__name"));
        verify(mockConfiguration, times(1)).get(eq("__type"));
        verify(mockConfiguration, times(1)).get(eq("__name"));
        verify(mockConfiguration, times(1)).remove(eq("__name"));

        PowerMockito.verifyNew(ConfigurationWriteDelegate.class).withArguments(any(ConfigurationDefinition.class),
            eq(mockASConnection), any(Address.class));
    }

}
