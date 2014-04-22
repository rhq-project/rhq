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
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.ValidationEventCollector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.clientapi.descriptor.AgentPluginDescriptorUtil;
import org.rhq.core.clientapi.descriptor.DescriptorPackages;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ResourceDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ServerDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ServiceDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.SubCategoryDescriptor;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceSubCategory;
import org.rhq.core.domain.resource.ResourceType;

 /**
  * @author Charles Crouch
  * @author Heiko W. Rupp
  */
public class NestedSubCategoriesMetadataParserTest {
     private static final String DESCRIPTOR_FILENAME = "test-subcategories-nested.xml";
     private final Log LOG = LogFactory.getLog(NestedSubCategoriesMetadataParserTest.class);

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
     public void parseSingleSubCategory() {
         List<ServerDescriptor> servers = pluginDescriptor.getServers();
         ServerDescriptor server0 = servers.get(0);
         ResourceDescriptor.Subcategories subCategoriesDescriptor = server0.getSubcategories();
         assert subCategoriesDescriptor != null : "No subcategories element: " + server0.getName();

         List<SubCategoryDescriptor> subCategoryDescriptors = subCategoriesDescriptor.getSubcategory();

         assert subCategoryDescriptors != null : "No subcategory elements: " + server0.getName();
         assert !subCategoryDescriptors.isEmpty() : "No subcategory elements: " + server0.getName();

         ResourceSubCategory subCat;

         ResourceType resType = new ResourceType("testResType", "myplugin", ResourceCategory.SERVER, null);
        //TODO: Re-enable Subcategory
        /*subCat = SubCategoriesMetadataParser.getSubCategory(subCategoryDescriptors.get(0), resType);

        assert subCat != null : "Null subcategory received from parser";
        assert subCat.getName().equals("applications") : "Name not read correctly";
        assert subCat.getDisplayName().equals("Apps") : "Display name not read correctly";
        assert subCat.getDescription().equals("The apps.") : "Description not read correctly";*/
         // getSubCategory is no longer responsible for setting resourcetype information, that is done in PluginMetadataParser
         //assert subCat.getResourceType().equals(resType) : "ResourceType not set correctly";

     }

     @Test
     public void parseNestedSubCategories() {
         List<ServerDescriptor> servers = pluginDescriptor.getServers();
         ServerDescriptor server2 = servers.get(1);
         assert server2.getName().equals("testServer2");
         ResourceDescriptor.Subcategories subCategoriesDescriptor = server2.getSubcategories();
         assert subCategoriesDescriptor == null : "Unexpected subcategories element: " + server2.getName();
         assert server2.getSubCategory().equals("applications");

         List<ServiceDescriptor> services = pluginDescriptor.getServices();
         ServiceDescriptor service1 = services.get(0);
         assert service1.getName().equals("testService");
         assert service1.getSubCategory().equals("applications");
     }

     @Test
     public void testParseViaMetaDataManager() throws Exception {

         PluginDescriptor pluginDescriptor;

         URL descriptorUrl = this.getClass().getClassLoader().getResource(DESCRIPTOR_FILENAME);
         System.out.println("Loading plugin descriptor at: " + descriptorUrl);

         pluginDescriptor = AgentPluginDescriptorUtil.parsePluginDescriptor(descriptorUrl
             .openStream());

         PluginMetadataManager metadataManager = new PluginMetadataManager();
         Set<ResourceType> typeSet = metadataManager.loadPlugin(pluginDescriptor);
         assert typeSet != null : "Got no types!!";
         assert typeSet.size()==5 : "Expected 5 types, but got " + typeSet.size();

         ResourceType testService = findType(typeSet,"testService");
         assert testService.getSubCategory().equals("applications");

         ResourceType testService2 = findType(typeSet,"testService2");
         assert testService2.getSubCategory().equals("applications");

         ResourceType testService3 = findType(typeSet,"testService3");
         assert testService3.getSubCategory().equals("fooBar");
     }

     private ResourceType findType(Set<ResourceType> types, String name) {
         for (ResourceType type : types ) {
             if (type.getName().equals(name)) {
                 return type;
             }
         }
         assert false : "Type with name " + name + " not found";
         return null;
     }
 }