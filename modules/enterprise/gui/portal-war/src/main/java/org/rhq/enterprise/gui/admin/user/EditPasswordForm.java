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
import org.rhq.enterprise.gui.legacy.action.BaseValidatorForm;

/**
 * A form for editing passwords.
 */
public class EditPasswordForm extends BaseValidatorForm {
    //-------------------------------------instance variables

    /**
     * Holds value of property newPassword.
     */
    private String newPassword;

    /**
     * Holds value of property confirmPassword.
     */
    private String confirmPassword;

    /**
     * Holds value of property currentPassword.
     */
    private String currentPassword;

    /**
     * Holds value of property id.
     */
    private Integer id;

    //-------------------------------------constructors

    public EditPasswordForm() {
    }

    //-------------------------------------public methods

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        this.newPassword = null;
        this.confirmPassword = null;
        this.currentPassword = null;
        this.id = null;
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

    public String toString() {
        StringBuffer s = new StringBuffer();
        s.append("id= " + id + " ");
        s.append("newPassword=" + newPassword + " ");
        s.append("confirmPassword=" + confirmPassword + " ");
        s.append("currentPassword=" + currentPassword + " ");

        return super.toString() + s.toString();
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

    /**
     * Getter for property currentPassword.
     *
     * @return Value of property currentPassword.
     */
    public String getCurrentPassword() {
        return this.currentPassword;
    }

    /**
     * Setter for property currentPassword.
     *
     * @param currentPassword New value of property currentPassword.
     */
    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    /**
     * Getter for property id.
     *
     * @return Value of property id.
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * Setter for property id.
     *
     * @param id New value of property id.
     */
    public void setId(Integer id) {
        this.id = id;
    }
}