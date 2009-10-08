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
package org.rhq.enterprise.gui.legacy.portlet;

// XXX: remove when ImageBeanButton works
import org.rhq.enterprise.gui.legacy.action.BaseValidatorForm;

/**
 * A subclass of <code>ValidatorForm</code> that adds convenience methods for dealing with image-based form buttons.
 */
public class DashboardBaseForm extends BaseValidatorForm {
    //-------------------------------------instance variables
    private Integer pageSize;

    /**
     * Holds value of property displayOnDash.
     */
    private boolean displayOnDash;

    /**
     * Holds value of property portletName.
     */
    private String portletName;

    //-------------------------------------constructors

    public DashboardBaseForm() {
        super();
    }

    //-------------------------------------public methods
    public String toString() {
        StringBuffer s = new StringBuffer();

        s.append("displayOnDash=" + isDisplayOnDash() + " ");

        return s.toString();
    }

    /**
     * Getter for property displayOnDash.
     *
     * @return Value of property displayOnDash.
     */
    public boolean isDisplayOnDash() {
        return this.displayOnDash;
    }

    /**
     * Setter for property displayOnDash.
     *
     * @param displayOnDash New value of property displayOnDash.
     */
    public void setDisplayOnDash(boolean displayOnDash) {
        this.displayOnDash = displayOnDash;
    }

    /**
     * Getter for property removePortlet.
     *
     * @return Value of property removePortlet.
     */
    public String getPortletName() {
        return this.portletName;
    }

    /**
     * Setter for property removePortlet.
     *
     * @param removePortlet New value of property removePortlet.
     */
    public void setPortletName(String portletName) {
        this.portletName = portletName;
    }
}