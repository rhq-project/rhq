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
package org.rhq.enterprise.gui.legacy.portlet.admin;

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.rhq.enterprise.gui.legacy.portlet.DashboardBaseForm;

// XXX: remove when ImageBeanButton works

/**
 * A subclass of <code>ValidatorForm</code> that adds convenience methods for dealing with image-based form buttons.
 */
public class PortalPropertiesForm extends DashboardBaseForm {
    /**
     * Holds value of property rightContent.
     */
    private String[] rightContent;

    /**
     * Holds value of property leftContent.
     */
    private String[] leftContent;

    /**
     * Holds value of property leftSel.
     */
    private String[] leftSel;

    /**
     * Holds value of property rightSel.
     */
    private String[] rightSel;

    //-------------------------------------instance variables

    //-------------------------------------constructors

    public PortalPropertiesForm() {
        super();
    }

    //-------------------------------------public methods

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
    }

    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        if (shouldValidate(mapping, request)) {
            return super.validate(mapping, request);
        }

        return null;
    }

    public String toString() {
        StringBuffer s = new StringBuffer();
        return s.toString();
    }

    /**
     * Getter for property rightContent.
     *
     * @return Value of property rightContent.
     */
    public String[] getRightContent() {
        return this.rightContent;
    }

    /**
     * Setter for property rightContent.
     *
     * @param rightContent New value of property rightContent.
     */
    public void setRightContent(String[] rightContent) {
        this.rightContent = rightContent;
    }

    /**
     * Getter for property leftContent.
     *
     * @return Value of property leftContent.
     */
    public String[] getLeftContent() {
        return this.leftContent;
    }

    /**
     * Setter for property leftContent.
     *
     * @param leftContent New value of property leftContent.
     */
    public void setLeftContent(String[] leftContent) {
        this.leftContent = leftContent;
    }

    /**
     * Getter for property leftSel.
     *
     * @return Value of property leftSel.
     */
    public String[] getLeftSel() {
        return this.leftSel;
    }

    /**
     * Setter for property leftSel.
     *
     * @param leftSel New value of property leftSel.
     */
    public void setLeftSel(String[] leftSel) {
        this.leftSel = leftSel;
    }

    /**
     * Getter for property rightSel.
     *
     * @return Value of property rightSel.
     */
    public String[] getRightSel() {
        return this.rightSel;
    }

    /**
     * Setter for property rightSel.
     *
     * @param rightSel New value of property rightSel.
     */
    public void setRightSel(String[] rightSel) {
        this.rightSel = rightSel;
    }
}