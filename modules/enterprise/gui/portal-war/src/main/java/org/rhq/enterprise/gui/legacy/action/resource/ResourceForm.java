/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.legacy.action.resource;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.gui.legacy.action.ScheduleForm;

/**
 * A subclass of <code>BaseValidatorForm</code> that adds convenience methods for dealing with appdef resource objects
 * like Platform Server, & Service.
 */
public class ResourceForm extends ScheduleForm {
    //-------------------------------------instance variables
    private String name;
    private String description;
    private String location;
    @Deprecated
    private Integer rid;
    private Integer id;
    private Integer type;
    private Integer resourceType;
    private List resourceTypes;

    //-------------------------------------constructors

    //-------------------------------------public methods

    /**
     * Returns the name.
     *
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * sets the name.
     *
     * @return String
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the description.
     *
     * @return String
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     *
     * @param description The description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the location.
     *
     * @return Integer
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the location.
     *
     * @param location The location to set
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Returns the resourceTypes.
     *
     * @return List
     */
    public List getResourceTypes() {
        return resourceTypes;
    }

    /**
     * Sets the resourceTypes.
     *
     * @param resourceTypes The resourceTypes to set
     */
    public void setResourceTypes(List resourceTypes) {
        this.resourceTypes = resourceTypes;
    }

    /**
     * Returns the resourceType.
     *
     * @return Integer
     */
    public Integer getResourceType() {
        return resourceType;
    }

    /**
     * Sets the resourceType.
     *
     * @param resourceType The resourceType to set
     */
    public void setResourceType(Integer resourceType) {
        this.resourceType = resourceType;
    }

    /**
     * Returns the rid.
     *
     * @return String
     */
    public Integer getRid() {
        return rid;
    }

    /**
     * Sets the rid.
     *
     * @param rid The rid to set
     */
    public void setRid(Integer rid) {
        this.rid = rid;
    }

    /**
     * loads the server value
     */
    public void loadResourceValue(Resource resource) {
        this.name = resource.getName();
        this.description = resource.getDescription();
        this.location = resource.getLocation();
        this.rid = resource.getId();
        this.resourceType = resource.getResourceType().getId();
    }

    /**
     * loads the server value
     */
    public void updateResourceValue(Resource resource) {
        if (name != null) {
            resource.setName(name);
        }

        if (description != null) {
            resource.setDescription(description);
        }

        if (location != null) {
            resource.setLocation(location);
        }
    }

    /**
     */
    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        name = null;
        description = null;
        location = null;
        rid = null;
        resourceType = null;
        resourceTypes = null;
    }

    @Override
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        ActionErrors errors = super.validate(mapping, request);

        if ((errors == null) || errors.isEmpty()) {
            return null;
        }

        return errors;
    }

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());

        s.append(" ");
        s.append("rid=" + rid + " ");
        s.append("type=" + resourceType + " ");
        s.append("name=" + name + " ");
        s.append("location=" + location + " ");
        s.append("description=" + description + " ");

        return s.toString();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }
}