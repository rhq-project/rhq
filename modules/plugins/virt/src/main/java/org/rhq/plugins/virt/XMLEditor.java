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
package org.rhq.plugins.virt;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import org.rhq.core.domain.configuration.AbstractPropertyMap;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;

/**
 * Handles reading and saving changes to the libVirt xml format for domain and network definitions.
 *
 * @author Greg Hinkle
 */
public class XMLEditor {

    private static Log log = LogFactory.getLog(LibVirtConnection.class);

    /**
     * Only updates simple properties right now
     * TODO GH: update all properties
     * @param config
     * @param xmlToEdit
     * @return
     * @throws JDOMException
     * @throws IOException
     */
    public static String updateDomainXML(Configuration config, String xmlToEdit) {

        try {
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(new StringReader(xmlToEdit));

            Element root = doc.getRootElement();

            root.setAttribute("type", config.getSimple("type").getStringValue());

            updateSimpleNode(config, root, "name");
            updateSimpleNode(config, root, "uuid");

            updateSimpleNode(config, root, "memory");
            updateSimpleNode(config, root, "currentMemory");

            updateSimpleNode(config, root, "vcpu");

            updateSimpleNode(config, root, "on_poweroff");
            updateSimpleNode(config, root, "on_reboot");
            updateSimpleNode(config, root, "on_crash");

            XMLOutputter outputter = new XMLOutputter();
            outputter.getFormat().setIndent("    ");
            outputter.getFormat().setLineSeparator("\n");
            return outputter.outputString(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getDomainXml(Configuration config) {
        Document doc = new Document();

        Element root = new Element("domain");
        doc.setRootElement(root);

        root.setAttribute("type", config.getSimple("type").getStringValue());

        addSimpleNode(config, root, "name");
        addSimpleNode(config, root, "uuid");

        addSimpleNode(config, root, "memory");
        addSimpleNode(config, root, "currentMemory");

        addSimpleNode(config, root, "vcpu");

        addSimpleNode(config, root, "on_poweroff");
        addSimpleNode(config, root, "on_reboot");
        addSimpleNode(config, root, "on_crash");

        Element devices = new Element("devices");
        root.addContent(devices);

        PropertyList interfaces = config.getList("interfaces");
        for (Property intf : interfaces.getList()) {
            PropertyMap intfMap = (PropertyMap) intf;

            Element intfElement = new Element("interface");
            devices.addContent(intfElement);
            addSimpleAttribute(intfMap, intfElement, "type", "type");

            Element sourceElement = new Element("source");
            intfElement.addContent(sourceElement);
            addSimpleAttribute(intfMap, sourceElement, "source", "bridge");

            Element targetElement = new Element("target");
            intfElement.addContent(targetElement);
            addSimpleAttribute(intfMap, targetElement, "target", "dev");

            Element macElement = new Element("mac");
            intfElement.addContent(macElement);
            addSimpleAttribute(intfMap, macElement, "mac", "address");

            Element scriptElement = new Element("script");
            intfElement.addContent(scriptElement);
            addSimpleAttribute(intfMap, scriptElement, "script", "path");
        }

        PropertyList disks = config.getList("disks");
        for (Property disk : disks.getList()) {
            PropertyMap diskMap = (PropertyMap) disk;

            Element diskElement = new Element("disk");
            devices.addContent(diskElement);
            addSimpleAttribute(diskMap, diskElement, "type", "type");
            addSimpleAttribute(diskMap, diskElement, "device", "dev");

            Element driverElement = new Element("driver");
            diskElement.addContent(driverElement);
            addSimpleAttribute(diskMap, driverElement, "driverName", "name");
            addSimpleAttribute(diskMap, driverElement, "driverType", "type");

            Element sourceElement = new Element("source");
            diskElement.addContent(sourceElement);
            addSimpleAttribute(diskMap, driverElement, "sourceFile", "file");
            addSimpleAttribute(diskMap, driverElement, "sourceDevice", "dev");

            Element targetElement = new Element("target");
            diskElement.addContent(targetElement);
            addSimpleAttribute(diskMap, driverElement, "targetDevice", "dev");
            addSimpleAttribute(diskMap, driverElement, "targetBus", "bus");

        }

        XMLOutputter outputter = new XMLOutputter();
        outputter.getFormat().setIndent("    ");
        outputter.getFormat().setLineSeparator("\n");
        return outputter.outputString(doc);
    }

    /**
     * Parse the XML from calling libvirts virDomainGetXMLDesc()
     * @param xml XML String from libvirt
     * @return The resulting configuration
     * @see {http://libvirt.org/formatdomain.html}
     */
    public static Configuration getDomainConfiguration(String xml) {

        if (xml == null) {
            return null;
        }

        try {
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(new StringReader(xml));

            Configuration config = new Configuration();

            // Get the root element
            Element root = doc.getRootElement();

            String type = root.getAttribute("type").getValue();
            config.put(new PropertySimple("type", type));

            addChildTextSimpleProperty(config, root, "name");
            addChildTextSimpleProperty(config, root, "uuid");

            addChildTextSimpleProperty(config, root, "memory");
            addChildTextSimpleProperty(config, root, "currentMemory");

            addChildTextSimpleProperty(config, root, "vcpu");

            addChildTextSimpleProperty(config, root, "on_poweroff");
            addChildTextSimpleProperty(config, root, "on_reboot");
            addChildTextSimpleProperty(config, root, "on_crash");

            Element devices = root.getChild("devices");

            PropertyList interfaces = new PropertyList("interfaces");
            config.put(interfaces);

            List<Element> interfaceElementList = devices.getChildren("interface");
            for (Element interfaceElement : interfaceElementList) {
                PropertyMap intf = new PropertyMap("interface");
                String iType = interfaceElement.getAttribute("type").getValue();
                intf.put(new PropertySimple("type", iType));

                addChildAttribute(intf, interfaceElement, "mac", "address", "macAddress");
                addChildAttribute(intf, interfaceElement, "target", "dev", "target");
                addChildAttribute(intf, interfaceElement, "source", "bridge", "source");
                addChildAttribute(intf, interfaceElement, "script", "path", "script");

                interfaces.add(intf);
            }

            PropertyList disks = new PropertyList("disks");
            config.put(disks);

            List<Element> diskElementList = devices.getChildren("disk");
            for (Element diskElement : diskElementList) {
                PropertyMap disk = new PropertyMap("disk");
                Attribute diskType = diskElement.getAttribute("type");
                String dType;
                if (diskType != null)
                    dType = diskType.getValue();
                else
                    dType = "block"; // see http://libvirt.org/formatdomain.html#elementsDisks -- 'either: file or block
                disk.put(new PropertySimple("type", dType));

                Attribute diskDevice = diskElement.getAttribute("device");
                if (diskDevice != null) {
                    disk.put(new PropertySimple("device", diskDevice.getValue()));
                }

                addChildAttribute(disk, diskElement, "driver", "name", "driverName");
                addChildAttribute(disk, diskElement, "driver", "type", "driverType");
                addChildAttribute(disk, diskElement, "source", "file", "sourceFile");
                addChildAttribute(disk, diskElement, "source", "dev", "sourceDevice");
                addChildAttribute(disk, diskElement, "target", "dev", "targetDevice");
                addChildAttribute(disk, diskElement, "target", "bus", "targetBus");

                disks.add(disk);
            }

            return config;
        } catch (Exception e) {
            log.error("Error parsing the domain XML", e);
        }

        return null;
    }

    /**
     * Parse the XML for libvirts network definition()
     * @param xml XML String from libvirt
     * @return The resulting configuration
     * @see {http://www.libvirt.org/formatnetwork.html}
     */
    public static Configuration getNetworkConfiguration(String xml, boolean autostart) {

        if (xml == null) {
            return null;
        }
        Configuration config = new Configuration();

        try {
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(new StringReader(xml));

            // Get the root element
            Element root = doc.getRootElement();
            addChildTextSimpleProperty(config, root, "name");
            addChildTextSimpleProperty(config, root, "uuid");
            config.put(new PropertySimple("autostart", autostart));
            addChildAttribute(config, root, "bridge", "name", "bridge");
            addChildAttribute(config, root, "forward", "mode", "forwardMode");
            addChildAttribute(config, root, "forward", "dev", "forwardDevice");
            addChildAttribute(config, root, "ip", "address", "ipaddress");
            addChildAttribute(config, root, "ip", "netmask", "netmask");
            Element ip = root.getChild("ip");
            if (ip != null) {
                Element dhcp = ip.getChild("dhcp");
                if (dhcp != null) {
                    addChildAttribute(config, dhcp, "range", "start", "dhcpStart");
                    addChildAttribute(config, dhcp, "range", "end", "dhcpEnd");
                }
            }
        } catch (Exception e) {
            log.error("Error parsing the network XML", e);
        }

        return config;
    }

    public static String getNetworkXml(Configuration config) {
        Document doc = new Document();

        Element root = new Element("network");
        doc.setRootElement(root);
        addSimpleNode(config, root, "name");
        addSimpleNode(config, root, "uuid");

        Element bridge = new Element("bridge");
        bridge.setAttribute("name", config.getSimpleValue("bridge", ""));
        root.addContent(bridge);

        Element forward = new Element("forward");
        forward.setAttribute("mode", config.getSimpleValue("forwardMode", ""));
        forward.setAttribute("dev", config.getSimpleValue("forwardDevice", ""));
        root.addContent(forward);

        Element ip = new Element("ip");
        ip.setAttribute("address", config.getSimpleValue("ipaddress", ""));
        ip.setAttribute("netmask", config.getSimpleValue("netmask", ""));
        root.addContent(ip);

        Element dhcp = new Element("dhcp");
        Element range = new Element("range");
        range.setAttribute("start", config.getSimpleValue("dhcpStart", ""));
        range.setAttribute("end", config.getSimpleValue("dhcpEnd", ""));
        dhcp.addContent(range);
        root.addContent(dhcp);

        XMLOutputter outputter = new XMLOutputter();
        outputter.getFormat().setIndent("    ");
        outputter.getFormat().setLineSeparator("\n");
        return outputter.outputString(doc);
    }

    private static void addSimpleNode(AbstractPropertyMap config, Element parent, String name) {
        PropertySimple prop = config.getSimple(name);
        if (prop != null) {
            Element e = new Element(name);
            e.setText(prop.getStringValue());
            parent.addContent(e);
        }
    }

    private static void addSimpleAttribute(AbstractPropertyMap config, Element element, String configName,
        String attributeName) {
        PropertySimple prop = config.getSimple(configName);
        if (prop != null) {
            element.setAttribute(attributeName, prop.getStringValue());
        }
    }

    private static void updateSimpleNode(AbstractPropertyMap config, Element parent, String name) {
        Element e = parent.getChild(name);
        e.setText(config.getSimple(name).getStringValue());
    }

    private static void addChildAttribute(AbstractPropertyMap config, Element element, String childElementName,
        String childAttributeName, String propertyName) {
        Element child = element.getChild(childElementName);
        if (child != null) {
            String attributeValue = child.getAttributeValue(childAttributeName);
            if (attributeValue != null) {
                config.put(new PropertySimple(propertyName, attributeValue));
            }
        }
    }

    private static void addChildTextSimpleProperty(Configuration config, Element root, String property) {
        String val = root.getChildText(property);
        config.put(new PropertySimple(property, val));
    }
}