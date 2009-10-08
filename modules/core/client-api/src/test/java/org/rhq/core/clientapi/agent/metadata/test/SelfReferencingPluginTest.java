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
 * Tests that plugins can reference themselves in Injection and Extension models.
 *
 * @author John Mazzitelli
 */
@Test
public class SelfReferencingPluginTest {
    private static final String DESCRIPTOR = "test-self-referencing-plugins.xml";

    private PluginMetadataManager metadataManager;

    @BeforeClass
    public void beforeClass() {
        try {
            System.out.println("~~~~~ START " + SelfReferencingPluginTest.class.getName() + " ~~~~~");
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

        assert rootTypes.size() == 2;
        ResourceType topPlatform1 = metadataManager.getType("topPlatform1", "SelfReferencingPluginTest");
        ResourceType topPlatform2 = metadataManager.getType("topPlatform2", "SelfReferencingPluginTest");
        ResourceType topService = metadataManager.getType("topService", "SelfReferencingPluginTest");
        assert rootTypes.contains(topPlatform1);
        assert rootTypes.contains(topPlatform2);
        assert topPlatform1.getCategory() == ResourceCategory.PLATFORM;
        assert topPlatform2.getCategory() == ResourceCategory.PLATFORM;
        assert topService.getCategory() == ResourceCategory.SERVICE;
        assert topService.getParentResourceTypes().size() == 2;
        assert topService.getParentResourceTypes().contains(topPlatform1);
        assert topService.getParentResourceTypes().contains(topPlatform2);

        topPlatform1.getChildResourceTypes().contains(topService);
        topPlatform2.getChildResourceTypes().contains(topService);
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