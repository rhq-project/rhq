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

import java.io.StringReader;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.jdom.input.SAXBuilder;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;

/**
 * Handles reading and saving changes to the libVirt xml format for domain definition.
 *
 * @author Greg Hinkle
 */
public class DomainConfigurationEditor {

    /**
     * Only updates simple properties right now
     * TODO GH: update all properties
     * @param config
     * @param xmlToEdit
     * @return
     * @throws JDOMException
     * @throws IOException
     */
    public static String updateXML(Configuration config, String xmlToEdit) {

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

    private static void updateSimpleNode(Configuration config, Element parent, String name) {
        Element e = parent.getChild(name);
        e.setText(config.getSimple(name).getStringValue());
    }


    public static String getXml(Configuration config) {
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
            intfElement.setAttribute("type", intfMap.getSimple("type").getStringValue());

            Element sourceElement = new Element("source");
            intfElement.addContent(sourceElement);
            sourceElement.setAttribute("bridge",intfMap.getSimple("source").getStringValue());

            Element targetElement = new Element("target");
            intfElement.addContent(targetElement);
            targetElement.setAttribute("dev",intfMap.getSimple("target").getStringValue());

            Element macElement = new Element("mac");
            intfElement.addContent(macElement);
            macElement.setAttribute("address",intfMap.getSimple("mac").getStringValue());

            Element scriptElement = new Element("script");
            intfElement.addContent(scriptElement);
            scriptElement.setAttribute("path",intfMap.getSimple("script").getStringValue());
        }

        PropertyList disks = config.getList("disks");
        for (Property disk : disks.getList()) {
            PropertyMap diskMap = (PropertyMap) disk;

            Element diskElement = new Element("disk");
            devices.addContent(diskElement);
            diskElement.setAttribute("type", diskMap.getSimple("type").getStringValue());

            
            Element sourceElement = new Element("driver");
            diskElement.addContent(sourceElement);
            sourceElement.setAttribute("name",diskMap.getSimple("driverName").getStringValue());
            // TODO driverFile

            
            Element targetElement = new Element("source");
            diskElement.addContent(targetElement);
            targetElement.setAttribute("file",diskMap.getSimple("sourceFile").getStringValue());
            // TODO sourceDevice

            Element macElement = new Element("target");
            diskElement.addContent(macElement);
            macElement.setAttribute("dev",diskMap.getSimple("targetDevice").getStringValue());

        }


        XMLOutputter outputter = new XMLOutputter();
        outputter.getFormat().setIndent("    ");
        outputter.getFormat().setLineSeparator("\n");
        return outputter.outputString(doc);
    }

    private static void addSimpleNode(Configuration config, Element parent, String name) {
        Element e = new Element(name);
        e.setText(config.getSimple(name).getStringValue());
        parent.addContent(e);
    }


    public static Configuration getConfiguration(String xml) {
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

                String macAddress = interfaceElement.getChild("mac").getAttribute("address").getValue();
                intf.put(new PropertySimple("macAddress", macAddress));

                String targetDevice = interfaceElement.getChild("target").getAttribute("dev").getValue();
                intf.put(new PropertySimple("target", targetDevice));

                String source = interfaceElement.getChild("source").getAttribute("bridge").getValue();
                intf.put(new PropertySimple("source", source));

                String scriptPath = interfaceElement.getChild("script").getAttribute("path").getValue();
                intf.put(new PropertySimple("script", scriptPath));

                interfaces.add(intf);
            }

            PropertyList disks = new PropertyList("disks");
            config.put(disks);

            List<Element> diskElementList = devices.getChildren("disk");
            for (Element diskElement :diskElementList) {
                PropertyMap disk = new PropertyMap("disk");
                disk.put(new PropertySimple("type", diskElement.getAttribute("type").getValue()));

                String driverName = diskElement.getChild("driver").getAttribute("name").getValue();
                disk.put(new PropertySimple("driverName",driverName));

                String sourceFile = diskElement.getChild("source").getAttributeValue("file");
                disk.put(new PropertySimple("sourceFile",sourceFile));

                String targetDevice = diskElement.getChild("target").getAttributeValue("dev");
                disk.put(new PropertySimple("targetDevice",targetDevice));

                disks.add(disk);
            }

            return config;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static void addChildTextSimpleProperty(Configuration config, Element root, String property) {
        String val = root.getChildText(property);
        config.put(new PropertySimple(property, val));
    }

    public static void main(String[] args) throws JDOMException, IOException {
        String xml = "<domain type='xen' id='6'>\n" + "  <name>ghvirt1</name>\n"
            + "  <uuid>81731cbe-009c-0f8c-772a-46c9a345a4c4</uuid>\n" + "  <bootloader>/usr/bin/pygrub</bootloader>\n"
            + "  <os>\n" + "    <type>linux</type>\n" + "  </os>\n" + "  <memory>819200</memory>\n"
            + "  <currentMemory>512000</currentMemory>\n" + "  <vcpu>1</vcpu>\n"
            + "  <on_poweroff>destroy</on_poweroff>\n" + "  <on_reboot>restart</on_reboot>\n"
            + "  <on_crash>restart</on_crash>\n" + "  <devices>\n" + "    <interface type='bridge'>\n"
            + "      <source bridge='virbr0'/>\n" + "      <target dev='vif6.0'/>\n"
            + "      <mac address='00:16:3e:5e:ef:b6'/>\n" + "      <script path='vif-bridge'/>\n"
            + "    </interface>\n" + "    <disk type='file' device='disk'>\n" + "      <driver name='file'/>\n"
            + "      <source file='/home/ghinkle/ghvirt1'/>\n" + "      <target dev='xvda'/>\n" + "    </disk>\n"
            + "    <input type='mouse' bus='xen'/>\n" + "    <graphics type='vnc' port='5900'/>\n"
            + "    <console tty='/dev/pts/4'/>\n" + "  </devices>\n" + "</domain>";

        Configuration c = getConfiguration(xml);
        for (Property p : c.getProperties()) {
            if (p instanceof PropertySimple) {
                System.out.println(p.getName() + " = " + ((PropertySimple) p).getStringValue());
            }
        }

        System.out.println("-------------------------");
        System.out.println(xml);
        System.out.println("-------------------------");
        c.getSimple("currentMemory").setIntegerValue(768000);
        System.out.println(updateXML(c, xml));





    }
}