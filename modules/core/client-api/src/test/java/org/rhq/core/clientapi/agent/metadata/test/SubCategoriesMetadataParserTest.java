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
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.ValidationEventCollector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.rhq.core.clientapi.agent.metadata.SubCategoriesMetadataParser;
import org.rhq.core.clientapi.descriptor.DescriptorPackages;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ResourceDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ServerDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.SubCategoryDescriptor;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceSubCategory;
import org.rhq.core.domain.resource.ResourceType;

/**
 * @author Charles Crouch
 */
public class SubCategoriesMetadataParserTest {
    private static final String DESCRIPTOR_FILENAME = "test-subcategories.xml";
    private final Log LOG = LogFactory.getLog(SubCategoriesMetadataParserTest.class);

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
        subCat = SubCategoriesMetadataParser.getSubCategory(subCategoryDescriptors.get(0), resType);

        assert subCat != null : "Null subcategory received from parser";
        assert subCat.getName().equals("applications") : "Name not read correctly";
        assert subCat.getDisplayName().equals("Apps") : "Display name not read correctly";
        assert subCat.getDescription().equals("The apps.") : "Description not read correctly";
        // getSubCategory is no longer responsible for setting resourcetype information, that is done in PluginMetadataParser
        //assert subCat.getResourceType().equals(resType) : "ResourceType not set correctly";
    }

    @Test
    public void parseNestedSubCategories() {
        List<ServerDescriptor> servers = pluginDescriptor.getServers();
        ServerDescriptor server1 = servers.get(1);
        ResourceDescriptor.Subcategories subCategoriesDescriptor = server1.getSubcategories();
        assert subCategoriesDescriptor != null : "No subcategories element: " + server1.getName();

        List<SubCategoryDescriptor> subCategoryDescriptors = subCategoriesDescriptor.getSubcategory();

        assert subCategoryDescriptors != null : "No subcategory elements: " + server1.getName();
        assert !subCategoryDescriptors.isEmpty() : "No subcategory elements: " + server1.getName();

        ResourceType resType = new ResourceType("testResType", "myplugin", ResourceCategory.SERVER, null);
        ResourceSubCategory subCat0 = SubCategoriesMetadataParser
            .getSubCategory(subCategoryDescriptors.get(0), resType);

        assert subCat0 != null : "Null subcategory received from parser";
        assert subCat0.getName().equals("resource") : "Name not read correctly";
        // getSubCategory is no longer responsible for setting resourcetype information, that is done in PluginMetadataParser
        //assert subCat0.getResourceType().equals(resType) : "ResourceType not set correctly";

        List<ResourceSubCategory> childSubCats = subCat0.getChildSubCategories();
        assert childSubCats != null : "Null child subcategories received from parser";
        assert childSubCats.size() == 2 : "Incorrect number of subcategories received from parser";
        ResourceSubCategory firstChildSubCategory = (childSubCats.toArray(new ResourceSubCategory[0]))[0];
        assert firstChildSubCategory != null : "Null child subcategory";
        assert firstChildSubCategory.getName().equals("dataSource") : "Incorrect child subcategory name";
        assert firstChildSubCategory.getParentSubCategory() != null : "Parent not specified on child subcategory";
        assert firstChildSubCategory.getParentSubCategory().equals(subCat0) : "Parent from child subcategory does not equal expected subcategory ";
        // resourceType never gets set on child sub categories
        //assert firstChildSubCategory.getResourceType().equals(resType) : "ResourceType not set correctly on child sub category";

        ResourceSubCategory secondChildSubCategory = (childSubCats.toArray(new ResourceSubCategory[0]))[1];
        assert secondChildSubCategory != null : "Null child subcategory";
        assert secondChildSubCategory.getName().equals("destinations") : "Incorrect child subcategory name";
        assert secondChildSubCategory.getParentSubCategory() != null : "Parent not specified on child subcategory";
        assert secondChildSubCategory.getParentSubCategory().equals(subCat0) : "Parent from child subcategory does not equal expected subcategory ";
        // resourceType never gets set on child sub categories
        //assert secondChildSubCategory.getResourceType().equals(resType) : "ResourceType not set correctly on child sub category";
    }
}