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
package org.rhq.enterprise.gui.legacy.portlet.addcontent;

// XXX: remove when ImageBeanButton works
import org.rhq.enterprise.gui.legacy.portlet.DashboardBaseForm;

/**
 * A subclass of <code>ValidatorForm</code> that adds convenience methods for dealing with image-based form buttons.
 */
public class PropertiesForm extends DashboardBaseForm {
    /**
     * Holds value of property wide.
     */
    private boolean wide;

    /**
     * Holds value of property portlet.
     */
    private String portlet;

    //-------------------------------------instance variables

    //-------------------------------------constructors

    public PropertiesForm() {
        super();
    }

    //-------------------------------------public methods

    public String toString() {
        StringBuffer s = new StringBuffer();
        return s.toString();
    }

    /**
     * Getter for property wide.
     *
     * @return Value of property wide.
     */
    public boolean isWide() {
        return this.wide;
    }

    /**
     * Setter for property wide.
     *
     * @param wide New value of property wide.
     */
    public void setWide(boolean wide) {
        this.wide = wide;
    }

    /**
     * Getter for property portlet.
     *
     * @return Value of property portlet.
     */
    public String getPortlet() {
        return this.portlet;
    }

    /**
     * Setter for property portlet.
     *
     * @param portlet New value of property portlet.
     */
    public void setPortlet(String portlet) {
        this.portlet = portlet;
    }
}