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

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;

/**
 * A subclass of <code>EditForm</code> representing the <em>New User</em> form. This has the additional properties for
 * password.
 */
public class NewForm extends EditForm {
    //-------------------------------------instance variables

    /**
     * Holds value of property newPassword.
     */
    private String newPassword;

    /**
     * Holds value of property confirmPassword.
     */
    private String confirmPassword;

    //-------------------------------------constructors

    public NewForm() {
    }

    //-------------------------------------public methods

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        this.newPassword = null;
        this.confirmPassword = null;
    }

    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        ActionErrors errors = super.validate(mapping, request);
        if (errors == null) {
            errors = new ActionErrors();
        }

        if (errors.isEmpty()) {
            return null;
        }

        return errors;
    }

    /**
     * Getter for property newPassword.
     *
     * @return Value of property newPassword.
     */
    public String getNewPassword() {
        return this.newPassword;
    }

    /**
     * Setter for property newPassword.
     *
     * @param newPassword New value of property newPassword.
     */
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    /**
     * Getter for property confirmPassword.
     *
     * @return Value of property confirmPassword.
     */
    public String getConfirmPassword() {
        return this.confirmPassword;
    }

    /**
     * Setter for property confirmPassword.
     *
     * @param confirmPassword New value of property confirmPassword.
     */
    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}