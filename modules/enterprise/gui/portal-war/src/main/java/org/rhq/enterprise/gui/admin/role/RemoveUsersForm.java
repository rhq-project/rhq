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
 * A subclass of <code>BaseValidatorForm</code> representing the <em>Remove Role Users</em> form.
 */
public class RemoveUsersForm extends BaseValidatorForm {
    //-------------------------------------instance variables

    private Integer r;
    private Integer[] users;

    //-------------------------------------constructors

    public RemoveUsersForm() {
        super();
    }

    //-------------------------------------public methods

    public Integer getPsu() {
        return getPs();
    }

    public void setPsu(Integer pageSize) {
        setPs(pageSize);
    }

    public Integer getR() {
        return this.r;
    }

    public void setR(Integer r) {
        this.r = r;
    }

    public Integer[] getU() {
        return this.users;
    }

    public Integer[] getUsers() {
        return getU();
    }

    public void setU(Integer[] users) {
        this.users = users;
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        this.r = null;
        this.users = new Integer[0];
    }

    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());

        s.append("r=" + r + " ");

        s.append("users={");
        listToString(s, users);
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
}