/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation,
 * Inc51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.plugins.cassandra;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.testng.IObjectFactory;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;
import org.rhq.plugins.jmx.MBeanResourceDiscoveryComponent;

/**
 * @author Michael Burman
 */
@PrepareForTest({MBeanResourceDiscoveryComponent.class})
public class DiscoveryTest {

    private Log log = LogFactory.getLog(DiscoveryTest.class);

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory();
    }

    @Test
    public void testResourceKeyParsing() throws Exception {
        // Mock discovered resources
        Configuration pluginConfiguration = mock(Configuration.class);

        DiscoveredResourceDetails details = new DiscoveredResourceDetails(mock(ResourceType.class),
                "org.apache.cassandra.db:type=ColumnFamilies,keyspace=rhq,columnfamily=raw_metrics", "1", "2", "3",
                mock(Configuration.class), mock(ProcessInfo.class));
        DiscoveredResourceDetails details2 = new DiscoveredResourceDetails(mock(ResourceType.class),
                "org.apache.cassandra.db:type=ColumnFamilies,columnfamily=raw_metrics", "1", "2", "3",
                mock(Configuration.class), mock(ProcessInfo.class));
        final Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();
        discoveredResources.add(details);
        discoveredResources.add(details2);

        // Replace method..
        Method discover = MBeanResourceDiscoveryComponent.class.getMethod("discoverResources", ResourceDiscoveryContext.class, boolean.class);
        PowerMockito.replace(discover).with(new InvocationHandler() {
            @Override
            public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                return discoveredResources;
            }
        });

        ColumnFamilyDiscoveryComponent columnFamilyDiscoveryComponent = new ColumnFamilyDiscoveryComponent();

        // Mock keyspace context
        ResourceDiscoveryContext mockContext = mock(ResourceDiscoveryContext.class);
        ResourceContext mockParentContext = mock(ResourceContext.class);
        when(mockContext.getParentResourceContext()).thenReturn(mockParentContext);
        when(mockParentContext.getResourceKey()).thenReturn("rhq");

        assertEquals(mockContext.getParentResourceContext().getResourceKey(), "rhq");

        // Do the actual discovery parsing
        Set<DiscoveredResourceDetails> discoveredResourceDetails = columnFamilyDiscoveryComponent.discoverResources(mockContext);
        assertEquals(discoveredResourceDetails.size(), 1);
        for (DiscoveredResourceDetails discoveredResource : discoveredResourceDetails) {
            assertEquals(discoveredResource.getResourceKey(), "raw_metrics");
        }
    }
}
