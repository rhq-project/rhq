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
package org.rhq.enterprise.server.jaxb.adapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;

/**This adapter is a JAXB wrapper for the Configuration class.  JAXB works with JavaBeans and 
 * Configuration does not adhere to those conventions strictly and creates wrapper types 
 * on the fly which JAXB has difficulty with.  This means that on the Webservice side 
 * instead of Configuration JAXB types there necessarily has to be WsConfiguration alternative types.
 * 
 * @author Simeon Pinder
 *
 */
public class ConfigurationAdapter extends XmlAdapter<WsConfiguration, Configuration> {

    /**Converts a Configuration type back to marshallable JAXB type.
     * 
     */
    public WsConfiguration marshal(Configuration opaque) throws Exception {
        WsConfiguration config = null;
        if (opaque != null) {
            config = new WsConfiguration(opaque);
        } else {
            throw new IllegalArgumentException("The configuration passed in was null.");
        }
        return config;
    }

    /**
     * Converts the WsConfiguration type back into familiar Configuration type on server side.
     * 
     */
    public Configuration unmarshal(WsConfiguration marshallable) throws Exception {
        Configuration config = null;
        if (marshallable != null) {

            //create new Config instance to be returned
            config = new Configuration();

            ArrayList<Property> allProperties = new ArrayList<Property>();

            //go through all property(Simple,List,Map) and add to properties if not already there
            for (PropertySimple type : marshallable.propertySimpleContainer) {
                //set reference to configuration. Causes cycle but needed back on server side?
                type.setConfiguration(config);
                allProperties.add(type);
            }
            for (PropertyMap type : marshallable.propertyMapContainer) {
                allProperties.add(type);
                type.setConfiguration(config);
            }
            for (PropertyList type : marshallable.propertyListContainer) {
                allProperties.add(type);
                type.setConfiguration(config);
            }
            config.setProperties(allProperties);
        } else {
            throw new IllegalArgumentException("The WsConfiguration type passed in was null.");
        }
        return config;
    }
}

/**Purpose of this class is to create a JAXB marshallable class for Configuration
 * that does not have the same problems being serialized as Configuration.
 * 
 * @author Simeon Pinder
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
class WsConfiguration {

    //FIELDS : BEGIN
    @XmlTransient
    private Configuration CONFIG = null;

    private int id = -1;
    private String notes = "";
    private long version;
    private long ctime;
    private long mtime;

    public List<Property> properties = new ArrayList<Property>();

    //now individual property maps
    public List<PropertyList> propertyListContainer = new ArrayList<PropertyList>();
    public List<PropertySimple> propertySimpleContainer = new ArrayList<PropertySimple>();
    public List<PropertyMap> propertyMapContainer = new ArrayList<PropertyMap>();
    public Collection<String> names = new ArrayList<String>();

    //FIELDS: END

    //default no args constructor for JAXB and bean requirement
    public WsConfiguration() {
        //set default for new Configuration
        this.ctime = System.currentTimeMillis();
        this.mtime = System.currentTimeMillis();
        this.notes = "";
    }

    public WsConfiguration(Configuration opaque) {
        //copy over all core information
        this.CONFIG = opaque;
        this.id = opaque.getId();
        this.notes = opaque.getNotes();
        this.version = opaque.getVersion();
        this.ctime = opaque.getCreatedTime();
        this.mtime = opaque.getModifiedTime();

        this.propertyListContainer = new ArrayList<PropertyList>();
        this.propertyMapContainer = new ArrayList<PropertyMap>();
        this.propertySimpleContainer = new ArrayList<PropertySimple>();

        //Now copy values over.
        populatePropertyMaps();
    }

    private void populatePropertyMaps() {
        if ((this.properties != null) && (this.properties.size() > 0)) {
            for (Property type : this.properties) {
                //store away key in names
                this.names.add(type.getName());
                //now check each property
                Property property = type;
                if (property instanceof PropertySimple) {
                    propertySimpleContainer.add((PropertySimple) type);
                } else if (property instanceof PropertyList) {
                    propertyListContainer.add((PropertyList) type);
                } else if (property instanceof PropertyMap) {
                    propertyMapContainer.add((PropertyMap) type);
                }
            }
        }
    }
    //METHODS :END
}