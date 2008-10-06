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
import org.rhq.core.clientapi.agent.metadata.ContentMetadataParser;
import org.rhq.core.clientapi.descriptor.DescriptorPackages;
import org.rhq.core.clientapi.descriptor.plugin.ContentDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ServerDescriptor;
import org.rhq.core.domain.content.PackageCategory;
import org.rhq.core.domain.content.PackageType;

/**
 * @author Jason Dobies
 */
public class ContentMetadataParserTest {
    // Constants  --------------------------------------------

    private static final String DESCRIPTOR_FILENAME = "test1-plugin.xml";

    // Attributes  --------------------------------------------

    private final Log LOG = LogFactory.getLog(ContentMetadataParserTest.class);

    private PluginDescriptor pluginDescriptor;

    // Tests  --------------------------------------------

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
    public void parseValidDescriptor() throws Exception {
        List<ServerDescriptor> servers = pluginDescriptor.getServers();
        ServerDescriptor server0 = servers.get(0);
        List<ContentDescriptor> contentDescriptors = server0.getContent();

        assert contentDescriptors != null : "No content descriptors in server: " + server0.getName();

        PackageType packageType;

        packageType = ContentMetadataParser.parseContentDescriptor(contentDescriptors.get(0));

        assert packageType != null : "Null type received from parser";
        assert packageType.getName().equals("artifact1") : "Name not read correctly";
        assert packageType.getDisplayName().equals("Artifact One") : "Display name not read correctly ["
            + packageType.getDisplayName() + "]";
        assert packageType.getCategory() == PackageCategory.CONFIGURATION : "Category not read correctly";
        assert packageType.getDescription().equals("Artifact Description") : "Description not read correctly";

        packageType = ContentMetadataParser.parseContentDescriptor(contentDescriptors.get(1));

        assert packageType != null : "Null type recevied from parser";
        assert packageType.getName().equals("artifact2") : "Name not read correctly";
        assert packageType.getDescription() == null : "Description was not null";
        assert packageType.getDiscoveryInterval() == 5000 : "Discovery interval read incorrectly";
    }
}