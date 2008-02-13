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

public class ChangeOwnerForm extends BaseValidatorForm {
    //-------------------------------------instance variables

    private Integer owner;
    private Integer r;

    //-------------------------------------constructors

    public ChangeOwnerForm() {
        super();
    }

    //-------------------------------------public methods

    public Integer getR() {
        return this.r;
    }

    public void setR(Integer r) {
        this.r = r;
    }

    public Integer getO() {
        return this.owner;
    }

    public Integer getOwner() {
        return getO();
    }

    public void setO(Integer owner) {
        this.owner = owner;
    }

    public void setOwner(Integer owner) {
        setO(owner);
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        this.r = null;
        this.owner = null;
    }

    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());
        s.append("r=" + r + " ");
        s.append("owner=" + owner);
        return s.toString();
    }
}