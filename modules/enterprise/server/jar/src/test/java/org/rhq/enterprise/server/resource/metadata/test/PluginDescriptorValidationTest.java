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
package org.rhq.enterprise.server.resource.metadata.test;

import java.io.File;
import java.net.URL;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.ValidationEventCollector;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.rhq.core.clientapi.descriptor.DescriptorPackages;

/**
 * Try to validate the rhq-plugin.xml plugin descriptors
 *
 * @author Heiko W. Rupp TODO get the plugins dynamically - see comment in {@link #testPluginDescriptors()}
 */
public class PluginDescriptorValidationTest extends TestBase {
    // we stand in modules/enterprise/jar
    String BASE = "../../../plugins/";
    String EXT = "/src/main/resources/META-INF/rhq-plugin.xml";

    /**
     * Plugins we want to test. Note that they need to appear here in the correct dependency order.
     */
    String[] plugins = { "database", "postgres", "oracle", "platform", "perftest", "jmx", "custom-jmx", "tomcat",

    //"mock-jboss",
        "rhq-agent", "rhq-server", "jboss-as", "jboss-as-5", "hibernate", "apache" };

    @BeforeSuite
    @Override
    protected void init() {
        super.init();
    }

    /**
     * Load all the plugin descriptors thus running them through the XML Schema validation.
     *
     * @throws Exception
     */
    @Test
    public void testPluginDescriptors() throws Exception {
        int failed = 0;

        // TODO: dynamically determine the list of plugins and their sort order.
        // first part could be done by a simple filesystem walk in BASE, picking up
        // the directories. The second part is harder. Perhaps take some logik from
        // the plugin deployer code.

        getTransactionManager().begin();
        try {
            for (String plugin : plugins) {
                String descriptor = BASE + plugin + EXT;
                try {
                    System.out.println("--- Testing plugin [" + plugin + "] ---");
                    File f = new File(descriptor);
                    descriptor = f.getCanonicalPath();

                    long then = System.currentTimeMillis();
                    validatePluginDescriptor(descriptor);
                    long now = System.currentTimeMillis();
                    System.out.println("   Validation took " + (now - then) + "ms");
                } catch (Exception e) {
                    failed++;
                    System.out.println("!!! Validation of " + descriptor + " failed: " + e.getMessage() + " !!!");
                }
            }
        } finally {
            getTransactionManager().rollback();
        }

        if (failed > 0) {
            String msg = "Vaildation of [" + failed + "/" + plugins.length + "] descriptors failed";
            System.err.println(msg);
            throw new Exception(msg);
        } else {
            System.out.println("All tests passed");
        }
    }

    /**
     * Load the descriptor file from the file system, umarshalls and thus parses it.
     */
    public void validatePluginDescriptor(String descriptorFile) throws Exception {
        String PROTO = "file:///";
        URL descriptorUrl = new URL(PROTO + descriptorFile);

        JAXBContext jaxbContext = JAXBContext.newInstance(DescriptorPackages.PC_PLUGIN);
        URL pluginSchemaURL = getClass().getClassLoader().getResource("rhq-plugin.xsd");
        Schema pluginSchema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(pluginSchemaURL);

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        ValidationEventCollector vec = new ValidationEventCollector();
        unmarshaller.setEventHandler(vec);
        unmarshaller.setSchema(pluginSchema);
        unmarshaller.unmarshal(descriptorUrl.openStream());
    }
}