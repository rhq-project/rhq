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
package org.rhq.core.pc.plugin.descriptor.test;

import java.io.File;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.util.ValidationEventCollector;
import org.testng.annotations.Test;
import org.rhq.core.clientapi.descriptor.DescriptorPackages;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ServiceDescriptor;

@Test
public class DescriptorTest {
    @Test
    public void readDescriptor() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(DescriptorPackages.PC_PLUGIN);

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        ValidationEventCollector vec = new ValidationEventCollector();
        unmarshaller.setEventHandler(vec);
        PluginDescriptor plugin = (PluginDescriptor) unmarshaller.unmarshal(new File(
            "src/test/xml/rhq-plugin-jmx-server-test.xml"));

        for (ValidationEvent event : vec.getEvents()) {
            System.out.println(event.getSeverity() + ":" + event.getMessage() + "    " + event.getLinkedException());
        }

        System.out.println(plugin.getDisplayName());
    }

    @Test
    public void writeDescriptor() throws JAXBException {
        PluginDescriptor p = new PluginDescriptor();
        p.setName("Postgres");
        p.setDisplayName("Postgres");
        p.setPackage("org.rhq.plugins.postgres");

        ServiceDescriptor table = new ServiceDescriptor();
        table.setName("Table");
        table.setClazz("org.rhq.plugins.postgres.TableServiceComponent");

        p.getServices().add(table);

        JAXBContext jaxbContext = JAXBContext.newInstance(DescriptorPackages.PC_PLUGIN);

        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true); //JMMarshallerImpl.JAXME_INDENTATION_STRING, "\r\n");

        marshaller.marshal(p, System.out);
    }
}