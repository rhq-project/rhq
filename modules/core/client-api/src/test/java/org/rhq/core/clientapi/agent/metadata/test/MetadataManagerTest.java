/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.core.clientapi.agent.metadata.test;

import java.net.URL;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.ValidationEventCollector;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.clientapi.descriptor.DescriptorPackages;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.configuration.definition.constraint.RegexConstraint;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;

/**
 * Test the management and loading of plugin metadata.
 *
 * @author Greg Hinkle
 */
public class MetadataManagerTest {
    private static final String DESCRIPTOR_FILENAME_TEST1 = "metadata-manager-test-1.xml";
    private static final String DESCRIPTOR_FILENAME_TEST2 = "metadata-manager-test-2.xml";
    private static final String DESCRIPTOR_FILENAME_TEST3 = "metadata-manager-test-3.xml";

    private PluginMetadataManager metadataManager;

    @BeforeClass
    public void beforeClass() {
        try {
            this.metadataManager = new PluginMetadataManager();
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

        System.out.println("\n~~~~~~~~~~~~DESCRIPTOR TEST 1");
        Set<ResourceType> allTypes = metadataManager.getRootTypes();
        for (ResourceType type : allTypes) {
            outputType(type, 0);
        }
    }

    @Test(dependsOnMethods = "loadPluginDescriptorTest1")
    public void testDefinitionParsing1() {
        ResourceType serverAType = this.metadataManager.getType("Server A", ResourceCategory.SERVER);
        assert serverAType.getName().equals("Server A");
        assert serverAType.getPlugin().equals("Test1");
        assertServerTypeIsOK(serverAType);
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

        System.out.println("\n~~~~~~~~~~~~DESCRIPTOR TEST 2");
        Set<ResourceType> allTypes = metadataManager.getRootTypes();
        for (ResourceType type : allTypes) {
            outputType(type, 0);
        }
    }

    @Test(dependsOnMethods = "loadPluginDescriptorTest2")
    public void testDefinitionParsing2() {
        ResourceType serverBType = this.metadataManager.getType("Extension Server B", ResourceCategory.SERVER);
        assert serverBType.getName().equals("Extension Server B");
        assert serverBType.getPlugin().equals("Test2");
        assertServerTypeIsOK(serverBType);
    }

    @Test
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

        System.out.println("\n~~~~~~~~~~~~DESCRIPTOR TEST 3");
        Set<ResourceType> allTypes = metadataManager.getRootTypes();
        for (ResourceType type : allTypes) {
            outputType(type, 0);
        }

        ResourceType parentA = getResourceType(new ResourceType("Server A", "Test1", ResourceCategory.SERVER, null));
        ResourceType parentB = getResourceType(new ResourceType("Extension Server B", "Test2", ResourceCategory.SERVER,
            null));
        assert parentA != null;
        assert parentB != null;

        ResourceType testC = getResourceType(new ResourceType("Injection C To Server A", "Test3",
            ResourceCategory.SERVER, null));
        assert testC != null;
        assert parentA.getChildResourceTypes().contains(testC);
        assert parentB.getChildResourceTypes().contains(testC); // shows that a type can have more than one parent

        ResourceType testD = getResourceType(new ResourceType("Injection D To Server A", "Test3",
            ResourceCategory.SERVICE, null));
        assert testD != null;
        assert parentA.getChildResourceTypes().contains(testD);
        assert !parentB.getChildResourceTypes().contains(testD);

        // now check the many-to-many relationship - child can have more than one parent
        assert testC.getParentResourceTypes().contains(parentA);
        assert testC.getParentResourceTypes().contains(parentB);
        assert testD.getParentResourceTypes().contains(parentA);
        assert !testD.getParentResourceTypes().contains(parentB);
    }

    private ResourceType getResourceType(ResourceType typeToGet) {
        for (ResourceType type : metadataManager.getAllTypes()) {
            if (type.equals(typeToGet)) {
                return type;
            }
        }

        return null;
    }

    /**
     * @param serverType
     */
    private void assertServerTypeIsOK(ResourceType serverType) {
        assert serverType != null : "Expected type not found";

        ConfigurationDefinition def = serverType.getPluginConfigurationDefinition();

        PropertyDefinitionSimple alpha = def.getPropertyDefinitionSimple("alpha");
        assert alpha.getType() == PropertySimpleType.STRING;
        assert alpha.isRequired();
        assert alpha.getConstraints().size() == 1;
        RegexConstraint regexConstraint = (RegexConstraint) alpha.getConstraints().iterator().next();
        assert regexConstraint.getDetails().equals("^\\w+@[a-zA-Z_]+?\\.[a-zA-Z]{2,3}$");

        PropertyDefinitionSimple bravo = def.getPropertyDefinitionSimple("bravo");
        assert bravo.getType() == PropertySimpleType.STRING;
        assert bravo.getEnumeratedValues().size() == 4;
        assert bravo.getEnumeratedValues().get(0).getName().equals("First Option Name");
        assert bravo.getEnumeratedValues().get(0).getValue().equals("FirstOptionValue");
        assert bravo.getEnumeratedValues().get(0).isDefault();
        assert !bravo.getEnumeratedValues().get(1).isDefault();

        assert def.getPropertyDefinitionSimple("charlie").getType() == PropertySimpleType.BOOLEAN;
        assert def.getPropertyDefinitionSimple("delta").getType() == PropertySimpleType.INTEGER;
        assert def.getPropertyDefinitionSimple("deltalong").getType() == PropertySimpleType.LONG;
        assert def.getPropertyDefinitionSimple("echo").getType() == PropertySimpleType.PASSWORD;
        assert def.getPropertyDefinitionSimple("foxtrot").getType() == PropertySimpleType.FLOAT;
        assert def.getPropertyDefinitionSimple("foxtrotdouble").getType() == PropertySimpleType.DOUBLE;
        assert def.getPropertyDefinitionSimple("golf").getType() == PropertySimpleType.DIRECTORY;
        assert def.getPropertyDefinitionSimple("hotel").getType() == PropertySimpleType.FILE;
        assert def.getPropertyDefinitionSimple("india").getType() == PropertySimpleType.LONG_STRING;

        PropertyDefinitionList juliet = def.getPropertyDefinitionList("juliet");
        assert juliet.getMin() == 3;
        assert juliet.getMax() == 4;
        PropertyDefinitionSimple foo = (PropertyDefinitionSimple) juliet.getMemberDefinition();
        assert foo != null;
        assert foo.getName().equals("foo");
        assert foo.getType() == PropertySimpleType.STRING;

        // This is an arbitrary entry map
        assert def.getPropertyDefinitionMap("kilo").getPropertyDefinitions().size() == 0;
        assert def.getPropertyDefinitionMap("kilo").getName().equals("kilo");

        PropertyDefinitionSimple mapEmbeddedSimpleDef = (PropertyDefinitionSimple) def.getPropertyDefinitionMap("lima")
            .getPropertyDefinitions().get("file");
        assert mapEmbeddedSimpleDef != null;
        assert mapEmbeddedSimpleDef.getType() == PropertySimpleType.FILE;

        assert def.getPropertyDefinitionList("mike").getMin() == 0;
        assert def.getPropertyDefinitionList("mike").getMax() == Integer.MAX_VALUE;

        Map<String, ConfigurationTemplate> templates = def.getTemplates();
        ConfigurationTemplate t = templates.get("First Template");
        assert !t.isDefault();
        assert t.getConfiguration().getSimple("alpha").getStringValue().equals("template1:alpha value");
        assert t.getConfiguration().getSimple("charlie").getBooleanValue();
        assert t.getConfiguration().getSimple("delta").getIntegerValue() == 42;
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