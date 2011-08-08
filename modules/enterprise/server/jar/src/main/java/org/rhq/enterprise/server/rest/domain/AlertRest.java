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
package org.rhq.enterprise.server.rest.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;

import org.jboss.resteasy.annotations.providers.jaxb.json.Mapped;
import org.jboss.resteasy.annotations.providers.jaxb.json.XmlNsMap;
import org.jboss.resteasy.links.AddLinks;
import org.jboss.resteasy.links.LinkResource;
import org.jboss.resteasy.links.RESTServiceDiscovery;
import org.jboss.resteasy.spi.touri.URITemplate;

/**
 * A single alert
 * @author Heiko W. Rupp
 */
@URITemplate("/alert/{id}")
@XmlRootElement
//@XmlType(propOrder = {"id","name","alertTime","description","alertDefinitionId","definitionEnabled","resourceId","resourceName","ackBy","ackTime","rest"})
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
@Mapped(namespaceMap = @XmlNsMap(jsonName = "atom", namespace = "http://www.w3.org/2005/Atom"))
public class AlertRest {

    private int id;
    private String name;
    private AlertDefinitionRest alertDefinition;
    private boolean definitionEnabled;
    String ackBy;
    long ackTime;
    long alertTime;
    String description;


	private RESTServiceDiscovery rest;

    public void setResource(ResourceWithType resource) {
        this.resource = resource;
    }

    private ResourceWithType resource
            ;

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAlertDefinition(AlertDefinitionRest alertDefinitionId) {
        this.alertDefinition = alertDefinitionId;
    }

    @XmlID
    @XmlAttribute
    public String getId() {
        return ""+id;
    }

    @XmlElement
    public String getName() {
        return name;
    }

    @AddLinks
    @LinkResource(rel="definition",value = AlertDefinitionRest.class)
    @XmlElementRef
    public AlertDefinitionRest getAlertDefinition() {
        return alertDefinition;
    }


    public boolean isDefinitionEnabled() {
        return definitionEnabled;
    }

    public void setDefinitionEnabled(boolean definitionEnabled) {
        this.definitionEnabled = definitionEnabled;
    }

    public String getAckBy() {
        return ackBy;
    }

    public void setAckBy(String ackBy) {
        this.ackBy = ackBy;
    }

    public long getAckTime() {
        return ackTime;
    }

    public void setAckTime(long ackTime) {
        this.ackTime = ackTime;
    }

    public long getAlertTime() {
        return alertTime;
    }

    public void setAlertTime(long alertTime) {
        this.alertTime = alertTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @AddLinks
    @LinkResource(rel="resource",value = ResourceWithType.class)
    @XmlElementRef
    ResourceWithType getResource() {
        return resource;
    }

    @XmlElementRef
    public RESTServiceDiscovery getRest() {
        return rest;
    }

    public void setRest(RESTServiceDiscovery rest) {
        this.rest = rest;
    }
}
