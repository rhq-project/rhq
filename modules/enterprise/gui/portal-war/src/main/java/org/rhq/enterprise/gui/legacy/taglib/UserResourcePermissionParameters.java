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
package org.rhq.enterprise.gui.legacy.taglib;

import javax.servlet.jsp.tagext.TagSupport;
import org.rhq.enterprise.gui.legacy.Constants;

/**
 * generate pagination info for a spicified list.
 */
public class UserResourcePermissionParameters extends TagSupport {
    //----------------------------------------------------instance variables

    /**
     * Holds value of property pageAtServer.
     */
    private boolean pageAtServer = true;

    private String order = Constants.SORTORDER_DEFAULT;

    private String path = "";

    /**
     * Resource id for the resource being viewed
     */
    private int resourceId;

    /**
     * Holds value of property debug.
     */
    private String debug;

    //----------------------------------------------------constructors

    public UserResourcePermissionParameters() {
        super();
    }

    //----------------------------------------------------public methods

    /**
     * Release tag state.
     */
    public void release() {
        super.release();
    }

    /**
     * Getter for property resource.
     *
     * @return Value of property resource.
     */
    public int getResource() {
        return this.resourceId;
    }

    /**
     * Setter for property resource.
     *
     * @param resource New value of property resource.
     */
    public void setResource(int resource) {
        this.resourceId = resource;
    }

    /**
     * Getter for property debug.
     *
     * @return Value of property debug.
     */
    public String getDebug() {
        return this.debug;
    }

    /**
     * Setter for property debug.
     *
     * @param debug New value of property debug.
     */
    public void setDebug(String debug) {
        this.debug = debug;
    }
}