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
package org.rhq.plugins.sshd;

import java.net.URL;
import java.io.File;
import java.util.HashMap;
import java.util.Set;
import java.util.Collection;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.ValidationEventCollector;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.descriptor.DescriptorPackages;
import org.rhq.core.clientapi.agent.metadata.PluginMetadataParser;

/**
 * @author Jason Dobies
 */
public class OpenSSHDComponentTest {

    private OpenSSHDComponent component = new OpenSSHDComponent();

    private Configuration pluginConfiguration = new Configuration();
    private ConfigurationDefinition resourceConfigurationDefinition;

    private final Log log = LogFactory.getLog(this.getClass());

    @BeforeSuite
    public void initPluginConfiguration() throws Exception {
        pluginConfiguration.put(new PropertySimple("lenses-path", "/usr/local/share/augeas/lenses"));
        pluginConfiguration.put(new PropertySimple("root-path", "/"));
        pluginConfiguration.put(new PropertySimple("config-path", "/files/etc/ssh/sshd_config"));
    }

    @BeforeSuite
    public void initResourceConfigurationDefinition() throws Exception {
        PluginDescriptor pluginDescriptor;

        URL descriptorUrl = this.getClass().getClassLoader().getResource("META-INF" + File.separator + "rhq-plugin.xml");
        System.out.println("Loading plugin descriptor at: " + descriptorUrl);

        JAXBContext jaxbContext = JAXBContext.newInstance(DescriptorPackages.PC_PLUGIN);

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        ValidationEventCollector vec = new ValidationEventCollector();
        unmarshaller.setEventHandler(vec);
        pluginDescriptor = (PluginDescriptor) unmarshaller.unmarshal(descriptorUrl.openStream());

        PluginMetadataParser parser = new PluginMetadataParser(pluginDescriptor, new HashMap<String, PluginMetadataParser>());
        Set<ResourceType> resourceTypes = parser.getRootResourceTypes();

        assert resourceTypes.size() == 1 : "Incorrect number of resource types found. Expected: 1, Found: " + resourceTypes.size();

        ResourceType sshdType = resourceTypes.iterator().next();
        resourceConfigurationDefinition = sshdType.getResourceConfigurationDefinition();
    }

    @Test
    public void loadResourceConfiguration() throws Exception {
        Configuration configuration = component.loadResourceConfiguration(pluginConfiguration, resourceConfigurationDefinition);

        assert configuration != null : "Configuration returned as null";

        Collection<Property> allProperties = configuration.getProperties();

        for (Property property : allProperties) {
            if (property instanceof PropertySimple) {
                PropertySimple propertySimple = (PropertySimple)property;
                log.info("Property: " + propertySimple.getName() + " - " + propertySimple.getStringValue());
                assert propertySimple.getStringValue() != null : "Null string value found for property: " + propertySimple.getName();
            }
        }
    }

}
