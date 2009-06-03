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
package org.rhq.core.clientapi.agent.metadata.test;

import java.net.URL;
import java.util.Iterator;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.ValidationEventCollector;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.metadata.PluginDependencyGraph;
import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.clientapi.descriptor.AgentPluginDescriptorUtil;
import org.rhq.core.clientapi.descriptor.DescriptorPackages;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;

/**
 * Tests that optional plugins can be ignored.
 *
 * @author John Mazzitelli
 */
@Test
public class OptionalPluginTest {
    private static final String DESCRIPTOR = "test-optional-plugins.xml";

    private PluginMetadataManager metadataManager;

    @BeforeClass
    public void beforeClass() {
        try {
            System.out.println("~~~~~ START " + OptionalPluginTest.class.getName() + " ~~~~~");
            this.metadataManager = new PluginMetadataManager();
        } catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    public void loadPluginDescriptors() throws Exception {
        PluginDescriptor descriptor = loadPluginDescriptor(DESCRIPTOR);
        outputAllTypes();
        assertTypes();

        PluginDependencyGraph graph = new PluginDependencyGraph();
        AgentPluginDescriptorUtil.addPluginToDependencyGraph(graph, descriptor);
        assert graph.isComplete(null);
    }

    private void assertTypes() {
        Set<ResourceType> rootTypes = metadataManager.getRootTypes();

        assert rootTypes.size() == 3;
        ResourceType topPlatform = metadataManager.getType("topPlatform", "OptionalPluginTest");
        assert rootTypes.contains(topPlatform);
        assert topPlatform.getName().equals("topPlatform");
        assert topPlatform.getPlugin().equals("OptionalPluginTest");
        assert topPlatform.getCategory() == ResourceCategory.PLATFORM;
        assert topPlatform.getDescription() == null;
        assert topPlatform.getParentResourceTypes().size() == 0;
        assert topPlatform.getChildResourceTypes().size() == 2;
        Iterator<ResourceType> iterator = topPlatform.getChildResourceTypes().iterator();
        ResourceType topPlatform_server1 = iterator.next();
        assert topPlatform_server1.getName().equals("topPlatform.server1");
        assert topPlatform_server1.getDescription() == null;
        assert topPlatform_server1.getPlugin().equals("OptionalPluginTest");
        assert topPlatform_server1.getCategory() == ResourceCategory.SERVER;
        assert topPlatform_server1.getChildResourceTypes().size() == 0;
        assert topPlatform_server1.getParentResourceTypes().size() == 1;
        assert topPlatform_server1.getParentResourceTypes().iterator().next().equals(topPlatform);
        ResourceType topPlatform_service1 = iterator.next();
        assert topPlatform_service1.getName().equals("topPlatform.service1");
        assert topPlatform_service1.getDescription() == null;
        assert topPlatform_service1.getPlugin().equals("OptionalPluginTest");
        assert topPlatform_service1.getCategory() == ResourceCategory.SERVICE;
        assert topPlatform_service1.getChildResourceTypes().size() == 0;
        assert topPlatform_service1.getParentResourceTypes().size() == 1;
        assert topPlatform_service1.getParentResourceTypes().iterator().next().equals(topPlatform);

        ResourceType topServer = metadataManager.getType("topServer", "OptionalPluginTest");
        assert rootTypes.contains(topServer);
        assert topServer.getName().equals("topServer");
        assert topServer.getPlugin().equals("OptionalPluginTest");
        assert topServer.getCategory() == ResourceCategory.SERVER;
        assert topServer.getDescription() == null;
        assert topServer.getParentResourceTypes().size() == 0;
        assert topServer.getChildResourceTypes().size() == 2;
        iterator = topServer.getChildResourceTypes().iterator();
        ResourceType topServer_server1 = iterator.next();
        assert topServer_server1.getName().equals("topServer.server1");
        assert topServer_server1.getDescription() == null;
        assert topServer_server1.getPlugin().equals("OptionalPluginTest");
        assert topServer_server1.getCategory() == ResourceCategory.SERVER;
        assert topServer_server1.getChildResourceTypes().size() == 0;
        assert topServer_server1.getParentResourceTypes().size() == 1;
        assert topServer_server1.getParentResourceTypes().iterator().next().equals(topServer);
        ResourceType topServer_service1 = iterator.next();
        assert topServer_service1.getName().equals("topServer.service1");
        assert topServer_service1.getDescription() == null;
        assert topServer_service1.getPlugin().equals("OptionalPluginTest");
        assert topServer_service1.getCategory() == ResourceCategory.SERVICE;
        assert topServer_service1.getChildResourceTypes().size() == 0;
        assert topServer_service1.getParentResourceTypes().size() == 1;
        assert topServer_service1.getParentResourceTypes().iterator().next().equals(topServer);

        ResourceType topService = metadataManager.getType("topService", "OptionalPluginTest");
        assert rootTypes.contains(topService);
        assert topService.getName().equals("topService");
        assert topService.getPlugin().equals("OptionalPluginTest");
        assert topService.getCategory() == ResourceCategory.SERVICE;
        assert topService.getDescription() == null;
        assert topService.getParentResourceTypes().size() == 0;
        assert topService.getChildResourceTypes().size() == 1;
        iterator = topService.getChildResourceTypes().iterator();
        ResourceType topService_service1 = iterator.next();
        assert topService_service1.getName().equals("topService.service1");
        assert topService_service1.getDescription() == null;
        assert topService_service1.getPlugin().equals("OptionalPluginTest");
        assert topService_service1.getCategory() == ResourceCategory.SERVICE;
        assert topService_service1.getChildResourceTypes().size() == 0;
        assert topService_service1.getParentResourceTypes().size() == 1;
        assert topService_service1.getParentResourceTypes().iterator().next().equals(topService);
    }

    private PluginDescriptor loadPluginDescriptor(String file) throws Exception {
        PluginDescriptor pluginDescriptor;

        URL descriptorUrl = this.getClass().getClassLoader().getResource(file);
        System.out.println("Loading plugin descriptor at: " + descriptorUrl);

        JAXBContext jaxbContext = JAXBContext.newInstance(DescriptorPackages.PC_PLUGIN);

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        ValidationEventCollector vec = new ValidationEventCollector();
        unmarshaller.setEventHandler(vec);
        pluginDescriptor = (PluginDescriptor) unmarshaller.unmarshal(descriptorUrl.openStream());

        this.metadataManager.loadPlugin(pluginDescriptor);

        return pluginDescriptor;
    }

    private void outputAllTypes() {
        System.out.println("\n~~~~~~~~~~~~~~~~");
        Set<ResourceType> allTypes = metadataManager.getRootTypes();
        for (ResourceType type : allTypes) {
            outputType(type, 0);
        }

        System.out.println("~~~~~~~~~~~~~~~~");
    }

    private void outputType(ResourceType type, int depth) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            line.append("  ");
        }

        line.append("+ ");
        line.append(type.getName());
        line.append(":");
        line.append(type.getPlugin());
        line.append(":");
        line.append(type.getCategory());
        line.append(":");
        for (ResourceType parent : type.getParentResourceTypes()) {
            line.append(parent.getName());
            line.append("/");
            line.append(parent.getPlugin());
            line.append(" ");
        }

        System.out.println(line.toString());
        for (ResourceType child : type.getChildResourceTypes()) {
            outputType(child, depth + 1);
        }
    }
}