/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.plugins.hadoop;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.util.file.FileUtil;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class HadoopServerConfigurationDelegate {

    private static final Log LOG = LogFactory.getLog(HadoopServerConfigurationDelegate.class);

    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();
    private static final XMLOutputFactory XML_OUTPUT_FACTORY = XMLOutputFactory.newInstance();
    
    private static final String CONFIGURATION_TAG_NAME = "configuration";
    private static final String PROPERTY_TAG_NAME = "property";
    private static final String NAME_TAG_NAME = "name";
    private static final String VALUE_TAG_NAME = "value";
    
    private static final Pattern PROPERTY_NAME_EXTRACT_PATTERN = Pattern.compile("<name>(.*)<\\/name>");
    private static final Pattern PROPERTY_VALUE_REPLACE_PATTERN = Pattern.compile("<value>.*<\\/value>");
    
    private static class DetectedPropertyNameAndUpdatedTag {
        String propertyName;
        String updatedTag;
    }
    
    private static class PropertiesPerConfigFileBuilder {
        private static class ConfigFileAndConfigName {
            File configFile;
            String propertyName;

            ConfigFileAndConfigName(File homeDir, String propertyDefinitionName) {
                String[] parts = propertyDefinitionName.split(":");

                String configFileName = parts[0];

                configFile = new File(homeDir, configFileName);
                propertyName = parts[1];

                if (!configFile.exists()) {
                    throw new IllegalArgumentException("The expected configuration file ("
                        + configFile.getAbsolutePath() + ") doesn't exist.");
                }
            }
        }

        private Map<File, Map<String, PropertySimple>> propertiesPerFile =
            new HashMap<File, Map<String, PropertySimple>>();
        private File homeDir;

        public PropertiesPerConfigFileBuilder(File homeDir) {
            this.homeDir = homeDir;
        }

        void addProperty(String name, Configuration parentConfig) {
            PropertySimple ret = new PropertySimple();
            ret.setName(name);
            parentConfig.put(ret);
            addProperty(ret);
        }

        void addProperty(PropertySimple property) {
            ConfigFileAndConfigName fn = new ConfigFileAndConfigName(homeDir, property.getName());

            Map<String, PropertySimple> props = propertiesPerFile.get(fn.configFile);
            if (props == null) {
                props = new HashMap<String, PropertySimple>();
                propertiesPerFile.put(fn.configFile, props);
            }

            props.put(fn.propertyName, property);
        }

        Map<File, Map<String, PropertySimple>> getPropertiesPerFilePerConfigName() {
            return propertiesPerFile;
        }
    }

    private ResourceContext<ResourceComponent<?>> componentContext;

    public HadoopServerConfigurationDelegate(ResourceContext<ResourceComponent<?>> componentContext) {
        this.componentContext = componentContext;
    }

    public Configuration loadConfiguration() throws Exception {
        ConfigurationDefinition definition = componentContext.getResourceType().getResourceConfigurationDefinition();
        Configuration config = new Configuration();

        File homeDir = getHomeDir();

        fillResourceConfiguration(homeDir, config, definition);

        return config;
    }

    public void updateConfiguration(Configuration config) throws Exception {
        //gather the files to update
        PropertiesPerConfigFileBuilder bld = new PropertiesPerConfigFileBuilder(getHomeDir());
        
        for (Property p : config.getProperties()) {
            if (!(p instanceof PropertySimple)) {
                continue;
            }
        
            PropertySimple property = (PropertySimple) p;
            bld.addProperty(property);
        }
        
        for (Map.Entry<File, Map<String, PropertySimple>> e : bld.getPropertiesPerFilePerConfigName().entrySet()) {
            updateFile(e.getKey(), e.getValue());
        }
    }

    private File getHomeDir() {
        File homeDir =
            new File(componentContext.getPluginConfiguration().getSimpleValue(HadoopServerDiscovery.HOME_DIR_PROPERTY));

        if (!homeDir.exists()) {
            throw new IllegalArgumentException("The configured home directory of this Hadoop instance ("
                + homeDir.getAbsolutePath() + ") no longer exists.");
        }

        if (!homeDir.isDirectory()) {
            throw new IllegalArgumentException("The configured home directory of this Hadoop instance ("
                + homeDir.getAbsolutePath() + ") is not a directory.");
        }

        if (!homeDir.canRead()) {
            throw new IllegalArgumentException("The configured home directory of this Hadoop instance ("
                + homeDir.getAbsolutePath() + ") is not readable.");
        }

        return homeDir;
    }

    public static void
        fillResourceConfiguration(File homeDir, Configuration config, ConfigurationDefinition definition)
            throws XMLStreamException, IOException {
        //the config is just a bunch of simples, so this is rather easy.. no cumbersome traversal of property maps and lists

        PropertiesPerConfigFileBuilder bld = new PropertiesPerConfigFileBuilder(homeDir);

        for (PropertyDefinition pd : definition.getPropertyDefinitions().values()) {
            if (!(pd instanceof PropertyDefinitionSimple)) {
                //hmm... well, someone thought it's enough to change the config and the code would be clever.
                //it's not ;)
                continue;
            }

            String propertyName = pd.getName();
            bld.addProperty(propertyName, config);
        }

        for (Map.Entry<File, Map<String, PropertySimple>> e : bld.getPropertiesPerFilePerConfigName().entrySet()) {
            File configFile = e.getKey();
            Map<String, PropertySimple> propertiesToFind = e.getValue();

            parseAndAssignProps(configFile, propertiesToFind);
        }
    }

    public static void parseAndAssignProps(File configFile, Map<String, PropertySimple> props)
        throws XMLStreamException, IOException {
        FileInputStream in = new FileInputStream(configFile);
        XMLStreamReader rdr = XML_INPUT_FACTORY.createXMLStreamReader(in);
        try {
            boolean inProperty = false;
            String propertyName = null;
            String propertyValue = null;

            while (rdr.hasNext()) {
                int event = rdr.next();

                String tag = null;

                switch (event) {
                case XMLStreamReader.START_ELEMENT:
                    tag = rdr.getName().getLocalPart();
                    if (PROPERTY_TAG_NAME.equals(tag)) {
                        inProperty = true;
                    } else if (inProperty && NAME_TAG_NAME.equals(tag)) {
                        propertyName = rdr.getElementText();
                    } else if (inProperty && VALUE_TAG_NAME.equals(tag)) {
                        propertyValue = rdr.getElementText();
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    tag = rdr.getName().getLocalPart();
                    if (PROPERTY_TAG_NAME.equals(tag)) {
                        inProperty = false;

                        PropertySimple prop = props.get(propertyName);
                        if (prop != null) {
                            prop.setValue(propertyValue);
                        }

                        propertyName = null;
                        propertyValue = null;
                    }
                    break;
                }
            }
        } finally {
            rdr.close();
            in.close();
        }
    }

    private static void updateFile(File configFile, Map<String, PropertySimple> allProps) throws IOException,
        InterruptedException, XMLStreamException {
        InputStream in = null;
        XMLStreamReader rdr = null;

        OutputStream out = null;
        XMLStreamWriter outWrt = null;

        try {
            Set<String> processedPropertyNames = new HashSet<String>();
            
            in = new BufferedInputStream(new FileInputStream(configFile));
            rdr = XML_INPUT_FACTORY.createXMLStreamReader(in);

            File tmpFile = File.createTempFile("hadoop-plugin", null);
            out = new FileOutputStream(tmpFile);
            outWrt = XML_OUTPUT_FACTORY.createXMLStreamWriter(out);

            ByteArrayOutputStream stash = new ByteArrayOutputStream();
            XMLStreamWriter stashWrt = XML_OUTPUT_FACTORY.createXMLStreamWriter(stash);
            boolean outputActive = true;

            outWrt.writeStartDocument();

            while (rdr.hasNext()) {
                int event = rdr.next();

                XMLStreamWriter wrt = outputActive ? outWrt : stashWrt;

                switch (event) {
                case XMLStreamConstants.ATTRIBUTE:
                    break;
                case XMLStreamConstants.CDATA:
                    wrt.writeCData(rdr.getText());
                    break;
                case XMLStreamConstants.CHARACTERS:
                    wrt.writeCharacters(rdr.getText());
                    break;
                case XMLStreamConstants.COMMENT:
                    wrt.writeComment(rdr.getText());
                    break;
                case XMLStreamConstants.DTD:
                    wrt.writeDTD(rdr.getText());
                    break;
                case XMLStreamConstants.END_DOCUMENT:
                    wrt.writeEndDocument();
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (PROPERTY_TAG_NAME.equals(rdr.getName().getLocalPart())) {
                        String encoding = rdr.getEncoding();
                        if (encoding == null) {
                            encoding = "UTF-8";
                        }

                        String propertyTagSoFar =
                            Charset.forName(encoding).decode(ByteBuffer.wrap(stash.toByteArray())).toString();
                        DetectedPropertyNameAndUpdatedTag propAndTag = updateProperty(propertyTagSoFar, allProps);
                                                
                        //yes, we're intentionally circumventing the xml stream writer, because we already have the XML data we want to write.
                        outWrt.flush();
                        out.write(propAndTag.updatedTag.getBytes("UTF-8"));

                        processedPropertyNames.add(propAndTag.propertyName);
                        
                        //reset stuff
                        stash.reset();
                        wrt = outWrt;
                        outputActive = true;
                    } else if (CONFIGURATION_TAG_NAME.equals(rdr.getName().getLocalPart())) {
                        //now add the new props
                        for(String prop : processedPropertyNames) {
                            allProps.remove(prop);
                        }
                        
                        for(Map.Entry<String, PropertySimple> e : allProps.entrySet()) {
                            outWrt.writeStartElement(PROPERTY_TAG_NAME);
                            
                            outWrt.writeStartElement(NAME_TAG_NAME);
                            outWrt.writeCharacters(e.getKey());
                            outWrt.writeEndElement();
                            
                            outWrt.writeStartElement(VALUE_TAG_NAME);
                            outWrt.writeCharacters(e.getValue().getStringValue());
                            outWrt.writeEndElement();

                            outWrt.writeEndElement();
                        }
                    }
                    wrt.writeEndElement();
                    break;
                case XMLStreamConstants.ENTITY_DECLARATION:
                    //XXX could not find what to do with this
                    break;
                case XMLStreamConstants.ENTITY_REFERENCE:
                    wrt.writeEntityRef(rdr.getText());
                    break;
                case XMLStreamConstants.NAMESPACE:
                    for (int i = 0; i < rdr.getNamespaceCount(); ++i) {
                        wrt.writeNamespace(rdr.getNamespacePrefix(i), rdr.getNamespaceURI(i));
                    }
                    break;
                case XMLStreamConstants.NOTATION_DECLARATION:
                    //XXX could not find what to do with this
                    break;
                case XMLStreamConstants.PROCESSING_INSTRUCTION:
                    wrt.writeProcessingInstruction(rdr.getPITarget(), rdr.getPIData());
                    break;
                case XMLStreamConstants.SPACE:
                    wrt.writeCharacters(rdr.getText());
                    break;
                case XMLStreamConstants.START_DOCUMENT:
                    //this seems to be never called for some strange reason
                    //wrt.writeStartDocument();
                    break;
                case XMLStreamConstants.START_ELEMENT:
                    wrt.writeStartElement(rdr.getName().getPrefix(), rdr.getName().getLocalPart(), rdr.getName()
                        .getNamespaceURI());

                    for (int i = 0; i < rdr.getAttributeCount(); ++i) {
                        wrt.writeAttribute(rdr.getAttributePrefix(i), rdr.getAttributeNamespace(i),
                            rdr.getAttributeLocalName(i), rdr.getAttributeValue(i));
                    }
                    
                    if (PROPERTY_TAG_NAME.equals(rdr.getName().getLocalPart())) {
                        wrt.writeCharacters("");
                        outputActive = false;
                    }
                    break;
                }
            }

            outWrt.flush();
            out.flush();
            out.close();

            in.close();

            //now copy the temp file in the place of the original one
            FileUtil.copyFile(tmpFile, configFile);
        } finally {
            rdr.close();

            outWrt.flush();
            outWrt.close();

            try {
                in.close();
            } finally {
                out.flush();
                out.close();
            }
        }
    }

    private static DetectedPropertyNameAndUpdatedTag updateProperty(String propertyTag, Map<String, PropertySimple> allProps) {

        DetectedPropertyNameAndUpdatedTag ret = new DetectedPropertyNameAndUpdatedTag();
        //for now
        ret.updatedTag = propertyTag;
        
        //extract the name of the property we're dealing with
        Matcher propertyNameMatcher = PROPERTY_NAME_EXTRACT_PATTERN.matcher(propertyTag);
        if (!propertyNameMatcher.find()) {
            return ret;
        }

        String propertyName = propertyNameMatcher.group(1);
        ret.propertyName = propertyName;
        
        if (propertyName == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Possibly invalid property tag?\n" + propertyTag);
            }

            return ret;
        }

        //try to find the property in allProps
        PropertySimple prop = allProps.get(propertyName);

        if (prop == null) {
            return ret;
        }

        if (prop.getStringValue() == null) {
            //this property has been unset
            ret.updatedTag = "";
            return ret;
        }

        Matcher propertyValueMatcher = PROPERTY_VALUE_REPLACE_PATTERN.matcher(propertyTag);
        if (!propertyValueMatcher.find()) {
            return ret;
        }

        ret.updatedTag = propertyValueMatcher.replaceAll("<value>" + prop.getStringValue() + "</value>");
        
        return ret;
    }
}
