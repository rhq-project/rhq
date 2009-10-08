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
package org.rhq.enterprise.gui.admin.role;

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionMapping;
import org.rhq.enterprise.gui.legacy.action.BaseValidatorForm;

/**
 * A subclass of <code>BaseValidatorForm</code> representing the <em>Remove Roles</em> form.
 */
public class RemoveForm extends BaseValidatorForm {
    //-------------------------------------instance variables

    private Integer[] roles;

    //-------------------------------------constructors

    public RemoveForm() {
        super();
    }

    //-------------------------------------public methods

    public Integer[] getR() {
        return this.roles;
    }

    public Integer[] getRoles() {
        return getR();
    }

    public void setR(Integer[] roles) {
        this.roles = roles;
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        this.roles = new Integer[0];
    }

    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());

        s.append("roles={");
        listToString(s, roles);
        s.append("} ");

        return s.toString();
    }

    private void listToString(StringBuffer s, Integer[] l) {
        if (l != null) {
            for (int i = 0; i < l.length; i++) {
                s.append(l[i]);
                if (i < (l.length - 1)) {
                    s.append(", ");
                }
            }
        }
    }

    /**
     * Setter for property roles.
     *
     * @param roles New value of property roles.
     */
    public void setRoles(Integer[] roles) {
        this.setR(roles);
    }
}