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

package org.rhq.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jetbrains.annotations.NotNull;

/**
 * This class produces a JAXB serialized collection of objects and can convert
 * such serialized data back to  a list of objects.
 * <p>
 * The objects passed to this class must therefore be JAXB serializable.
 * <p>
 * This class is intended to be used in tests that need to persist the state
 * of some object collection.
 * 
 * @author Lukas Krejci
 */
public class ObjectCollectionSerializer {
    private List<Object> objects = new ArrayList<Object>();
    private Set<Class<?>> classes = new HashSet<Class<?>>();
    
    /**
     * Returns a list of objects that are added to this serializer.
     * This list is NOT modifiable, use it only for inspection.
     * 
     * @return
     */
    public @NotNull List<Object> getObjects() {
        return Collections.unmodifiableList(objects);
    }
    
    public void addObject(@NotNull Object o) {
        this.objects.add(o);
        this.classes.add(o.getClass());
    }
    
    public void addObjects(@NotNull Collection<?> objects) {
        for(Object o : objects) {
            if (o == null) {
                continue;
            }
            
            addObject(o);
        }       
    }
    
    /**
     * Returns a set of classes of objects that are to be serialized.
     * This set is not modifiable, use it only for informational purposes. 
     * @return
     */
    public @NotNull Set<Class<?>> getClasses() {
        return Collections.unmodifiableSet(classes);
    }
    
    /**
     * Serializes the objects added to this serialize to the given output.
     * 
     * @param output the output stream to serialize to
     * 
     * @throws IOException 
     * @throws JAXBException 
     */
    public void serialize(OutputStream output) throws IOException, JAXBException {
        PrintStream out = new PrintStream(output);
        out = new PrintStream(out, true, "UTF-8");

        out.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
        out.append("<inventory-dump>\n");

        out.append("<classes-used>\n");

        for (Class<?> cls : classes) {
            out.append("<class>").append(cls.getName()).append("</class>\n");
        }

        out.append("</classes-used>\n");

        out.append("<objects>\n");

        JAXBContext context = JAXBContext.newInstance(classes.toArray(new Class<?>[classes.size()]));

        Marshaller marshaller = context.createMarshaller();

        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

        for (Object o : objects) {
            marshaller.marshal(o, out);
        }

        out.append("</objects>\n");

        out.append("</inventory-dump>\n");
    }
    
    public List<?> deserialize(InputStream inputStream) throws IOException, XMLStreamException, ClassNotFoundException, JAXBException {
        XMLStreamReader rdr = XMLInputFactory.newFactory().createXMLStreamReader(inputStream);
        return deserialize(rdr);
    }
    
    public List<?> deserialize(Reader inputReader) throws IOException, XMLStreamException, ClassNotFoundException, JAXBException {
        XMLStreamReader rdr = XMLInputFactory.newFactory().createXMLStreamReader(inputReader);
        return deserialize(rdr);
    }

    /**
     * This deserializes given input stream into a list of objects.
     * <p>
     * This method uses the current thread's context classloader to resolve
     * the classes of the objects to be deserialized if such classloader is set.
     * 
     * @param inputStream the input stream to read the data from
     * 
     * @return the list of deserialized objects
     * @throws XMLStreamException 
     * @throws ClassNotFoundException 
     * @throws JAXBException 
     */
    public List<?> deserialize(XMLStreamReader reader) throws IOException, XMLStreamException, ClassNotFoundException, JAXBException {
        List<Object> ret = new ArrayList<Object>();
        
        boolean inObjects = false;
        
        Unmarshaller unmarshaller = null;
        
        while (reader.hasNext()) {
            reader.next();
            
            switch(reader.getEventType()) {
            case XMLStreamReader.START_ELEMENT:
                if ("classes-used".equals(reader.getName().getLocalPart())) {
                    Set<Class<?>> classesUsed = deserializedClassesToUse(reader);
                    JAXBContext context = JAXBContext.newInstance(classesUsed.toArray(new Class<?>[classesUsed.size()]));
                    unmarshaller = context.createUnmarshaller();                    
                } else if ("objects".equals(reader.getName().getLocalPart())) {
                    inObjects = true;
                } else if (inObjects) {
                    Object o = unmarshaller.unmarshal(reader);
                    ret.add(o);
                }
                break;
            case XMLStreamReader.END_ELEMENT:
                if ("objects".equals(reader.getName().getLocalPart())) {
                    inObjects = false;
                }
                break;
            }
        }        
           
        return ret;
    }
    
    private Set<Class<?>> deserializedClassesToUse(XMLStreamReader rdr) throws XMLStreamException, ClassNotFoundException {
        HashSet<Class<?>> ret = new HashSet<Class<?>>();
        
        boolean insideClass = false;
        while (rdr.hasNext()) {
            rdr.next();
            
            switch (rdr.getEventType()) {
            case XMLStreamReader.START_ELEMENT:
                if ("class".equals(rdr.getName().getLocalPart())) {
                    insideClass = true;
                } else {
                    insideClass = false;
                }
                break;
            case XMLStreamReader.END_ELEMENT:
                if ("class".equals(rdr.getName().getLocalPart())) {
                    insideClass = false;
                } else if ("classes-used".equals(rdr.getName().getLocalPart())) {
                    return ret;
                }
                break;
            case XMLStreamReader.CHARACTERS:
                if (insideClass) {
                    Class<?> c = null;
                    if (Thread.currentThread().getContextClassLoader() == null) {
                        c = Class.forName(rdr.getText());
                    } else {
                        c = Class.forName(rdr.getText(), false, Thread.currentThread().getContextClassLoader());
                    }
                    
                    ret.add(c);
                }
                break;
            }
        }
        
        return ret;
    }
    
}
