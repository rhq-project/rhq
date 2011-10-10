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

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A lightweight resource with a list of children
 * @author Heiko W. Rupp
 */
@XmlRootElement
public class ResourceWithChildren {

    String id;
    String name;
    List<ResourceWithChildren> children;

    public ResourceWithChildren() {
    }

    public ResourceWithChildren(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @XmlElement
    public String getId() {
        return id;
    }

    @XmlElement
    public String getName() {
        return name;
    }

    @XmlElement
    public List<ResourceWithChildren> getChildren() {
        return children;
    }

    public void setChildren(List<ResourceWithChildren> children) {
        this.children = children;
    }


}
