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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.ValidationEventCollector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.descriptor.DescriptorPackages;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ResourceDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ServerDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.SubCategoryDescriptor;

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
    }
}