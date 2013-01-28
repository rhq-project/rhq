/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.server.rest.domain;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import com.wordnik.swagger.annotations.ApiClass;
import com.wordnik.swagger.annotations.ApiProperty;

/**
 * An Alert notification sender
 * @author Heiko W. Rupp
 */
@ApiClass
@XmlRootElement
public class AlertSender {

    String senderName;
    String description;
    Link link;
    private Map<String, String> configDefinition = new HashMap<String, String>();

    public AlertSender() {
    }

    public AlertSender(String senderName) {
        this.senderName = senderName;
    }

    @ApiProperty("Name of the alert sender - this is also its unique identifier")
    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    @ApiProperty("A description of this sender")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Link getLink() {
        return link;
    }

    public void setLink(Link link) {
        this.link = link;
    }

    @ApiProperty("The configuration definition of the sender")
    public Map<String, String> getConfigDefinition() {
        return configDefinition;
    }

    public void setConfigDefinition(Map<String, String> configDefinition) {
        this.configDefinition = configDefinition;
    }
}
