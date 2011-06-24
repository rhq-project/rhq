 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2011 Red Hat, Inc.
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
 import java.util.List;

 import javax.xml.bind.JAXBContext;
 import javax.xml.bind.Unmarshaller;
 import javax.xml.bind.util.ValidationEventCollector;


 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.testng.annotations.BeforeSuite;
 import org.testng.annotations.Test;

 import org.rhq.core.clientapi.agent.metadata.ConfigurationMetadataParser;
 import org.rhq.core.clientapi.agent.metadata.InvalidPluginDescriptorException;
 import org.rhq.core.clientapi.descriptor.DescriptorPackages;
 import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
 import org.rhq.core.clientapi.descriptor.plugin.ServerDescriptor;
 import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
 import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
 import org.rhq.core.domain.configuration.definition.PropertySimpleType;

 /**
  * @author Jason Dobies
  * @author Heiko W. Rupp
  */
 public class ConfigurationMetadataParser2Test {
     private static final Log LOG = LogFactory.getLog(ConfigurationMetadataParser2Test.class);
     private static final String DESCRIPTOR_FILENAME = "test2-plugin.xml";

     private PluginDescriptor pluginDescriptor;

     @BeforeSuite
     public void loadPluginDescriptor() throws Exception {
         try {
             URL descriptorUrl = this.getClass().getClassLoader().getResource(DESCRIPTOR_FILENAME);
             LOG.info("Loading plugin descriptor at: " + descriptorUrl);

             JAXBContext jaxbContext = JAXBContext.newInstance(DescriptorPackages.PC_PLUGIN);

             Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
             ValidationEventCollector vec = new ValidationEventCollector();
             unmarshaller.setEventHandler(vec);
             pluginDescriptor = (PluginDescriptor) unmarshaller.unmarshal(descriptorUrl.openStream());
         } catch (Throwable t) {
             // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
             // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
             t.printStackTrace();
             throw new RuntimeException(t);
         }
     }

     @Test
     public void parseValidDefinitionServer2() throws InvalidPluginDescriptorException {
         ConfigurationDefinition definition = loadDescriptor("testServer2");

         assertSimplePropertyType(definition, "prop1", PropertySimpleType.STRING);
         assertSimplePropertyType(definition, "prop2", PropertySimpleType.STRING);
         assertSimplePropertyType(definition, "prop3", PropertySimpleType.STRING);

         PropertyDefinitionSimple pds = (PropertyDefinitionSimple) definition.get("prop1");
         assert pds.getEnumeratedValues().size()==2;
         assert pds.getOptionsSource()==null;

         pds = (PropertyDefinitionSimple) definition.get("prop2");
         assert pds.getEnumeratedValues().isEmpty();
         assert pds.getOptionsSource()!=null;

         pds = (PropertyDefinitionSimple) definition.get("prop3");
         assert pds.getEnumeratedValues().size()==1;
         assert pds.getOptionsSource()!=null;

     }






     private void assertSimplePropertyType(ConfigurationDefinition definition, String propertyName,
         PropertySimpleType type) {
         PropertyDefinitionSimple simple = definition.getPropertyDefinitionSimple(propertyName);
         assert simple != null : propertyName + " was not loaded";
         assert simple.getType() == type : propertyName + " was read with incorrect type";
         assert simple.getName().equals(propertyName) : propertyName + " was read with no name";
     }


     private ConfigurationDefinition loadDescriptor(String serverName) throws InvalidPluginDescriptorException {
         List<ServerDescriptor> servers = pluginDescriptor.getServers();

         ServerDescriptor serverDescriptor = findServer(serverName, servers);
         assert serverDescriptor != null : "Server descriptor not found in test plugin descriptor";

         return ConfigurationMetadataParser.parse("null", serverDescriptor.getResourceConfiguration());
     }

     private ServerDescriptor findServer(String name, List<ServerDescriptor> servers) {
         for (ServerDescriptor server : servers) {
             if (server.getName().equals(name)) {
                 return server;
             }
         }

         return null;
     }
 }
