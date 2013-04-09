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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.ValidationEventCollector;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.clientapi.agent.metadata.ResourceTypeNotEnabledException;
import org.rhq.core.clientapi.descriptor.DescriptorPackages;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;

/**
 * Tests disabling resource types.
 *
 * @author John Mazzitelli
 */
public class MetadataManagerDisabledTypesTest {
    private static final String DESCRIPTOR_FILENAME_TEST1 = "metadata-manager-test-1.xml";
    private static final String DESCRIPTOR_FILENAME_TEST2 = "metadata-manager-test-2.xml";
    private static final String DESCRIPTOR_FILENAME_TEST3 = "metadata-manager-test-3.xml";

    private PluginMetadataManager metadataManager;

    @BeforeClass
    public void beforeClass() {
        try {
            this.metadataManager = new PluginMetadataManager();

            List<String> disabledTypes = new ArrayList<String>();
            disabledTypes.add("Test1>Server A>Service B"); // from DESCRIPTOR_FILENAME_TEST1
            disabledTypes.add("Test1>Server A>Service D>Dependent Service E"); // from DESCRIPTOR_FILENAME_TEST1
            disabledTypes.add("Test2>Extension Service C"); // from DESCRIPTOR_FILENAME_TEST2
            disabledTypes.add("Test2>Extension Server B>Service D"); // from DESCRIPTOR_FILENAME_TEST2
            disabledTypes.add("Test1>Server A>Injection D To Server A"); // from DESCRIPTOR_FILENAME_TEST3
            disabledTypes.add("Test2>Extension Server B>Injection C To Server A"); // from DESCRIPTOR_FILENAME_TEST3
            this.metadataManager.setDisabledResourceTypes(disabledTypes);

        } catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @Test
    public void loadPluginDescriptorTest1() throws Exception {
        PluginDescriptor pluginDescriptor;

        URL descriptorUrl = this.getClass().getClassLoader().getResource(DESCRIPTOR_FILENAME_TEST1);
        System.out.println("Loading plugin descriptor at: " + descriptorUrl);

        JAXBContext jaxbContext = JAXBContext.newInstance(DescriptorPackages.PC_PLUGIN);

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        ValidationEventCollector vec = new ValidationEventCollector();
        unmarshaller.setEventHandler(vec);
        pluginDescriptor = (PluginDescriptor) unmarshaller.unmarshal(descriptorUrl.openStream());

        this.metadataManager.loadPlugin(pluginDescriptor);

        System.out.println("\n~~~~~~~~~~~~DISABLE TYPES DESCRIPTOR TEST 1");
        outputTypes();
    }

    @Test(dependsOnMethods = "loadPluginDescriptorTest1")
    public void loadPluginDescriptorTest2() throws Exception {
        PluginDescriptor pluginDescriptor;

        URL descriptorUrl = this.getClass().getClassLoader().getResource(DESCRIPTOR_FILENAME_TEST2);
        System.out.println("Loading plugin descriptor at: " + descriptorUrl);

        JAXBContext jaxbContext = JAXBContext.newInstance(DescriptorPackages.PC_PLUGIN);

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        ValidationEventCollector vec = new ValidationEventCollector();
        unmarshaller.setEventHandler(vec);
        pluginDescriptor = (PluginDescriptor) unmarshaller.unmarshal(descriptorUrl.openStream());

        this.metadataManager.loadPlugin(pluginDescriptor);

        System.out.println("\n~~~~~~~~~~~~DISABLE TYPES DESCRIPTOR TEST 2");
        outputTypes();
    }

    @Test(dependsOnMethods = "loadPluginDescriptorTest2")
    public void loadPluginDescriptorTest3() throws Exception {
        PluginDescriptor pluginDescriptor;

        URL descriptorUrl = this.getClass().getClassLoader().getResource(DESCRIPTOR_FILENAME_TEST3);
        System.out.println("Loading plugin descriptor at: " + descriptorUrl);

        JAXBContext jaxbContext = JAXBContext.newInstance(DescriptorPackages.PC_PLUGIN);

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        ValidationEventCollector vec = new ValidationEventCollector();
        unmarshaller.setEventHandler(vec);
        pluginDescriptor = (PluginDescriptor) unmarshaller.unmarshal(descriptorUrl.openStream());

        this.metadataManager.loadPlugin(pluginDescriptor);

        System.out.println("\n~~~~~~~~~~~~DISABLE TYPES DESCRIPTOR TEST 3");
        outputTypes();

        // now see that we disabled the types

        ResourceType serverA = getResourceType(new ResourceType("Server A", "Test1", ResourceCategory.SERVER, null));
        assert serverA != null;
        assertIsNOTDisabledType(serverA);

        ResourceType serviceB = getResourceType(new ResourceType("Service B", "Test1", ResourceCategory.SERVICE, null));
        assert serviceB != null;
        assertIsDisabledType(serviceB);

        ResourceType serviceD = getResourceType(new ResourceType("Service D", "Test1", ResourceCategory.SERVICE, null));
        assert serviceD != null;
        assertIsNOTDisabledType(serviceD);

        ResourceType serviceE = getResourceType(new ResourceType("Dependent Service E", "Test1",
            ResourceCategory.SERVICE, null));
        assert serviceE != null;
        assertIsDisabledType(serviceE);

        ResourceType serviceC = getResourceType(new ResourceType("Extension Service C", "Test2",
            ResourceCategory.SERVICE, null));
        assert serviceC != null;
        assertIsDisabledType(serviceC);

        ResourceType extB = getResourceType(new ResourceType("Extension Server B", "Test2", ResourceCategory.SERVER,
            null));
        assert extB != null;
        assertIsNOTDisabledType(extB);

        ResourceType serviceD2 = getResourceType(new ResourceType("Service D", "Test2", ResourceCategory.SERVICE, null));
        assert serviceD2 != null;
        assertIsDisabledType(serviceD2);

        ResourceType extDtoA = getResourceType(new ResourceType("Injection D To Server A", "Test3",
            ResourceCategory.SERVICE, null));
        assert extDtoA != null;
        assertIsDisabledType(extDtoA);

        // Note this type has a <runs-inside> to more than one parent. Even though it is injected into
        // multiple parent hierarchies, if you specify it in only one hierarchy, it will be disabled in all
        // other hierarchies it is injected into. This could be considered a feature or not :-)
        // For example, in this test, we have disabled the type:
        //    Test2>Extension Server B>Injection C To Server A
        // but if we specified to disable this type (the second parent it was injected into), the same thing would result:
        //    Test1>Server A>Injection C To Server A
        // Finally, if we specified the type as it was directly declared in its plugin, again, the same thing would result,
        // this type is disabled for all of the hierarchies it was injected into.
        //    Test3>Injection C To Server A
        ResourceType extCtoA = getResourceType(new ResourceType("Injection C To Server A", "Test3",
            ResourceCategory.SERVICE, null));
        assert extCtoA != null;
        assertIsDisabledType(extCtoA);
    }

    private void assertIsDisabledType(ResourceType type) {
        try {
            metadataManager.getDiscoveryClass(type);
            assert false : "Discovery: should have been disabled: " + type;
        } catch (ResourceTypeNotEnabledException ok) {
        }

        try {
            metadataManager.getComponentClass(type);
            assert false : "Component: should have been disabled: " + type;
        } catch (ResourceTypeNotEnabledException ok) {
        }
    }

    private void assertIsNOTDisabledType(ResourceType type) {
        try {
            metadataManager.getDiscoveryClass(type);
        } catch (ResourceTypeNotEnabledException ok) {
            assert false : "Discovery: should not have been disabled: " + type;
        }

        try {
            metadataManager.getComponentClass(type);
        } catch (ResourceTypeNotEnabledException ok) {
            assert false : "Component: should not have been disabled: " + type;
        }
    }

    private ResourceType getResourceType(ResourceType typeToGet) {
        for (ResourceType type : metadataManager.getAllTypes()) {
            if (type.equals(typeToGet)) {
                return type;
            }
        }
        return null;
    }

    private void outputTypes() {
        Set<ResourceType> allTypes = metadataManager.getRootTypes();
        for (ResourceType type : allTypes) {
            outputType(type, 0);
        }
        System.out.flush();
    }

    private void outputType(ResourceType type, int depth) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            line.append("  ");
        }

        line.append("+ ");
        line.append(type.toString());
        System.out.println(line.toString());
        for (ResourceType child : type.getChildResourceTypes()) {
            outputType(child, depth + 1);
        }
    }
}