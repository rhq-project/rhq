/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.sync.exporters;

import java.util.Arrays;
import java.util.Iterator;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;

import org.rhq.enterprise.server.sync.ExportWriter;

/**
 * 
 *
 * @author Lukas Krejci
 */
public abstract class JAXBExportingIterator<T, E> extends AbstractDelegatingExportingIterator<T, E> {

    private Marshaller marshaller;
    
    public JAXBExportingIterator(Iterator<E> sourceIterator, Class<?>... jaxbBoundClasses) {
        super(sourceIterator);
        
        try {
            JAXBContext context = JAXBContext.newInstance(jaxbBoundClasses);
            marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");            
        } catch (JAXBException e) {
            throw new IllegalStateException("Could not create JAXB serializer for classes " + Arrays.asList(jaxbBoundClasses), e);
        }
    }
    
    @Override
    public void export(ExportWriter output) throws XMLStreamException {
        try {
            marshaller.marshal(getCurrent(), output);
        } catch (JAXBException e) {
            throw new XMLStreamException("Failed to export an entity.", e);
        }
    }
}
