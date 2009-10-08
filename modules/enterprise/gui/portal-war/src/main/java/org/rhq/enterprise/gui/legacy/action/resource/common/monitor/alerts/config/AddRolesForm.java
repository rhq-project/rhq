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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.config;

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionMapping;

/**
 * An extension of <code>BaseValidatorForm</code> representing the <em>Add Role</em> form.
 */
public class AddRolesForm extends AddNotificationsForm {
    private Integer[] availableRoles;
    private Integer[] pendingRoles;
    private Integer psa;
    private Integer psp;

    public AddRolesForm() {
        super();
    }

    public Integer[] getAvailableRole() {
        return this.availableRoles;
    }

    public Integer[] getAvailableRoles() {
        return getAvailableRole();
    }

    public void setAvailableRole(Integer[] availableRoles) {
        this.availableRoles = availableRoles;
    }

    public void setAvailableRoles(Integer[] availableRoles) {
        setAvailableRole(availableRoles);
    }

    public Integer[] getPendingRole() {
        return this.pendingRoles;
    }

    public Integer[] getPendingRoles() {
        return getPendingRole();
    }

    public void setPendingRole(Integer[] pendingRoles) {
        this.pendingRoles = pendingRoles;
    }

    public void setPendingRoles(Integer[] pendingRoles) {
        setPendingRole(pendingRoles);
    }

    public Integer getPsa() {
        return this.psa;
    }

    public void setPsa(Integer ps) {
        this.psa = ps;
    }

    public Integer getPsp() {
        return this.psp;
    }

    public void setPsp(Integer ps) {
        this.psp = ps;
    }

    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        this.availableRoles = new Integer[0];
        this.pendingRoles = new Integer[0];
        this.psa = null;
        this.psp = null;
        super.reset(mapping, request);
    }

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());
        s.append("ad=" + ad + " ");
        s.append("psa=" + psa + " ");
        s.append("psp=" + psp + " ");

        s.append("availableRoles={");
        listToString(s, availableRoles);
        s.append("} ");

        s.append("pendingRoles={");
        listToString(s, pendingRoles);
        s.append("}");

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