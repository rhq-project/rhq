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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionMapping;
import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.gui.legacy.action.BaseValidatorForm;

/**
 * A subclass of <code>ActionForm</code> representing the <em>RolePropertiesForm</em> form.
 */
public final class RoleForm extends BaseValidatorForm {
    //-------------------------------------instance variables

    private String description;
    private String name;
    private String[] permissions = new String[0]; // the Permission .toString()
    private Integer r;

    //-------------------------------------constructors

    public RoleForm() {
        super();
    }

    //-------------------------------------public methods

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getP() {
        return this.permissions;
    }

    public List<String> getPermissionsStrings() {
        return Arrays.asList(this.permissions);
    }

    public List<Permission> getPermissions() {
        ArrayList<Permission> list = new ArrayList<Permission>();
        for (String p : this.permissions) {
            list.add(Enum.valueOf(Permission.class, p));
        }

        return list;
    }

    public void setP(String[] permissions) {
        if (permissions == null) {
            this.permissions = new String[0];
        } else {
            this.permissions = permissions;
        }
    }

    public void setPermissions(List<Permission> permissions) {
        if (permissions == null) {
            setP(null);
        } else {
            ArrayList<String> list = new ArrayList<String>(permissions.size());
            for (Permission p : permissions) {
                list.add(p.name());
            }

            setP(list.toArray(new String[list.size()]));
        }
    }

    public Integer getR() {
        return this.r;
    }

    public void setR(Integer r) {
        this.r = r;
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        this.description = null;
        this.name = null;
        this.permissions = new String[0];
        this.r = null;
    }

    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());
        s.append(" r=");
        s.append(r);
        s.append(" name=");
        s.append(name);
        s.append(" description=");
        s.append(description);
        s.append(" permissions={");
        s.append(getPermissions());
        s.append("} ");

        return s.toString();
    }
}