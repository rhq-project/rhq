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

import javax.xml.bind.annotation.XmlRootElement;

import com.wordnik.swagger.annotations.ApiClass;
import com.wordnik.swagger.annotations.ApiProperty;

/**
 * A single alert
 * @author Heiko W. Rupp
 */
@ApiClass(value = "This class represents a single fired alert.")
@XmlRootElement(name = "alert")
public class AlertRest {

    private int id;
    private String name;
    private AlertDefinitionRest alertDefinition;
    private ResourceWithType resource;
    private boolean definitionEnabled;
    String ackBy;
    long ackTime;
    long alertTime;
    long recoveryTime;
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

    @ApiProperty("The id of the alert")
    public int getId() {
        return id;
    }

    @ApiProperty("Name of the alert, taken from the AlertDefinition")
    public String getName() {
        return name;
    }

    @ApiProperty("AlertDefinition from which the alert was fired")
    public AlertDefinitionRest getAlertDefinition() {
        return alertDefinition;
    }

    @ApiProperty("Is the definition enabled (=active)?")
    public boolean isDefinitionEnabled() {
        return definitionEnabled;
    }

    public void setDefinitionEnabled(boolean definitionEnabled) {
        this.definitionEnabled = definitionEnabled;
    }

    @ApiProperty("The user that acknowledged the alert (if any)")
    public String getAckBy() {
        return ackBy;
    }

    public void setAckBy(String ackBy) {
        this.ackBy = ackBy;
    }

    @ApiProperty("Timestamp of the acknowledgement")
    public long getAckTime() {
        return ackTime;
    }

    public void setAckTime(long ackTime) {
        this.ackTime = ackTime;
    }

    @ApiProperty("Timestamp when the alert has been fired")
    public long getAlertTime() {
        return alertTime;
    }

    public void setAlertTime(long alertTime) {
        this.alertTime = alertTime;
    }

    @ApiProperty("Timestamp when the alert has recovered")
    public long getRecoveryTime() {
        return recoveryTime;
    }

    public void setRecoveryTime(long recoveryTime) {
        this.recoveryTime = recoveryTime;
    }

    @ApiProperty("Description of the alert")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ApiProperty("The resource on which the alert was fired")
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
