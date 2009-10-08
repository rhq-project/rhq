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

import javax.servlet.jsp.JspException;
import org.rhq.enterprise.gui.legacy.Constants;

/**
 * Set the scoped variable as the value of <code>Constants.ADDTOLIST_SIZE_DEFAULT</code>.
 */
public class AddToListSizeTag extends VarSetterBaseTag {
    //----------------------------------------------------instance variables

    public AddToListSizeTag() {
        super();
    }

    //----------------------------------------------------public methods

    /**
     * Process the tag, setting the value of the scoped variable as the size of the Add To List widget.
     *
     * @exception JspException if there is an error processing the tag
     */
    public final int doStartTag() throws JspException {
        setScopedVariable(Constants.ADDTOLIST_SIZE_DEFAULT);
        return SKIP_BODY;
    }
}