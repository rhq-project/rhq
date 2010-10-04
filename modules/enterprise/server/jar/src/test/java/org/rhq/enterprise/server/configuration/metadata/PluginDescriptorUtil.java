package org.rhq.enterprise.server.configuration.metadata;

import java.io.FileNotFoundException;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.ValidationEventCollector;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.jxpath.JXPathContext;

import org.rhq.core.clientapi.agent.metadata.ConfigurationMetadataParser;
import org.rhq.core.clientapi.descriptor.DescriptorPackages;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ResourceDescriptor;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;

public class PluginDescriptorUtil {

    public static PluginDescriptor loadPluginDescriptor(String file) {
        try {
            URL pluginDescriptorURL = PluginDescriptorUtil.class.getResource(file);
            if (pluginDescriptorURL == null) {
                throw new FileNotFoundException("File " + file + " not found");
            }

            JAXBContext jaxbContext = JAXBContext.newInstance(DescriptorPackages.PC_PLUGIN);
            URL pluginSchemaURL = PluginDescriptorUtil.class.getClassLoader().getResource("rhq-plugin.xsd");
            Schema pluginSchema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(
                pluginSchemaURL);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            ValidationEventCollector vec = new ValidationEventCollector();
            unmarshaller.setEventHandler(vec);
            unmarshaller.setSchema(pluginSchema);

            return (PluginDescriptor) unmarshaller.unmarshal(pluginDescriptorURL.openStream());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object find(PluginDescriptor descriptor, String path) {
        JXPathContext context = JXPathContext.newContext(descriptor);
        return context.getValue(path);
    }

    public static ConfigurationDefinition loadPluginConfigDefFor(PluginDescriptor descriptor, String path,
        String configName) {
        try {
            ResourceDescriptor resourceDescriptor = (ResourceDescriptor) find(descriptor, path);
            return ConfigurationMetadataParser.parse(configName, resourceDescriptor.getPluginConfiguration());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ConfigurationDefinition loadResourceConfigDefFor(PluginDescriptor descriptor, String path,
        String configName) {
        try {
            ResourceDescriptor resourceDescriptor = (ResourceDescriptor) find(descriptor, path);
            return ConfigurationMetadataParser.parse(configName, resourceDescriptor.getResourceConfiguration());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
