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

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.metadata.PluginDependencyGraph;
import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.clientapi.descriptor.AgentPluginDescriptorUtil;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;

/**
 * Test the management and loading of plugin metadata that models a realistic use case of both Embedded and Injection
 * Extension Models.
 *
 * @author John Mazzitelli
 */
@Test
public class ExtensionModelTest {
    private static final String DESCRIPTOR_JMX = "test-jmx.xml";
    private static final String DESCRIPTOR_TOMCAT = "test-tomcat.xml";
    private static final String DESCRIPTOR_JBOSSAS = "test-jbossas.xml";
    private static final String DESCRIPTOR_HIBERNATE = "test-hibernate.xml";
    private static final String DESCRIPTOR_CUSTOMJMX = "test-custom-jmx.xml"; // what the custom-jmx plugin wants to look like
    private static final String DESCRIPTOR_CUSTOM_EXT = "test-extension.xml";

    private PluginMetadataManager metadataManager;

    @BeforeClass
    public void beforeClass() {
        try {
            System.out.println("~~~~~ START " + ExtensionModelTest.class.getName() + " ~~~~~");
            this.metadataManager = new PluginMetadataManager();
        } catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    public void loadPluginDescriptors() throws Exception {
        PluginDescriptor descriptor_jmx = loadPluginDescriptor(DESCRIPTOR_JMX);
        assertJmxTypes();

        PluginDescriptor descriptor_tomcat = loadPluginDescriptor(DESCRIPTOR_TOMCAT);
        assert this.metadataManager.getEmbeddedExtensions(descriptor_tomcat.getName()).isEmpty(); // jbossas not yet registered
        assertTomcatTypes();

        PluginDescriptor descriptor_jbossas = loadPluginDescriptor(DESCRIPTOR_JBOSSAS);
        assertJBossASTypes();

        PluginDescriptor descriptor_hibernate = loadPluginDescriptor(DESCRIPTOR_HIBERNATE);
        assertHibernateTypes();

        PluginDescriptor descriptor_customjmx = loadPluginDescriptor(DESCRIPTOR_CUSTOMJMX);
        assertCustomJmxTypes();

        outputAllTypes();

        PluginDependencyGraph graph = new PluginDependencyGraph();
        AgentPluginDescriptorUtil.addPluginToDependencyGraph(graph, descriptor_jmx);
        AgentPluginDescriptorUtil.addPluginToDependencyGraph(graph, descriptor_tomcat);
        AgentPluginDescriptorUtil.addPluginToDependencyGraph(graph, descriptor_jbossas);
        AgentPluginDescriptorUtil.addPluginToDependencyGraph(graph, descriptor_hibernate);
        AgentPluginDescriptorUtil.addPluginToDependencyGraph(graph, descriptor_customjmx);
        assert graph.isComplete(null);

        // these are not extended via embedded extension model
        assert this.metadataManager.getEmbeddedExtensions(descriptor_jmx.getName()).isEmpty();
        assert this.metadataManager.getEmbeddedExtensions(descriptor_jbossas.getName()).isEmpty();
        assert this.metadataManager.getEmbeddedExtensions(descriptor_hibernate.getName()).isEmpty();
        assert this.metadataManager.getEmbeddedExtensions(descriptor_customjmx.getName()).isEmpty();
        // tomcat plugin is extended by the jbossas plugin via the embedded extension model
        assert this.metadataManager.getEmbeddedExtensions(descriptor_tomcat.getName()).size() == 1;
        assert this.metadataManager.getEmbeddedExtensions(descriptor_tomcat.getName()).containsKey(
            descriptor_jbossas.getName());

        // deploy another plugin that extends the tomcat plugin so we can see multiple extensions returned
        PluginDescriptor descriptor_customext = loadPluginDescriptor(DESCRIPTOR_CUSTOM_EXT);
        assert this.metadataManager.getEmbeddedExtensions(descriptor_tomcat.getName()).size() == 2;
        assert this.metadataManager.getEmbeddedExtensions(descriptor_tomcat.getName()).containsKey(
            descriptor_jbossas.getName());
        assert this.metadataManager.getEmbeddedExtensions(descriptor_tomcat.getName()).containsKey(
            descriptor_customext.getName());
    }

    @Test(dependsOnMethods = "loadPluginDescriptors")
    public void testOperationMetadata() {
        ResourceType jbossServer = metadataManager.getType("JBossASServer", "JBossAS");
        assert jbossServer.getOperationDefinitions() != null;
        assert jbossServer.getOperationDefinitions().size() == 1;
        OperationDefinition stopOp = jbossServer.getOperationDefinitions().iterator().next();
        assert stopOp.getName().equals("stop");
        assert stopOp.getDisplayName().equals("Stop JBossAS Server");
        assert stopOp.getDescription().equals("Kills the server");
        assert stopOp.getTimeout() != null;
        assert stopOp.getTimeout().equals(new Integer(30));

        assert stopOp.getParametersConfigurationDefinition() != null;
        ConfigurationDefinition parameters = stopOp.getParametersConfigurationDefinition();
        assert parameters.getPropertyDefinitions().size() == 1;
        PropertyDefinition def1 = parameters.get("force");
        PropertyDefinition def2 = parameters.getPropertyDefinitions().get("force");
        assert def1 != null;
        assert def2 != null;
        assert def2.equals(def1);
        assert def1.getName().equals("force");
        assert def1.getDisplayName().equals("Force Kill");
        assert def1
            .getDescription()
            .equals(
                "If true, use operating system to kill the process; otherwise, use JBoss remote JMX method to shut it down");
        assert !def1.isRequired();
        assert def1 instanceof PropertyDefinitionSimple;
        assert ((PropertyDefinitionSimple) def1).getType().equals(PropertySimpleType.BOOLEAN);
        PropertySimple prop = (PropertySimple) def1.getConfigurationDefinition().getDefaultTemplate()
            .getConfiguration().get("force");
        assert prop.getBooleanValue().booleanValue() == false;

        assert stopOp.getResultsConfigurationDefinition() != null;
        ConfigurationDefinition results = stopOp.getResultsConfigurationDefinition();
        assert results.getPropertyDefinitions().size() == 1;
        def1 = results.get("confirmed");
        def2 = results.getPropertyDefinitions().get("confirmed");
        assert def1 != null : results.getPropertyDefinitions();
        assert def2 != null : results.getPropertyDefinitions();
        assert def2.equals(def1);
        assert def1.getName().equals("confirmed");
        assert def1.getDisplayName().equals("Confirmed Down");
        assert def1
            .getDescription()
            .equals(
                "If true, the server is definitely down; otherwise, the shutdown was issued but it is unclear if it really died");
    }

    private void assertJmxTypes() {
        Set<ResourceType> rootTypes = metadataManager.getRootTypes();

        assert rootTypes.size() == 1;
        ResourceType jmxServer = metadataManager.getType("JMXServer", "JMX");
        assert rootTypes.contains(jmxServer);
        assert jmxServer.getName().equals("JMXServer");
        assert jmxServer.getPlugin().equals("JMX");
        assert jmxServer.getCategory() == ResourceCategory.SERVER;
        assert jmxServer.getDescription().equals("JMXServer Description");
        assert jmxServer.getParentResourceTypes().size() == 0;

        assert jmxServer.getChildResourceTypes().size() == 1;
        ResourceType osService = jmxServer.getChildResourceTypes().iterator().next();
        assert osService.getName().equals("OperatingSystem");
        assert osService.getDescription().equals("OperatingSystem Description");
        assert osService.getPlugin().equals("JMX");
        assert osService.getCategory() == ResourceCategory.SERVICE;
        assert osService.getChildResourceTypes().size() == 0;
        assert osService.getParentResourceTypes().size() == 1;
        assert osService.getParentResourceTypes().iterator().next().equals(jmxServer);
    }

    private void assertTomcatTypes() {
        Set<ResourceType> rootTypes = metadataManager.getRootTypes();

        assert rootTypes.size() == 2;
        assert rootTypes.contains(metadataManager.getType("JMXServer", "JMX"));
        ResourceType tomcatServer = metadataManager.getType("TomcatServer", "Tomcat");
        assert rootTypes.contains(tomcatServer);
        assert tomcatServer.getName().equals("TomcatServer");
        assert tomcatServer.getPlugin().equals("Tomcat");
        assert tomcatServer.getCategory() == ResourceCategory.SERVER;
        assert tomcatServer.getDescription().equals("Tomcat Web Application Container Description");
        assert tomcatServer.getParentResourceTypes().size() == 0;

        assert tomcatServer.getChildResourceTypes().size() == 1;
        ResourceType webappService = tomcatServer.getChildResourceTypes().iterator().next();
        assert webappService.getName().equals("WebappService");
        assert webappService.getPlugin().equals("Tomcat");
        assert webappService.getCategory() == ResourceCategory.SERVICE;
        assert webappService.getDescription().equals("Webapp Service Description");
        assert webappService.getChildResourceTypes().size() == 0;
        assert webappService.getParentResourceTypes().size() == 1;
        assert webappService.getParentResourceTypes().iterator().next().equals(tomcatServer);
    }

    private void assertJBossASTypes() {
        Set<ResourceType> rootTypes = metadataManager.getRootTypes();

        assert rootTypes.size() == 3;
        assert rootTypes.contains(metadataManager.getType("JMXServer", "JMX"));
        assert rootTypes.contains(metadataManager.getType("TomcatServer", "Tomcat"));
        ResourceType jbossServer = metadataManager.getType("JBossASServer", "JBossAS");
        assert rootTypes.contains(jbossServer);
        assert jbossServer.getName().equals("JBossASServer");
        assert jbossServer.getPlugin().equals("JBossAS");
        assert jbossServer.getCategory().equals(ResourceCategory.SERVER);
        assert jbossServer.getDescription().equals("JBoss Application Server Description");
        assert jbossServer.getParentResourceTypes().size() == 0;

        assert jbossServer.getChildResourceTypes().size() == 1;
        ResourceType embeddedTomcatServer = jbossServer.getChildResourceTypes().iterator().next();
        assert embeddedTomcatServer.getName().equals("EmbeddedTomcatServer");
        assert embeddedTomcatServer.getPlugin().equals("JBossAS");
        assert embeddedTomcatServer.getCategory() == ResourceCategory.SERVER;
        assert embeddedTomcatServer.getDescription().equals("Embedded Tomcat Web Server Description");
        assert embeddedTomcatServer.getParentResourceTypes().size() == 1;
        assert embeddedTomcatServer.getParentResourceTypes().iterator().next().equals(jbossServer);
        assert embeddedTomcatServer.getChildResourceTypes().size() == 1;
        ResourceType webappService = embeddedTomcatServer.getChildResourceTypes().iterator().next();
        assert webappService.getName().equals("WebappService");
        assert webappService.getPlugin().equals("JBossAS");
        assert webappService.getCategory() == ResourceCategory.SERVICE;
        assert webappService.getDescription().equals("Webapp Service Description");
        assert webappService.getChildResourceTypes().size() == 0;
        assert webappService.getParentResourceTypes().size() == 1;
        assert webappService.getParentResourceTypes().iterator().next().equals(embeddedTomcatServer);
    }

    private void assertHibernateTypes() {
        Set<ResourceType> rootTypes = metadataManager.getRootTypes();

        // hibernate injects itself as a child, so even though its a root type in its descriptor, it really isn't a root type
        assert rootTypes.size() == 3;
        assert rootTypes.contains(metadataManager.getType("JMXServer", "JMX"));
        assert rootTypes.contains(metadataManager.getType("TomcatServer", "Tomcat"));
        assert rootTypes.contains(metadataManager.getType("JBossASServer", "JBossAS"));

        ResourceType hibernateService = metadataManager.getType("HibernateService", "Hibernate");
        assert !rootTypes.contains(hibernateService);
        assert hibernateService.getName().equals("HibernateService");
        assert hibernateService.getPlugin().equals("Hibernate");
        assert hibernateService.getCategory() == ResourceCategory.SERVICE;
        assert hibernateService.getDescription().equals("Hibernate Service Description");
        assert hibernateService.getChildResourceTypes().size() == 0;
        assert hibernateService.getParentResourceTypes().size() == 3;
        assert hibernateService.getSubCategory().equals("Framework");

        ResourceType tomcatServer = metadataManager.getType("TomcatServer", "Tomcat");
        ResourceType jbossServer = metadataManager.getType("JBossASServer", "JBossAS");
        ResourceType embeddedTomcatServer = metadataManager.getType("EmbeddedTomcatServer", "JBossAS");

        assert hibernateService.getParentResourceTypes().contains(tomcatServer);
        assert hibernateService.getParentResourceTypes().contains(jbossServer);
        assert hibernateService.getParentResourceTypes().contains(embeddedTomcatServer);

        assert tomcatServer.getChildResourceTypes().size() == 2; // now has the new injected hibernate service
        assert tomcatServer.getChildResourceTypes().contains(hibernateService); // now has the new injected hibernate service

        assert jbossServer.getChildResourceTypes().size() == 2; // now has the new injected hibernate service
        assert jbossServer.getChildResourceTypes().contains(hibernateService);

        assert embeddedTomcatServer.getChildResourceTypes().size() == 2; // now has the new injected hibernate service
        assert embeddedTomcatServer.getChildResourceTypes().contains(hibernateService);
    }

    private void assertCustomJmxTypes() {
        Set<ResourceType> rootTypes = metadataManager.getRootTypes();

        // customjmx not only injects something but it also defines a true root type
        assert rootTypes.size() == 4;
        assert rootTypes.contains(metadataManager.getType("JMXServer", "JMX"));
        assert rootTypes.contains(metadataManager.getType("TomcatServer", "Tomcat"));
        assert rootTypes.contains(metadataManager.getType("JBossASServer", "JBossAS"));
        ResourceType customJmxServer = metadataManager.getType("Custom JMX Server", "CustomJmx");
        assert rootTypes.contains(customJmxServer);
        assert customJmxServer.getName().equals("Custom JMX Server");
        assert customJmxServer.getPlugin().equals("CustomJmx");
        assert customJmxServer.getCategory() == ResourceCategory.SERVER;
        assert customJmxServer.getDescription().equals(
            "A JMX Server that houses the custom MBeans that are to be managed");
        assert customJmxServer.getParentResourceTypes().size() == 0;

        ResourceType customJmxService = metadataManager.getType("CustomJmxService", "CustomJmx");
        assert !rootTypes.contains(customJmxService);
        assert customJmxService.getName().equals("CustomJmxService");
        assert customJmxService.getPlugin().equals("CustomJmx");
        assert customJmxService.getCategory() == ResourceCategory.SERVICE;
        assert customJmxService.getDescription().equals("Describes your custom service");
        assert customJmxService.getChildResourceTypes().size() == 0;
        assert customJmxService.getParentResourceTypes().size() == 3;

        ResourceType tomcatServer = metadataManager.getType("TomcatServer", "Tomcat");
        ResourceType jbossServer = metadataManager.getType("JBossASServer", "JBossAS");

        assert customJmxService.getParentResourceTypes().contains(tomcatServer);
        assert customJmxService.getParentResourceTypes().contains(jbossServer);
        assert customJmxService.getParentResourceTypes().contains(customJmxServer);

        assert tomcatServer.getChildResourceTypes().size() == 3; // now has the new injected service
        assert tomcatServer.getChildResourceTypes().contains(customJmxService); // now has the new injected service

        assert jbossServer.getChildResourceTypes().size() == 3; // now has the new injected service
        assert jbossServer.getChildResourceTypes().contains(customJmxService);

        assert customJmxServer.getChildResourceTypes().size() == 2; // now has the new injected service
        assert customJmxServer.getChildResourceTypes().contains(customJmxService);
    }

    private PluginDescriptor loadPluginDescriptor(String file) throws Exception {
        PluginDescriptor pluginDescriptor;

        URL descriptorUrl = this.getClass().getClassLoader().getResource(file);
        System.out.println("Loading plugin descriptor at: " + descriptorUrl);

        pluginDescriptor = AgentPluginDescriptorUtil.parsePluginDescriptor(descriptorUrl
            .openStream());

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
