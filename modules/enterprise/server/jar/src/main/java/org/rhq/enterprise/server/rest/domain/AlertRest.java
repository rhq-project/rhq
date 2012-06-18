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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;

import org.jboss.resteasy.links.RESTServiceDiscovery;

/**
 * A single alert
 * @author Heiko W. Rupp
 */
@XmlRootElement
public class AlertRest {

    private int id;
    private String name;
    private AlertDefinitionRest alertDefinition;
    private ResourceWithType resource;
    private boolean definitionEnabled;
    String ackBy;
    long ackTime;
    long alertTime;
    String description;
    List<Link> links = new ArrayList<Link>();

    public void setResource(ResourceWithType resource) {
        this.resource = resource;
    }


    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAlertDefinition(AlertDefinitionRest alertDefinitionId) {
        this.alertDefinition = alertDefinitionId;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

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

    public ResourceWithType getResource() {
        return resource;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public void addLink(Link link) {
        links.add(link);
    }
}
