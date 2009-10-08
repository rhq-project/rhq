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
 * A subclass of <code>BaseValidatorForm</code> representing the <em>Remove Role Resource Groups</em> form.
 */
public class RemoveResourceGroupsForm extends BaseValidatorForm {
    //-------------------------------------instance variables

    private Integer[] groups;
    private Integer r;

    //-------------------------------------constructors

    public RemoveResourceGroupsForm() {
        super();
    }

    //-------------------------------------public methods

    public Integer[] getG() {
        return this.groups;
    }

    public Integer[] getGroups() {
        return getG();
    }

    public void setG(Integer[] groups) {
        this.groups = groups;
    }

    public Integer getPsg() {
        return getPs();
    }

    public void setPsg(Integer pageSize) {
        setPs(pageSize);
    }

    public Integer getR() {
        return this.r;
    }

    public void setR(Integer r) {
        this.r = r;
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        this.groups = new Integer[0];
        this.r = null;
    }

    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());

        s.append("r=" + r + " ");

        s.append("groups={");
        listToString(s, groups);
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