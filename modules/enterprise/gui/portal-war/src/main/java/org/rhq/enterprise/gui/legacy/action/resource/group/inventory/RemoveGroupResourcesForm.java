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
/*
 * Created on Feb 14, 2003
 *
 */
package org.rhq.enterprise.gui.legacy.action.resource.group.inventory;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionMapping;
import org.rhq.enterprise.gui.legacy.action.BaseValidatorForm;

/**
 * removes resources specified with identifier in the string form:
 */
public class RemoveGroupResourcesForm extends BaseValidatorForm {
    private static final long serialVersionUID = -2373276520075913194L;

    private String[] resources;
    protected Integer resourceType;
    protected List resourceTypes;
    private Integer groupId;
    private String category;

    public RemoveGroupResourcesForm() {
        super();
    }

    public Integer getF() {
        return getResourceType();
    }

    public void setF(Integer f) {
        setResourceType(f);
    }

    public String[] getR() {
        return getResources();
    }

    public void setR(String[] r) {
        setResources(r);
    }

    /**
     * Getter for property users.
     *
     * @return Value of property users.
     */
    public String[] getResources() {
        return resources;
    }

    /**
     * Setter for property users.
     *
     * @param users New value of property users.
     */
    public void setResources(String[] resources) {
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
    public Integer getGroupId() {
        return groupId;
    }

    /**
     * Sets the rid.
     *
     * @param rid The rid to set
     */
    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    /**
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        resources = new String[0];
        resourceType = null;
        resourceTypes = null;
        groupId = null;
        category = null;
    }

    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());

        s.append(" groupId=");
        s.append(groupId);
        s.append(" resources=");
        s.append(resources);
        s.append(" resourceType=");
        s.append(resourceType);
        s.append(" resourceTypes=");
        s.append(resourceTypes);

        return s.toString();
    }

    /**
     * @return String
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the GroupCategory as a string.
     *
     * @param category The GroupCategory object as a string
     */
    public void setCategory(String category) {
        this.category = category;
    }
}