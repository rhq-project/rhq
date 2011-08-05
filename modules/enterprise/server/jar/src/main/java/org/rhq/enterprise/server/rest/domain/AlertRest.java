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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.jboss.resteasy.links.LinkResource;

/**
 * A single alert
 * @author Heiko W. Rupp
 */
@XmlRootElement
@XmlType(propOrder = {"id","name","alertTime","description","alertDefinitionId","definitionEnabled","resourceId","resourceName","ackBy","ackTime"})
public class AlertRest {

    private int id;
    private String name;
    private int alertDefinitionId;
    private int resourceId;
    private String resourceName;
    private boolean definitionEnabled;
    String ackBy;
    long ackTime;
    long alertTime;
    String description;

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAlertDefinitionId(int alertDefinitionId) {
        this.alertDefinitionId = alertDefinitionId;
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

    @XmlElement
    @LinkResource(rel="definition",value = AlertDefinitionRest.class, pathParameters = "id")
    public int getAlertDefinitionId() {
        return alertDefinitionId;
    }

    @XmlElement
    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    @XmlElement
    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
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
}
