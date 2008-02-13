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
package org.rhq.enterprise.gui.admin.user;

import org.rhq.enterprise.gui.legacy.action.BaseValidatorForm;

/**
 * A subclass of <code>BaseValidatorForm</code> representing the <em>Remove Roles</em> form.
 */
public class RemoveRolesForm extends BaseValidatorForm {
    //-------------------------------------instance variables
    private Integer u;

    /**
     * Holds value of property roles.
     */
    private Integer[] roles;

    /**
     * Holds value of property u.
     */
    private Integer i;

    //-------------------------------------constructors

    public RemoveRolesForm() {
        super();
    }

    /**
     * Getter for property roles.
     *
     * @return Value of property roles.
     */
    public Integer[] getRoles() {
        return this.roles;
    }

    /**
     * Setter for property roles.
     *
     * @param roles New value of property roles.
     */
    public void setRoles(Integer[] roles) {
        this.roles = roles;
    }

    /**
     * Getter for property i.
     *
     * @return Value of property i.
     */
    public Integer getU() {
        return this.i;
    }

    /**
     * Setter for property i.
     *
     * @param i New value of property i.
     */
    public void setU(Integer i) {
        this.i = i;
    }

    /**
     * Setter for property i.
     *
     * @param i New value of property i.
     */
    public void reset() {
        this.roles = new Integer[0];
    }

    //-------------------------------------public methods
}