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
 * An extension of <code>BaseValidatorForm</code> representing the <em>Add Role Users</em> form.
 */
public class AddUsersForm extends AddNotificationsForm {
    private Integer[] availableUsers;
    private Integer[] pendingUsers;
    private Integer psa;
    private Integer psp;

    public AddUsersForm() {
        super();
    }

    public Integer[] getAvailableUser() {
        return this.availableUsers;
    }

    public Integer[] getAvailableUsers() {
        return getAvailableUser();
    }

    public void setAvailableUser(Integer[] availableUsers) {
        this.availableUsers = availableUsers;
    }

    public void setAvailableUsers(Integer[] availableUsers) {
        setAvailableUser(availableUsers);
    }

    public Integer[] getPendingUser() {
        return this.pendingUsers;
    }

    public Integer[] getPendingUsers() {
        return getPendingUser();
    }

    public void setPendingUser(Integer[] pendingUsers) {
        this.pendingUsers = pendingUsers;
    }

    public void setPendingUsers(Integer[] pendingUsers) {
        setPendingUser(pendingUsers);
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
        this.availableUsers = new Integer[0];
        this.pendingUsers = new Integer[0];
        this.psa = null;
        this.psp = null;
        this.ad = null;
        super.reset(mapping, request);
    }

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());
        s.append("ad=" + ad + " ");
        s.append("psa=" + psa + " ");
        s.append("psp=" + psp + " ");

        s.append("availableUsers={");
        listToString(s, availableUsers);
        s.append("} ");

        s.append("pendingUsers={");
        listToString(s, pendingUsers);
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