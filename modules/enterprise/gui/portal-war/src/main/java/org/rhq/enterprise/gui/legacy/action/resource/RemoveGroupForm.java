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

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionMapping;
import org.rhq.enterprise.gui.legacy.action.BaseValidatorForm;

/**
 * removes a list of groups from a resource
 */
public class RemoveGroupForm extends BaseValidatorForm {
    //-------------------------------------instance variables

    private Integer[] g;
    private Integer rid;
    private Integer type;

    //-------------------------------------constructors

    public RemoveGroupForm() {
        super();
    }

    //-------------------------------------public methods

    /**
     * Getter for property g.
     *
     * @return Value of property g.
     */
    public Integer[] getG() {
        return this.g;
    }

    /**
     * Setter for property g.
     *
     * @param groups New value of property g.
     */
    public void setG(Integer[] groups) {
        this.g = groups;
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

    /**
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        g = null;
    }

    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());

        s.append(" rid=").append(rid);
        s.append(" type=").append(type);
        s.append(" groups=").append(g);

        return s.toString();
    }
}