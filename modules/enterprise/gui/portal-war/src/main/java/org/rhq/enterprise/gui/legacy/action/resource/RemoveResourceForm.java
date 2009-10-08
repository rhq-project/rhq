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
import org.apache.struts.action.ActionMapping;
import org.rhq.enterprise.gui.legacy.action.BaseValidatorForm;

/**
 * removes a list of resources
 */
public class RemoveResourceForm extends BaseValidatorForm {
    //-------------------------------------instance variables

    private Integer[] resources;
    protected Integer resourceType;
    protected List resourceTypes;
    private Integer rid;
    private Integer type;

    //-------------------------------------constructors

    public RemoveResourceForm() {
        super();
    }

    //-------------------------------------public methods

    public Integer getF() {
        return getResourceType();
    }

    public void setF(Integer f) {
        setResourceType(f);
    }

    public Integer[] getR() {
        return getResources();
    }

    public void setR(Integer[] r) {
        setResources(r);
    }

    /**
     * Getter for property users.
     *
     * @return Value of property users.
     */
    public Integer[] getResources() {
        return resources;
    }

    /**
     * Setter for property users.
     *
     * @param users New value of property users.
     */
    public void setResources(Integer[] resources) {
        this.resources = resources;
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
     * Returns the type.
     *
     * @return String
     */
    public Integer getType() {
        return type;
    }

    /**
     * Sets the type.
     *
     * @param type The type to set
     */
    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getPss() {
        return getPs();
    }

    public void setPss(Integer pageSize) {
        setPs(pageSize);
    }

    /**
     */
    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        resources = new Integer[0];
        resourceType = null;
        resourceTypes = null;
        rid = null;
    }

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());

        s.append(" rid=");
        s.append(rid);
        s.append(" type=");
        s.append(type);
        s.append(" resources=");
        s.append(resources);
        s.append(" resourceType=");
        s.append(resourceType);
        s.append(" resourceTypes=");
        s.append(resourceTypes);

        return s.toString();
    }
}