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
 * A subclass of <code>ActionForm</code> representing the <em>AuthzSubjectValue</em>s to remove.
 */
public class RemoveForm extends BaseValidatorForm {
    //-------------------------------------instance variables
    private Integer[] users;

    //-------------------------------------constructors

    public RemoveForm() {
    }

    //-------------------------------------public methods

    public String toString() {
        if (users == null) {
            return "";
        } else {
            return users.toString();
        }
    }

    /**
     * Getter for property users.
     *
     * @return Value of property users.
     */
    public Integer[] getUsers() {
        return this.users;
    }

    /**
     * Setter for property users.
     *
     * @param users New value of property users.
     */
    public void setUsers(Integer[] users) {
        this.users = users;
    }
}