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
import org.apache.struts.action.ActionMessage;
import org.rhq.enterprise.gui.legacy.action.BaseValidatorForm;

/**
 * A subclass of <code>EditForm</code> representing the <em>EditUserProperties</em> form.
 */
public class EditForm extends BaseValidatorForm {
    //-------------------------------------instance variables
    private Integer id;

    /**
     * Holds value of property lastName.
     */
    private String lastName;

    /**
     * Holds value of property firstName.
     */
    private String firstName;

    /**
     * Holds value of property department.
     */
    private String department;

    /**
     * Holds value of property name.
     */
    private String name;

    /**
     * Holds value of property emailAddress.
     */
    private String emailAddress;

    private String smsAddress;

    /**
     * Holds value of property phoneNumber.
     */
    private String phoneNumber;

    /**
     * Holds value of property enableLogin.
     */
    private String enableLogin;

    /**
     * Period to use for refreshing dashboard/charts
     */
    private String pageRefreshPeriod;

    /**
     * Are we editing the currently logged in user
     */
    private boolean editingCurrentUser;

    //-------------------------------------constructors

    public EditForm() {
    }

    //-------------------------------------public methods

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * Getter for property lastName.
     *
     * @return Value of property lastName.
     */
    public String getLastName() {
        return this.lastName;
    }

    /**
     * Setter for property lastName.
     *
     * @param lastName New value of property lastName.
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Getter for property firstName.
     *
     * @return Value of property firstName.
     */
    public String getFirstName() {
        return this.firstName;
    }

    /**
     * Setter for property firstName.
     *
     * @param firstName New value of property firstName.
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Getter for property department.
     *
     * @return Value of property department.
     */
    public String getDepartment() {
        return this.department;
    }

    /**
     * Setter for property department.
     *
     * @param department New value of property department.
     */
    public void setDepartment(String department) {
        this.department = department;
    }

    /**
     * Getter for property userName.
     *
     * @return Value of property userName.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Setter for property userName.
     *
     * @param userName New value of property userName.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Getter for property email.
     *
     * @return Value of property email.
     */
    public String getEmailAddress() {
        return this.emailAddress;
    }

    /**
     * Setter for property email.
     *
     * @param email New value of property email.
     */
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    /**
     * Getter for property phoneNumber.
     *
     * @return Value of property phoneNumber.
     */
    public String getPhoneNumber() {
        return this.phoneNumber;
    }

    /**
     * Setter for property phoneNumber.
     *
     * @param phoneNumber New value of property phoneNumber.
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * Getter for property enableLogin.
     *
     * @return Value of property enableLogin.
     */
    public String getEnableLogin() {
        return this.enableLogin;
    }

    /**
     * Setter for property enableLogin.
     *
     * @param confirmPassword New value of property enableLogin.
     */
    public void setEnableLogin(String enableLogin) {
        this.enableLogin = enableLogin;
    }

    public String getSmsAddress() {
        return this.smsAddress;
    }

    public void setSmsAddress(String add) {
        this.smsAddress = add;
    }

    //-------- form methods-------------------------

    // for validation, please see web/WEB-INF/validation/validation.xml

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer();
        s.append("id=" + id + " ");
        s.append("name=" + name + " firstName=" + firstName + " ");
        s.append("lastName=" + lastName + " emailAddress=" + emailAddress + " ");
        s.append("phoneNumber=" + phoneNumber + " ");
        s.append("department=" + department + " ");
        s.append("enableLogin=" + enableLogin + " ");
        s.append("smsAddress=" + smsAddress + " ");
        s.append("pageRefreshPeriod=" + pageRefreshPeriod + " ");
        return s.toString();
    }

    /**
     * Get the pageRefreshPeriod.
     *
     * @return the pageRefreshPeriod.
     */
    public String getPageRefreshPeriod() {
        return pageRefreshPeriod;
    }

    /**
     * Set the pageRefreshPeriod.
     *
     * @param pageRefreshPeriod The pageRefreshPeriod to set.
     */
    public void setPageRefreshPeriod(String pageRefreshPeriod) {
        this.pageRefreshPeriod = pageRefreshPeriod;
    }

    /**
     * Get the editingCurrentUser.
     *
     * @return the editingCurrentUser.
     */
    public boolean isEditingCurrentUser() {
        return editingCurrentUser;
    }

    /**
     * Set the editingCurrentUser.
     *
     * @param editingCurrentUser The editingCurrentUser to set.
     */
    public void setEditingCurrentUser(boolean editingCurrentUser) {
        this.editingCurrentUser = editingCurrentUser;
    }

    /**
     * Validate the preferences associated with the current user if they are editing them.
     */
    @Override
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        ActionErrors errors = super.validate(mapping, request);
        if (shouldValidateMyPreferences(mapping, request)) {
            Integer refreshPeriod = new Integer(0);
            ActionMessage errorMessage = null;
            try {
                refreshPeriod = Integer.valueOf(getPageRefreshPeriod());
            } catch (NumberFormatException e) {
                errorMessage = new ActionMessage("admin.user.error.mypreferences.pageRefreshPeriod");
            }

            if (errorMessage == null) {
                if ((refreshPeriod.intValue() < 0) || (refreshPeriod.intValue() > 86400)) {
                    errorMessage = new ActionMessage("admin.user.error.mypreferences.pageRefreshPeriod.range");
                }
            }

            if (errorMessage != null) {
                if (errors == null) {
                    errors = new ActionErrors();
                }

                errors.add("pageRefreshPeriod", errorMessage);
            }
        }

        return errors;
    }

    private boolean shouldValidateMyPreferences(ActionMapping mapping, HttpServletRequest request) {
        return (isEditingCurrentUser() && super.shouldValidate(mapping, request));
    }

    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
    }
}