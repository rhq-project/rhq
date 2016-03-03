/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.modules.plugins.wildfly10;

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
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.modules.plugins.wildfly10.json.Address;

/**
 * @author Stefan Negrea
 *
 */
@PrepareForTest({ TemplatedSubResourcesComponent.class })
public class TemplatedSubResourcesComponentTest {

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory();
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void createResourceWithType() throws Exception {
        //tell the method story as it happens: mock or create dependencies and configure
        //those dependencies to get the method under test to completion.
        CreateResourceReport mockReport = mock(CreateResourceReport.class);

        Configuration mockConfiguration = mock(Configuration.class);
        when(mockReport.getResourceConfiguration()).thenReturn(mockConfiguration);

        PropertySimple typePropertySimple = new PropertySimple("__type", "xyz");
        when(mockConfiguration.get(eq("__type"))).thenReturn(typePropertySimple);

        ResourceType mockResourceType = mock(ResourceType.class);
        when(mockReport.getResourceType()).thenReturn(mockResourceType);

        ConfigurationDefinition mockConfigurationDefinition = mock(ConfigurationDefinition.class);
        when(mockResourceType.getResourceConfigurationDefinition()).thenReturn(mockConfigurationDefinition);

        Map<String, PropertyDefinition> mockMap = (Map<String, PropertyDefinition>) mock(Map.class);
        when(mockConfigurationDefinition.getPropertyDefinitions()).thenReturn(mockMap);

        CreateResourceDelegate mockCreateResourceDelegate = mock(CreateResourceDelegate.class);
        PowerMockito.whenNew(CreateResourceDelegate.class)
            .withParameterTypes(ConfigurationDefinition.class, ASConnection.class, Address.class)
            .withArguments(any(ConfigurationDefinition.class), any(ASConnection.class), any(Address.class))
            .thenReturn(mockCreateResourceDelegate);

        when(mockReport.getResourceConfiguration()).thenReturn(mockConfiguration);

        when(mockReport.getPluginConfiguration()).thenReturn(mockConfiguration);

        PropertySimple pathPropertySimple = new PropertySimple("path", "xyz");
        PowerMockito.whenNew(PropertySimple.class).withParameterTypes(String.class, Object.class)
            .withArguments(eq("path"), eq("xyz")).thenReturn(pathPropertySimple);

        when(mockCreateResourceDelegate.createResource(eq(mockReport))).thenReturn(mockReport);

        ASConnection mockASConnection = mock(ASConnection.class);

        //create object to test and inject required dependencies
        TemplatedSubResourcesComponent objectUnderTest = new TemplatedSubResourcesComponent();
        objectUnderTest.testConnection = mockASConnection;

        //run code under test
        CreateResourceReport result = objectUnderTest.createResource(mockReport);

        //verify the results (Assert and mock verification)
        Assert.assertEquals(result, mockReport);

        verify(mockMap, times(1)).remove(eq("__type"));
        verify(mockConfiguration, times(2)).get(eq("__type"));
        verify(mockConfiguration, never()).get(eq("__name"));
        verify(mockConfiguration, times(1)).put(eq(pathPropertySimple));

        PowerMockito.verifyNew(PropertySimple.class).withArguments(eq("path"), eq("xyz"));
        PowerMockito.verifyNew(CreateResourceDelegate.class).withArguments(any(ConfigurationDefinition.class),
            eq(mockASConnection), any(Address.class));
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void createResourceWithName() throws Exception {
        //tell the method story as it happens: mock or create dependencies and configure
        //those dependencies to get the method under test to completion.
        CreateResourceReport mockReport = mock(CreateResourceReport.class);

        Configuration mockConfiguration = mock(Configuration.class);
        when(mockReport.getResourceConfiguration()).thenReturn(mockConfiguration);

        PropertySimple namePropertySimple = new PropertySimple("__name", "xyz");
        when(mockConfiguration.get(eq("__type"))).thenReturn(null);
        when(mockConfiguration.get(eq("__name"))).thenReturn(namePropertySimple);

        ResourceType mockResourceType = mock(ResourceType.class);
        when(mockReport.getResourceType()).thenReturn(mockResourceType);

        ConfigurationDefinition mockConfigurationDefinition = mock(ConfigurationDefinition.class);
        when(mockResourceType.getResourceConfigurationDefinition()).thenReturn(mockConfigurationDefinition);

        Map<String, PropertyDefinition> mockMap = (Map<String, PropertyDefinition>) mock(Map.class);
        when(mockConfigurationDefinition.getPropertyDefinitions()).thenReturn(mockMap);

        CreateResourceDelegate mockCreateResourceDelegate = mock(CreateResourceDelegate.class);
        PowerMockito.whenNew(CreateResourceDelegate.class)
            .withParameterTypes(ConfigurationDefinition.class, ASConnection.class, Address.class)
            .withArguments(any(ConfigurationDefinition.class), any(ASConnection.class), any(Address.class))
            .thenReturn(mockCreateResourceDelegate);

        when(mockReport.getResourceConfiguration()).thenReturn(mockConfiguration);

        when(mockReport.getPluginConfiguration()).thenReturn(mockConfiguration);

        when(mockCreateResourceDelegate.createResource(eq(mockReport))).thenReturn(mockReport);

        ASConnection mockASConnection = mock(ASConnection.class);

        //create object to test and inject required dependencies
        TemplatedSubResourcesComponent objectUnderTest = new TemplatedSubResourcesComponent();
        objectUnderTest.testConnection = mockASConnection;

        //run code under test
        CreateResourceReport result = objectUnderTest.createResource(mockReport);

        //verify the results (Assert and mock verification)
        Assert.assertEquals(result, mockReport);

        verify(mockMap, times(1)).remove(eq("__name"));
        verify(mockConfiguration, times(1)).get(eq("__type"));
        verify(mockConfiguration, times(2)).get(eq("__name"));

        verify(mockReport, times(1)).setUserSpecifiedResourceName(eq("xyz"));

        PowerMockito.verifyNew(CreateResourceDelegate.class).withArguments(any(ConfigurationDefinition.class),
            eq(mockASConnection), any(Address.class));
    }
}
