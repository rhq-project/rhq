/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A group definition ("Dyna Group definition")
 * @author Heiko W. Rupp
 */
@XmlRootElement(name="groupDefinition")
public class GroupDefinitionRest {

    private int id;
    private String name;
    private String description;
    private List<String>  expression;
    private long recalcInterval;
    private boolean recursive=false;
    List<Integer> generatedGroupIds;
    List<Link> links = new ArrayList<Link>();

    public GroupDefinitionRest() {
    }

    public GroupDefinitionRest(int id, String name, String description, long recalcInterval) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.recalcInterval = recalcInterval;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getExpression() {
        return expression;
    }

    public void setExpression(List<String> expression) {
        this.expression = expression;
    }

    public long getRecalcInterval() {
        return recalcInterval;
    }

    public void setRecalcInterval(long recalcInterval) {
        this.recalcInterval = recalcInterval;
    }

    public List<Integer> getGeneratedGroupIds() {
        return generatedGroupIds;
    }

    public void setGeneratedGroupIds(List<Integer> generatedGroupIds) {
        this.generatedGroupIds = generatedGroupIds;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public void addLink(Link link) {
        this.links.add(link);
    }

}
